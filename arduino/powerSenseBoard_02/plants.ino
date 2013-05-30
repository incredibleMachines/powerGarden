


void setupPlants(){
    for(int i = 0; i<NUM_PLANT; i++) {
        plant[i].capSense = new CapacitiveSensor(CAP_PIN, plant_pin[i] );
        delay(50);
        plant[i].capSense->set_CS_AutocaL_Millis(0xFFFFFFFF);  
        delay(50);
    }
}

void updatePlants(){
  
   for(int i = 0; i<NUM_PLANT; i++){
   plant[i].start = millis();
   plant[i].touch = plant[i].capSense->capacitiveSensor(TIMEOUT);
   plantTouch(&plant[i]);
   Serial.print("Plant");
   Serial.print(i);
   Serial.print("   ");
   Serial.print(plant[i].touch);
   Serial.print("\t");
 }
 Serial.println();
}

void plantTouch(Plant *t){
  
  t->diff = abs(t->touch - t->last_touch);
  t->diff/=1000;
  t->dataOut = t->diff;
  t->last_touch = t->touch;
}

