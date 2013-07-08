package com.incredibleMachines.powergarden;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Random;
import java.util.TimerTask;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class AudioManager extends Activity {
	
	String TAG = "AudioManager";
	
	private int numSamples = 0;
	
	AudioManager(){
		
	}
	
	public void setupAudio(){ //parses out all audio file names for this plant type from the dialog.json file
		
		try {
			
			//int index = 0;
			for(int i=0; i<6; i++){ //6 types of audio
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
	}
	
	
	public int getNumAudioSamples() {
		int nSamples = numSamples;
		return nSamples;
	}
	
	
	public void playSound(int plantIndex){
		Log.d(TAG, "playAudio HIT");
		//PowerGarden.stateManager.getNumAudioSamples();
		if (PowerGarden.audioLoaded && plantIndex > -1) {
			int state = PowerGarden.stateManager.getPlantState(plantIndex);
			Log.d("playAudio", "state: "+Integer.toString(state));
			//.play(audio[i], leftVol, rightVol, priority, loop, rate);
			if(state == 0){
	        	PowerGarden.soundPool.play(PowerGarden.touchRequestAudio.get((int)(Math.random()*PowerGarden.touchRequestAudio.size())), 1.0f, 1.0f, 1, 0, 1f); 
			}
			if(state == 1){
				PowerGarden.soundPool.play(PowerGarden.touchResponseGoodAudio.get((int)(Math.random()*PowerGarden.touchResponseGoodAudio.size())), 1.0f, 1.0f, 1, 0, 1f); 
			}
			if(state == 2){
				PowerGarden.soundPool.play(PowerGarden.touchResponseBadAudio.get((int)(Math.random()*PowerGarden.touchResponseBadAudio.size())), 1.0f, 1.0f, 1, 0, 1f); 
			}
			if(state == 3){
				PowerGarden.soundPool.play(PowerGarden.waterResponseGoodAudio.get((int)(Math.random()*PowerGarden.waterResponseGoodAudio.size())), 1.0f, 1.0f, 1, 0, 1f); 
			}
			if(state == 4){
				PowerGarden.soundPool.play(PowerGarden.waterResponseBadAudio.get((int)(Math.random()*PowerGarden.waterResponseBadAudio.size())), 1.0f, 1.0f, 1, 0, 1f); 
			}
			if(state == 5){
				PowerGarden.soundPool.play(PowerGarden.waterRequestAudio.get((int)(Math.random()*PowerGarden.waterRequestAudio.size())), 1.0f, 1.0f, 1, 0, 1f); 
			}
		} else if (plantIndex == -1){ //debug flag : "play sound" button from sensor config view
			if ( PowerGarden.waterResponseGoodAudio.size() > 0) {
				
				Vector <Integer> thisAudioType = PowerGarden.waterResponseGoodAudio; //which audio type to test
				
				Log.d("size() =  ", Integer.toString(thisAudioType.size()));
				int whichFile = (int)(Math.random()*thisAudioType.size());
				Log.d("play audio test: ", Integer.toString(whichFile));
				PowerGarden.soundPool.play(thisAudioType.get(whichFile), 1.0f, 1.0f, 1, 0, 1f);
			} else {
				Toast toast = Toast.makeText(getApplicationContext(), "no files for this type", Toast.LENGTH_SHORT);
				toast.show();
			}
		}
	}
	
	
	static void destroyAudio(){ //needs to be called when plantType is CHANGED !
		
		PowerGarden.touchRequestAudio.removeAllElements();
		PowerGarden.touchResponseBadAudio.removeAllElements();
		PowerGarden.touchResponseGoodAudio.removeAllElements();
		PowerGarden.touchResponseBadAudio.removeAllElements();
		PowerGarden.waterResponseGoodAudio.removeAllElements();
		PowerGarden.waterResponseBadAudio.removeAllElements();
		PowerGarden.waterRequestAudio.removeAllElements();
	}
	
	  // Implementing FisherÐYates shuffle // for later
	static void shuffleArray(int[] ar) {
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
}

