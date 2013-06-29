//RUN ON TEENSY!
//need teensyduino to upload to teensy board through arduino
//download teensyduino from pjrc.com

#define NUMCAPS 8
int touchVals[NUMCAPS];
int touchPins[NUMCAPS] = {0,1,15,16,17,18,19,22}; //SET CAP SENSE PINS 


void setup(){
  Serial2.begin(9600); 
  pinMode(13,OUTPUT);
  digitalWrite(13,HIGH);
}

boolean flag = false;
void loop(){
  
  if(Serial2.read()==99){ //data was requested0
    flag = true;
  }
  
  if(flag==true){
    for(int i=0;i<NUMCAPS;i++){
      touchVals[i] = touchRead(touchPins[i]);
      //http://www.instructables.com/id/Sending-a-multi-byte-integer-to-Arduinos-serial/
      unsigned char LSB = touchVals[i] & 0xff;
      unsigned char MSB = (touchVals[i] >> 8) & 0xff;
      Serial2.write(MSB); //msb
      Serial2.write(LSB); //lsb
    }
    flag = false;
  }
  
}
