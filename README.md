powerGarden
===========

Naked Juice Power Garden - Firmware and Software

The android terminal app requires Google APIs 10 or higher
The app is designed to use this arduino lib - https://github.com/felis/USB_Host_Shield_2.0
	The Examples from that lib in the USB_Host_Shield_2.0 -> adk folder work
	Need to change one file in the Lib to work with the ADK board in avrpins.h
	/* Uncomment the following if you have Arduino Mega ADK board with MAX3421e built-in */
	#define BOARD_MEGA_ADK
