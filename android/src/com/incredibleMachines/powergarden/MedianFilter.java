

package com.incredibleMachines.powergarden;

class MedianFilter {
	
  int NUM_VALS_MEDIAN = 7;
  
  int HYSTERISIS_LOW = 20000;
  
  int [] vals = new int[NUM_VALS_MEDIAN];
  int [] valsSorted = new int[NUM_VALS_MEDIAN];
  boolean firstVal = true;
  
  void medianFilterBubbleSort() {
    int out, in;
    int swapper;
    for(out=0 ; out < NUM_VALS_MEDIAN; out++) {  // outer loop  
      for(in=out; in<NUM_VALS_MEDIAN-1; in++)  {  // inner loop
        if( valsSorted[in] > valsSorted[in+1] ) {   // out of order?
          // swap them:
          swapper = valsSorted[in];
          valsSorted [in] = valsSorted[in+1];
          valsSorted[in+1] = swapper;
        }
      }
    }
  }

  void medianFilterSetup(){
    firstVal = true;
  }

  void medianFilterAddValue(int val){
    if (firstVal == true){
      for (int i = 0; i < NUM_VALS_MEDIAN; i++) valsSorted[i] = val;
      for (int i = 0; i < NUM_VALS_MEDIAN; i++) vals[i] = val;
      firstVal = false;
    } 
    else {
      for (int i = 1; i < NUM_VALS_MEDIAN; i++) vals[i-1] = vals[i];
      vals[NUM_VALS_MEDIAN-1] = val;
      for (int i = 0; i < NUM_VALS_MEDIAN; i++) valsSorted[i] = vals[i];
      // sort array
      medianFilterBubbleSort();
    }
    firstVal = false;
  }
  
  int medianFilterGetMedian(){
	  int thisMedian = valsSorted[ (int)Math.floor(NUM_VALS_MEDIAN/2) ];
	  if (thisMedian < HYSTERISIS_LOW) HYSTERISIS_LOW = thisMedian;
     return thisMedian;
  }
  
};

// public int medianNoise(){
//	  int thisNoise;
//	  return thisNoise;
//}
