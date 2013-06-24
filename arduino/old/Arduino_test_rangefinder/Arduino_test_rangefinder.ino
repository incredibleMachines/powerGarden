
const int analogInPin = A0;  // Analog input pin that the potentiometer is attached to

int sensorValue = 0;        // value read from the pot

void setup() {
  // initialize serial communications at 9600 bps:
  Serial.begin(9600); 
}

void loop() {
  sensorValue = analogRead(analogInPin);            
  Serial.print("sensor = " );                       
  Serial.print(sensorValue);    
  Serial.println();  
  delay(2);                     
}
