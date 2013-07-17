package com.incredibleMachines.powergarden;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Random;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class StateManager extends Activity {
	
	static String TAG = "StateManager";
	
	private String currentTweet;
	private String currentHandle;
	
	StateManager(){
		int i = 0;
	}
	
	public void updatePlantStates(){
		Log.d("StateManager", "updatePlantStates");
		int totalNumAllPlantTouches = 0;
		
		for(int i=0; i<PowerGarden.Device.PlantNum; i++){
			
			//--- compare last touched timestamp of this plant -- if it's been more than 3 min, then lower 
			Log.d(TAG, "curr touchWindow setting: "+ PowerGarden.Device.touchWindow);
			if(System.currentTimeMillis() - PowerGarden.Device.plants[i].touchedTimestamp > PowerGarden.Device.touchWindow*1000){ 
				if (PowerGarden.Device.plants[i].stateIndex > 0){
					PowerGarden.Device.plants[i].stateIndex =  PowerGarden.Device.plants[i].stateIndex - 1;
					
					PowerGarden.Device.plants[i].touchedTimestamp = System.currentTimeMillis();  //set timestamp to now, will take another changeDur to lower again
				}
				else{
					PowerGarden.Device.plants[i].stateIndex = 0; //stay at 0 if we're already at 0
				}
			}
	
			
			for(int j=0; j < PowerGarden.Device.plants[i].touchStamps.size(); j++){ //go through entre vector of all touch timestamps for this plant 
				
				//if any of them happened before changeDur (3 min?) then remove them from the vector
				if(System.currentTimeMillis() - (long) PowerGarden.Device.plants[i].touchStamps.elementAt(j) > PowerGarden.Device.touchWindow*1000 ){
					
					Log.d("touchStamps.removing: ", Long.toString(PowerGarden.Device.plants[i].touchStamps.elementAt(j)));
					PowerGarden.Device.plants[i].touchStamps.remove(j); //
					j--;
				}
			}
			
			int touchesThisPeriod = PowerGarden.Device.plants[i].touchStamps.size();
			
			totalNumAllPlantTouches += touchesThisPeriod; //running total of ALL TOUCHES IN THIS PERIOD
			
			Log.d("FINAL TOUCHES THIS PERIOD FOR "+Integer.toString(i)+": ", Integer.toString(touchesThisPeriod));
			
			int currentPlantState = 0;
			if (touchesThisPeriod < PowerGarden.Device.touchLowThresh)
				currentPlantState = 0; //lonely
			else if(touchesThisPeriod < PowerGarden.Device.touchHighThresh)
				currentPlantState = 1; //content
			else if(touchesThisPeriod >= PowerGarden.Device.touchHighThresh)
				currentPlantState = 2; //worked_up
				
			PowerGarden.Device.plants[i].state = PowerGarden.plantState[currentPlantState]; //set state String with index
		}
		PowerGarden.Device.totalNumPlantTouches = totalNumAllPlantTouches;
	}
	


	public static String getDeviceState(){
		String thisState = "null";

//		if(PowerGarden.Device.totalNumPlantTouches < PowerGarden.Device.touchLowThresh){
//			thisState = "lonely";
//			PowerGarden.deviceStateIndex = 0;
//		}
//		if(PowerGarden.Device.totalNumPlantTouches >= PowerGarden.Device.touchLowThresh){
//			thisState = "content";
//			PowerGarden.deviceStateIndex = 1;
//		}		
//		if(PowerGarden.Device.totalNumPlantTouches >= PowerGarden.Device.touchHighThresh){
//			thisState = "worked_up";
//			PowerGarden.deviceStateIndex = 2;
//		}
			
		//check distance and moisture if state:lonely AND range is inside thresh
//		if(thisState.equals("lonely") && PowerGarden.Device.distance < PowerGarden.Device.rangeLowThresh && PowerGarden.Device.rangeActive
//				&& System.currentTimeMillis() - PowerGarden.Device.lastRangeHitTime > 4000){
//			PowerGarden.Device.lastRangeHitTime = System.currentTimeMillis();
//			Log.wtf(TAG, "lonely AND range thresh hit -- TOUCH request Audio!");
//			PowerGarden.audioManager.playSound(-2); //TOUCH_REQUEST audio !
//			//PowerGarden.Device.rangeActive = false; //only play it once
//		}
//		
//		if(thisState.equals("worked_up") && PowerGarden.Device.moistureActive && PowerGarden.Device.moisture > PowerGarden.Device.moistureLowThresh){
//			Log.wtf(TAG, "worked_up AND moisture thresh hit -- WATER request Audio!");
//			PowerGarden.audioManager.playSound(-3); //WATER_REQUEST audio !
//		}
		
		if(Math.abs(PowerGarden.Device.distance - PowerGarden.Device.lastDistance) > 5 && PowerGarden.Device.rangeActive){
			
//			if(PowerGarden.Device.distance < 35
//						&& System.currentTimeMillis() - PowerGarden.Device.lastRangeHitTime > 4000 
//						&& PowerGarden.Device.distance != PowerGarden.Device.lastDistance){
//					PowerGarden.Device.lastDistance = PowerGarden.Device.distance;
//					PowerGarden.Device.deviceState ="content";
//					PowerGarden.deviceStateIndex = 1;
//					PowerGarden.audioManager.playSound(-11);
//					PowerGarden.Device.lastRangeHitTime = System.currentTimeMillis();
//				}
			
			if (PowerGarden.Device.distance > 75
					&& System.currentTimeMillis() - PowerGarden.Device.lastRangeHitTime > 4000 && 
					PowerGarden.Device.distance != PowerGarden.Device.lastDistance &&
					PowerGarden.Device.rangeActive){
					PowerGarden.Device.lastDistance = PowerGarden.Device.distance;
					PowerGarden.Device.deviceState ="content";
					PowerGarden.audioManager.playSound(-10);
					PowerGarden.Device.lastRangeHitTime = System.currentTimeMillis();
				} 
			
			
//			else if (Math.abs(PowerGarden.Device.distance - PowerGarden.Device.lastDistance) > 5 
//					&&	System.currentTimeMillis() - PowerGarden.Device.lastRangeHitTime > 4000){ //(PowerGarden.Device.distance > 75 && System.currentTimeMillis() - PowerGarden.Device.lastRangeHitTime > 4000){
//						PowerGarden.Device.lastDistance = PowerGarden.Device.distance;
//						PowerGarden.Device.deviceState ="lonely";
//						PowerGarden.deviceStateIndex = 0;
//						PowerGarden.audioManager.playSound(-10);
//						PowerGarden.Device.lastRangeHitTime = System.currentTimeMillis();
//			}
		}
			
		
		return thisState;
	}
	
	
	public static String getDeviceMoisture(){
		String currMoisture = "null";
		if (PowerGarden.Device.moisture <= PowerGarden.Device.moistureLowThresh){
			currMoisture = "low";
		}
		else if (PowerGarden.Device.moisture >= PowerGarden.Device.moistureHighThresh){
			currMoisture = "high";
		} else 
			currMoisture = "medium";
		return currMoisture;
	}

	public boolean updateDeviceState() { //check getDeviceState(), if it's new send an update
		
		if(PowerGarden.Device.deviceState.equals(PowerGarden.stateManager.getDeviceState())){ //are we still at the same device state?
			Log.wtf("current device state:", PowerGarden.Device.deviceState);
			return false;
			
		} else {	
			PowerGarden.Device.deviceState = PowerGarden.stateManager.getDeviceState(); //we have changed the device state !
			Log.wtf("updateDeviceState()", "NEW: "+PowerGarden.Device.deviceState);
//			Toast toast = Toast.makeText(getApplicationContext(), "NEW device state: "+PowerGarden.Device.deviceState, Toast.LENGTH_LONG);
//			toast.show();
			PowerGarden.SM.updateData("update", PowerGarden.Device.ID, null); //send an update to server about our state change
			return true; //true if there is a change to device state 
		}
	}
	
	public String updateCopy() {
		String thisCopy = "null";

		JSONArray thisPlantCopy = new JSONArray();
		
		
		try {
			String thisCopyType = PowerGarden.copyType[PowerGarden.deviceStateIndex];
			
			thisPlantCopy = PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType)
					.getJSONObject(thisCopyType).getJSONArray("stage_copy");
			

			int numLinesCopy = thisPlantCopy.length();
//			int[] orderArray = new int[numLinesCopy];
//
//			for(int i=0; i<numLinesCopy; i++){
//				orderArray[i] = i; //initialize array
//				shuffleArray(orderArray);
//			}
//			
//			for(int i=0; i<numLinesCopy; i++){
//				Log.d("Randomized Copy Index "+Integer.toString(i), (String) thisPlantCopy.get(i));
//			}
			
			thisCopy = thisPlantCopy.getString((int)(Math.random()*numLinesCopy));
		
		} catch (JSONException e) {

			e.printStackTrace();
		}
		
		return thisCopy;
	}


	  
	  
	public String getPlantState(int plantIndex) {
		String thisState = "null";
		thisState = PowerGarden.Device.plants[plantIndex].state;
		return thisState;
	}
	
	
	public void prepareTweet(){
		int index;
		currentTweet = PowerGarden.Device.tweetCopy.lastElement();
		index = PowerGarden.Device.tweetCopy.indexOf(currentTweet);
		currentHandle = PowerGarden.Device.tweetUsername.get(index);
	}
	
	public String getHandle(){ //always call prepareTweet first !
		String thisHandle;
		thisHandle = currentHandle;
		return thisHandle;
	}
	
	public String getTweet(){ //always call prepareTweet first !
		String thisTweet;
		thisTweet = currentTweet;
		return thisTweet;
	}
}

