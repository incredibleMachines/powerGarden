//RUN ON TEENSY!
//need teensyduino to upload to teensy board through arduino
//download teensyduino from pjrc.com


int touchVals[3];
int touchPins[3] = {0,1,15}; //SET CAP SENSE PINS 


void setup(){
  Serial2.begin(9600); 
}

boolean flag = false;
void loop(){
  
  if(Serial2.read()==99){ //data was requested
    flag = true;
  }
  
  if(flag==true){
    for(int i=0;i<3;i++){
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

