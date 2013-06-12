#include <Wire.h>
#include "TSL2561.h"


//set address low
TSL2561 tsl(TSL2561_ADDR_LOW);  //pin is low!
boolean debug;
long read_light_every, send_light_every;
long read_light_timer, send_light_timer;


void setup(void) {
  
  //set your data
  debug=true;
  read_light_every = 2000;
  send_light_every = 10000;
  
  if(debug==true){
    Serial.println("DEBUG MODE"); 
  }
  
  Serial.begin(9600);
  
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


void loop(void) {
  
  //__________________________________________________________
  // LIGHT SENSOR
  if(millis()-read_light_timer > read_light_every){
    read_light_timer = millis();
    tsl.read_all_data();

    if(debug==true){
      Serial.print("<- READING ");
      Serial.print(counter);
      Serial.print("\t");
      Serial.print("VS:");
      Serial.print(visible_light);
      Serial.print("\t");
      Serial.print("FS:");
      Serial.print(fullspectrum_light);
      Serial.print("\t");
      Serial.print("IR:");
      Serial.print(infrared_light);
      Serial.print("\t");
      Serial.print("LUX:");
      Serial.print(lux);
      Serial.println();
    }  
  }
  if(millis()-send_light_timer > send_light_every){
    send_light_timer = millis();  
    tsl.get_all_avg();
  }

    

    
    if(millis()-timer_send > send_data_every){
      visible_light_avg = int(visible_light_total/counter);
      fullspectrum_light_avg = int(fullspectrum_light_total/counter);
      infrared_light_avg = int(infrared_light_total/counter);
      lux_avg = int(lux_total/counter);
      
      if(debug==true){
        Serial.print("-> SENDING (total readings, ");
        Serial.print(counter);
        Serial.print(")\t");
        Serial.print("VS:");
        Serial.print(visible_light_avg);
        Serial.print("\t");
        Serial.print("FS:");
        Serial.print(fullspectrum_light_avg);
        Serial.print("\t");
        Serial.print("IR:");
        Serial.print(infrared_light_avg);
        Serial.print("\t");
        Serial.print("LUX:");
        Serial.print(lux_avg);
        Serial.println();
      }
      
      //reset
      visible_light_total = fullspectrum_light_total = infrared_light_total = lux_total = 0;
      counter = 0; //this is set to '1' right away (below)
      timer_send = millis();
      
    }
    
    counter++; 
    timer_read = millis();
  }
  
  delay(100); 
  
}
