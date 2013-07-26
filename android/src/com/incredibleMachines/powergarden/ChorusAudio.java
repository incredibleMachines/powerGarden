package com.incredibleMachines.powergarden;

import java.util.TimerTask;

import android.app.Activity;
import android.util.Log;

/*** runnable called by a chorus timer set in PresentationActivity ***/

class ChorusAudio extends TimerTask {
	String TAG = "ChorusAudio";
	private PresentationActivity activity_;
	private Boolean title = false;
	  
	@Override
	public void run() {
		
		Runnable runner = new Runnable(){
			public void run() {
				//activity_.updateStage();
				//int i = (int)(Math.random()*PowerGarden.Device.PlantNum);
				//PowerGarden.audioManager.playSound(0);
				PowerGarden.audioManager.playSound(-99); //chorus ID
				
				Log.wtf(TAG, "PLAYING CHORUS AUDIO NOW");
			}
		};
		if(runner != null){
			activity_.runOnUiThread(runner);
		}
	}
	
	public void setActivity(Activity activity) {
		
		/** onCreate, only hit once at startup **/
		
		Log.d(TAG, "setActivity");
        if (activity_ == activity) {
        	Log.d(TAG, "activty_ == activity, returning --");
        	return;
        }
		activity_ = (PresentationActivity) activity;
	}
}