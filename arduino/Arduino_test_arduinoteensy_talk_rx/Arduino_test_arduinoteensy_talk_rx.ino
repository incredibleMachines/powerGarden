//rx - run on arduino mega


void setup(){
  Serial.begin(9600);
  Serial2.begin(9600); 
  Serial2.write(99); //request data
}

int in = 0;
void loop(){
  
  if(Serial2.available() > 0){
    in = Serial2.read();
    Serial.println(in);
    Serial2.write(99); //request more data
  }
  
}
