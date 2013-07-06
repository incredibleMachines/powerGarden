package com.incredibleMachines.powergarden;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.TimerTask;

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
				
				int touchesThisPeriod = PowerGarden.Device.plants[i].touchStamps.size();
				Log.d("touchesThisPeriod: ", Integer.toString(touchesThisPeriod));
			}
			
			int touchesThisPeriod = PowerGarden.Device.plants[i].touchStamps.size();
			Log.d("FINAL TOUCHES THIS PERIOD FOR "+Integer.toString(i)+": ", Integer.toString(touchesThisPeriod));
			
			if (touchesThisPeriod < 3)
				PowerGarden.Device.plants[i].state = PowerGarden.plantState[0]; //lonely
			else if(touchesThisPeriod < 15)
				PowerGarden.Device.plants[i].state = PowerGarden.plantState[1]; //content
			else if(touchesThisPeriod >=15 )
				PowerGarden.Device.plants[i].state = PowerGarden.plantState[2]; //
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
}

