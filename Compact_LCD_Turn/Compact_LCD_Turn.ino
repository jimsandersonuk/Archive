/* -------------------------------  Changelog  ----------------------------------------
Date:	
-
 -------------------------------------------------------------------------------------

Date:	14-08-15
-	Removed CustomChars 4-6
-	Removed ScrollBars void
-	Removed UpDown arrow char

-	Reworked EEPROM read from array -  Address Step change by 2 per run
-	Completed Check and Save Calibration point.
-	Merged fakeTurnMove void to SetStepperTargetLocation with boolean for dummy calibration.  

-------------------------------------------------------------------------------------

Date: 13-08-2015
-	redefined void String Builders to top level declaration 
-	redefined strings in:
-	startupLCD.
-	displayMenu
-	selectMenu
-	moveManualTurntableMain
-	dispProgrammeTurntableSteps
-	fakeTurnMove
-	checkOverwriteTrack
-	displayCalibrationRuns
-	displayResultCalibration

-	removed strings from  selectProgrammedTargetTracks

-------------------------------------------------------------------------------------

Date: 12-08-2015
-	Finished Save tracks and verify write.
-	Finished Calibration of track and print output.
-	**** Work needed: Check and Save Calibration point.

-------------------------------------------------------------------------------------

Date: 11-8-2015
-	Changing procedure to save tracks.

 -------------------------------------------------------------------------------------
*/

// Included Libaries
#include <Arduino.h>
#include <DCC_Decoder.h>
#include <AccelStepper.h>
#include <HardwareSerial.h>
#include <Wire.h>
#include <EEPROM.h>
#include <Adafruit_MotorShield.h>
#include "utility/Adafruit_PWMServoDriver.h"
#include <Servo.h>	//servo library reference
#include <LiquidCrystal.h>	

// Definitions

#define kDCC_INTERRUPT	0										// Define DCC commands to Arduino
typedef struct { int address; } DCCAccessoryAddress;	// Address to respond to
DCCAccessoryAddress gAddresses[7];							// Allows 7 dcc addresses: [XX] = number of addresses you need (including 0).

LiquidCrystal lcd (8, 9, 4, 5, 6, 7);		// initialize the library with the numbers of the interface pins

// Track Step Definitions
int PositionTrack[7] = { 0, 0, 0, 0, 0, 0 };		// Store EEPROM addresses to array - index 0 = calibration position
// const int PositionTrackDummy[7] = { 0, 710, 800, 890, 2310, 2400, 2490 };

//Define Corresponding Head and Tail
const int GetTrackTail[6] = { 4, 5, 6, 1, 2, 3 };	// Array for Tail Tracks 
int storeTargetTrack = 0;							// Store Target Track position
int storeStartTrack = 0;

//	Calibration Array
int sensorVal = digitalRead (3);					// enable Hall-effect Sensor on Pin 3
int arrayCalibrate[] = { 0, 0, 0, 0, 0 };		// Array to pass in calibration run results
boolean isTrackCalibration = false;

// Define custom characters
byte upArrow[8] = { B00000, B00100, B01110, B10101, B00100, B00100, B00100, B00000 };	 		// Up Arrow character
byte downArrow[8] = { B00000, B00100, B00100, B00100, B10101, B01110, B00100, B00000 };	 		// Down Arrow character
byte rightArrow[8] = { B00000, B00000, B00100, B00010, B11111, B00010, B00100, B00000 };		// Right Arrow character
byte leftArrow[8] = { B00000, B00000, B00100, B01000, B11111, B01000, B00100, B00000 };			// Left Arrow character
byte bothArrows[8] = { B00100, B01110, B10101, B00100, B00100, B10101, B01110, B00100 };		// Up & Down Arrow character
//byte upArrowScroll[8] = { B11111, B11011, B10001, B01010, B11011, B11011, B11011, B11111 };		// Up Arrow character
//byte downArrowScroll[8] = { B11111, B11011, B11011, B11011, B01010, B10001, B11011, B11111 };	// Down Arrow character

// Programming LCD Keys variables
boolean programmingMode = false;
boolean programmingModeMoveForward = true;			//If in programming mode, are we moving forward?
String str1, str2, str3, str4, str5, str6;			// used as String builders within voids

String txtRuns = String ("Runs = ");
//long programmingLastMoveMillis = 0;				//When was the last programming move done?
//const int programRotateDelay = 100;				//Delay between steps in ms while holding button

// KeyPad parameters
int lcd_key = 0;			// current LCD key
int adc_key_in = 0;			// read key press from analog(0)
int adc_key_prev = 0;		// previous key press
int key = -1;				// default key
int lastKey = 0;			// Saves key press
int readAnalog = 0;			// Read value from Analogue Pin 1
boolean newMenu = true;		// Is new menu
int runs = 0;				// Set Calibration runs 
int lastRun = 0;			// Stores no of runs

//States for the main menu
int checkMenu = 0;				//	Default menu value
int currentMenuItem = 0;		//	Current selected menu

// Parameters for Stepper
boolean isReleased = false;				//	isReleased tries to make sure the motor is not continuously released
long stepperLastMoveTime = 0;			//	time taken for last stepper move
int mainDiff = 0;
const int MOTOR_OVERSHOOT = 10;			//	the amount of overshoot/ lash correction when approaching from CCW
int overshootDestination = -1;			//	overshoot CCW to take up Stepper Backlash
const int releaseTimeout_ms = 2000;		//	reduced to 2 seconds for now
const int MOTOR_STEP_COUNT = 200 * 16;	//	number of steps for a full rotation

// Parameters for turntable move

boolean tableTargetHead = false;		//	
int tableTargetPosition = 0;
boolean newTargetLocation = false;
boolean inMotionToNewTarget = false;
boolean isTurntableHead = true;
boolean isTurntableHeadChanged = true;
int currentTrack = 1;
int newTrack = 1;
int newTargetLoc = -1;

//Servo Stuff
//Servo brakeservo;				// create servo object to control a servo
//const int servoBrake = 9;		// value for brake position
//const int servoRelease = 2;		// value for release position

//Do display rotation
//const int displayRotateDelay = 5;		// This is the minimum delay in ms between steps of the stepper motor
//boolean displayRotating = false;		// Is the "Display Rotate" function enabled?
boolean displayRotatingCW = true;		// In Display Rotate mode, are we rot
//int displayRotatingLastMoveMillis = 0;

// Programme Track Positions
int currentStepPosition = 0;	// current step number
boolean chkOverwrite = false;
//  START	----	AdaFruit Setup	-----

Adafruit_MotorShield AFMStop (0x60); // Default address, no jumpers
Adafruit_StepperMotor *mystepper = AFMStop.getStepper (200, 2);	 //Connect stepper with 200 steps per revolution (1.8 degree) to the M3, M4 terminals (blue,yellow,green,red)

//	you can change these to SINGLE, DOUBLE, INTERLEAVE or MICROSTEP!	 
//	wrapper for the motor!(3200 Microsteps / revolution)

void forwardstep () { mystepper->onestep (BACKWARD, MICROSTEP); }		// Move Stepper forward onestep
void backwardstep () { mystepper->onestep (FORWARD, MICROSTEP); }		// Move Stepper backwards onestep
void release () { mystepper->release (); }								// Release stepper motor to prevent contanst power usage and burnout
AccelStepper stepper = AccelStepper (forwardstep, backwardstep);		// wrap the stepper in an AccelStepper object

//  ----------- MAIN ARDUINO COMMANDS ----------

void setup ()
{
	// Create Custom Characters

	lcd.createChar (0, upArrow);
	lcd.createChar (1, downArrow);
	lcd.createChar (2, leftArrow);
	lcd.createChar (3, rightArrow);
	//	lcd.createChar (4, upArrowScroll);
	//	lcd.createChar (5, downArrowScroll);
	//	lcd.createChar (6, bothArrows);


	AFMStop.begin (); // Start the shield

	Serial.begin (9600);		// Start Console logging

	lcd.begin (16, 2);		// set up the LCD's number of columns and rows:

	pinMode (A0, INPUT);		// set input pin for buttons to A0
	//configure pin3 as an input and enable the internal pull-up resistor
	pinMode (3, INPUT_PULLUP);		//Hall Effect sensor: to reset position on startup

	stepper.setMaxSpeed (80.0);
	stepper.setAcceleration (10.0);



	DCC.SetBasicAccessoryDecoderPacketHandler (BasicAccDecoderPacket_Handler, true);
	ConfigureDecoder ();
	DCC.SetupDecoder (0x00, 0x00, kDCC_INTERRUPT);

	startupLCD ();
	newMenu = true;
	checkMenu = 0;

	//calibrateBridge();
}

void loop ()
{
	decideMenu ();
}

//	--------------------------------------------

//  START	----	HELPER VOIDS	-----

void decideMenu ()
{
	switch (checkMenu)
	{
		case 0:
			mainMenu ();
			break;
		case 1:
			manualMode ();
			break;
		case 2:
			quitManualTurntable ();
			break;
		case 3:
			calibrateBridge ();
			break;
		case 4:
			//mainMenu ();
			autoDCCMode ();
			break;
		case 5:
			selectProgrammedTargetTracks ();
			break;
		case 6:
			selectSaveTracks ();
			break;
		case 7:
			checkOverwriteTrack ();
			break;
		default:
			break;
	}
}

void keyPadState () //	Works out which key has been pressed by reading A0
{

	adc_key_prev = lcd_key;	// Looking for changes
	lcd_key = read_LCD_buttons ();	// read the buttons

	if (adc_key_in < 1020)
	{
		if (adc_key_prev != lcd_key)
		{
			//    Serial.print (adc_key_in);
			//    Serial.print ("	");
			//    Serial.print (lcd_key);
			//    Serial.println ();
			key = lcd_key;
		}
		else { key = 0; }
	}
}

void printToLCD (String rowA, String rowB)
{
	lcd.clear ();
	lcd.print (rowA);
	lcd.setCursor (0, 1);
	lcd.print (rowB);
	String lcdRowA = String ("");
	String lcdRowB = String ("");
}

void printToConsole (String rowA, String rowB)
{
	Serial.print (rowA);
	Serial.println ("");
	Serial.print (rowB);
	Serial.println ("");
	String lcdRowA = String ("");
	String lcdRowB = String ("");
}

void getTrackTail (int trackCurrent, int trackTarget)
{
	currentTrack = GetTrackTail[trackCurrent - 1];
	newTrack = GetTrackTail[trackTarget - 1];
	displayManualMove (newTrack);
}

void sortLowHigh (int a[], int size)
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

int read_LCD_buttons ()
{
	adc_key_in = analogRead (0);	// read the value from the sensor 
	delay (5); //switch debounce delay. Increase this delay if incorrect switch selections are returned.
	int k = (analogRead (0) - adc_key_in); //gives the button a slight range to allow for a little contact resistance noise
	if (5 < abs (k)) return 0;	// double checks the keypress. If the two readings are not equal +/-k value after debounce delay, it tries again.
	// my buttons when read are centered at these valies: 0, 144, 329, 504, 741
	// we add approx 50 to those values and check to see if we are close
	if (adc_key_in > 1000) return 0; // We make this the 1st option for speed reasons since it will be the most likely result
	if (adc_key_in < 50)	return 5;
	if (adc_key_in < 195)	return 3;
	if (adc_key_in < 380)	return 4;
	if (adc_key_in < 555)	return 2;
	if (adc_key_in < 790)	return 1;
	return 0;	// when all others fail, return this...
}

void scrollBarsUD ()
{
	/*	switch (key)
	//	{
	case 3:
	lcd.setCursor (15, 1);
	lcd.write (byte (1));
	lcd.setCursor (15, 0);
	lcd.write (byte (4));
	delay (500);
	lcd.setCursor (15, 0);
	lcd.write (byte (0));
	break;
	case 4:
	lcd.setCursor (15, 0);
	lcd.write (byte (0));
	lcd.write (byte (5));
	delay (500);
	lcd.setCursor (15, 1);
	lcd.write (byte (1));
	break;
	default:
	lcd.setCursor (15, 0);
	lcd.write (byte (0));
	lcd.setCursor (15, 1);
	lcd.write (byte (1));
	break;
	} */
}

void resetMenu (int cMenu, int sMenu)
{
	lcd.clear ();
	checkMenu = cMenu;
	if (sMenu > 0) { displayMenu (sMenu); }
}

//  -----------------------------------------

// START	----	EEPROM commands		-----

void EEPROMWritelong (int address, int value)
{
	if (isTrackCalibration) { address = 0; }
	else { address = address * 2; }

	int val1 = value / 100;
	int val2 = value % 100;

	for (int i = 0; i < 2; i++)
	{
		EEPROM.write (address, val1);
		EEPROM.write (address + 1, val2);
	}
}

void arrayWritelong (int address, int value)
{

	if (isTrackCalibration) { address = 0; }
	//else { address = address * 2; }

	int val1 = value / 100;
	int val2 = value % 100;

	//    String strA = String (val1);
	//    String strB = String (val2);

	int valueJoin = (val1 * 100) + val2;
	//    String strC = String (valueJoin);

	//    String strRowA = String (strA + " " + strB + " " + strC);
	PositionTrack[address] = valueJoin;

	//    String strRowB = String (address);

	//    printToConsole (strRowA, strRowB);

}

int EEPROMReadlong (int address)
{
	String EEPROMReadlongText;

	int valueJoin = 0;

	int val1 = EEPROM.read (address);
	int val2 = EEPROM.read (address + 1);
	valueJoin = (val1 * 100) + val2;

//	String strA = String (F ("Add 1 = "));
//	String strB = String (val1);
//	str1 = strA + strB;
//	String strC = String (F ("	Add 2 = "));
//	String strD = String (val2);
//	str2 = strC + strD;
//	String strE = String (F ("	EEPROM Step = "));
//	String strF = String (valueJoin, DEC);
//	str3 = strE + strF;
//
//
//	EEPROMReadlongText = str1 + str2 + str3;
//	Serial.println ("");
//	Serial.println (EEPROMReadlongText);
//	Serial.println ("");

	return valueJoin;
}

void readArrayEEPROM ()
{
	int c;
	//unsigned int arraySize = sizeof (PositionTrack);
		for ( int i = 0; i < 7; i++)
	{
		if (i == 0) { c = i; }
		else { c = i * 2; }

//		Serial.println (c, DEC);
//		
//		int d = EEPROMReadlong (c);
//		Serial.println (d, DEC);
		PositionTrack[i] = EEPROMReadlong (c);
		//PositionTrack[i] = PositionTrackDummy[i];
	}
}

void checkTrackPositions ()
{
	for (int a = 0; a < 7; a++)
	{
		int arrayReadVal = PositionTrack[a];
		Serial.println (arrayReadVal, DEC);
	}
}
//  -----------------------------------------

//  START	----	DCC Decoder Setup	-----

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

	for (int i = 0; i < ( int )(sizeof (gAddresses) / sizeof (gAddresses[0])); i++)
	{
		if (address == gAddresses[i].address)
		{

			// Serial.println ("");
			// Serial.print ("DCC addr: ");
			// Serial.print (address, DEC);
			// Serial.print ("	Head/Tail = (1/0) : ");
			// Serial.println (enable, DEC);

			//new stuff
			tableTargetHead = enable;
			tableTargetPosition = i;

			//New packet and we have a new target location, set the flag
			newTargetLocation = true;
			doStepperMove ();
		}
	}
}

//  -----------------------------------------	

//  START	----	Main LCD Menu	----

void startupLCD ()
{
	
	readArrayEEPROM (); 
	checkTrackPositions ();
	String startupLCDHeader = String (F ("Turntable Menu"));
	String startupLCDFooter = String (F ("Press any Key"));
	printToLCD (startupLCDHeader, startupLCDFooter);
	 // Read EEPROM track positions into Array to save
	//  printToConsole (startupLCDHeader, startupLCDFooter);
	//scrollBarsUD ();
}

void mainMenu ()
{
	keyPadState ();
	if (newMenu)
	{
		if (key > 0)
		{
			currentMenuItem = 0;
			displayMenu (currentMenuItem);
			newMenu = false;
			key = 0;
		}
	}
	else
	{
		if (currentMenuItem < 0) { currentMenuItem = 3; }	 //If we are out of bounds on the menu then reset it
		else if (currentMenuItem > 3) { currentMenuItem = 0; }	//If we are out of bounds on the menu then reset it	 

		if (key != lastKey)	//If we have changed Index, saves re-draws.
		{
			switch (key)
			{
				case 3: // Up
					currentMenuItem = currentMenuItem - 1;
					if (currentMenuItem < 0) { currentMenuItem = 3; }
					else if (currentMenuItem > 3) { currentMenuItem = 0; }
					displayMenu (currentMenuItem);
					break;

				case 4: // Down
					currentMenuItem = currentMenuItem + 1;
					if (currentMenuItem < 0) { currentMenuItem = 3; }
					else if (currentMenuItem > 3) { currentMenuItem = 0; }
					displayMenu (currentMenuItem);
					break;

				case 1: //If Selected
					selectMenu (currentMenuItem);
					break;
			}
		}

		lastKey = key;	//Save the last State to compare.				
		delay (50);	//Small delay
		newMenu = false;
	}
}

void displayMenu (int dispMenu)
{
	String diplayMenuHeader, diplayMenuFooter;

	diplayMenuHeader = String (F ("Turntable Menu"));

	switch (dispMenu)
	{
		case 0:
			diplayMenuFooter = String (F ("DCC Auto Mode"));
			break;
		case 1:
			diplayMenuFooter = String (F ("Manual Mode"));
			break;
		case 2:
			diplayMenuFooter = String (F ("Set Tracks"));
			break;
		case 3:
			diplayMenuFooter = String (F ("Calibrate Bridge"));
			break;
	}
	printToLCD (diplayMenuHeader, diplayMenuFooter);
	//	 //  printToConsole (diplayMenuHeader, diplayMenuFooter);
	//scrollBarsUD ();
}

void selectMenu (int selMenu)
{
	keyPadState ();

	String selectMenuHeader, selectMenuFooter;

	switch (selMenu)
	{
		case 0:
			selectMenuFooter = String (F ("DDC Mode enabled"));
			break;
		case 1:
			programmingMode = false;
			selectMenuHeader = String (F ("Manual Move"));
			selectMenuFooter = String ("");
			printToLCD (selectMenuHeader, selectMenuFooter);
			//  printToConsole (selectMenuHeader, selectMenuFooter);
			delay (500);
			lcd.clear ();
			lcd.setCursor (0, 0);
			lcd.write (byte (0));
			lcd.print (F (" Head    Tail "));
			lcd.setCursor (15, 0);
			lcd.write (byte (1));
			lcd.setCursor (0, 1);
			lcd.write (byte (2));
			lcd.print (F ("     Move     "));
			lcd.setCursor (15, 1);
			lcd.write (byte (3));
			delay (2000);

			lastKey = -2;
			newMenu = true;
			manualMode ();
			break;
		case 2:
			programmingMode = true;
			newMenu = true;
			programmingMode = false;
			selectMenuHeader = String (F ("Track Positions"));
			selectMenuFooter = String (F ("Move bridge"));
			printToLCD (selectMenuHeader, selectMenuFooter);
			selectProgrammedTargetTracks ();
			break;
		case 3:
			runs = 3;
			selectMenuHeader = String (F ("Set Calibration"));

			str2 = String (runs, DEC);
			selectMenuFooter = txtRuns + str2;
			printToLCD (selectMenuHeader, selectMenuFooter);
			lcd.setCursor (10, 2);
			lcd.write (byte (6));
			newMenu = true;
			calibrateBridge ();
			break;
	}
	//  printToConsole (selectMenuHeader, selectMenuFooter);
}

//  ------------------------------------

//  START -- Manually move turntable ----

void manualMode ()
{
	checkMenu = 1;
	if (newMenu)
	{
		key = -1;
		newMenu = false;
	}

	keyPadState ();
	if (programmingMode) {}
	else
	{
		if (key != lastKey)
		{
			switch (key)
			{
				case 2:
					newTrack = newTrack - 1;
					if (newTrack < 1) { newTrack = 6; }
					else if (newTrack > 6) { newTrack = 1; }
					displayRotatingCW = false;
					displayManualMove (newTrack);
					break;

				case 3:
					isTurntableHead = true;
					if (isTurntableHeadChanged != isTurntableHead) { getTrackTail (currentTrack, newTrack); }
					displayManualMove (newTrack);
					break;

				case 4:
					isTurntableHead = false;
					if (isTurntableHeadChanged != isTurntableHead) { getTrackTail (currentTrack, newTrack); }
					displayManualMove (newTrack);
					break;

				case 5:
					newTrack = newTrack + 1;
					if (newTrack < 1) { newTrack = 6; }
					else if (newTrack > 6) { newTrack = 1; }
					displayRotatingCW = true;
					displayManualMove (newTrack);
					break;

				case 1:
					storeStartTrack = currentTrack;
					storeTargetTrack = newTrack;
					newTargetLoc = newTrack;
					SetStepperTargetLocation (false);
					moveManualTurntableMain (newTrack);
					break;
			}
		}
	}
	lastKey = key;
	delay (50);
}

void autoDCCMode ()
{
	static int addr = 0;
	DCC.loop ();

	if (++addr >= ( int )(sizeof (gAddresses) / sizeof (gAddresses[0])))	  // Bump to next address to test
	{
		addr = 0;
	}
	stepperTimer ();	   //StepperAccel.Run() in this function
}

void displayManualMove (int dispMove) // passes across selected track 
{
	String displayManualMoveHeader, displayManualMoveFooter, str1;
	if (programmingMode) {}
	else
	{
		// Set track limits and direction Track 0 not allowed as reference point
		if (dispMove < 1) { dispMove = 6; }
		else if (dispMove > 6) { dispMove = 1; }

		if (isTurntableHead) { displayManualMoveHeader = String (F ("HEAD selected...")); }
		else { displayManualMoveHeader = String (F ("TAIL selected...")); }

		isTurntableHeadChanged = isTurntableHead;
		newTargetLoc = newTrack;
		SetStepperTargetLocation (false);
		//calcLeastSteps (currentTrack, dispMove);

		if (displayRotatingCW) { str1 = String (F ("CW: ")); }
		else { str1 = String (F ("CCW: ")); }

		str2 = String (currentTrack);
		str3 = String (F (" to "));
		str4 = String (dispMove);
		displayManualMoveFooter = str1 + str2 + str3 + str4;
	}

	printToLCD (displayManualMoveHeader, displayManualMoveFooter);
	//  printToConsole (displayManualMoveHeader, displayManualMoveFooter);
}

void moveManualTurntableMain (int manMove)
{
	String moveManualTurntableMainHeader, moveManualTurntableMainFooter;

	while (storeTargetTrack != currentTrack)
	{
			newTargetLocation = PositionTrack[manMove];
			//fakeTurnMove (currentTrack);
	}

	/// Current resharping position

	currentTrack = newTrack;
	str1 = String ("Reached Track ");
	str2 = String (currentTrack);
	moveManualTurntableMainHeader = str1 + str2;
	moveManualTurntableMainFooter = (""); //UP=Exit	L/R=New"); 
	lcd.setCursor (0, 1);
	lcd.write (byte (0));
	lcd.print (" =Exit New= ");
	lcd.setCursor (13, 1);
	lcd.write (byte (2));
	lcd.setCursor (14, 1);
	lcd.print ("|");
	lcd.setCursor (15, 1);
	lcd.write (byte (3));
	printToLCD (moveManualTurntableMainHeader, moveManualTurntableMainFooter);
	//  printToConsole (moveManualTurntableMainHeader, moveManualTurntableMainFooter);
	delay (50);
	newMenu = true;
	quitManualTurntable ();
}

void quitManualTurntable ()
{
	checkMenu = 2;
	keyPadState ();
	if (newMenu) { key = -1; }

	if (key != lastKey)
	{
		if (key == 3 || key == 4)
		{
			resetMenu (0, 1);
		}
		else if (key == 2 || key == 5)
		{
			manualMode ();
		}

		//Save the last State to compare.
	}

	lastKey = key;
	newMenu = false;
}

/*void calcLeastSteps (int trA, int trB)
{

	//int currentLoc = PositionTrack[trA];
	int newTargetLoc = PositionTrack[trB];
	int getMotorStepCount = (MOTOR_STEP_COUNT / 2);

	if (newTargetLoc > 0)
	{
		int currentLoc = stepper.currentPosition ();
		mainDiff = newTargetLoc - currentLoc;
		if (mainDiff > getMotorStepCount) { mainDiff = mainDiff - MOTOR_STEP_COUNT; }
		else if (mainDiff < -getMotorStepCount) { mainDiff = mainDiff + MOTOR_STEP_COUNT; }

		if (mainDiff < 0)
		{
			mainDiff -= MOTOR_OVERSHOOT;
			overshootDestination = MOTOR_OVERSHOOT;
		}

		if (mainDiff < 0) { displayRotatingCW = false; }
		else if (mainDiff > 0) { displayRotatingCW = true; }

		String textMove;
		if (displayRotatingCW) { textMove = "CW"; }
		else { textMove = "CCW"; }

		stepper.move (mainDiff);
		//        Serial.println("");
		//        Serial.print(printConsole);
	}
}

void fakeTurnMove (int fakeMove)
{
	String fakeTurnMoveHeader, fakeTurnMoveFooter;

	if (displayRotatingCW) { currentTrack = fakeMove + 1; }
	else { currentTrack = fakeMove - 1; }

	if (currentTrack < 1) { currentTrack = 6; }
	else if (currentTrack > 6) { currentTrack = 1; }

	str1 = String ("Moving: ");
	str2 = String (storeStartTrack);
	str3 = String (" to ");
	str4 = String (storeTargetTrack);

	fakeTurnMoveHeader = str1 + str2 + str3 + str4;
	delay (1000);

	str5 = String ("Track:");
	str6 = String (currentTrack);
	fakeTurnMoveFooter = str5 + str6;
	printToLCD (fakeTurnMoveHeader, fakeTurnMoveFooter);
	//  printToConsole (fakeTurnMoveHeader, fakeTurnMoveFooter);
}
*/
//  -------------------------------------

//  START	----  Select and Programme Turntable Tracks  ----


void selectProgrammedTargetTracks ()
{
	String lcdRowA, lcdRowB;
	checkMenu = 5;
	keyPadState ();

	switch (key)
	{
		case	3:			//If Up	== Large x 10 steps
			dispProgrammeTurntableSteps (+10);
			break;
		case 4: 		//If Down == Large x -10 steps
			dispProgrammeTurntableSteps (-10);
			break;
		case 2: 		//If Left	== -single steps
			dispProgrammeTurntableSteps (-1);
			break;
		case 5: 		//If Right == single steps
			dispProgrammeTurntableSteps (1);
			break;
		case 1:
			//dispProgrammeTurntableSteps(0);
			selectSaveTracks ();
			break;
	}
	//Save the last State to compare.
	lastKey = key;
	newMenu = false;
}

void dispProgrammeTurntableSteps (int fakeMultiple)
{
	String dispProgrammeTurntableStepsHeader, dispProgrammeTurntableStepsFooter;
	//    int totalSteps = MOTOR_STEP_COUNT;

	//    int totalPctSteps = 0;

	if (fakeMultiple != 0)
	{
		if (currentStepPosition < -MOTOR_STEP_COUNT || currentStepPosition > MOTOR_STEP_COUNT) { currentStepPosition = 0; }
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

		str3 = String (F ("Move: "));
		str4 = String (fakeMultiple);
		if (fakeMultiple == 1 || fakeMultiple == -1) { str5 = String (F (" step")); }
		else { str5 = String (F (" steps")); }

		dispProgrammeTurntableStepsFooter = str3 + str4 + str5;
	}
	else { dispProgrammeTurntableStepsFooter = String (""); }

	str1 = String (currentStepPosition, DEC);
	if (currentStepPosition == 1 || currentStepPosition == -1) { str2 = String (F (" step")); }
	else { str2 = String (F (" steps")); }

	dispProgrammeTurntableStepsHeader = str1 + str2;
	printToLCD (dispProgrammeTurntableStepsHeader, dispProgrammeTurntableStepsFooter);
	//  printToConsole (dispProgrammeTurntableStepsHeader, dispProgrammeTurntableStepsFooter);
}

void selectSaveTracks ()
{
	checkMenu = 6;
	String lcdRowA, lcdRowB, strD;
	String strA = String ("Position = ");
	String strB = String (currentStepPosition, DEC);
	String strC = String ("Save to Track ");
	lcdRowA = strA + strB;

	keyPadState ();

	if (newMenu)
	{
		key = -1; //	If come from different menu do nothing
		runs = 1;
		strD = String (runs);
		lcdRowB = strC + strD;
		printToLCD (lcdRowA, lcdRowB);
	}
	if (key != lastKey)	// Check key press is new  
	{
		switch (key)
		{
			case 3:			// Up	
				runs = runs + 1;
				if (runs < 1) { runs = 1; }
				else if (runs > 6) { runs = 6; }
				strD = String (runs);
				lcdRowB = strC + strD;
				printToLCD (lcdRowA, lcdRowB);
				break;
			case 4: 		// Down
				runs = runs - 1;
				if (runs < 1) { runs = 1; }
				else if (runs > 6) { runs = 6; }
				strD = String (runs);
				lcdRowB = strC + strD;
				printToLCD (lcdRowA, lcdRowB);
				break;
			case 1:	// Select
				saveProgrammingTracks ();
				// save currentStepPosition to selected track within array	!! NEW VOID !! 
				break;
			case 2:
				resetMenu (0, 2);
				break;
			case 5:
				resetMenu (0, 2);
				break;
			default:	// Nothing
				break;
		}

		// Correct Tracks between 1 and 6

		//  printToConsole (lcdRowA, lcdRowB);

	}

	lastKey = key;
	newMenu = false;
}

void saveProgrammingTracks ()
{
	boolean isStepTaken = false;
	boolean isTrackTaken = false;
	unsigned int c = 0;

	for (unsigned int i = 0; i < sizeof (PositionTrack); i++)
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
		// Serial.println ("chkOverwrite = true");
		// Serial.println ("newMenu = true");
		// Serial.println ("checkOverwriteTrack");
		checkMenu = 7;
		chkOverwrite = true;
		newMenu = true;
		checkOverwriteTrack ();
	}
	else
	{
		overwriteTrack ();
	}
}

void checkOverwriteTrack ()
{
	checkMenu = 7;

	keyPadState ();
	String checkOverwriteTrackHeader, checkOverwriteTrackFooter;

	checkOverwriteTrackHeader = String (F ("Track taken!"));
	str1 = String (F ("Overwrite?"));
	str2 = String (F ("Y n"));
	str3 = String (F ("y N"));

	if (newMenu)
	{
		key = -1;
		checkOverwriteTrackFooter = str1 + str3;
		printToLCD (checkOverwriteTrackHeader, checkOverwriteTrackFooter);
	}
	if (key != lastKey)
	{
		if (key == 3 || key == 4)
		{
			resetMenu (0, 2);
		}
		if (key == 2)
		{
			checkOverwriteTrackFooter = str1 + str2;
			printToLCD (checkOverwriteTrackHeader, checkOverwriteTrackFooter);
			chkOverwrite = true;
		}
		if (key == 5)
		{
			checkOverwriteTrackFooter = str1 + str3;
			printToLCD (checkOverwriteTrackHeader, checkOverwriteTrackFooter);
			chkOverwrite = false;
		}

		if (key == 1)
		{
			if (chkOverwrite)
			{
				// Serial.print ("Overwrite track");
				overwriteTrack ();
				resetMenu (0, 2);
			}
			else
			{
				// Serial.print ("Reset Menu");
				resetMenu (0, 2);
			}
		}
	}
	lastKey = key;
	newMenu = false;
}

void overwriteTrack ()
{
	String overwriteTrackHeader, overwriteTrackFooter;

	overwriteTrackHeader = String (F ("Saving new track"));
	str1 = String (F ("Track "));
	str2 = String (runs, DEC);
	str3 = String (F (" Pos "));
	str4 = String (currentStepPosition, DEC);

	overwriteTrackFooter = String (str1 + str2 + str3 + str4);

	printToLCD (overwriteTrackHeader, overwriteTrackFooter);
	//  printToConsole (overwriteTrackHeader, overwriteTrackFooter);

	delay (1500);

	arrayWritelong (runs, currentStepPosition);

	int readBack = PositionTrack[runs];
	// Serial.println (readBack);

	if (readBack == currentStepPosition)
	{
		overwriteTrackHeader = String (F ("*** FINISHED ***"));
		overwriteTrackFooter = String (F ("Saved new track "));
		EEPROMWritelong (runs, currentStepPosition);
		printToLCD (overwriteTrackHeader, overwriteTrackFooter);
		//  printToConsole (overwriteTrackHeader, overwriteTrackFooter);
		delay (1000);

	}
	else
	{
		overwriteTrackHeader = String (F ("***  FAILED  ***"));
		overwriteTrackFooter = String (F ("Track not saved!"));
		printToLCD (overwriteTrackHeader, overwriteTrackFooter);
		//  printToConsole (overwriteTrackHeader, overwriteTrackFooter);
		delay (1000);
		resetMenu (6, -1);
	}
}
//  ------------------------------------

//  START	----  Calibrate Bridge  ----

void calibrateBridge ()
{
	checkMenu = 3;

	if (newMenu) { key = -1; }

	keyPadState ();

	if (key != lastKey)
	{
		switch (key)
		{
			case 3:
				runs = runs + 1;
				displayCalibrationRuns (runs);
			case 4:
				runs = runs - 1;
				displayCalibrationRuns (runs);
			case 1:
				calibrateBridgeRun (runs);
			case 2:
				resetMenu (0, 3);
				break;
			case 5:
				resetMenu (0, 3);
				break;
		}
	}
	lastKey = key;
	lastRun = runs;
	newMenu = false;
}

void displayCalibrationRuns (int r)
{
	String displayCalibrationRunsHeader, displayCalibrationRunsFooter;

	if (r <= 0) { r = 1; }
	else if (r > 5) { r = 5; }

	displayCalibrationRunsHeader = String (F ("Set Calibration"));
	str2 = String (r);
	displayCalibrationRunsFooter = txtRuns + str2;
	printToLCD (displayCalibrationRunsHeader, displayCalibrationRunsFooter);
	//lcd.setCursor (10, 2);
	//lcd.write (byte (6));
	//  printToConsole (displayCalibrationRunsHeader, displayCalibrationRunsFooter);
}

void calibrateBridgeRun (int c)
{
	int calRuns = 0;
	for (int i = 0; i <= c - 1; i++)
	{
		// if near reference point move away
		sensorVal = digitalRead (7);
		while (sensorVal == LOW)
		{
			sensorVal = digitalRead (7);
			forwardstep ();
			delay (50);

		}

		if (c % 2 == 1)
		{
			// step forward to sensor index point
			while (sensorVal == HIGH)
			{
				sensorVal = digitalRead (3);
				forwardstep ();
				delay (50);
				arrayCalibrate[i] = sensorVal;
			}
		}
		else
		{// step backwards to sensor index point
			while (sensorVal == HIGH)
			{
				sensorVal = digitalRead (3);
				backwardstep ();
				delay (50);
				arrayCalibrate[i] = sensorVal;
			}
		}

		// Dummy calibration run function
		/*		int dummySensorValue = random (-5, 5);	// Random number between -5 and +5 to simulate ref point
				arrayCalibrate[i] = dummySensorValue;	// store dummy number in Array
				*/
		displayResultCalibration (calRuns, false);
		calRuns++;
	}

	displayResultCalibration (calRuns, true);
}

void displayResultCalibration (int nRuns, boolean isEnd)
{

	String displayResultCalibrationHeader, displayResultCalibrationFooter;

	if (isEnd)
	{
		sortLowHigh (arrayCalibrate, nRuns);			// Sort array by sensor reading size
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

		str1 = String (F ("Min ="));
		str2 = String (minRefValue, DEC);
		str3 = String (F (" Max ="));
		str4 = String (maxRefValue, DEC);
		displayResultCalibrationHeader = str1 + str2 + str3 + str4;
		str5 = String (F ("Step ave = "));
		str6 = String (aveRefValue, DEC);
		displayResultCalibrationFooter = str5 + str6;
		printToLCD (displayResultCalibrationHeader, displayResultCalibrationFooter);
		saveCalibrationStep (aveRefValue);
		resetMenu (0, -1);
		//  printToConsole (displayResultCalibrationHeader, displayResultCalibrationFooter); 
	}
	else
	{
		str1 = String (F ("Last Step = "));
		str2 = String (arrayCalibrate[nRuns - 1], DEC);
		String displayResultCalibrationHeader = str1 + str2;
		str3 = String (nRuns);
		String displayResultCalibrationFooter = txtRuns + str3;
		printToLCD (displayResultCalibrationHeader, displayResultCalibrationFooter);
		//  printToConsole (displayResultCalibrationHeader, displayResultCalibrationFooter);
	}

	delay (5000);
}

void saveCalibrationStep (int saveCalStep)
{
	isTrackCalibration = true;
	runs = 0;
	currentStepPosition = saveCalStep;
	overwriteTrack ();
}

//  ------------------------------------

//  START	----	Stepper Voids	----

void doStepperMove ()
{
	stepper.run ();	// Run the Stepper Motor			

	boolean isInMotion = (abs (stepper.distanceToGo ()) > 0);
	boolean newTargetSet = false;

	// If there is a new target location, set the target
	if (newTargetLocation)
	{
		//printToConsole ("Moving to New Target Location...", "");
		SetStepperTargetLocation (true);
		newTargetSet = true;
	}

	if (inMotionToNewTarget)
	{
		if ((!isInMotion) && (!newTargetSet))
		{
			/*            Serial.print ("Not Moving!	DtG: ");
						Serial.print (stepper.distanceToGo ());
						Serial.print (" TP: ");
						Serial.print (stepper.targetPosition ());
						Serial.print (" CP: ");
						Serial.print (stepper.currentPosition ());
						Serial.print (" S: ");
						Serial.print (stepper.speed ());
						Serial.println ();
						*/
		}
		//release the brake
		//brakeservo.write (servoRelease);
		//delay (5);
		inMotionToNewTarget = isInMotion;
	}
	else
	{
		if (programmingMode)

			if ((stepper.currentPosition () % MOTOR_STEP_COUNT) == 0)
			{
				//setCurrentPosition seems to always reset the position to 0, ignoring the parameter
				//    Serial.print ("Current location: ");
				// Serial.print (stepper.currentPosition ());
				// Serial.println (" % STEPCOUNT.	Why here?");
			}
	}
}
//stepper timer subroutine came from here.}

void SetStepperTargetLocation (boolean selectTrack)
{
	//	Takes the global variables: tableTargetHeadOrTail, and tableTargetPosition, and sets the stepper
	//	object moveTo() target position in steps-	inserts values back into "doStepperMove()"

	unsigned int getMotorStepCount = (MOTOR_STEP_COUNT / 2);

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
		int currentLoc = stepper.currentPosition ();
		int mainDiff = newTargetLoc - currentLoc;
		if (mainDiff > (getMotorStepCount)) { mainDiff = mainDiff - MOTOR_STEP_COUNT; }
		else if (mainDiff < (-getMotorStepCount)) { mainDiff = mainDiff + MOTOR_STEP_COUNT; }

		if (mainDiff < 0)
		{
			mainDiff -= MOTOR_OVERSHOOT;
			overshootDestination = MOTOR_OVERSHOOT;
		}

		if (mainDiff < 0) { displayRotatingCW = false; }
		else if (mainDiff > 0) { displayRotatingCW = true; }

		if (!selectTrack) { stepper.move (mainDiff); }

	}

	newTargetLocation = false;
}

void stepperTimer ()
{

	//	Stepper Timer sub routine this runs from the main loop. It also supports the release function.
	// Run the Stepper Motor //
	stepper.run ();

	boolean isInMotion = (abs (stepper.distanceToGo ()) > 0);
	//Check if we have any distance to move for release() timeout.	Can check the
	// buffered var isInMotion because we also check the other variables.
	if (isInMotion || programmingMode)
	{
		//We still have some distance to move, so reset the release timeout
		stepperLastMoveTime = millis ();
		isReleased = false;
	}
	else
	{
		if (!isReleased)
		{
			if (overshootDestination > 0)
			{
				stepper.move (overshootDestination);
				overshootDestination = -1;
			}
			if (((millis () - stepperLastMoveTime) >= releaseTimeout_ms))
			{
				//If isReleased, don't release again.
				isReleased = true;
				//Serial.print ("Relative Current Position: ");
				//Serial.print (stepper.currentPosition ());	//shows position the table thinks it is at (how it got here)

				int currentLoc = stepper.currentPosition ();	// Resets the positon to the actual positive number it should be
				currentLoc = currentLoc % MOTOR_STEP_COUNT;
				if (currentLoc < 0)
				{
					currentLoc += MOTOR_STEP_COUNT;
				}
				stepper.setCurrentPosition (currentLoc);
				stepper.moveTo (currentLoc);

				//Serial.print ("	Actual Current Position: ");
				//Serial.println (stepper.currentPosition ());	// shows the position value corrected.

				//Set the servo brake
				//brakeservo.write (servoBrake);
				//delay (750);

				//release the motor
				release ();
				//Serial.println ("	Brake Set & Motor Released ");
			}
		}
	}
}
//  ------------------------------------




