

void setup(){
  
}


void loop(){
  //__________________________________________________________
  //cap sensors
  //1. read pot values, to adjust cap-sense threshold
  if(enable_cap_sensor==true){
    for(int i=0;i<3;i++){
      capThreshold[i] = analogRead(potPins[i]); 
      capThresholdMap[i] = map(capThreshold[i],0,1023,0,20000);
      if(abs(capThresholdPrev[i]-capThreshold[i])>2){ //pot value changed
        if(debug==true){
//          int plant_num = i+1;
//          Serial.print("Threshold for plant ");
//          Serial.print(plant_num);
//          Serial.print(" was set to ");
//          Serial.print(capThresholdMap[i]);
//          Serial.println();
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
//      if(diff > 0){
//        if(millis()-capTouchTimer[i] > capTouchDebounce){
//          capTouchTimer[i] = millis();
//          //////SEND TOUCH TO ANDROID
//          if(debug==true){
//            Serial.print("Plant ");
//            Serial.print(plant_num);
//            Serial.print(" was TOUCHED with a val of ");
//            Serial.print(capRead[i]);
//            Serial.print(" (diff = ");
//            Serial.print(diff);
//            Serial.println(")");
//          }
//          else{
//            //real serial write  
//          }
//        }
//      }
    }
    sprintf(outgoing, "C,%d,%d,%d,%d,%d,%d,%d,%d",capDiff[0],capDiff[1],capDiff[2],capDiff[3],capDiff[4],capDiff[5],capDiff[6],capDiff[7]);
     rcode = adk.SndData( strlen( outgoing ), (uint8_t *)outgoing );
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
      }
      else{
        //do nothing  
      }
      //char holder[6];
      //memset(outgoing,0,sizeof(outgoing));
      Serial.println("HERE");
      //sprintf(outgoing, "L,%d,%d,%d,%d",tsl.visible_light,tsl.fullspectrum_light,tsl.infrared_light,tsl.lux);
//      strcpy(outgoing,"L,");
//      strcat(outgoing, itoa(tsl.visible_light, holder, 10));
//      strcat(outgoing, ",");
//      strcat(outgoing, itoa(tsl.fullspectrum_light, holder, 10));
//      strcat(outgoing, ",");
//      strcat(outgoing, itoa(tsl.infrared_light, holder, 10));
//      strcat(outgoing, ",");
//      strcat(outgoing, itoa(tsl.lux, holder, 10));
      //const char* lite = "Light Reading";
      //Serial.print(outgoing);
      //Serial.print("\t");
     // rcode = adk.SndData( strlen(lite),(uint8_t *)lite);
      //Serial.print(rcode);
      //Serial.print("\t");
      //rcode = adk.SndData( strlen( outgoing ), (uint8_t *)outgoing );
      //Serial.println(rcode);
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
        else{
          //real serial write  
        }

        tsl.counter = 1; //reset counter
      }
      else{
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
      }
      else{
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
        }
        else{
          //real serial write 
        } 
        moisture_counter = 1;
        moisture_total = 0;
      }
      else{
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
      }
      else{
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
        }
        else{
          //real serial write  
        }
        dht.counter = 1; //reset counter
      }
      else{
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
        }
        else{
          //real serial write  
        } 
      } 
    }  
  }
  delay(50); //this is IMPORTANT!
}



