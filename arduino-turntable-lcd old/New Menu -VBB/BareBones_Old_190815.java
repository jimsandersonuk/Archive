		import muvium.compatibility.arduino.Servo; //<include <Servo.h>
		import muvium.compatibility.arduino.LiquidCrystal; //<include <LiquidCrystal.h>
		import muvium.compatibility.arduino.Arduino; 

public class BareBones extends Arduino //Automatically Added VBB Framework Code - do not remove
{ 

			// Definitions

			LiquidCrystal lcd = new LiquidCrystal(this, 12, 13, 5, 4, 3, 2);

			// Track Step Definitions

			//Define Corresponding Head and Tail
			static final int [] GetTrackTail = { 4, 5, 6, 1, 2, 3 };	// Array for Tail Tracks
			int [] PositionTrack = { 0, 710, 800, 890, 2310, 2400, 2490 };		// Save EEPROM addresses to array - index 0 = calibration position

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
			int checkMenu = 0;				// Default menu value
			int currentMenuItem = 0;	// Current selected menu
			int decideLoopLCDMenu = 0;

			//  ----------- MAIN ARDUINO COMMANDS ----------

			void setup()
			{
				Serial.begin(9600);		// Start Console logging
				lcd.begin(16, 2);		// set up the LCD's number of columns and rows:

				newMenu = true;
				startupLCD();
			}

			void loop()
			{
				decideMenu(decideLoopLCDMenu);

			}

			//	--------------------------------------------

			//  START	----	HELPER VOIDS	-----


	void keyPadState () //	Works out which key has been pressed by reading A0
	{

		adc_key_prev = lcd_key;	// Looking for changes
		lcd_key = read_LCD_buttons();	// read the buttons

		if (adc_key_in < 1020)
		{
			if (adc_key_prev != lcd_key)
			{
				Serial.print(adc_key_in);
				Serial.print("	");
				Serial.print(lcd_key);
				Serial.println();
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
		Serial.print(rowA);
		Serial.println("");
		Serial.print(rowB);
		Serial.println("");
		String lcdRowA = String("");
		String lcdRowB = String("");
	}
			void startupLCD ()
			{
				String startupLCDHeader = String("Turntable Menu");
				String startupLCDFooter = String("Press any Key");
				printToLCD(startupLCDHeader, startupLCDFooter);
				printToConsole(startupLCDHeader, startupLCDFooter);
				delay(2000);
				decideMenu(0);
			}

			void decideMenu (int inpSelMenu)
			{
			decideLoopLCDMenu = inpSelMenu;

				switch (decideLoopLCDMenu)
				{
					case 0:
						mainMenu (0,1,3);
						break;
					case 1:
						//manualMode ();
						break;
					case 2:
						//quitManualTurntable ();
						break;
					case 3:
						//calibrateBridge ();
						break;
					case 4:
						//mainMenu ();
						//autoDCCMode ();
						break;
					case 5:
						//selectProgrammedTargetTracks ();
						break;
					case 6:
						//selectSaveTracks ();
						break;
					case 7:
						//checkOverwriteTrack ();
						break;
					case 8:
						mainMenu (2, 3, 4);
						break;
				}
			}

			void mainMenu (int selMenu1, int selMenu2, int menuItems)
			{

			menuItems =	menuItems-1;

			keyPadState ();

				if (newMenu)
				{
					currentMenuItem = 0;
					chooseSubMenu (selMenu1);
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
							resetMenuList (menuItems);
							// if (currentMenuItem < 0) { currentMenuItem = menuItems; }
							// else if (currentMenuItem > menuItems) { currentMenuItem = 0; }
							chooseSubMenu (selMenu1);
							break;

						case 4: // Down
							currentMenuItem = currentMenuItem + 1;
							if (currentMenuItem < 0) { currentMenuItem = menuItems; }
							else if (currentMenuItem > menuItems) { currentMenuItem = 0; }
							chooseSubMenu (selMenu1);
							break;

						case 1: //If Selected
							chooseSubMenu (selMenu2);
							break;
						}
					}

				lastKey = key;	//Save the last State to compare.
				delay (50);	//Small delay
				newMenu = false;
				}
			}

			void chooseSubMenu (int subMenu)
			{
				switch (subMenu)
				{
				case 0:
					displayMainMenu (currentMenuItem);
					break;
				case 1:
					selectMainMenu (currentMenuItem);
					break;
				case 2:
					displaySubMenuA (currentMenuItem);
					break;
				case 3:
					selectSubMenuA (currentMenuItem);
					break;
				}
			}

			void resetMenuList (int menuItems)
			{
				if (currentMenuItem < 0) { currentMenuItem = menuItems; }
				else if (currentMenuItem > menuItems) { currentMenuItem = 0; }
			}

			//  START	----	Main LCD Menu	----

		void displayMainMenu (int dispMenu)
		{
			String displayMainMenuHeader, displayMainMenuFooter;
			decideLoopLCDMenu = 0;
			displayMainMenuHeader = String ("Turntable Menu");

			switch (dispMenu)
			{
				case 0:
					displayMainMenuFooter = String ("1 DCC Auto Mode");
					printToLCD(displayMainMenuHeader, displayMainMenuFooter);
					printToConsole(displayMainMenuHeader, displayMainMenuFooter);
				break;
				case 1:
					displayMainMenuFooter = String ("2 Manual Mode");
					printToLCD(displayMainMenuHeader, displayMainMenuFooter);
					printToConsole(displayMainMenuHeader, displayMainMenuFooter);
				break;
				case 2:
					displayMainMenuFooter = String("3 Setup");
					printToLCD(displayMainMenuHeader, displayMainMenuFooter);
					printToConsole(displayMainMenuHeader, displayMainMenuFooter);

				break;

			}
		}

		void selectMainMenu (int selMainMenu)
		{
			keyPadState ();
			decideLoopLCDMenu = 0;
			String selectMainMenuHeader, selectMainMenuFooter;

			switch (selMainMenu)
			{
				case 0:		//	Setup Tracks
					selectMainMenuHeader = String ("");
					selectMainMenuFooter = String ("DCC Mode enabled");
					printToLCD(selectMainMenuHeader, selectMainMenuFooter);
					printToConsole(selectMainMenuHeader, selectMainMenuFooter);
					break;
				case 1:
					selectMainMenuHeader = String ("Manual Move");
					selectMainMenuFooter = String ("");
					printToLCD(selectMainMenuHeader, selectMainMenuFooter);
					printToConsole(selectMainMenuHeader, selectMainMenuFooter);
				case 2:
					currentMenuItem = 0;
					chooseSubMenu(2);
					newMenu = true;
					break;
			}

		}

		//  START	---- LCD Menu - SETUP	----

		void displaySubMenuA (int dispMenu)
		{
			String displaySubMenuHeader, displaySubMenuFooter;
			decideLoopLCDMenu = 8;
			displaySubMenuHeader = String ("Setup Menu");

			switch (dispMenu)
			{
				case 0:
					displaySubMenuFooter = String ("1 Setup Tracks");
					printToLCD(displaySubMenuHeader, displaySubMenuFooter);
					printToConsole(displaySubMenuHeader, displaySubMenuFooter);
					break;
				case 1:
					displaySubMenuFooter = String ("2 Calibration");
					printToLCD(displaySubMenuHeader, displaySubMenuFooter);
					printToConsole(displaySubMenuHeader, displaySubMenuFooter);
					break;
				case 2:
					displaySubMenuFooter = String ("3 Clear Tracks");
					printToLCD(displaySubMenuHeader, displaySubMenuFooter);
					printToConsole(displaySubMenuHeader, displaySubMenuFooter);
					break;
				case 3:
					displaySubMenuFooter = String ("4 Quit");
					printToLCD(displaySubMenuHeader, displaySubMenuFooter);
					printToConsole(displaySubMenuHeader, displaySubMenuFooter);
					break;

			}
			//printToLCD (diplayMenuHeader, diplayMenuFooter);
			//printToConsole (diplayMenuHeader, diplayMenuFooter);

		}

		void selectSubMenuA (int selectSubMenu)
		{
			decideLoopLCDMenu = 8;

			keyPadState ();

			String selectSubMenuAHeader, selectSubMenuAFooter;

			switch (selectSubMenu)
			{
				case 0:		//	Setup Tracks
					selectSubMenuAHeader = String ("Track Positions");
					selectSubMenuAFooter = String ("Move bridge");
					printToLCD(selectSubMenuAHeader, selectSubMenuAFooter);
					printToConsole(selectSubMenuAHeader, selectSubMenuAFooter);
					break;
				case 1:
					selectSubMenuAHeader = String ("Set Calibration");
					printToLCD(selectSubMenuAHeader, selectSubMenuAFooter);
					printToConsole(selectSubMenuAHeader, selectSubMenuAFooter);	
				break;
				case 2:
					selectSubMenuAHeader = String ("Reset Memory");
					printToLCD(selectSubMenuAHeader, selectSubMenuAFooter);
					printToConsole(selectSubMenuAHeader, selectSubMenuAFooter);	
				break;
				case 3:
					delay (1000);
					currentMenuItem = 0;
					chooseSubMenu(0);
					newMenu = true;
					break;
			}

		}

}
