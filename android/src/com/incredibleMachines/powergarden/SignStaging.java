package com.incredibleMachines.powergarden;

import java.util.TimerTask;

import android.app.Activity;
import android.util.Log;


class SignStaging extends TimerTask {
	String TAG = "SignStaging";
	private PresentationActivity activity_;
	private Boolean title = false;
	  
	@Override
	public void run() {
		PowerGarden.Device.messageCopy = PowerGarden.stateManager.updateCopy();
		//if(title) PowerGarden.Device.displayMode = PowerGarden.DisplayMode.PlantTitle;
		//PowerGarden.Device.displayMode = PowerGarden.DisplayMode.MessageCopy;
		//title = !title;
		
		Runnable runner = new Runnable(){
			public void run() {
				activity_.updateStage();
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