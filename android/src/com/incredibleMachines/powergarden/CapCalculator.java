package com.incredibleMachines.powergarden;

public class CapCalculator {
	
	static int FILTER_SIZE = 10;
	int runningVals[] = new int [FILTER_SIZE];
	int currIndex = 0;
	
	CapCalculator(){
		
		for (int i : runningVals) runningVals[i] = 0; //init array
	}
	
	public void addValue(int val){
		
		runningVals [ currIndex ]  = val;
		
		currIndex++;
		
		if(currIndex >= FILTER_SIZE){
			currIndex = 0;
		}
	}
	
	public int getDelta(){
		int thisDelta = 0;
		
		int diffs[] = new int[FILTER_SIZE-1];
		
		for (int i=0; i<runningVals.length-1; i++){
			diffs[i] =  Math.abs(runningVals[i] - runningVals[i+1]);
		}
		
		for (int i=0; i<diffs.length-1; i++){
			thisDelta += diffs[i];
		}
		
		thisDelta /= diffs.length-1;
		
		return thisDelta;
	}

}
