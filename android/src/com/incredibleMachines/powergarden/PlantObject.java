package com.incredibleMachines.powergarden;

public class PlantObject {
	MedianFilter MF = new MedianFilter();
	public boolean triggered = false;
    public long trig_timestamp = 0;
    public int threshold = 10000;
    public int mood;
	private static final String PREFS_NAME = "PowerGarden";
    
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
