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
		
		Log.d("setupAudio", "plantType: "+ PowerGarden.Device.plantType);
		
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
//				else if(i==5){
//					for (int j=0; j<PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").length(); j++){
//						PowerGarden.plantAudio_waterResponseBad.put(PowerGarden.dialogue.getJSONObject(PowerGarden.Device.plantType).getJSONObject(PowerGarden.copyType[i]).getJSONArray("audio").get(j));
//						numSamples++;
//					}
//				}
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
		Log.d("plantType: ", PowerGarden.Device.plantType);
		Log.d("plantIndex: ", Integer.toString(plantIndex));
		//PowerGarden.stateManager.getNumAudioSamples();
		if (PowerGarden.audioLoaded && plantIndex > -1) {
			String state = PowerGarden.stateManager.getPlantState(plantIndex);
			Log.d("playAudio", "state: "+state);
			
			float leftChannelVol = 1.0f;
			float rightChannelVol = 1.0f;
			
//			if(plantIndex >= 0 && plantIndex <= 3){ //all plants on all channels for now !
//				rightChannelVol = 0.0f;
//			}
//			if(plantIndex > 3){
//				leftChannelVol = 0.0f;
//			}
		//**************
			if(state.equals("lonely")){
	        	PowerGarden.soundPool.play(PowerGarden.touchRequestAudio.get((int)(Math.random()*PowerGarden.touchRequestAudio.size())), leftChannelVol, rightChannelVol, 1, 0, 1f); 
			}
			if(state.equals("content")){
				PowerGarden.soundPool.play(PowerGarden.touchResponseGoodAudio.get((int)(Math.random()*PowerGarden.touchResponseGoodAudio.size())), leftChannelVol, rightChannelVol, 1, 0, 1f); 
			}
			if(state.equals("worked_up")){
				PowerGarden.soundPool.play(PowerGarden.touchResponseBadAudio.get((int)(Math.random()*PowerGarden.touchResponseBadAudio.size())), leftChannelVol, rightChannelVol, 1, 0, 1f); 
			}
		} 
		
		else if (plantIndex == -4){ //**** watering audio ****//
			Vector <Integer> thisAudioType = PowerGarden.waterResponseGoodAudio;
			int whichFile = (int)(Math.random()*thisAudioType.size());
			PowerGarden.soundPool.play(PowerGarden.waterResponseGoodAudio.get(whichFile), 1.0f, 1.0f, 1, 0, 1f); 
			Log.wtf("play audio: ", thisAudioType.get(whichFile).toString());
		}
		else if(plantIndex == -10){
			Log.d("plantIndex: ", "-10");
			Vector <Integer> thisAudioType = PowerGarden.touchRequestAudio;
			int whichFile = (int)(Math.random()*thisAudioType.size());
			PowerGarden.soundPool.play(PowerGarden.touchRequestAudio.get(whichFile), 1.0f, 1.0f, 1, 0, 1f); 
			PowerGarden.Device.deviceState = "lonely";
			Log.wtf("play audio: ", thisAudioType.get(whichFile).toString());
		}
		else if(plantIndex == -11){
			Log.d("plantIndex: ", "-11");
			Vector <Integer> thisAudioType = PowerGarden.touchResponseGoodAudio;
			int whichFile = (int)(Math.random()*thisAudioType.size());
			PowerGarden.Device.deviceState = "content";
			PowerGarden.soundPool.play(PowerGarden.touchResponseGoodAudio.get(whichFile), 1.0f, 1.0f, 1, 0, 1f); 
			Log.wtf("play audio: ", thisAudioType.get(whichFile).toString());
		}
		
		else if(plantIndex == -99){ //chorus !
			Log.d("plantIndex: ", "-99");
			PowerGarden.chorusAudio.start(); 
			Log.wtf(TAG, "play chorus audio");
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

