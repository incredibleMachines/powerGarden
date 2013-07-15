package com.incredibleMachines.powergarden;

import java.lang.Character.UnicodeBlock;
import java.util.Vector;

public class PlantObject {
	
	MedianFilter MF = new MedianFilter();
	
	CapCalculator capCalculator = new CapCalculator();
	
	public boolean enabled = true;
	public boolean triggered = false;
    public int threshold = 10000;
    
    public String state = "lonely";
    public int stateIndex; //0 = lonely  1 = content  2 = worked_up
    
    public long touchedTimestamp = 0;
    
    //public long[] touchTimestamps = new long[20];
    
    Vector<Long> touchStamps = new Vector<Long>(); //holds timestamps of all touches last X mins
    
    
	//private static final String PREFS_NAME = "PowerGarden";
    
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


