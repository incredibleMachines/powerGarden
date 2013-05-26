



void readSensors(){

//  int sensorVal;
//  sensorVal = analogRead( ANALOG_SENSOR );
  //byte msg = map(sensorVal, 0, 1023, 0, 254);
//  long start = millis();
//  long touch = plant.capacitiveSensor(50);

  //byte msg = map(diff, -10, 10, 0, 254);

  byte plant1 = abs(diff); 
  byte plant2 = abs(diff2);

  Serial.print("SENDING plant1: ");
  Serial.print(plant1);
  Serial.print("     plant2: ");
  Serial.println(plant2);
  
  numBytesToSend = 2;
  infoToSend[0] = plant1;
  infoToSend[1] = plant2;
  haveInfoToSend = true;
}


