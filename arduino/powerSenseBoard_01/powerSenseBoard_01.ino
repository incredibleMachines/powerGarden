/*
Cosm Socket Example with Arduino ADK
 
 */

#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

#include <CapacitiveSensor.h>
#define CAP_PIN 4
#define PLANT_1 2

#define PLANT_2 8

CapacitiveSensor plant = CapacitiveSensor(CAP_PIN, PLANT_1);//set up one plant
CapacitiveSensor plant2 = CapacitiveSensor(CAP_PIN, PLANT_2);//set up one plant

boolean debug = false; //debug mode
long touch_length;
long touch_timeout = 6000;
int t, last_t, diff, last_diff, last_noTouch;//last cap state
int change = 2;//value of cap change during check.
boolean bActive = false;
int touchCount=0;

long touch_length2;
long touch_timeout2 = 6000;
int t2, last_t2, diff2, last_diff2, last_noTouch2;//last cap state
boolean bActive2 = false;
int touchCount2=0;

boolean haveInfoToSend = false;
byte infoToSend[5];
int numBytesToSend = 5;
boolean currLedStatus = false; //true==on

boolean getReading = false;

/* sensor vars */
#define ANALOG_SENSOR  A1

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
  
  plant.set_CS_AutocaL_Millis(0xFFFFFFFF);
  delay(50);
  plant2.set_CS_AutocaL_Millis(0xFFFFFFFF);

  Serial.println("...SETUP DONE");
}

void loop () {

  checkForAndroidComm();


  long start = millis();
  long touch = plant.capacitiveSensor(30);
  long touch2 = plant2.capacitiveSensor(30);

  if (plantTouch(touch) ) {

    //Serial.print("touch ");
    //Serial.println(touchCount);
  }
  
    if (plantTouch2(touch2) ) {

    //Serial.print("touch ");
    //Serial.println(touchCount);
  }
}

boolean plantTouch (long touch) {

  t = touch/1000;
  diff = t-last_t;

//  if(debug){
//    Serial.print(" ");
//    Serial.print(diff);
//    Serial.print(" ");   
//    Serial.println(t);
//    Serial.println("--------------------");    
//  }
  last_t = t;
  last_diff = diff;
  Serial.print("diff: ");
  Serial.println(diff);

  if (diff>=change) {
    if(!bActive){ //if its not active
      last_t = last_noTouch;
      touch_length = millis();
      bActive=true; 
      touchCount++;
    }
    return true;  
  }
  else{ 
    if(bActive && ( last_t > last_noTouch ) ) {
      if(touch_length-millis()> touch_timeout){ 
        return false;
      }
      touchCount++;
      return true;
    }
    bActive=false;
    return false;
  }
}


boolean plantTouch2 (long touch) {

  t2 = touch/1000;
  diff2 = t2-last_t2;

  if(debug){
    Serial.print(" ");
    Serial.print(diff);
    Serial.print(" ");   
    Serial.println(t);
    Serial.println("--------------------");    
  }
  last_t2 = t2;
  last_diff2 = diff2;
  
  Serial.print("diff2: ");
  Serial.println(diff2);

  if (diff2>=change) {
    if(!bActive2){ //if its not active
      last_t2 = last_noTouch2;
      touch_length2 = millis();
      bActive2=true; 
      touchCount2++;
    }
    return true;  
  }
  else{ 
    if(bActive2 && ( last_t2 > last_noTouch2 ) ) {
      if(touch_length2-millis()> touch_timeout2){ 
        return false;
      }
      touchCount2++;
      return true;
    }
    bActive2=false;
    return false;
  }
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







