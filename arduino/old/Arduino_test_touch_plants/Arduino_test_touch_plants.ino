void setup(){
  Serial.begin(9600); 
}

int prev_val1 = 0;
int prev_val2 = 0;

void loop(){

  //plant a
  int val1 = touchRead(23);
  int diff1 = abs(val1-prev_val1);  
  if(diff1>500){
    Serial.print("plant a: ");
    Serial.println(diff1);
  }
  prev_val1 = val1;
  
  //plant b
//  int val2 = touchRead(1);
//  int diff2 = abs(val2-prev_val2);
//  if(diff2>100){
//    Serial.print("plant b: ");
//    Serial.println(diff2);
//  }
//  prev_val2 = val2;
  
  delay(5);
}

