

void readCapSense(){
  //1. read pot values, to adjust cap-sense threshold
  if(enable_cap_sensor==true){
    for(int i=0;i<NUMCAPS;i++){
      capThreshold[i] = analogRead(potPins[i]); 
      capThresholdMap[i] = map(capThreshold[i],0,1023,0,20000);
      if(abs(capThresholdPrev[i]-capThreshold[i])>2){ //pot value changed
        if(debug==true){
          int plant_num = i+1;
          Serial.print("Threshold for plant ");
          Serial.print(plant_num);
          Serial.print(" was set to ");
          Serial.print(capThresholdMap[i]);
          Serial.println();
        }
        else{
          //real serial write  
        }
        capThresholdPrev[i] = capThreshold[i];
      }
    }
    // 2. read cap-sense from Teensy
    //maybe we should flush the serial first - ensure there is no bogus data?
    //or at least do a check for bogus data, then flush?
    //bogus data would be a very large, or a negative number (usually)
    //Serial.println("hit readCapSense");
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
      msb = Serial2.read();
      lsb = Serial2.read();
      capRead[3] = word(msb,lsb);
      msb = Serial2.read();
      lsb = Serial2.read();
      capRead[4] = word(msb,lsb);
      msb = Serial2.read();
      lsb = Serial2.read();
      capRead[5] = word(msb,lsb);
      msb = Serial2.read();
      lsb = Serial2.read();
      capRead[6] = word(msb,lsb);
      msb = Serial2.read();
      lsb = Serial2.read();
      capRead[7] = word(msb,lsb);
      //    Serial.print(capRead[0]);
      //    Serial.print("|");
      //    Serial.print(capRead[1]);
      //    Serial.print("|");
      //    Serial.print(capRead[2]);
      //    Serial.print("|");
      //    Serial.println();
    }  

    // 3. touch trigger of plants
    for(int i=0;i<NUMCAPS;i++){
      int plant_num = i+1;
      capDiff[i] = abs(capRead[i] - capThresholdMap[i]);

      if(debug==true){
        
        //                    Serial.print("Plant ");
        //                   Serial.print(plant_num);
        //                    Serial.print(" was TOUCHED with a val of ");
        //                    Serial.print(capRead[i]);
        //                    Serial.println(")");
      }
      else{
        //real serial write  
        sprintf(outgoing, "C,%d,%d,%d,%d,%d,%d,%d,%d",capDiff[0],capDiff[1],capDiff[2],capDiff[3],capDiff[4],capDiff[5],capDiff[6],capDiff[7]);
        Serial.println(outgoing);
        Serial.println("------------------");
        rcode = adk.SndData( strlen( outgoing ), (uint8_t *)outgoing );
      }
    }
  } 
}

void readLight(){
  //light sensor
  if(enable_light_sensor == true){

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
    else{
      //do nothing  
    }
    tsl.counter++; //increase counter
  }
}

//-----

void readMoisture(){

  if(enable_moisture_sensor == true){

    moisture_val = analogRead(MOIST_PIN);
    moisture_avg += moisture_val;
    moisture_counter++;

    if(debug==true){
      Serial.print("<- READING MOISTURE SENSOR ");
      Serial.print(moisture_counter);
      Serial.print("\t");
      Serial.print("VAL:");
      Serial.print(moisture_val);
      Serial.println();
    } 
  }
  else{
    //not enabled, do nothing
  }
}




//__________________________________________________________
//temp-hum sensor
void readTempHum(){
  if(enable_temphum_sensor == true){

    //dht.read_all_data();
    dhtTemp = dht.getTemperature();
    dhtHum = dht.getHumidity();
    dhtTempAvg += dhtTemp;
    dhtHumAvg += dhtHum;
    if(debug==true){
      Serial.print("<- READING TEMPHUM SENSOR ");
      Serial.print("TEMP:");
      Serial.print(dhtTemp);
      Serial.print("\t");
      Serial.print("HUM:");
      Serial.print(dhtHum);
      Serial.println();
    }
    else{
      //do nothing 
    } 
    tempCounter++; //increase counter
    humCounter++;
  }
}

//__________________________________________________________
//range sensor
void readRange(){
  if(enable_rangefinder == true){
    rangefinderVal = analogRead(RANGE_PIN);
    if(debug==true){
      //      Serial.print("Rangefinder val ");
      //      Serial.print(rangefinderVal);
      //      Serial.print(" (cm)");
      //      Serial.println();
    }
    else{
      //nothing
    }
  }
}

void setupSensors(){
  //initial readings for cap sense threshold
  if(enable_cap_sensor==true){
    for(int i=0;i<NUMCAPS;i++){
      capThreshold[i] = analogRead(potPins[i]); 
      capThresholdMap[i] = map(capThreshold[i],0,1023,0,20000);
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
    } 
    else {
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
    delay(100);
    dht.setup(DHTPIN); // data pin 2
    delay(100);
  }

  //__________________________________________________________
  //rangefinder setup
  if(enable_rangefinder==true){  
    //rangerfinderDebounce = 1000; //one second debounce
    rangefinderTriggerThresholdCm = 0; //2cm min, 300cm max
  }  

  //__________________________________________________________
  //moisture sensor setup  
  //no setup required for moisture sensor (simple analog devices)
}



