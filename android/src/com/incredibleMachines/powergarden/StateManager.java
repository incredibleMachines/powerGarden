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

public class StateManager extends Activity {
	
	
	StateManager(){
		int i = 0;
	}
	
	public void updatePlantStates(){
		Log.d("StateManager", "updatePlantStates");
		
		for(int i=0; i<PowerGarden.Device.PlantNum; i++){
			
			//compare last touched timestamp of this plant -- if it's been more than 3 min, then lower 
			if(System.currentTimeMillis() - PowerGarden.Device.plants[i].touchedTimestamp > PowerGarden.plantStateChangeDur){
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
			
			if (touchesThisPeriod < 2)
				PowerGarden.deviceStateIndex = 0; //lonely
			else if(touchesThisPeriod < 10)
				PowerGarden.deviceStateIndex = 1; //content
			else if(touchesThisPeriod >=10 )
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
			if (PowerGarden.Device.plants[i].state.contains("lonely"))
				numLonely++;
			else if (PowerGarden.Device.plants[i].state.contains("content"))
				numContent++;
			else if (PowerGarden.Device.plants[i].state.contains("worked_up"))
				numWorkedUp++;
		}
		
		if(numLonely > numContent && numLonely > numWorkedUp)
			thisState = "lonely";
		else if(numWorkedUp > numContent && numWorkedUp > numLonely)
			thisState = "worked_up";
		else if(numContent > numWorkedUp && numContent > numLonely)
			thisState = "content";
		
		if(numContent == numLonely) {
			if(numContent > numWorkedUp) thisState = "content";
			else thisState = "worked_up";
		}
			
		else if(numContent == numWorkedUp) {
			if(numContent > numLonely) thisState = "content";
			else thisState = "lonely";
		}
			
		else if(numWorkedUp == numLonely) {
			if(numWorkedUp > numContent) thisState = "worked_up";
			else thisState = "content";
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

	public boolean updateDeviceState() {
		
		if(PowerGarden.Device.deviceState.contains(PowerGarden.stateManager.getDeviceState())){
			Log.d("updateDeviceState()", "current device state = "+PowerGarden.Device.deviceState);
			return false;
		} else {
			PowerGarden.Device.deviceState = PowerGarden.stateManager.getDeviceState();
			Log.d("updateDeviceState()", "current device state = "+PowerGarden.Device.deviceState);
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
			
			thisCopy = thisPlantCopy.getString((int)Math.random()*numLinesCopy);
		
		} catch (JSONException e) {

			e.printStackTrace();
		}
		
		return thisCopy;
	}
	
	public int getNumAudioSamples(){
		int numSamples = 0;
		try {
			
			
			//int index = 0;
			for(int i=0; i<6; i++){
				if(i==0) {
					for (int j=0; j<PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").length(); j++){
						PowerGarden.plantAudio_touchRequest.put(PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").get(j));
						numSamples++;
					}
				}
				else if(i==1){
					for (int j=0; j<PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").length(); j++){
						PowerGarden.plantAudio_touchResponseGood.put(PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").get(j));
						numSamples++;
					}
				}
				else if(i==2){
					for (int j=0; j<PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").length(); j++){
						PowerGarden.plantAudio_touchResponseBad.put(PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").get(j));
						numSamples++;
					}
				}
				else if(i==3){
					for (int j=0; j<PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").length(); j++){
						PowerGarden.plantAudio_waterRequest.put(PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").get(j));
						numSamples++;
					}
				}
				else if(i==4){
					for (int j=0; j<PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").length(); j++){
						PowerGarden.plantAudio_waterResponseGood.put(PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").get(j));
						numSamples++;
					}
				}
				else if(i==5){
					for (int j=0; j<PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").length(); j++){
						PowerGarden.plantAudio_waterResponseBad.put(PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").get(j));
						numSamples++;
					}
				}
			}
			
			Log.d("total audio samples: ", Integer.toString(numSamples));
			
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return numSamples;
	}
	
	  // Implementing FisherÐYates shuffle
	  static void shuffleArray(int[] ar)
	  {
	    Random rnd = new Random();
	    for (int i = ar.length - 1; i >= 0; i--)
	    {
	      int index = rnd.nextInt(i + 1);
	      // Simple swap
	      int a = ar[index];
	      ar[index] = ar[i];
	      ar[i] = a;
	    }
	  }

	  
	  
	public int getPlantState(int plantIndex) {
		int thisState = 0;
		
		thisState = PowerGarden.Device.plants[plantIndex].stateIndex;
		return thisState;
	}
}

