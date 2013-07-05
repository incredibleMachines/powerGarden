#include <Wire.h> //for i2c
#include "TSL2561.h" //library for light sensor (with edits)
#include "DHT.h" //library for temp & humidity (with edits)
#include <Ping.h>
#include <avrpins.h>
#include <max3421e.h>
#include <usbhost.h>
#include <usb_ch9.h>
#include <Usb.h>
#include <usbhub.h>
#include <avr/pgmspace.h>
#include <address.h>

#include <adk.h>

USB Usb;
//USBHub     Hub(&Usb);

ADK adk(&Usb,"IncredibleMachines, Inc.",
"PowerGardenBoard",
"Arduino Terminal for Android",
"1.0",
"http://www.incrediblemachines.net",
"0000000000000001");

//connect moisture sensor to A0
//connect the light sensor to i2c, and the address pin to ground
//connect the temp/humidity sensor to digital in 2 (with a 10k to 5V)
//'Teensy_capsense_send_serial.ino' runs on the Teensy, sends serial to Arduino
//connect cap sensors to Teensy pins (0,1,15,16,17,18,19,22,23)
//touchRead() pins must be defined in the 'Teensy_capsense_send_serial.ino' file


//*******************
boolean debug = false;
//*******************

//__________________________________________________________
// INIT CAP SENSE 
#define NUMCAPS 8
boolean enable_cap_sensor;
int capRead[NUMCAPS];
int capDiff[NUMCAPS];
int capThreshold[NUMCAPS]; //read from pot
int capThresholdMap[NUMCAPS]; //map to range 100-4000
int capThresholdPrev[NUMCAPS]; 
long capTouchTimer[NUMCAPS] = {
  0,0,0,0,0,0,0,0};
int potPins[NUMCAPS] = {
  A8,A9,A10,A11,A12,A13,A14,A15};
int capTouchDebounce = 500; //set our debounce duration for plant trigger

//__________________________________________________________
// INIT OTHER SENSORS
//___light sensor (i2c)
boolean enable_light_sensor;
TSL2561 tsl(TSL2561_ADDR_LOW);  //address pin is low!
long read_light_every, send_light_every;
long read_light_timer, send_light_timer;
int lightVal;

//___moisture sensor (analog)
#define MOIST_PIN A0
boolean enable_moisture_sensor;
long read_moisture_every, send_moisture_every;
long read_moisture_timer, send_moisture_timer;
int moisture_val, moisture_avg;
int moisture_counter = 1;

//___temphum sensor (digital in)
boolean enable_temphum_sensor;
#define DHTPIN 2     //what pin we're connected to
//#define DHTTYPE DHT22   //DHT 22 SENSOR  (AM2302)
DHT dht;
//int dhtCounter = 0;
int tempCounter;
int humCounter;
int dhtTemp;
int dhtHum;
int dhtTempAvg;
int dhtHumAvg;
long read_temphum_every, send_temphum_every;
long read_temphum_timer, send_temphum_timer;

//___rangefinder (analog)
#define RANGE_PIN A1
boolean enable_rangefinder;
int rangefinderVal, rangefinderValPrev;
int rangefinderTriggerThresholdCm; //in cm units
long rangefinderTimer;
int rangerfinderDebounce;

//long millis();
long timeDataOut;
long send_out_every;

//OUTPUT TO ANDROID
char outgoing[64];
uint8_t rcode;

//// SENDING DATA/////////
/*
 D = ALL SENSOR DATA (NO CAP DATA)
 C = CAP DATA
 L = LIGHT DATA
 M = MOISTURE
 T = TEMP,HUMIDITY DATA
 R = RANGE
 */

// SETUP *********************************************************************
void setup(){

  Serial.begin(115200);
  Serial.println("CODE BEGIN");
  if(!debug){
    Serial.println("\r\nADK start");

    if (Usb.Init() == -1) {
      Serial.println("OSCOKIRQ failed to assert");
      while(1); //halt
    }//if (Usb.Init() == -1...
  }
  else{
    Serial.println("DEBUG START"); 
  }
  Serial2.begin(9600);

  //turn on/off sensors
  enable_light_sensor = true;
  enable_moisture_sensor = true;
  enable_temphum_sensor = true  ;
  enable_cap_sensor = true;
  enable_rangefinder = true;

  if(!enable_light_sensor){
    lightVal = -99;
  }
  if(!enable_moisture_sensor){
    moisture_avg = -99;
  }
  if(!enable_temphum_sensor){
    dhtTempAvg = -99;
    dhtHumAvg = -99;
  }
  if(!enable_light_sensor){
    rangefinderVal = -99;
  }

  //read/send durations
  read_light_every = 2000;
  send_light_every = 60000;
  read_moisture_every = 2000;
  send_moisture_every = 60000;  
  read_temphum_every = 2000;
  send_temphum_every = 60000;
  send_out_every = 45000;

  pinMode(13, OUTPUT);

  setupSensors();
}



// LOOP *********************************************************************
void loop(){

  //----------------------
  //ADK STUFF
  if(!debug){
    uint8_t msg[64] = { 
      0x00                                       };
    Usb.Task();

    if( adk.isReady() == false ) {
      return;
    }
    uint16_t len = 64;

    rcode = adk.RcvData(&len, msg);
    if(len > 0) {
      if(debug){
        Serial.println((char*)msg); 
      }
      if(strcasecmp((char * )msg,"setup") == 0){
        if(debug){
          Serial.println("GOT Setup");
        }
      }
    }
  }

  //  millis() = millis();
  //__________________________________________________________
  //cap sensors
  readCapSense();
  //if(debug)Serial.println("-- end readCapSense");

  //__________________________________________________________
  //--- light sensor
  if(millis() - read_light_timer > read_light_every){
    read_light_timer = millis();
    readLight(); 
  }
  //if(debug)Serial.println("-- end readLight");

  //__________________________________________________________
  //--- moisture sensor
  if(millis()-read_moisture_timer > read_moisture_every){ // READ
    read_moisture_timer = millis();
    readMoisture();
  }
  //if(debug)Serial.println("-- end readMoisture");
  //__________________________________________________________
  //temp-hum sensor
  if(enable_temphum_sensor == true){
    if(millis()-read_temphum_timer > read_temphum_every){ //READ
      read_temphum_timer = millis();
      readTempHum();
    }   
  }
  //if(debug)Serial.println("-- end readTempHum");
  //__________________________________________________________
  //rangefinder  
  if(enable_rangefinder == true){
    readRange();
    if(!debug) { 
      sprintf(outgoing, "R,%d",rangeAvg());
      rcode = adk.SndData( strlen( outgoing ), (uint8_t *)outgoing );
    }
  }
  //if(debug)Serial.println("-- end readRange");
  //----------------------------------------------------------
  // SEND AUX SENSOR DATA
  if(millis()-timeDataOut > send_out_every){
    //LIGHT, TEMP, HUM, MOIST, RANGE
    timeDataOut = millis();
    digitalWrite(13, HIGH);
    if(debug){

      Serial.println("--------------------");
      //      Serial.print(lightVal);
      //      Serial.print("\t");
      //      Serial.print(dhtTempAvg);
      //      Serial.print("\t");
      //      Serial.print(dhtHumAvg);
      //      Serial.print("\t");
      //      Serial.print(moisture_avg);
      //      Serial.print("\t");
      //      Serial.print(rangefinderVal);
      //      Serial.println();
      sprintf(outgoing, "D,%d,%d,%d,%d,%d",lightAvg(),tempAvg(),humAvg(),moistureAvg(),rangeAvg());
      Serial.println(outgoing);
      Serial.println("--------------------");
    } 
    else {
      sprintf(outgoing, "D,%d,%d,%d,%d,%d",lightAvg(),tempAvg(),humAvg(),moistureAvg(),rangeAvg());
      Serial.println(outgoing);
      rcode = adk.SndData( strlen( outgoing ), (uint8_t *)outgoing );
      Serial.println("--------------------");
    }
  } //else Serial.println("--------------------");
  delay(50); //this is IMPORTANT!
  digitalWrite(13, LOW);
}








