



void readSensors(){

  
  numBytesToSend = 3;
  infoToSend[0] = plant[0].diff;
  infoToSend[1] = plant[1].diff;
  infoToSend[2] = plant[2].diff;
  haveInfoToSend = true;
}


