

	//    >>>>    START     -----------------------------    VBB Config    -----------------------------

		import muvium.compatibility.arduino.EEPROM;
		import muvium.compatibility.arduino.Servo; //<include <Servo.h>
		import muvium.compatibility.arduino.LiquidCrystal; //<include <LiquidCrystal.h>
		import muvium.compatibility.arduino.Serial;
		import muvium.compatibility.arduino.Arduino;

class BareBones extends Arduino //Automatically Added VBB Framework Code - do not remove
{

	//Define Corresponding Head and Tail

	LiquidCrystal lcd = new LiquidCrystal(this, 12, 13, 5, 4, 3, 2);

	static final int [] GetTrackTail = { 4, 5, 6, 1, 2, 3 };	// Array for Tail Tracks
	int [] PositionTrack  = { 0, 710, 800, 890, 2310, 2400, 2490 };
	static final int [] PositionTrackDummy = { 0, 710, 800, 890, 2310, 2400, 2490 };
	int storeTargetTrack = 0;							// Store Target Track position
	int storeStartTrack = 0;
	String str1, str2, str3, str4, str5, str6;			// used as String builders within voids

	//	Calibration Array

	int sensorVal = digitalRead(3);					// enable Hall-effect Sensor on Pin 3
	int [] arrayCalibrate = { 0, 0, 0, 0, 0 };		// Array to pass in calibration run results
	boolean isTrackCalibration = false;

	// Programme Track Positions

	int currentStepPosition = 0;	// current step number
	int [] storeProgTracks = { 0, 0, 0, 0, 0, 0 };
	boolean chkOverwrite = false;

	// Programming LCD Keys variables

	boolean programmingMode = false;
	boolean programmingModeMoveForward = true;			//If in programming mode, are we moving forward?
	//long programmingLastMoveMillis = 0;				//When was the last programming move done?
	//static final int programRotateDelay = 100;		//Delay between steps in ms while holding button

	// Debug Variables
	boolean	isDebugMode = true;		// Set debug to console default is off in INO

	// KeyPad parameters
	int lcd_key = 0;			// current LCD key
	int adc_key_in = 0;			// read key press from analogue(0)
	int adc_key_prev = 0;		// previous key press
	int key = -1;				// default key
	int lastKey = 0;			// Saves key press
	int readAnalog = 0;			// Read value from Analogue Pin 1
	boolean newMenu = true;		// Is new menu
	int runs = 0;				// Set Calibration runs
	int lastRun = 0;			// Stores no of runs

	//States for the main menu
	int checkMenu = 0;				// Default menu value
	int currentMenuItem = 0;		// Current selected menu
	boolean	stayInMenu = true;

	// Parameters for Stepper
	boolean isReleased = false;				// isReleased tries to make sure the motor is not continuously released
	long stepperLastMoveTime = 0;
	int mainDiff = 0;
	static final int MOTOR_OVERSHOOT = 10;			// the amount of overshoot/ lash correction when approaching from CCW
	int overshootDestination = -1;
	static final int releaseTimeout_ms = 2000;		//reduced to 2 seconds for now
	static final int  MOTOR_STEP_COUNT = 200 * 16;	//number of steps for a full rotation

	// Parameters for turntable move

	boolean tableTargetHead = false;		//
	int tableTargetPosition = 0;
	int tableTargetTrack = 0;
	boolean newTargetLocation = false;
	boolean inMotionToNewTarget = false;
	boolean isTurntableHead = true;
	boolean isTurntableHeadChanged = true;
	int currentTrack = 1;
	int newTrack = 1;
	static final int  noTrack = 5;
	int	distanceToGo = 0;

	//Do display rotation
	static final int displayRotateDelay = 5;		// This is the minimum delay in ms between steps of the stepper motor
	boolean displayRotating = false;		// Is the "Display Rotate" function enabled?
	boolean displayRotatingCW = true;		// In Display Rotate mode, are we rot

	// Helper declarations
	boolean isYesNo = false;
	String overwriteHeader;

	int forwardstep2() 	{return dummyStepper(1,  25); } //75 real
	int backwardstep2() { return dummyStepper(-1, 25);  } //75 real


	//    >>>>    START     ---------------------------    Arduino Setup     ---------------------------

	void setup ()
	{
	//	Serial.begin(9600);		// Start Console logging
		Serial.begin(57600);
		lcd.begin(16, 2);		// set up the LCD's number of columns and rows:
		newMenu = true;
		startupLCD();
	}

	//    <<<<    FINISH    ---------------------------    Arduino Setup     ---------------------------

	//    <<<<    FINISH    -----------------------------    VBB Config    -----------------------------

/*

	//    >>>>    START     -----------------------------    UNO Config    -----------------------------

    // Included Libraries
		//#include <SoftwareSerial.h>
		#include "Arduino.h"
		#include <DCC_Decoder\DCC_Decoder.h>
		#include <AccelStepper.h>
		#include <Wire.h>
		#include <EEPROM.h>
		#include <Adafruit_MotorShield.h>
		 // <Adafruit_PWMServoDriver.h>
//	#include <Servo.h>	//servo library reference
		#include <LiquidCrystal.h>

	// Definitions

  	#define kDCC_INTERRUPT	0										// Define DCC commands to Arduino
  	typedef struct	{ int address; } DCCAccessoryAddress;	// Address to respond to
  	DCCAccessoryAddress gAddresses[7];							// Allows 7 DCC addresses: [XX] = number of addresses you need (including 0).

		LiquidCrystal lcd (8, 9, 4, 5, 6, 7);		// initialize the library with the numbers of the interface pins

		// Track Step Definitions

  	//Define Corresponding Head and Tail	 UNO
  	const int GetTrackTail [] = { 4, 5, 6, 1, 2, 3 };	// Array for Tail Tracks
	int [] PositionTrack = { 0, 0, 0, 0, 0, 0, 0 };		// Save EEPROM addresses to array - index 0 = calibration position
	//static final int [] PositionTrackDummy = { 0, 710, 800, 890, 2310, 2400, 2490 };
  	int storeTargetTrack = 0;							// Store Target Track position
  	int storeStartTrack = 0;
  	String str1, str2, str3, str4, str5, str6;
  	// used as String builders within voids
  	//	Calibration Array
  	int sensorVal = digitalRead (3);					// enable Hall-effect Sensor on Pin 3
  	int arrayCalibrate []= { 0, 0, 0, 0, 0 };		// Array to pass in calibration run results
  	boolean isTrackCalibration = false;

  	// Programme Track Positions
  	int currentStepPosition = 0;	// current step number
  	int storeProgTracks [] = { 0, 0, 0, 0, 0, 0 };
  	boolean chkOverwrite = false;

  	// Define custom characters
  	byte upArrow[8] = { B00000, B00100, B01110, B10101, B00100, B00100, B00100, B00000 };	 		// Up Arrow character
  	byte downArrow[8] = { B00000, B00100, B00100, B00100, B10101, B01110, B00100, B00000 };	 		// Down Arrow character
  	byte rightArrow[8] = { B00000, B00000, B00100, B00010, B11111, B00010, B00100, B00000 };		// Right Arrow character
  	byte leftArrow[8] = { B00000, B00000, B00100, B01000, B11111, B01000, B00100, B00000 };			// Left Arrow character
  	byte upArrowScroll[8] = { B11111, B11011, B10001, B01010, B11011, B11011, B11011, B11111 };		// Up Arrow character
  	byte downArrowScroll[8] = { B11111, B11011, B11011, B11011, B01010, B10001, B11011, B11111 };	// Down Arrow character
  	byte bothArrows[8] = { B00100, B01110, B10101, B00100, B00100, B10101, B01110, B00100 };		// Up & Down Arrow character
  	byte degreeMoved[8] = { B00110, B01001, B01001, B00110, B00000, B00000, B00000, B00000 };		// Degrees moved
  	byte reverseY[8] = { B01110, B01110, B01110, B10101, B11011, B11011, B11011, B11011 };			// Reverse Y char
  	byte reverseN[8] = { B01110, B01110, B01110, B00110, B01010, B01100, B01110, B01110 };			// Reverse N char


  	// Programming LCD Keys variables
  	boolean programmingMode = false;
  	boolean programmingModeMoveForward = true;			//If in programming mode, are we moving forward?
  	//long programmingLastMoveMillis = 0;				//When was the last programming move done?
  	//static final int programRotateDelay = 100;				//Delay between steps in ms while holding button

  	// KeyPad parameters
  	int lcd_key = 0;			// current LCD key
  	int adc_key_in = 0;			// read key press from analogue(0)
  	int adc_key_prev = 0;		// previous key press
  	int key = -1;				// default key
  	int lastKey = 0;			// Saves key press
  	int readAnalog = 0;			// Read value from Analogue Pin 1
  	boolean newMenu = true;		// Is new menu
  	int runs = 0;				// Set Calibration runs
  	int lastRun = 0;			// Stores no of runs

	// Debug Variables
	boolean	isDebugMode = false;		// Set debug to console default is off

	//States for the main menu
	int checkMenu = 0;				// Default menu value
	int currentMenuItem = 0;	// Current selected menu

	// Parameters for Stepper
	boolean isReleased = false;				// isReleased tries to make sure the motor is not continuously released
	long stepperLastMoveTime = 0;
	int mainDiff = 0;
	const int MOTOR_OVERSHOOT = 10;			// the amount of overshoot/ lash correction when approaching from CCW
	int overshootDestination = -1;
	const int releaseTimeout_ms = 2000;		//reduced to 2 seconds for now
	const int  MOTOR_STEP_COUNT = 200 * 16;	//number of steps for a full rotation

	// Parameters for turntable move

	boolean tableTargetHead = false;		//
	int tableTargetTrack = 0;
	int tableTargetPosition = 0;
	boolean newTargetLocation = false;
	boolean inMotionToNewTarget = false;
	boolean isTurntableHead = true;
	boolean isTurntableHeadChanged = true;
	int currentTrack = 1;
	int newTrack = 1;
	//const int  noTrack = 5;
	int	distanceToGo = 0;


	//Servo Stuff
	//	Servo brakeservo;				// create servo object to control a servo
	//	static final int servoBrake = 9;		// value for brake position
	//	static final int servoRelease = 2;		// value for release position

	//Do display rotation
	const int displayRotateDelay = 5;		// This is the minimum delay in ms between steps of the stepper motor
	boolean displayRotating = false;		// Is the "Display Rotate" function enabled?
	boolean displayRotatingCW = true;		// In Display Rotate mode, are we rot
	//int displayRotatingLastMoveMillis = 0;

	// Helper declarations
	boolean isYesNo = false;
	String overwriteHeader;
	boolean	stayInMenu = false;

	//    >>>>    START     ---------------------------    Adafruit Setup    ---------------------------

	Adafruit_MotorShield AFMStop(0x60); // Default address, no jumpers
	Adafruit_StepperMotor *mystepper = AFMStop.getStepper(200, 2);	 //Connect stepper with 200 steps per revolution (1.8 degree) to the M3, M4 terminals (blue,yellow,green,red)

	//you can change these to SINGLE, DOUBLE, INTERLEAVE or MICROSTEP!
	//wrapper for the motor!(3200 Microsteps / revolution)
//	void forwardstep2() { mystepper->onestep(BACKWARD, MICROSTEP); }
//	void backwardstep2() { mystepper->onestep(FORWARD, MICROSTEP); }
//	void release2()	{ mystepper->release(); }
//	AccelStepper stepper = AccelStepper(forwardstep2, backwardstep2);	// wrap the stepper in an AccelStepper object


	//    <<<<    FINISH    ---------------------------    Adafruit Setup    ---------------------------

	//    >>>>    START     ---------------------------    Arduino Setup     ---------------------------

	void setup ()
	{
		Serial.begin (9600);		// Start Console logging
		lcd.begin (16, 2);		// set up the LCD's number of columns and rows:

		initialiseDCC ();

		newMenu = true;
		startupLCD ();
	}

	//    <<<<    FINISH    ---------------------------    Arduino Setup     ---------------------------

	//    <<<<    FINISH    -----------------------------    UNO Config    ----------------------------

	*/
	//    >>>>    START     -------------------------    Main Arduino Loop     -------------------------

	void loop ()
	{
		decideMenu(checkMenu);
	}
	//    <<<<    FINISH    -------------------------    Main Arduino Loop     -------------------------

	///    >>>>    START     ----------------------------    Helper Voids    ----------------------------

	void keyPadState ()
	{

		adc_key_prev = lcd_key;	// Looking for changes
		lcd_key = read_LCD_buttons();	// read the buttons

		if (adc_key_in < 1020)
		{
			if (adc_key_prev != lcd_key)
			{
				String str1 = String (adc_key_in);
				String str2 = String ("	");
				String str3 = String (lcd_key);
				String strKeyOutput  =  str1 +str2 +str3;
				displayOutput(true ,strKeyOutput, "");

				key = lcd_key;
			}
			else { key = 0; }
		}
	}

	int read_LCD_buttons ()
	{
		adc_key_in = analogRead(0);	// read the value from the sensor
		delay(5); //switch debounce delay. Increase this delay if incorrect switch selections are returned.
		int k = (analogRead(0) - adc_key_in); //gives the button a slight range to allow for a little contact resistance noise
		if (5 < abs(k)) return 0;	// double checks the keypress. If the two readings are not equal +/-k value after debounce delay, it tries again.
	  	// my buttons when read are centered at these values: 0, 144, 329, 504, 741
	  	// we add approx 50 to those values and check to see if we are close
		if (adc_key_in > 1000) return 0; // We make this the 1st option for speed reasons since it will be the most likely result
		if (adc_key_in < 50)	return 5;
		if (adc_key_in < 195)	return 3;
		if (adc_key_in < 380)	return 4;
		if (adc_key_in < 555)	return 2;
		if (adc_key_in < 790)	return 1;
		return 0;	// when all others fail, return this...
	}

	void displayOutput(boolean consoleOnly, String rowA, String rowB)
	{
		if(consoleOnly)
		{
			if (isDebugMode) {printToConsole(rowA, rowB);}
		}
		else
		{
			printToLCD(rowA, rowB);
			if (isDebugMode) {printToConsole(rowA, rowB);}
		}
	}

	void printToLCD (String rowA, String rowB)
	{
		lcd.clear();
		lcd.print(rowA);
		lcd.setCursor(0, 1);
		lcd.print(rowB);
		String lcdRowA = String("");
		String lcdRowB = String("");
	}

	void printToConsole (String rowA, String rowB)
	{
		Serial.println(rowA);
		Serial.println(rowB);
		String lcdRowA = String("");
		String lcdRowB = String("");
	}

	void startupLCD ()
	{
		String startupLCDHeader = String("Peco Turntable");
		String startupLCDFooter = String("Indexed Tracks");
		displayOutput(false,startupLCDHeader, startupLCDFooter);
		delay(2000);
     //   readArrayEEPROM ();
		decideMenu(0);
	}

	void decideMenu (int inpSelMenu)
	{
		checkMenu = inpSelMenu;

		switch (checkMenu)
		{
			case 0:
				mainMenu (0, 1, 3);
				break;
			case 1:
				mainMenu (2, 3, 5);
				break;
			case 2:
				mainMenu (4, 5, 4);
				break;
			case 3:
                //autoDCCMode();
				break;
			case 4:
				manualMode ();
				break;
			case 5:
				checkYesNo ();
				break;
			case 6:
				calibrateBridge ();
				break;
			case 7:
				selectProgrammedTargetTracks ();
				break;
			case 8:
				selectSaveTracks ();
				break;
		}
	}

	void mainMenu (int selMenu1, int selMenu2, int menuItems)
	{

		menuItems = menuItems - 1;

		keyPadState();

		if (newMenu)
		{
			currentMenuItem = 0;
			chooseSubMenu(selMenu1);
			newMenu = false;
			key = 0;
		}
		else
		{
			if (currentMenuItem < 0) { currentMenuItem = menuItems; }	 //If we are out of bounds on the menu then reset it
			else if (currentMenuItem > menuItems) { currentMenuItem = 0; }	//If we are out of bounds on the menu then reset it

			if (key != lastKey)	//If we have changed Index, saves re-draws.
			{
				switch (key)
				{
					case 3: // Up
						currentMenuItem = currentMenuItem - 1;
						resetMenuList(menuItems);
						chooseSubMenu(selMenu1);
						break;

					case 4: // Down
						currentMenuItem = currentMenuItem + 1;
						resetMenuList(menuItems);
						chooseSubMenu(selMenu1);
						break;

					case 1: //If Selected
						chooseSubMenu(selMenu2);
						break;
				}
			}

			lastKey = key;	//Save the last State to compare.
			delay(50);	//Small delay
			newMenu = false;
		}
	}

	void chooseSubMenu (int subMenu)
	{
		switch (subMenu)
		{
			case 0:
				displayMainMenu(currentMenuItem);
				break;
			case 1:
				selectMainMenu(currentMenuItem);
				break;
			case 2:
				displaySubMenuA(currentMenuItem);
				break;
			case 3:
				selectSubMenuA(currentMenuItem);
				break;
			case 4:
				displaySubMenuB (currentMenuItem);
				break;
			case 5:
				selectSubMenuB (currentMenuItem);
				break;
		}
	}

	void resetMenuList (int menuItems)
	{
		if (currentMenuItem < 0) { currentMenuItem = menuItems; }
		else if (currentMenuItem > menuItems) { currentMenuItem = 0; }
	}

	int resetTracks(int currentTrack)
	{
		if (currentTrack < 1) { currentTrack = 6; }
		else if (currentTrack > 6) { currentTrack = 1; }
		return currentTrack;
	}

	void resetMenu (int subMenu, int menuPosition)
	{
		stayInMenu = false;
		delay(1000);
		checkMenu = 0;
		currentMenuItem = menuPosition;
		chooseSubMenu(subMenu);
		newMenu = true;
	}

	void getTrackTail(int trackCurrent, int trackTarget)
	{
		currentTrack = GetTrackTail[trackCurrent - 1];
		newTrack = GetTrackTail[trackTarget - 1];
		displayManualMove(newTrack);
	}

	void sortLowHigh(int a[], int size)
	{
		for (int i = 0; i < (size - 1); i++)
		{
			for (int o = 0; o < (size - (i + 1)); o++)
			{
				if (a[o] > a[o + 1])
				{
					int t = a[o];
					a[o] = a[o + 1];
					a[o + 1] = t;
				}
			}
		}
	}

	void checkYesNo ()
	{
		checkMenu = 5;
		stayInMenu = true;
		keyPadState();
		String lcdRowA, lcdRowB;

		lcdRowA=overwriteHeader;
		lcdRowB = String("Update?  yes  NO");

		if (newMenu)
		{
			key = -1;
			displayOutput(false,lcdRowA, lcdRowB);
		}
		if (key != lastKey)
		{
			if (key == 3 || key == 4)
			{
				resetMenu(2, 0);
			}
			if (key == 2)
			{
				lcdRowB = String("Update?  YES  no");
				displayOutput(false,lcdRowA, lcdRowB);
				chkOverwrite = true;
			}
			if (key == 5)
			{
				lcdRowB = String("Update?  yes  NO");
				displayOutput(false,lcdRowA, lcdRowB);
				chkOverwrite = false;
			}

			if (key == 1)
			{
				if (chkOverwrite)
				{
					 isYesNo =  true;
					 stayInMenu = false;
				}
				else
				{
					 isYesNo =  false;
					 stayInMenu = false;
				}
			}
		}
		lastKey = key;
		newMenu = false;
	}

	//    <<<<    FINISH    ----------------------------    Helper Voids    ----------------------------

	//    >>>>    START     --------------------------    EEPROM Commands     --------------------------

	void EEPROMWritelong (int address, int value)
	{
		if (isTrackCalibration) { address = 0; }
		else { address = address * 2; }

		int val1 = value / 100;
		int val2 = value % 100;

			EEPROM.write(address, val1);
			EEPROM.write(address + 1, val2);
	}

	void arrayWritelong (int address, int value)
	{

		if (isTrackCalibration) { address = 0; }

		int val1 = value / 100;
		int val2 = value % 100;

		  String strA = String (val1);
		  String strB = String (val2);

		int valueJoin = (val1 * 100) + val2;
		String strC = String (valueJoin);

		 String strRowA = String (strA + " " + strB + " " + strC);
		PositionTrack[address] = valueJoin;

		String strRowB = String (address);

   		displayOutput(true ,strRowA, strRowB);
	}

	int EEPROMReadlong (int address)
	{
		String EEPROMReadlongText;

		if (isTrackCalibration) { address = 0; }
		else { address = address * 2; }

		int valueJoin = 0;

		int val1 = EEPROM.read(address);
		int val2 = EEPROM.read(address + 1);
		valueJoin = (val1 * 100) + val2;

		String strA = String("Add 1 = ");
		String strB = String(val1);
		str1 = strA + strB;
		String strC = String("	Add 2 = ");
		String strD = String(val2);
		str2 = strC + strD;
		String strE = String("	EEPROM Step = ");
		String strF = String(valueJoin, DEC);
		str3 = strE + strF;
		EEPROMReadlongText = str1 + str2 + str3;

		displayOutput(true ,EEPROMReadlongText, "");

		return valueJoin;
	}

	void readArrayEEPROM ()
	{
		int c;
		int arraySize = sizeof(PositionTrack);
		for ( int i = 0; i < arraySize; i++)
		{
			if (i == 0) { c = i; }
			else { c = i * 2; }

			str1 = String(c, DEC);
			str2 = String(arraySize, DEC);

			PositionTrack[i] = EEPROMReadlong(c);
			//PositionTrack[i] = PositionTrackDummy[i];
		}
	}

	void clearEEPROM ()
	{
		int c;
		//unsigned int arraySize = sizeof (PositionTrack);
		for (int i = 0; i < 7; i++)
		{
			if (i == 0) { c = i; }
			else { c = i * 2; }
			if (PositionTrack[i] != 0)
			{
				EEPROM.write(c, 0);
				EEPROM.write(c + 1, 0);
			}
		}
	}

	void writeDummyTrackPositions ()
	{
	//	for (int a = 1; a < sizeof (PositionTrack); a++)
	//	{
	//		PositionTrack[a] = PositionTrackDummy[a];
	//	}
	}

	void checkTrackPositions ()
	{
		for (int a = 0; a < 7; a++)
		{
			int arrayReadVal = PositionTrack[a];
			str1 = String(arrayReadVal, DEC);
		}
	}
	//    <<<<    FINISH    --------------------------    EEPROM Commands     --------------------------
/*
	//    >>>>    START     -------------------------    DCC Decoder Setup     -------------------------

	void initialiseDCC ()
	{
		DCC.SetBasicAccessoryDecoderPacketHandler (BasicAccDecoderPacket_Handler, true);
		ConfigureDecoder ();
		DCC.SetupDecoder (0x00, 0x00, kDCC_INTERRUPT);
	}

	void ConfigureDecoder ()
	{ //Put all the decoder #'s you need here.	Remember to change
		//DCCAccessoryAddress gAddresses[XX];(above) where XX = number of addresses you need.
		gAddresses[0].address = 200;
		gAddresses[1].address = 201;
		gAddresses[2].address = 202;
		gAddresses[3].address = 203;
		gAddresses[4].address = 204;
		gAddresses[5].address = 205;
		gAddresses[6].address = 206;
	}

	void BasicAccDecoderPacket_Handler (int address, boolean activate, byte data)	// Basic accessory packet handler
	{
		// Convert NMRA packet address format to human address
		address -= 1;
		address *= 4;
		address += 1;
		address += (data & 0x06) >> 1;

		boolean enable = (data & 0x01) ? 1 : 0;

		for (int i = 0; i < ( int ) (sizeof(gAddresses) / sizeof(gAddresses[0])); i++)
		{
			if (address == gAddresses[i].address)
			{
        str1 = String (F ("DCC addr: "));
        str2 = String (address, DEC);
        str3 = String (F ("	Head/Tail = (1/0) : "));
        str4 = String (enable, DEC);
        str5 = str1 + str2 + str3 + str4;
        displayOutput (true, str5, "");
				//new stuff
				tableTargetHead = enable;
				tableTargetPosition = i;
				//New packet and we have a new target location, set the flag
				newTargetLocation = true;
				doStepperMove();
			}
		}
	}

	void autoDCCMode ()
	{
		checkMenu = 3;
		int addr = 0;
		DCC.loop ();
		// Bump to next address to test
		if (++addr >= ( int )(sizeof(gAddresses) / sizeof(gAddresses[0]))) 	{ addr = 0; }
		stepperTimer();
	}

	//    <<<<    FINISH    -------------------------    DCC Decoder Setup     -------------------------
*/

	//    >>>>    START     ---------------------------    Main LCD Menu     ---------------------------

	void displayMainMenu (int dispMenu)
	{
		String displayMainMenuHeader, displayMainMenuFooter;
		checkMenu = 0;
		displayMainMenuHeader = String("Turntable Menu");

		switch (dispMenu)
		{
			case 0:
				displayMainMenuFooter = String("1 DCC Auto Mode");
				displayOutput(false,displayMainMenuHeader, displayMainMenuFooter);
				break;
			case 1:
				displayMainMenuFooter = String("2 Manual Mode");
				displayOutput(false,displayMainMenuHeader, displayMainMenuFooter);
				break;
			case 2:
				displayMainMenuFooter = String("3 Setup Menu");
				displayOutput(false,displayMainMenuHeader, displayMainMenuFooter);
				break;

		}
	}

	void selectMainMenu (int selMainMenu)
	{
		keyPadState();
		checkMenu = 0;
		String selectMainMenuHeader, selectMainMenuFooter;

		switch (selMainMenu)
		{
			case 0:		//	Setup Tracks

                selectMainMenuFooter = String("DCC Mode enabled");
				displayOutput(false,selectMainMenuHeader, selectMainMenuFooter);
				break;
			case 1:
				programmingMode = false;
                selectMainMenuHeader = String("Manual Move");

				displayOutput(false,selectMainMenuHeader, selectMainMenuFooter);
				delay(500);
				lcd.clear();
				lcd.setCursor(0, 0);
				//lcd.write (byte (0));
				lcd.print(" Head   Tail ");
				//lcd.write (byte (1));
				lcd.setCursor(0, 1);
				//lcd.write (byte (2));
				lcd.print("    Move     ");
				//lcd.write (byte (3));
				delay(2000);
				lastKey = -2;
				newMenu = true;
				stayInMenu = true;
				do
				{
					manualMode();
				}	while (stayInMenu);

				break;
			case 2:
				newMenu = true;
				currentMenuItem = 0;
				chooseSubMenu(2);
				break;
		}
	}

	//    <<<<    FINISH    ---------------------------    Main LCD Menu     ---------------------------

	//    >>>>    START     -----------------------    LCD Menu - Setup Menu     -----------------------

	void displaySubMenuA (int dispMenu)
	{
		String displaySubMenuAHeader, displaySubMenuAFooter;
		checkMenu = 1;

		displaySubMenuAHeader = String("Setup Menu");

		switch (dispMenu)
		{
			case 0:
				displaySubMenuAFooter = String("1 Turntable Menu");
				displayOutput(false, displaySubMenuAHeader, displaySubMenuAFooter);
				break;
			case 1:
				displaySubMenuAFooter = String("2 Calibration");
				displayOutput(false, displaySubMenuAHeader, displaySubMenuAFooter);
				break;
			case 2:
				displaySubMenuAFooter = String("3 Clear Tracks");
				displayOutput(false, displaySubMenuAHeader, displaySubMenuAFooter);
				break;
			case 3:
				displaySubMenuAFooter = String("4 Debug Mode");
				displayOutput(false, displaySubMenuAHeader, displaySubMenuAFooter);
				break;
			case 4:
				displaySubMenuAFooter = String("5 Main Menu");
				displayOutput(false, displaySubMenuAHeader, displaySubMenuAFooter);
				break;

		}
	}

	void selectSubMenuA (int selectSubMenu)
	{
		checkMenu = 1;

		keyPadState();

		String selectSubMenuAHeader, selectSubMenuAFooter;

		switch (selectSubMenu)
		{
			case 0:		//	Setup Tracks
				newMenu = true;
				currentMenuItem = 0;
				chooseSubMenu(4);
				break;
			case 1:
				selectSubMenuAHeader = String("Set Calibration");
				displayOutput(false, selectSubMenuAHeader, selectSubMenuAFooter);
				newMenu = true;
				stayInMenu = true;
				do
				{
					calibrateBridge();
				}	while (stayInMenu);
				resetMenu(2, 1);
				break;
			case 2:
				selectSubMenuAHeader = String("Reset Memory");
				displayOutput(false, selectSubMenuAHeader, selectSubMenuAFooter);
				clearEEPROM();
				delay(2000);
				selectSubMenuAFooter = String("Memory wiped");
				displayOutput(false, selectSubMenuAHeader, selectSubMenuAFooter);
				resetMenu(2, 2);
				break;
			case 3:
				selectSubMenuAHeader = String("Debug Mode");
				displayOutput(false, selectSubMenuAHeader, selectSubMenuAFooter);
				newMenu = true;
				overwriteHeader = selectSubMenuAHeader;
        stayInMenu = true;
				do
				{
					checkYesNo();
				}	while (stayInMenu);

				if(isYesNo)
				{
					isDebugMode = true;
					selectSubMenuAFooter = String("Debug mode on");
					displayOutput(false, selectSubMenuAHeader, selectSubMenuAFooter);
				}
				else {isDebugMode = false;}
				resetMenu(2, 4);

				break;
			case 4:
				delay(1000);
				currentMenuItem = 0;
				chooseSubMenu(0);
				newMenu = true;
				break;
		}
	}

	//    <<<<    FINISH    -----------------------    LCD Menu - Setup Menu     -----------------------

	//    >>>>    START     ------------------    LCD Menu - Turntable Setup Menu     ------------------

	void displaySubMenuB (int dispMenu)
	{
		String displaySubMenuBHeader, displaySubMenuBFooter;
		checkMenu = 2;

		displaySubMenuBHeader = String ("Turntable Menu");

		switch (dispMenu)
		{
			case 0:
				displaySubMenuBFooter = String ("1 Setup Tracks");
				displayOutput (false, displaySubMenuBHeader, displaySubMenuBFooter);
				break;
			case 1:
				displaySubMenuBFooter = String ("2 Check Tracks");
				displayOutput (false, displaySubMenuBHeader, displaySubMenuBFooter);
				break;
			case 2:
				displaySubMenuBFooter = String ("3 Track Settings");
				displayOutput (false, displaySubMenuBHeader, displaySubMenuBFooter);
				break;
			case 3:
				displaySubMenuBFooter = String ("4 Setup Menu");
				displayOutput (false, displaySubMenuBHeader, displaySubMenuBFooter);
				break;
		}
	}

	void selectSubMenuB (int selectSubMenu)
	{
		checkMenu = 2;

		keyPadState ();

		String selectSubMenuBHeader, selectSubMenuBFooter;

		switch (selectSubMenu)
		{
			case 0:		//	Setup Tracks
				selectSubMenuBHeader = String ("Track Positions");
				selectSubMenuBFooter = String ("Move bridge");
				displayOutput (false, selectSubMenuBHeader, selectSubMenuBFooter);
				newMenu = true;
				selectProgrammedTargetTracks ();
				break;
			case 1:
                selectSubMenuBHeader = String ("Verify Tracks");
				displayOutput (false, selectSubMenuBHeader, selectSubMenuBFooter);
				newMenu = true;
				stayInMenu = true;
				do
				{
		            checkTrackMove ();
		        }	while (stayInMenu);
				resetMenu (4, 1);
				break;
			case 2:
	          for (int i = 0; i < 7; i++)
	          {
	              str1 = String ("Track:      ");
	              str2 = String (i, DEC);
	              selectSubMenuBHeader = String (str1 + str2);
	              str3 = String ("Saved Step: ");
	              //int getArrayStep = PositionTrack[i];
	              str4 = String (PositionTrack[i], DEC);
	              selectSubMenuBFooter = String (str3 + str4);
	              displayOutput (false, selectSubMenuBHeader, selectSubMenuBFooter);
	              delay (2500);
	          }
				resetMenu (4, 2);
				break;
			case 3:
				delay (1000);
				currentMenuItem = 0;
				chooseSubMenu (2);
				newMenu = true;
				break;
		}
	}

	//    <<<<    FINISH    ------------------    LCD Menu - Turntable Setup Menu     ------------------

	//    >>>>    START     ----------------------    Manually Move Turntable     ----------------------

	void manualMode()
	{
		checkMenu = 4;
		stayInMenu = true;
		if (newMenu)
		{
			key = -1;
			newMenu = false;
			displayManualMove(0);
		}

		keyPadState();
		if (key != lastKey)
		{
			switch (key)
			{
				case 2:
					newTrack = newTrack - 1;
					newTrack = resetTracks(newTrack);
					displayRotatingCW = false;
					displayManualMove(newTrack);
					break;

				case 3:
					isTurntableHead = true;
					if (isTurntableHeadChanged != isTurntableHead) { getTrackTail(currentTrack, newTrack); }
					displayManualMove(newTrack);
					break;

				case 4:
					isTurntableHead = false;
					if (isTurntableHeadChanged != isTurntableHead) { getTrackTail(currentTrack, newTrack); }
					displayManualMove(newTrack);
					break;

				case 5:
					newTrack = newTrack + 1;
					newTrack = resetTracks(newTrack);
					displayRotatingCW = true;
					displayManualMove(newTrack);
					break;

				case 1:
					storeStartTrack = currentTrack;
					storeTargetTrack = newTrack;
					moveManualTurntableMain(newTrack);
					break;
			}
		}
		lastKey = key;
		newMenu = false;
	}

	void displayManualMove(int dispMove) // passes across selected track
	{
		String lcdRowA, lcdRowB, str1;

			// Set track limits and direction Track 0 not allowed as reference point
			if (dispMove < 1){ dispMove = 6; }
			else if (dispMove > 6) { dispMove = 1; }

			if (isTurntableHead) { lcdRowA = String("HEAD selected..."); }
			else { lcdRowA = String("TAIL selected..."); }

			isTurntableHeadChanged = isTurntableHead;

			calcLeastSteps(currentTrack, dispMove);

			if (displayRotatingCW) { str1 = String("CW: "); }
			else { str1 = String("CCW: "); }

			String str2 = String(currentTrack);
			String str3 = String(" to ");
			String str4 = String(dispMove);
			lcdRowB = str1 + str2 + str3 + str4;

		displayOutput(false,lcdRowA, lcdRowB);
	}

	void moveManualTurntableMain(int manMove)
	{
		String lcdRowA, lcdRowB;
		key = 0;
		do
		{
			//	newTargetLocation = PositionTrack[manMove];
			fakeTurnMove(currentTrack);
		} 	while (storeTargetTrack != currentTrack);

		currentTrack = newTrack;
		String str1 = String("Reached Track ");
		String str2 = String(currentTrack);
		lcdRowA = str1 + str2;
		lcdRowB = ("UP=Exit	L/R=New");
		displayOutput(false,lcdRowA, lcdRowB);

		// 		Arduino only
		//		lcd.setCursor(0, 1);
		//		lcd.write(byte(0));
		//		lcd.print(" =Exit	New= ");
		//		lcd.write(byte(2));
		//		lcd.print("|");
		//		lcd.write(byte(3));

				delay(50);

				do
				{
					keyPadState();
					if (key == 3 || key == 4) { resetMenu(0, 1);}			// Pressed Up or Down
					else if (key == 2 || key == 5) { stayInMenu = false;}	// Pressed Left or Right
				}	while (stayInMenu);
	}

	void calcLeastSteps(int trA, int trB)
	{

		int currentLoc = PositionTrack[trA];
		int newTargetLoc = PositionTrack[trB];
		int getMotorStepCount = (MOTOR_STEP_COUNT / 2);

		if (newTargetLoc > 0)
		{
//		int currentLoc = stepper.currentPosition();
			mainDiff = newTargetLoc - currentLoc;
			if (mainDiff > getMotorStepCount) { mainDiff = mainDiff - MOTOR_STEP_COUNT; }
			else if (mainDiff < -getMotorStepCount) { mainDiff = mainDiff + MOTOR_STEP_COUNT; }

			if (mainDiff < 0)
			{
				mainDiff -= MOTOR_OVERSHOOT;
				overshootDestination = MOTOR_OVERSHOOT;
			}

			if (mainDiff < 0) { displayRotatingCW = false; }
			else if (mainDiff > 0){ displayRotatingCW = true; }

			String textMove;
			if (displayRotatingCW){ textMove = "CW"; }
			else { textMove = "CCW"; }

			//stepper.move(mainDiff);
		}
	}

	void fakeTurnMove(int fakeMove)
	{
		if (displayRotatingCW) 	{ currentTrack = fakeMove + 1; }
		else { currentTrack = fakeMove - 1; }

		if (currentTrack < 1) { currentTrack = 6; }
		else if (currentTrack > 6) { currentTrack = 1; }

		String str1 = String("Moving: ");
		String str2 = String(storeStartTrack);
		String str3 = String(" to ");
		String str4 = String(storeTargetTrack);

		String lcdRowA = str1 + str2 + str3 + str4;
		delay(1000);

		String str5 = String("Track:");
		String str6 = String(currentTrack);
		String lcdRowB = str5 + str6;
		displayOutput(false,lcdRowA, lcdRowB);
	}

	//    <<<<    FINISH    ----------------------    Manually Move Turntable     ----------------------

    //    >>>>    START     -----------------------    Check Track Positions     -----------------------

   void checkTrackMove ()
    {
        tableTargetPosition = 0;

            for (int i = 1; i < 7; i++)
            {
                tableTargetTrack = i;
			
                tableTargetPosition = PositionTrack[tableTargetTrack];
				displayCheckTracks (i);
				newMenu = true;
                verifyTrackMove ();
            }
    }

    void displayCheckTracks(int checkCurrentTrack)
    {
		String LCDRowA, LCDRowB;
    	do
    	{
    		currentStepPosition = forwardstep2();
 			showVariedStepPosition(currentStepPosition);
    	}	while  (currentStepPosition != tableTargetPosition);

		str1 = String("Step = ");
		str2 = String(currentStepPosition, DEC);
		LCDRowA = String(str1 + str2);
		str3 = String("At Track ");
		str4 = String(checkCurrentTrack, DEC);
		LCDRowB = String(str3 + str4);
		overwriteHeader  = String(LCDRowA);
		displayOutput(false, LCDRowA, LCDRowB);
    }

	void  showVariedStepPosition(int checkCurrentTrack)
	{
		String strShowVariedStepPosition;
		int moveVarDiff=0;
		int moveSmall = 0;

		moveVarDiff = tableTargetPosition - checkCurrentTrack;
		moveSmall = abs(tableTargetPosition - checkCurrentTrack);

		str3 =  String("Track ");
		str4 =  String( tableTargetTrack,DEC);

		String showVariedTrackPosition =  String(str3 + str4);

		if (moveSmall < 10 )
		{
			str1 = String("Step = ");
			str2 = String(checkCurrentTrack, DEC);
			strShowVariedStepPosition = String(str1 + str2);
			displayOutput(false, showVariedTrackPosition, strShowVariedStepPosition);
		}

		else if((checkCurrentTrack % 25) == 0)
		{
			str1 = String ("Step = ");
			str2  = String(checkCurrentTrack,DEC);
			strShowVariedStepPosition = String(str1 + str2);
			displayOutput(false, showVariedTrackPosition, strShowVariedStepPosition);
		}
	}

	void verifyTrackMove ()
	{
		stayInMenu = true;
//		do { fakeTurnMove (currentTrack); }	while (storeTargetTrack != currentTrack);

		do { checkYesNo (); }  while (stayInMenu);

		if (!isYesNo) { selectProgrammedTargetTracks (); }
		else 
		{ stayInMenu = false; }
	
	}

    //    <<<<    FINISH    -----------------------    Check Track Positions     -----------------------

	//    >>>>    START     ---------------    Select And Programme Turntable Tracks     ---------------

	void selectProgrammedTargetTracks()
	{
		String lcdRowA, lcdRowB;
		stayInMenu = true;
		checkMenu = 7;
		keyPadState();

		if (newMenu)
		{
			currentTurntablePosition(false, 0);
			key = -1;
		}

		if (key != lastKey)
		{
			switch (key)
			{
				case	3:			//If Up	== Large x 10 steps
					currentTurntablePosition (true,+10);
					break;
				case 4: 		//If Down == Large x -10 steps
					currentTurntablePosition (true, -10);
					break;
				case 2: 		//If Left	== -single steps
					currentTurntablePosition (true, -1);
					break;
				case 5: 		//If Right == single steps
					currentTurntablePosition (true, 1);
					break;
				case 1:
					currentTurntablePosition (false,0);
					newMenu = true;
					selectSaveTracks();
					break;
			}
		}
		lastKey = key;
		newMenu = false;
	}
	/*
	void dispProgrammeTurntableSteps (int fakeMultiple)
	{
		String lcdRowA, lcdRowB;
		if (fakeMultiple != 0)
		{
			switch (fakeMultiple)
			{
				case -1:
					currentStepPosition = currentStepPosition - 1;
					if (currentStepPosition < 0) { currentStepPosition = MOTOR_STEP_COUNT - 1; }
					break;
				case -10:
					currentStepPosition = currentStepPosition - 10;
					if (currentStepPosition < 0) { currentStepPosition = MOTOR_STEP_COUNT - 10; }
					break;
				case 1:
					currentStepPosition = currentStepPosition + 1;
					if (currentStepPosition > MOTOR_STEP_COUNT) { currentStepPosition = 0; }
					break;
				case 10:
					currentStepPosition = currentStepPosition + 10;
					if (currentStepPosition > MOTOR_STEP_COUNT) { currentStepPosition = 10; }
					break;
			}

			if (currentStepPosition < -MOTOR_STEP_COUNT || currentStepPosition > MOTOR_STEP_COUNT - 1) { currentStepPosition = 0; }
		currentTurntablePosition(true,fakeMultiple);
		}
	}
	*/

	void currentTurntablePosition (boolean isMoving, int fakeMultiple)
	{
		String lcdRowA, lcdRowB;

		if (isMoving)
		{
			if (fakeMultiple != 0)
			{
				currentStepPosition = currentStepPosition + fakeMultiple;
				if (currentStepPosition < 0) { currentStepPosition = MOTOR_STEP_COUNT; }
				if(	currentStepPosition > MOTOR_STEP_COUNT -1) { currentStepPosition = 0; }
			}

			str3 = String("Move: ");
			str4 = String(fakeMultiple);
			str5 = String(" steps");
			lcdRowB = str3 + str4 + str5;
		}
		else
		{
			str3 = String("Last Track: ");
			str4 = String(runs,DEC);
			lcdRowB = str3 + str4;
		}

		str1 = String(currentStepPosition, DEC);
		str2 = String(" steps.");

		lcdRowA = str1 + str2;
		displayOutput(false,lcdRowA, lcdRowB);
	}

	void selectSaveTracks ()
	{
		//int test = currentStepPosition;

		checkMenu = 8;
		String lcdRowA, lcdRowB, strD;
		// display and pick track number
		String strA = String("Position = ");
		String strB = String(currentStepPosition, DEC);
		String strC = String("Save to Track ");
		lcdRowA = strA + strB;

		keyPadState();

			if (newMenu)
			{
				key = -1; //	If come from different menu do nothing
				runs = 1;
				strD = String(runs);
				lcdRowB = strC + strD;
				displayOutput(false,lcdRowA, lcdRowB);
			}
			if (key != lastKey)	// Check key press is new
			{

				switch (key)
				{
					case 3:			// Up
						runs = runs + 1;
						runs = resetTracks(runs);
						strD = String(runs);
						lcdRowB = strC + strD;
						displayOutput(false,lcdRowA, lcdRowB);
						break;
					case 4: 		// Down
						runs = runs - 1;
						runs = resetTracks(runs);
						strD = String(runs);
						lcdRowB = strC + strD;
						displayOutput(false,lcdRowA, lcdRowB);
						break;
					case 1:	// Select
						saveProgrammingTracks();
						break;
					case 2:
						resetMenu(4, 0);
						break;
					case 5:
						resetMenu(4, 0);
						break;
				}
			}

		lastKey = key;
		newMenu = false;
	}

	void saveProgrammingTracks ()
	{
		boolean isStepTaken = false;
		boolean isTrackTaken = false;
		int c = 0;

		for ( int i = 0; i < sizeof(PositionTrack); i++)


		{
			for (int t = -10; t < 10; t++)
			{
				if (PositionTrack[c] == currentStepPosition + t) { isStepTaken = true; }
			}
			c++;
		}

		if (PositionTrack[runs] != 0) { isTrackTaken = true; }

		if (isStepTaken || isTrackTaken)
		{

			String str1 = String("chkOverwrite = true    ");
			String str2 = String("newMenu = true    ");
			String str3 = String("checkOverwriteTrack");
			String	lcdRowA = str1 + str2 + str3;
			displayOutput(true, lcdRowA, "");
			checkMenu = 8;
			chkOverwrite = true;
			newMenu = true;
			checkOverwriteTrack();
		}
		else
		{
			overwriteTrack();
		}
	}

	void checkOverwriteTrack ()
	{
		overwriteHeader = String("Overwrite track");
		do {checkYesNo();}
		while (stayInMenu);

		if (isYesNo)
		{
      displayOutput(false, overwriteHeader,"");
			overwriteTrack();
		}
		else
		{
			Serial.print("Reset Menu");
			resetMenu(2, 0);
		}
	}

	void overwriteTrack ()
	{

		int readBackArray, readBackEEPROM;

		String lcdRowA, lcdRowB, strA,strB,strC,strD;

		lcdRowA = String("Saving new track");

		strA = String("Track ");
		strB = String(runs, DEC);
		strC = String(" Pos ");
		strD = String(currentStepPosition, DEC);

		lcdRowB = String(strA + strB + strC + strD);

		displayOutput(false,lcdRowA, lcdRowB);
		delay(1500);

		arrayWritelong(runs, currentStepPosition);
		readBackArray = PositionTrack[runs];
		Serial.println(readBackArray);

		if (readBackArray == currentStepPosition)
		{
			EEPROMWritelong(runs, currentStepPosition);
			readBackEEPROM  = EEPROMReadlong(runs);
			Serial.println(readBackEEPROM);
		}

		if (readBackArray == readBackEEPROM)
		{
			lcdRowA = ("!!! FINISHED !!!");
			lcdRowB = ("Saved new track ");
			displayOutput(false,lcdRowA, lcdRowB);
			delay(3000);
			resetMenu(4, 0);
		}
		else
		{
			lcdRowA = ("***  FAILED  ***");
			lcdRowB = ("Track not saved!");
			displayOutput(false,lcdRowA, lcdRowB);
			delay(3000);
			resetMenu(4, 0);
		}
	}

	//    <<<<    FINISH    ---------------    Select And Programme Turntable Tracks     ---------------

	//    >>>>    START     --------------------------    Calibrate Bridge    --------------------------

	void calibrateBridge()
	{
		checkMenu = 6;
		keyPadState();
		if (newMenu)
		{
			runs = 3;
			displayCalibrationRuns(runs);
			key = -1;
			newMenu = false;
		}

		if (key != lastKey)
		{

			switch (key)
			{
				case 3:
					runs = runs + 1;
					displayCalibrationRuns(runs);
					break;
				case 4:
					runs = runs - 1;
					displayCalibrationRuns(runs);
					break;
				case 1:
					calibrateBridgeRun(runs);
					break;
				case 2:
					resetMenu(2, 1);
					break;
				case 5:
					resetMenu(2, 1);
					break;
			}
		}

		//Save the last State to compare.

		lastKey = key;
		//	lastRun = runs;
		newMenu = false;
	}

	void displayCalibrationRuns(int r)
	{
		String lcdRowA, lcdRowB;

		if (r <= 0) 	{ r = 1; }
		else if (r > 5) 	{ r = 5; }

		lcdRowA = "Set Calibration";
		String str1 = String("Runs = ");
		String str2 = String(r);
		lcdRowB = str1 + str2;
		displayOutput(false,lcdRowA, lcdRowB);
		//	lcd.setCursor(10, 2);
		//	lcd.write(byte(6));
	}

	void calibrateBridgeRun(int c)
	{
		int calRuns = 0;
		for (int i = 0; i <= c - 1; i++)
		{

			// if near reference point move away
			/*			sensorVal = digitalRead(7);
			do
			{
				sensorVal = digitalRead(7);
				forwardstep2();
				delay(50);

			}	while (sensorVal == LOW);


			if (c % 2 == 1)
			{
				// step forward to sensor index point
				do
				{
					sensorVal = digitalRead(7);
					forwardstep2();
					delay(50);
					arrayCalibrate[i] = sensorVal;
				} while (sensorVal == HIGH);
			}
			else {// step backwards to sensor index point
				do
				{
					sensorVal = digitalRead(7);
					backwardstep2();
					delay(50);
					arrayCalibrate[i] = sensorVal;
				}	while (sensorVal == HIGH);
			}

			*/
			// Dummy calibration run function
			int dummySensorValue = random(-5, 5);	// Random number between -5 and +5 to simulate ref point
			arrayCalibrate[i] = dummySensorValue;	// store dummy number in Array
			displayResultCalibration(calRuns, false);
			calRuns++;
		}
		displayResultCalibration(calRuns, true);
	}

	void displayResultCalibration(int nRuns, boolean isEnd)
	{

		if (isEnd)
		{
			sortLowHigh(arrayCalibrate, nRuns);			// Sort array by sensor reading size
			int maxRefValue = arrayCalibrate[nRuns - 1];		// work out max
			int minRefValue = arrayCalibrate[0];		// work out min

			int sumRefValue = 0;
			int c = 0;
			// work out average step
			for (int i = 0; i < nRuns; i++)
			{

				c = arrayCalibrate[i];
				sumRefValue = sumRefValue + c;
			}

			int aveRefValue = sumRefValue / nRuns;

			String strA = String("Min =");
			String strB = String(minRefValue, DEC);
			String strC = String(" Max =");
			String strD = String(maxRefValue, DEC);

			String lcdRowA = strA + strB + strC + strD;
			String strE = String("Step ave = ");
			String strF = String(aveRefValue, DEC);
			String lcdRowB = strE + strF;

			displayOutput(false, lcdRowA, lcdRowB);
			saveCalibrationStep(aveRefValue);

		}
		else
		{
			String strG = String("Last Step = ");
			String strH = String(arrayCalibrate[nRuns - 1], DEC);
			String lcdRowA = strG + strH;
			String str1 = String("Runs = ");
			String str2 = String(nRuns);
			String lcdRowB = str1 + str2;
			displayOutput(false,lcdRowA, lcdRowB);
		}

		delay(1000);
	}

	void saveCalibrationStep(int saveCalStep)
	{
		isTrackCalibration = true;
		runs = 0;
		currentStepPosition = saveCalStep;
		overwriteTrack();
		isTrackCalibration = false;
	}

	//    <<<<    FINISH    --------------------------    Calibrate Bridge    --------------------------

    //    >>>>    START     ---------------------------    Stepper Voids     ---------------------------

    int dummyStepper(int dummySteps, int delaySteps)
    {
        int dummyStepPosition = 0;

        if (delaySteps == 0) { delaySteps = 75; }
//		if (isRotatingCW){dummyStepPosition = currentStepPosition +1;}
//		else (dummyStepPosition = currentStepPosition -1;)

        if (dummySteps>0){dummyStepPosition = currentStepPosition +1;}
        else {dummyStepPosition = currentStepPosition - 1;}

        if(dummyStepPosition > MOTOR_STEP_COUNT){dummyStepPosition = 0;}
        if (dummyStepPosition < 0 ) {dummyStepPosition = MOTOR_STEP_COUNT;}

        delay(delaySteps);

        distanceToGo = tableTargetPosition - currentStepPosition;

        return dummyStepPosition;
    }

    void doStepperMove()
    {
//      stepper.run();	// Run the Stepper Motor
//      boolean isInMotion = (abs(stepper.distanceToGo()) > 0);
        boolean isInMotion = (abs (distanceToGo) > 0);
        boolean newTargetSet = false;

        // If there is a new target location, set the target
        if (newTargetLocation)
        {
            //    printToConsole("Moving to New Target Location...", ""));
            SetStepperTargetLocation();
            newTargetSet = true;
        }

        if (inMotionToNewTarget)
        {
            if ((!isInMotion) && (!newTargetSet))
            {
                //str1 = String(F("Not Moving!	DtG: "));
                //str1 = String(stepper.distanceToGo());
                //String(F(" TP: "));
                //str2 = String(stepper.targetPosition());
                //String(F(" CP: "));
                //str3 = String(stepper.currentPosition());
                //String(F(" S: "));
                //str4 = String(stepper.speed());

            }
            //release the brake
        //	brakeservo.write(servoRelease);
        //	delay(5);
        //	inMotionToNewTarget = isInMotion;
        }
        else
        {
//				if ((stepper.currentPosition() % MOTOR_STEP_COUNT) == 0)
            if ((currentStepPosition % MOTOR_STEP_COUNT) == 0)
            {
                //setCurrentPosition seems to always reset the position to 0, ignoring the parameter
                str1 = String("Current location: ");
//				str2 = String(stepper.currentPosition());
                str2 = String (currentStepPosition );
                str3 = String(" % STEPCOUNT.	Why here?");
                displayOutput(true, str1 + str2 , str3);
            }
}
    }

    //stepper timer subroutine came from here.}

    //    ----------------------------------------------------------------------------------------------
    //
    // Subroutine: SetStepperTargetLocation()
    //	Takes the global variables: tableTargetHeadOrTail, and tableTargetPosition, and sets the stepper
    //	object moveTo() target position in steps-	inserts values back into "doStepperMove()"
    //
    //    ----------------------------------------------------------------------------------------------

    void SetStepperTargetLocation()
    {
        int newTargetLoc = -1;
        if (tableTargetHead)
        {	//use head location variable
            {
                newTargetLoc = PositionTrack[tableTargetPosition];
                inMotionToNewTarget = true;
            }
        }
        else
        {	//use tail location variable
            {
                newTargetLoc = PositionTrack[tableTargetPosition];
                inMotionToNewTarget = true;
            }
        }

        if (newTargetLoc > 0)
        {
            //int currentLoc = stepper.currentPosition();
            int currentLoc = currentStepPosition;
            int mainDiff = newTargetLoc - currentLoc;
            if (mainDiff > (MOTOR_STEP_COUNT / 2))
            {
                mainDiff = mainDiff - MOTOR_STEP_COUNT;
            }
            else if (mainDiff < (-MOTOR_STEP_COUNT / 2))
            {
                mainDiff = mainDiff + MOTOR_STEP_COUNT;
            }

            if (mainDiff < 0)
            {
                mainDiff -= MOTOR_OVERSHOOT;
                overshootDestination = MOTOR_OVERSHOOT;
            }
            dummyStepper (mainDiff, 0);
//			stepper.move(mainDiff);
        }
        //programmingMode = false;
        newTargetLocation = false;
    }

      //    ----------------------------------------------------------------------------------------------
    //
    //	Stepper Timer sub routine this runs from the main loop. It also supports the release function.
    //
    //    ----------------------------------------------------------------------------------------------

    void stepperTimer()
    {
        int currentLoc = 0;

        // Run the Stepper Motor //
//			stepper.run();
//		boolean isInMotion = (abs(stepper.distanceToGo()) > 0);

        boolean isInMotion = (abs (distanceToGo) > 0);

        //Check if we have any distance to move for release() timeout.	Can check the
        // buffered var isInMotion because we also check the other variables.
        if (isInMotion || programmingMode)
        {
            //We still have some distance to move, so reset the release timeout
            stepperLastMoveTime = millis();
            isReleased = false;
        }
        else
        {
            if (!isReleased)
            {
                if (overshootDestination > 0)
                {

                    dummyStepper (overshootDestination, 0);
//					stepper.move(overshootDestination);
                    overshootDestination = -1;
                }

                if (((millis() - stepperLastMoveTime) >= releaseTimeout_ms))
                {
                    //If isReleased, don't release again.
                    isReleased = true;
                    str1 = String("Relative Current Position: ");
                    str2 = String (currentStepPosition);	//shows position the table thinks it is at (how it got here)
                    displayOutput (true, str1 + str2,"");

                    currentLoc = currentStepPosition;	// Resets the position to the actual positive number it should be

//					  str1 = String(stepper.currentPosition());	//shows position the table thinks it is at (how it got here)
//                    int currentLoc = stepper.currentPosition();	// Resets the position to the actual positive number it should be

                    currentLoc = currentLoc % MOTOR_STEP_COUNT;

                    if (currentLoc < 0) { currentLoc += MOTOR_STEP_COUNT; }


//                    stepper.setCurrentPosition(currentLoc);
//                    stepper.moveTo(currentLoc);
                    dummyStepper (currentLoc, 0);




//                    String(F("	Actual Current Position: "));
//                    str2 = String(stepper.currentPosition());	// shows the position value corrected.

                    //Set the servo brake
                    //	brakeservo.write(servoBrake);
                    //	delay(750);

                    //release the motor
//                  release2();
                   // str1 = String(F("	Brake Set & Motor Released "));

                }
            }
        }
    }

    //    <<<<    FINISH    ---------------------------    Stepper Voids     ---------------------------


}
