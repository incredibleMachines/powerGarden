/*

PowerGarden ADK Firmware

*/

#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#include <CapacitiveSensor.h>
#include "plant.h"

#define NUM_PLANT 3
#define CAP_PIN 4
#define PLANT_1 2
#define PLANT_2 6
#define PLANT_3 8
#define TIMEOUT 30

int plant_pin[] = { 2, 6, 8};
Plant plant[NUM_PLANT]; 

boolean haveInfoToSend = false;
byte infoToSend[5];
int numBytesToSend = 5;
boolean currLedStatus = false; //true==on

//boolean getReading = false;

AndroidAccessory acc("Joe Saavedra",  //developer/manufacturer
"Cosm Socket Example Board",          //model
"Cosm Socket Example Board",                //project
"1.0",                                //version
"http://jos.ph",                      //URL
"0000000012345678");                  //Serial

void setup() {
  // connect to the serial port
  Serial.begin(57600);  
  Serial.println("SETUP BEGIN...");
  initLeds();

  delay(500);   
  acc.powerOn();

  pinMode(A0, OUTPUT);
  digitalWrite(A0, HIGH);
  pinMode(A2, OUTPUT);
  digitalWrite(A2, LOW);
 
  setupPlants();
  Serial.println("...SETUP DONE");
}

void loop () {

  checkForAndroidComm();
  updatePlants();
}




void checkForAndroidComm(){

  byte msgIn[3];
  if (acc.isConnected()) {
    int len = acc.read(msgIn, sizeof(msgIn), 1);

    if (len > 0) {
      Serial.println("-----msgIn------");
      Serial.print((char)msgIn[0]);
      Serial.println((char)msgIn[1]);

      char command = (char)msgIn[1];

      Serial.println("command: ");
      Serial.println(command);
      Serial.println();
      ledsOff();

      switch(command) {
      case 'Y':
        Serial.println("received Y");
        readSensors();
        blinkLed();
        break;

      case 'S':
        Serial.println("received S"); //prepare for SLIDE sensor data
        break;

      }
    }
    else {
      if(haveInfoToSend){
        //redOn(); // turn on light
        blinkLed();
        acc.write(infoToSend, numBytesToSend);
        delay(10);
        haveInfoToSend = false;
        ledsOff();
      }
    }
  } 
  else {
    //solid green when not connected to board
    haveInfoToSend = false;
    redOn();
  }
}

void reset(){
  haveInfoToSend = false;
  ledsOff();
}







