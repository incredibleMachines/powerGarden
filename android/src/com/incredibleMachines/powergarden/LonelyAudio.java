package com.incredibleMachines.powergarden;

import java.util.TimerTask;

import android.app.Activity;
import android.util.Log;


class LonelyAudio extends TimerTask {
	String TAG = "LonelyAudio";
	private PresentationActivity activity_;
	private Boolean title = false;
	  
	@Override
	public void run() {
		//PowerGarden.Device.messageCopy = PowerGarden.stateManager.updateCopy();
		//if(title) PowerGarden.Device.displayMode = PowerGarden.DisplayMode.PlantTitle;
		//PowerGarden.Device.displayMode = PowerGarden.DisplayMode.MessageCopy;
		//title = !title;
		
		Runnable runner = new Runnable(){
			public void run() {
				//activity_.updateStage();
				int i = (int)(Math.random()*PowerGarden.Device.PlantNum);
				PowerGarden.audioManager.playSound(i);
			}
		};
		if(runner != null){
			activity_.runOnUiThread(runner);
			if(PowerGarden.Device.deviceState.equals("lonely")){ //if we're still lonely!
				activity_.ScheduleLonelyAudio();
			}
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