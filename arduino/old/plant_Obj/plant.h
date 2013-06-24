//class Plant
//{
//   public:
//     Plant(CapacitiveSensor* plant);
//     long touch,start,diff,last_touch;
//     int plantTouch();
//     
//     
//   private:
//     CapacitiveSensor* _plant;
//   
//};
#ifndef plant_H
#define plant_H

struct Plant{

  CapacitiveSensor * capSense;
  long touch,last_touch, diff, start;
  
  
};

#endif;
