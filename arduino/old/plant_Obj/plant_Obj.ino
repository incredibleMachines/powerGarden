#include <CapacitiveSensor.h>
#include "plant.h"

#define NUM_PLANT 3
#define CAP_PIN 4
#define PLANT_1 2
#define PLANT_2 6
#define PLANT_3 8
#define TIMEOUT 30

int plant_pin[] = { 2, 6, 8};

Plant plant[3]; 

void setup(){
  
  for(int i = 0; i<NUM_PLANT; i++) {
                  plant[i].capSense = new CapacitiveSensor(CAP_PIN, plant_pin[i] );
                  delay(50);
                  plant[i].capSense->set_CS_AutocaL_Millis(0xFFFFFFFF);  
                  delay(50);
              }
  
  Serial.begin(57600); 
}

void loop() {
  

 for(int i = 0; i<NUM_PLANT; i++){
   plant[i].start = millis();
   plant[i].touch = plant[i].capSense->capacitiveSensor(TIMEOUT);
   plantTouch(&plant[i]);
   Serial.print("Plant");
   Serial.print(i);
   Serial.print("   ");
   Serial.print(plant[i].diff);
   Serial.print("\t");
 }
 Serial.println();
 
}


void plantTouch(Plant *t){
  
  t->diff = abs(t->touch - t->last_touch);
  t->diff/=1000;
  t->last_touch = t->touch;
}

