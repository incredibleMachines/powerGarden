int potVals[6];
int potValsPrev[6];
int potPins[6] = {2,3,4,5,6,7};

void setup(){
 
 Serial.begin(9600);
 
}

void loop(){
  
  for(int i=0;i<6;i++){
    int potVal = analogRead(potPins[i]);
    if(abs(potValsPrev[i]-potVal)>2){ //threshold of 2
      int p = i+1;
      Serial.print("POT ");
      Serial.print(p);
      Serial.print(": ");
      Serial.print(potVal);
      Serial.println();
      potValsPrev[i] = potVal;
    }
  
  }
  
}
