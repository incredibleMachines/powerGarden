#include <CapacitiveSensor.h>
#include "plant.h"

#define NUM_PLANT 3
#define CAP_PIN 4
#define PLANT_1 2
#define PLANT_2 6
#define PLANT_3 8

int plant_pin[] = { 2, 6, 8};

Plant plant[3]; 

void setup(){
  
  for(int i = 0; i<NUM_PLANT; i++) plant[i].capSense = new CapacitiveSensor(CAP_PIN, plant_pin[i] );
  
  Serial.begin(57600); 
}

void loop() {
  

 for(int i = 0; i<NUM_PLANT; i++){
   plant[i].start = millis();
   plant[i].touch = plant[i].capSense->capacitiveSensor(30);
   plantTouch(&plant[i]);
   Serial.println(plant[i].diff);
 }
 
}


void plantTouch(Plant *t){
  
  t->diff = abs(t->touch - t->last_touch);
  t->last_touch = t->touch;
}

//boolean plantTouch (long touch) {
//  
//  t = touch/1000;
//  diff = t-last_t;
//  
//  
//  if(debug){
//    Serial.print(" ");
//    Serial.print(diff);
//    Serial.print(" ");   
//    Serial.println(t);
//    Serial.println("--------------------");    
//  }
//  last_t = t;
//  last_diff = diff;
//  
//  Serial.println(diff);
//  
//  if (diff>=change) {
//    if(!bActive){ //if its not active
//      last_t = last_noTouch;
//      touch_length = millis();
//      bActive=true; 
//      touchCount++;
//    }
//    return true;  
//  }else{ 
//    if(bActive && ( last_t > last_noTouch ) ) {
//      if(touch_length-millis()> touch_timeout){ 
//        return false;
//      }
//      touchCount++;
//      return true;
//    }
//    bActive=false;
//    return false;
//  }
//  
//
//  
//}


