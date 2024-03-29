#include <Wire.h> //for i2c
#include "TSL2561.h" //library for light sensor (with edits)
#include "DHT.h" //library for temp & humidity (with edits)
#include <Ping.h>


//connect moisture sensor to A0
//connect rangefinder to digital 7
//connect the light sensor to i2c, and the address pin to ground
//connect the temp/humidity sensor to digital in 2 (with a 10k to 5V)
//'Teensy_capsense_send_serial.ino' runs on the Teensy, sends serial to Arduino
//connect cap sensors to Teensy pins (0,1,15,16,17,18,19,22,23)
//touchRead() pins must be defined in the 'Teensy_capsense_send_serial.ino' file


//*******************
boolean debug = true;
//*******************


//__________________________________________________________
// INIT CAP SENSE 
//for three plants
boolean enable_cap_sensor;
int capRead[3];
int capThreshold[3]; //read from pot
int capThresholdMap[3]; //map to range 100-4000
int capThresholdPrev[3]; 
long capTouchTimer[3] = {0,0,0};
int potPins[3] = {2,3,4};
int capTouchDebounce = 500; //set our debounce duration for plant trigger


//__________________________________________________________
// INIT OTHER SENSORS
//___light sensor (i2c)
  boolean enable_light_sensor;
  TSL2561 tsl(TSL2561_ADDR_LOW);  //address pin is low!
  long read_light_every, send_light_every;
  long read_light_timer, send_light_timer;
//___moisture sensor (analog)
  boolean enable_moisture_sensor;
  long read_moisture_every, send_moisture_every;
  long read_moisture_timer, send_moisture_timer;
  int moisture_val, moisture_total, moisture_avg;
  int moisture_counter = 1;
//___temphum sensor (digital in)
  boolean enable_temphum_sensor;
  #define DHTPIN 2     //what pin we're connected to
  #define DHTTYPE DHT22   //DHT 22 SENSOR  (AM2302)
  DHT dht(DHTPIN, DHTTYPE);
  long read_temphum_every, send_temphum_every;
  long read_temphum_timer, send_temphum_timer;
//___rangefinder (analog)
  Ping ping = Ping(7); //digital pin 7
  boolean enable_rangefinder;
  int rangefinderVal, rangefinderValPrev;
  int rangefinderTriggerThresholdCm; //in cm units
  long rangefinderTimer;
  int rangerfinderDebounce;



// SETUP *********************************************************************
void setup(){
  
  Serial.begin(9600);
  Serial2.begin(9600);
  
  //turn on/off sensors
  enable_light_sensor = false;
  enable_moisture_sensor = false;
  enable_temphum_sensor = false;
  enable_cap_sensor = true;
  enable_rangefinder = false;
  
  //read/send durations
  read_light_every = 2000;
  send_light_every = 10000;
  read_moisture_every = 2000;
  send_moisture_every = 10000;  
  read_temphum_every = 2000;
  send_temphum_every = 10000; 
  
  //initial readings for cap sense threshold
  if(enable_cap_sensor==true){
    for(int i=0;i<3;i++){
      capThreshold[i] = analogRead(potPins[i]); 
      capThresholdMap[i] = map(capThreshold[i],0,1023,900,2600);
    }
  }

  //__________________________________________________________
  //light sensor setup  
  if(enable_light_sensor == true){
    if (tsl.begin()) {
      if(debug==true){
        Serial.println("FOUND THE LIGHT SENSOR");
        Serial.println();
      }
    } else {
      if(debug==true){
        Serial.println("NO LIGHT SENSOR FOUNT");
        Serial.println();
      }
      while (1);
    }
    //settings for light sensor
    tsl.setGain(TSL2561_GAIN_16X);    
    tsl.setTiming(TSL2561_INTEGRATIONTIME_13MS);  
  }
  
  //__________________________________________________________
  //humidity & temp sensor setup
  if(enable_temphum_sensor==true){  
    dht.begin();
  }

  //__________________________________________________________
  //rangefinder setup
  if(enable_rangefinder==true){  
    rangerfinderDebounce = 1000; //one second debounce
    rangefinderTriggerThresholdCm = 0; //2cm min, 300cm max
  }  

  //__________________________________________________________
  //moisture sensor setup  
  //no setup required for moisture sensor (simple analog devices)

  
}



// LOOP *********************************************************************
void loop(){
  
  
  //__________________________________________________________
  //cap sensors
  //1. read pot values, to adjust cap-sense threshold
  if(enable_cap_sensor==true){
    for(int i=0;i<3;i++){
      capThreshold[i] = analogRead(potPins[i]); 
      capThresholdMap[i] = map(capThreshold[i],0,1023,800,2500);
      if(abs(capThresholdPrev[i]-capThreshold[i])>2){ //pot value changed
        if(debug==true){
          int plant_num = i+1;
          Serial.print("Threshold for plant ");
          Serial.print(plant_num);
          Serial.print(" was set to ");
          Serial.print(capThresholdMap[i]);
          Serial.println();
        }else{
          //real serial write  
        }
        capThresholdPrev[i] = capThreshold[i];
      }
    }
    // 2. read cap-sense from Teensy
    //maybe we should flush the serial first - ensure there is no bogus data?
    //or at least do a check for bogus data, then flush?
    //bogus data would be a very large, or a negative number (usually)
    Serial2.write(99); //request data from teensy
    delay(1);
    unsigned char msb,lsb;
    if(Serial2.available() > 0){
      msb = Serial2.read();
      lsb = Serial2.read();
      capRead[0] = word(msb,lsb);
      msb = Serial2.read();
      lsb = Serial2.read();
      capRead[1] = word(msb,lsb);
      msb = Serial2.read();
      lsb = Serial2.read();
      capRead[2] = word(msb,lsb);
      //    Serial.print(capRead[0]);
      //    Serial.print("|");
      //    Serial.print(capRead[1]);
      //    Serial.print("|");
      //    Serial.print(capRead[2]);
      //    Serial.print("|");
      //    Serial.println();
    }  
    // 3. touch trigger of plants
    for(int i=0;i<3;i++){
      int plant_num = i+1;
      int diff = capRead[i] - capThresholdMap[i];
      if(diff > 0){
        if(millis()-capTouchTimer[i] > capTouchDebounce){
          capTouchTimer[i] = millis();
          if(debug==true){
            Serial.print("Plant ");
            Serial.print(plant_num);
            Serial.print(" was TOUCHED with a val of ");
            Serial.print(capRead[i]);
            Serial.print(" (diff = ");
            Serial.print(diff);
            Serial.println(")");
          }else{
            //real serial write  
          }
        }
      }
    }
  }
  
  
  //__________________________________________________________
  //light sensor
  if(enable_light_sensor == true){
    if(millis()-read_light_timer > read_light_every){ //READ
      read_light_timer = millis();
      tsl.read_all_data();
      if(debug==true){
        Serial.print("<- READING LIGHT SENSOR ");
        Serial.print(tsl.counter);
        Serial.print("\t");
        Serial.print("VS:");
        Serial.print(tsl.visible_light);
        Serial.print("\t");
        Serial.print("FS:");
        Serial.print(tsl.fullspectrum_light);
        Serial.print("\t");
        Serial.print("IR:");
        Serial.print(tsl.infrared_light);
        Serial.print("\t");
        Serial.print("LUX:");
        Serial.print(tsl.lux);
        Serial.println();
      }else{
        //do nothing  
      }
      if(millis()-send_light_timer > send_light_every){ //SEND
        send_light_timer = millis();  
        tsl.get_all_avg();
        if(debug==true){
          Serial.print("-> SENDING LIGHT SENSOR AVG (total readings, ");
          Serial.print(tsl.counter);
          Serial.print(")\t");
          Serial.print("VS:");
          Serial.print(tsl.visible_light_avg);
          Serial.print("\t");
          Serial.print("FS:");
          Serial.print(tsl.fullspectrum_light_avg);
          Serial.print("\t");
          Serial.print("IR:");
          Serial.print(tsl.infrared_light_avg);
          Serial.print("\t");
          Serial.print("LUX:");
          Serial.print(tsl.lux_avg);
          Serial.println();
        }else{
          //real serial write  
        }
        tsl.counter = 1; //reset counter
      }else{
        tsl.counter++; //increase counter
      }
    }
  }


  //__________________________________________________________
  //moisture sensor
  if(enable_moisture_sensor == true){
    if(millis()-read_moisture_timer > read_moisture_every){ // READ
      read_moisture_timer = millis();
      moisture_val = analogRead(A0);
      moisture_total += moisture_val;
      if(debug==true){
        Serial.print("<- READING MOISTURE SENSOR ");
        Serial.print(moisture_counter);
        Serial.print("\t");
        Serial.print("VAL:");
        Serial.print(moisture_val);
        Serial.println();
      }else{
        //do nothing 
      } 
      if(millis()-send_moisture_timer > send_moisture_every){ //SEND
        send_moisture_timer = millis();
        moisture_avg = int(moisture_total/moisture_counter);
        if(debug==true){
          Serial.print("-> SENDING MOISTURE SENSOR AVG (total readings, ");
          Serial.print(moisture_counter);
          Serial.print(")\t");
          Serial.print("AVG:");
          Serial.print(moisture_avg);
          Serial.println();
        }else{
          //real serial write 
        } 
        moisture_counter = 1;
        moisture_total = 0;
      }else{
        moisture_counter++;
      }
    }
  }


  //__________________________________________________________
  //temp-hum sensor
  if(enable_temphum_sensor == true){
    if(millis()-read_temphum_timer > read_temphum_every){ //READ
      read_temphum_timer = millis();
      dht.read_all_data();
      if(debug==true){
        Serial.print("<- READING TEMPHUM SENSOR ");
        Serial.print(dht.counter);
        Serial.print("\t");
        Serial.print("TEMP:");
        Serial.print(dht.temp_val);
        Serial.print("\t");
        Serial.print("HUM:");
        Serial.print(dht.humidity_val);
        Serial.println();
      }else{
        //do nothing 
      } 
      if(millis()-send_temphum_timer > send_temphum_every){ //SEND
        send_temphum_timer = millis();  
        dht.get_all_avg();
        if(debug==true){
          Serial.print("-> SENDING TEMPHUM SENSOR AVG (total readings, ");
          Serial.print(dht.counter);
          Serial.print(")\t");
          Serial.print("TEMP:");
          Serial.print(dht.temp_avg);
          Serial.print("\t");
          Serial.print("HUM:");
          Serial.print(dht.humidity_avg);
          Serial.println();
        }else{
          //real serial write  
        }
        dht.counter = 1; //reset counter
      }else{
        dht.counter++; //increase counter
      }
    }
  }

  //__________________________________________________________
  //rangefinder  
  if(enable_rangefinder == true){
    ping.fire();
    rangefinderVal = ping.centimeters();
    if(abs(rangefinderVal-rangefinderValPrev)>10){ //movement detected (threshold of 10cm)
      rangefinderValPrev = rangefinderVal;
      if(millis()-rangefinderTimer > rangerfinderDebounce){
        rangefinderTimer = millis();
        if(debug==true){
          Serial.print("Rangefinder val ");
          Serial.print(rangefinderVal);
          Serial.print(" (cm)");
          Serial.println();
        }else{
          //real serial write  
        } 
      } 
    }  
  }


  
  delay(50); //this is IMPORTANT!


  
}

