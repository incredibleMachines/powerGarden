



void readSensors(){

  if(plant[0].touch > 1200){
    infoToSend[0] = 1;
  } else infoToSend[0] = 0;
  
  if(plant[1].touch > 1200){
    infoToSend[1] = 1;
  } else infoToSend[1] = 0;

  if(plant[2].touch > 1200){
    infoToSend[2] = 1;
  } else infoToSend[2] = 0;  
  
  numBytesToSend = 3;
//  infoToSend[0] = plant[0].dataOut;
//  infoToSend[1] = plant[1].dataOut;
//  infoToSend[2] = plant[2].dataOut;
  haveInfoToSend = true;
}


