

int lightAvg(){
  if(enable_light_sensor == true && tsl.counter > 0){
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
      lightVal = tsl.lux_avg;
      //sprintf(outgoing, "L,%d",tsl.lux_avg);
      //rcode = adk.SndData( strlen( outgoing ), (uint8_t *)outgoing );
    }
    tsl.counter = 0; //reset counter

    return tsl.lux_avg;
  } 
  else return -99;
}


//---------------------------------------
//moisture sensor
int moistureAvg(){
  if(enable_moisture_sensor == true && moisture_counter > 0){
    moisture_avg = moisture_avg/moisture_counter;

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
      //sprintf(outgoing, "M,%d",moisture_avg);
      //rcode = adk.SndData( strlen( outgoing ), (uint8_t *)outgoing );
    }  
    int thisAvg = moisture_avg;
    moisture_avg = 0;
    moisture_counter = 0;
//    moisture_total = 0;
    return thisAvg;
    //   moisture_avg = 0;
  } 
  else return -99;
}

//__________________________________________________________
//temp sensor
int tempAvg(){
  if(enable_temphum_sensor == true && tempCounter > 0){
    dhtTempAvg /= tempCounter;
    int thisAvg = dhtTempAvg;
    if(debug==true){
      Serial.print("-> SENDING TEMP SENSOR AVG (total readings, ");
      Serial.print(tempCounter);
      Serial.print(")\t");
      Serial.print("AVG:");
      Serial.print(dhtTempAvg);
      Serial.println();
    }
    else{
      
    }
    dhtTempAvg = 0;
    tempCounter = 0;
    return thisAvg;
  } 
  else return -99;
}


//hum sensor
int humAvg(){
  if(enable_temphum_sensor == true && humCounter > 0){
    dhtHumAvg /= humCounter;
    int thisAvg = dhtHumAvg;
    if(debug==true){
      Serial.print("-> SENDING HUM SENSOR AVG (total readings, ");
      Serial.print(humCounter);
      Serial.print(")\t");
      Serial.print("AVG:");
      Serial.print(dhtHumAvg);
      Serial.println();
    }
    else{
      
    }
    dhtHumAvg = 0;
    humCounter = 0;
    return thisAvg;
  } 
  else return -99;
}

//__________________________________________________________
// range finder
int rangeAvg(){
  if(enable_rangefinder == true){
    return rangefinderVal; 
    //no real averaging for now !
  }
  else return -99;
}



