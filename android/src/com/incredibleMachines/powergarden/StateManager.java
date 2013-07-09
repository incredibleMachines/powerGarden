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
		
		for(int i=0; i<PowerGarden.Device.PlantNum; i++){
			
			//compare last touched timestamp of this plant -- if it's been more than 3 min, then lower 
			Log.d(TAG, "curr touchWindow setting: "+ PowerGarden.Device.touchWindow);
			if(System.currentTimeMillis() - PowerGarden.Device.plants[i].touchedTimestamp > PowerGarden.Device.touchWindow*10000){ 
				if (PowerGarden.Device.plants[i].stateIndex > 0){
					PowerGarden.Device.plants[i].stateIndex =  PowerGarden.Device.plants[i].stateIndex - 1;
					//set timestamp to now, will take another changeDur to lower again
					PowerGarden.Device.plants[i].touchedTimestamp = System.currentTimeMillis(); 
				}
				else
					PowerGarden.Device.plants[i].stateIndex = 0; //stay at 0 if we're already at 0
			}
	
			
			for(int j=0; j < PowerGarden.Device.plants[i].touchStamps.size(); j++){ //go through entre vector of touch timestamps for this plant 
				
				//if any of them happened before changeDur (3 min?) then remove them from the vector
				if(System.currentTimeMillis() - (long) PowerGarden.Device.plants[i].touchStamps.elementAt(j) > PowerGarden.plantStateChangeDur ){
					
					Log.d("touchStamps.removing: ", Long.toString(PowerGarden.Device.plants[i].touchStamps.elementAt(j)));
					PowerGarden.Device.plants[i].touchStamps.remove(j); //
					j--;
				}
			}
			
			int touchesThisPeriod = PowerGarden.Device.plants[i].touchStamps.size();
			Log.d("FINAL TOUCHES THIS PERIOD FOR "+Integer.toString(i)+": ", Integer.toString(touchesThisPeriod));
			
			if (touchesThisPeriod < PowerGarden.Device.touchLowThresh)
				PowerGarden.deviceStateIndex = 0; //lonely
			else if(touchesThisPeriod < PowerGarden.Device.touchHighThresh)
				PowerGarden.deviceStateIndex = 1; //content
			else if(touchesThisPeriod >= PowerGarden.Device.touchHighThresh)
				PowerGarden.deviceStateIndex = 2;; //worked_up
				
			PowerGarden.Device.plants[i].state = PowerGarden.plantState[PowerGarden.deviceStateIndex]; //set state String with index
		}
	}
	


	public static String getDeviceState(){
		String thisState = "null";
		int numLonely = 0;
		int numContent = 0;
		int numWorkedUp = 0;
		
		for(int i=0; i<PowerGarden.Device.PlantNum; i++){
			if (PowerGarden.Device.plants[i].state == PowerGarden.plantState[0])
				numLonely++;
			else if (PowerGarden.Device.plants[i].state== PowerGarden.plantState[1])
				numContent++;
			else if (PowerGarden.Device.plants[i].state == PowerGarden.plantState[2])
				numWorkedUp++;
		}
		
		Log.d(TAG, "numLonely: "+ Integer.toString(numLonely));
		Log.d(TAG, "numContent: "+ Integer.toString(numContent));
		Log.d(TAG, "numWorkedUp: "+ Integer.toString(numWorkedUp));
		
//		if(numLonely > numContent && numLonely > numWorkedUp) { 
//			thisState = "lonely";
//		}
//		else if(numWorkedUp > numContent && numWorkedUp > numLonely)
//			thisState = "worked_up";
//		else if(numContent > numWorkedUp && numContent > numLonely)
//			thisState = "content";
//		
//		if(numContent == numLonely) {
//			if(numContent > numWorkedUp) thisState = "content";
//			else thisState = "worked_up";
//		}
//			
//		else if(numContent == numWorkedUp) {
//			if(numContent > numLonely) thisState = "content";
//			else thisState = "lonely";
//		}
//			
//		else if(numWorkedUp == numLonely) {
//			if(numWorkedUp > numContent) thisState = "worked_up";
//			else thisState = "content";
//		}
		
		if(numContent > 0){
			thisState = "content";
		} 
		
		if(numContent > 2){
			thisState = "worked_up";
		}
		//check distance and moisture if state:lonely AND range is inside thresh
		if(thisState.equals(PowerGarden.plantState[0]) && PowerGarden.Device.distance < PowerGarden.Device.rangeLowThresh && PowerGarden.Device.rangeActive){
			Log.wtf(TAG, "lonely AND range thresh hit -- TOUCH request Audio!");
			PowerGarden.audioManager.playSound(-2); //TOUCH_REQUEST audio !
			PowerGarden.Device.rangeActive = false;
		}
		
		if(thisState.equals(PowerGarden.plantState[2]) && PowerGarden.Device.moistureActive && PowerGarden.Device.moisture > PowerGarden.Device.moistureLowThresh){
			Log.wtf(TAG, "worked_up AND moisture thresh hit -- WATER request Audio!");
			PowerGarden.audioManager.playSound(-3); //WATER_REQUEST audio !
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
		
		if(PowerGarden.Device.deviceState.equals(getDeviceState())){ //are we still at the same device state?
			Log.wtf("current device state:", PowerGarden.Device.deviceState);
			return false;
			
		} else {	
			PowerGarden.Device.deviceState = getDeviceState(); //we have changed the device state !
			Log.wtf("updateDeviceState()", "NEW: "+PowerGarden.Device.deviceState);
//			Toast toast = Toast.makeText(getApplicationContext(), "NEW device state: "+PowerGarden.Device.deviceState, Toast.LENGTH_LONG);
//			toast.show();
			PowerGarden.SM.updateData("update", PowerGarden.Device.ID, null);
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


	  
	  
	public int getPlantState(int plantIndex) {
		int thisState = 0;
		
		thisState = PowerGarden.Device.plants[plantIndex].stateIndex;
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

