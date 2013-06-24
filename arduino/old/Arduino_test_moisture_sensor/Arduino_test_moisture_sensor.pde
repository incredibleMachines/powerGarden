

int moisture_reading;

void setup(){
  Serial.begin(9600);
}

void loop(){
  moisture_reading = analogRead(A0);
  Serial.println(moisture_reading);
  delay(1000);
}
