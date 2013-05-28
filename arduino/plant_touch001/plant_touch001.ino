#include <CapacitiveSensor.h>
#include "pitches.h"
#define CAP_PIN 4
#define PLANT_1 2

 
CapacitiveSensor plant = CapacitiveSensor(CAP_PIN, PLANT_1);//set up one plant
boolean debug = false; //debug mode
long touch_length;
long touch_timeout = 6000;
int t, last_t, diff, last_diff, last_noTouch;//last cap state
int change = 2;//value of cap change during check.
boolean bActive = false;
int touchCount=0;



void setup(){
 
  Serial.begin(57600); 
  
  
}

void loop() {
  
 long start = millis();
 long touch = plant.capacitiveSensor(50);
 
 if (plantTouch(touch) ) {
  //Serial.print("touch ");
  //Serial.println(touchCount);
 }
 
}


boolean plantTouch (long touch) {
  
  t = touch/1000;
  diff = t-last_t;
  
  
  if(debug){
    Serial.print(" ");
    Serial.print(diff);
    Serial.print(" ");   
    Serial.println(t);
    Serial.println("--------------------");    
  }
  last_t = t;
  last_diff = diff;
  
  Serial.println(diff);
  
  if (diff>=change) {
    if(!bActive){ //if its not active
      last_t = last_noTouch;
      touch_length = millis();
      bActive=true; 
      touchCount++;
    }
    return true;  
  }else{ 
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
