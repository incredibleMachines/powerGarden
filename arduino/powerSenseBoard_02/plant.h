
#ifndef plant_H
#define plant_H

struct Plant{

  CapacitiveSensor * capSense;
  long touch,last_touch, diff, start;
  int dataOut;
  
};

#endif;
