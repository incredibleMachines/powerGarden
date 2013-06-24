#include <Wire.h>
#include "TSL2561.h" //library for light sensor (with edits)
#include "DHT.h" //library for temp & humidity (with edits)

//connect humidity sensor to ananlog zero
//connect the light sensor to i2c, and the address pin to ground
//connect the temp/humidity sensor to digital in 2 (with a 10k to 5V)

//__________________________________________________________
// INIT
  boolean debug;
//light sensor (i2c)
  boolean enable_light_sensor;
  TSL2561 tsl(TSL2561_ADDR_LOW);  //address pin is low!
  long read_light_every, send_light_every;
  long read_light_timer, send_light_timer;
//moisture sensor (analog)
  boolean enable_moisture_sensor;
  long read_moisture_every, send_moisture_every;
  long read_moisture_timer, send_moisture_timer;
  int moisture_read_analog_pin;
  int moisture_val, moisture_total, moisture_avg;
  int moisture_counter;
//temphum sensor (d in)
  boolean enable_temphum_sensor;
  #define DHTPIN 2     //what pin we're connected to
  #define DHTTYPE DHT22   //DHT 22  (AM2302)
  DHT dht(DHTPIN, DHTTYPE);
  long read_temphum_every, send_temphum_every;
  long read_temphum_timer, send_temphum_timer;
  
  


void setup(void) {
  
  //__________________________________________________________
  // SETUP
  Serial.begin(9600);
  debug=true;
  enable_light_sensor = false;
  read_light_every = 2000;
  send_light_every = 10000;
  enable_moisture_sensor = false;
  moisture_counter = 1;
  moisture_read_analog_pin = 0; //analog 0
  read_moisture_every = 3000;
  send_moisture_every = 10000; 
  enable_temphum_sensor = true;
  read_temphum_every = 2000;
  send_temphum_every = 10000; 
  
  //__________________________________________________________
  //light sensor
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
  //moisture sensor
  //(no setup needed)
  
  //__________________________________________________________
  //humidity & temp sensor
  dht.begin();
}


void loop(void) {
  
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
      moisture_val = analogRead(moisture_read_analog_pin);
      moisture_total += moisture_val;
      if(debug==true){
        Serial.print("<- READING MOISTURE SENSOR ");
        Serial.print(moisture_counter);
        Serial.print("\t");
        Serial.print("VAL:");
        Serial.print(moisture_val);
        Serial.println();
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
        } 
        moisture_counter = 1;
        moisture_total = 0;
      }else{
        moisture_counter++;
      }
    }
  }


  //__________________________________________________________
  //light sensor
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
        }
        dht.counter = 1; //reset counter
      }else{
        dht.counter++; //increase counter
      }
    }
  }
  
  
  delay(100); 
  
}
