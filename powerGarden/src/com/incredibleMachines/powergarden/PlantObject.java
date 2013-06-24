package com.incredibleMachines.powergarden;

public class PlantObject {
	MedianFilter MF = new MedianFilter();
	boolean triggered = false;
    long trig_time = 0;
    int threshold = 0;
    
    public void addValue(int value){
    	MF.medianFilterAddValue(value);
    }
    public int getFilteredValue(){
    	return MF.medianFilterGetMedian();
    }
    public int getThreshold(){
    	return threshold;
    }
    public int setThreshold(int thresh){
    	threshold = thresh;
    	return threshold;
    }
}
