//tx - run on teensy 3


void setup(){
  Serial2.begin(9600); 
}


int counter = 0;
boolean flag = false;
void loop(){
  
  if(Serial2.read()==99){ //data was requested
    flag = true;
  }
  
  if(flag==true){
    Serial2.write(counter);
    counter++;
    flag = false;
  }
  
  
  delay(1000);
}
