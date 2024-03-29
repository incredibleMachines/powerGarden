package com.incredibleMachines.powergarden;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Timer;

import org.json.JSONException;
import org.json.JSONObject;

import com.incredibleMachines.powergarden.R.color;
import com.incredibleMachines.powergarden.util.SystemUiHider;
import com.incredibleMachines.powergarden.util.UsbActivity;
import com.victorint.android.usb.interfaces.Connectable;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * PresentationActivity, 
 * 
 * contains all "staging" of app, central UI, 
 * all font and backgrounds
 */

public class PresentationActivity extends UsbActivity implements Connectable{
	private static String TAG = "PresentationActivity";
	
	private static final boolean AUTO_HIDE = true;			//show/hide menu vars
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
	private static final boolean TOGGLE_ON_CLICK = true;
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
	public SystemUiHider mSystemUiHider;
	FrameLayout wrapper;
	TextView stageCopy;
	TextView twitterHandle;
	LinearLayout twitterHeading;
	
	public ProgressDialog progressBar;
	public static Handler jsonDlHandler;
	
	SocketManager SM;
	SignStaging signStageUpdater;
    ChorusAudio chorusAudioUpdater = new ChorusAudio();
    Timer chorusAudioScheduling = new Timer();
	
    int frameCount = 0;
	
	@Override
	public void onResume(){
		super.onResume();
		Log.d(TAG, "onResume");
		resetView();
	}
	
	
	@Override
	public void signalToUi(int type, Object data){
		super.signalToUi(type, data);
		Log.d(TAG, "PresentationActivty signalToUi");
		if(data != null) Log.d(TAG, "DATA: " + data.toString());
		
		/*** connected ***/
		if(type == PowerGarden.SocketConnected){
			PowerGarden.bConnected = true;
			Log.d(TAG, "bConnected = true;");
			PowerGarden.Device.plantType = PowerGarden.getPrefString("plantType", null);
			PowerGarden.Device.PlantNum = Integer.parseInt(PowerGarden.getPrefString("numPlants", null));
			PowerGarden.SM.authDevice("register", PowerGarden.Device.ID, PowerGarden.Device.PlantNum, PowerGarden.Device.plantType, this);
		}
		
		/*** registered ***/
		else if(type == PowerGarden.Registered){
			Log.wtf(TAG, "bRegistered set TRUE");
			PowerGarden.bRegistered = true;
		}
		
		/*** message updated ***/
		else if(type == PowerGarden.MessageUpdated){
			stageCopy.setText(PowerGarden.Device.messageCopy);
		}
		
		/*** update thresholds ***/
		else if(type == PowerGarden.ThreshChange){
			Log.d(TAG, "presViewable signalToUi recevievd THRESHCHANGE");
			//updateThresholds( data.toString() );
		}
		
		/*** show arduino debug page ***/
		else if(type == PowerGarden.StreamModeUpdate){
			if(PowerGarden.Device.datastream_mode){
				 Runnable showDebug = null;
				 showDebug = new Runnable(){
					public void run(){       
						PresentationActivity.super.sendData("setup");
						if(currentViewable_ != null)
							currentViewable_.setState("debug");
						mSystemUiHider.hide();
					}
				 };
				if(showDebug != null){
					PresentationActivity.this.runOnUiThread(showDebug);	
				}
			}
		}
		
		/*** set chorus playback ***/
		else if(type == PowerGarden.SetChorusTime){
			if(data.toString().equals("stop")){
				Log.d(TAG, "STOP CHORUS");
				PowerGarden.chorusAudio.stop(); // shut the hell up
				PowerGarden.chorusAudio.prepareAsync(); // prepare the chorus audio to be sung again 
			}
			else {
		      ChorusAudio chorusAudioUpdater = new ChorusAudio();
		      chorusAudioUpdater.setActivity(this);
		      Timer chorusScheduling = new Timer();
		      chorusScheduling.schedule(chorusAudioUpdater, PowerGarden.Device.chorus_start_time);
			}
		}
		
		/*** set screen brightness & volume of tablet ***/
		else if(type == PowerGarden.TabletSettings){
			
			//--- brightness (needs to be updated on ui thread)
			Runnable adjustBrightness = null;
			adjustBrightness = new Runnable(){
				public void run(){       
				    WindowManager.LayoutParams lp = getWindow().getAttributes();
				    //previousScreenBrightness = lp.screenBrightness;
				    float newBrightness = PowerGarden.Device.tablet_brightness;
				    lp.screenBrightness = newBrightness; 
				    getWindow().setAttributes(lp); 
				}
			 };
			if(adjustBrightness != null)
				PresentationActivity.this.runOnUiThread(adjustBrightness);	//go
			
			
			//--- volume control
			AudioManager mgr=null;
			mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			int currVol = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
			Log.d(TAG, "CURR VOL: "+Integer.toString(currVol));
			
			if(currVol < PowerGarden.Device.tablet_volume){ //TURN IT UP
				int diff = PowerGarden.Device.tablet_volume - currVol;
				while(diff > 0){ //count up to the new volume
					mgr.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0 );
					diff--;
				}
			} else {										//TURN IT DOWN
				int diff = currVol - PowerGarden.Device.tablet_volume;
				while(diff > 0){
					mgr.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0 );
					diff--;
				}
			}
			
			//--- send an update to the server
			try {
				JSONObject j = new JSONObject();
				j.put("battery_status", PowerGarden.Device.tablet_battery_level)
					.put("brightness", (int)(PowerGarden.Device.tablet_brightness*100))
					.put("volume", PowerGarden.Device.tablet_volume)
					.put("device_id", PowerGarden.Device.ID);
				PowerGarden.SM.sendMonkey("tablet", PowerGarden.Device.ID, j, this);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	//--- listener for battery status change ----
	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){ 
		    @Override
		    public void onReceive(Context arg0, Intent intent) {
		      int level = intent.getIntExtra("level", 0);
		      PowerGarden.Device.tablet_battery_level = level;
		      Log.d(TAG, "BATTER STATUS CHANGE: "+Integer.toString(PowerGarden.Device.tablet_battery_level));
				try {
					JSONObject j = new JSONObject();
					j.put("battery_status", PowerGarden.Device.tablet_battery_level)
						.put("device_id", PowerGarden.Device.ID);
					PowerGarden.SM.sendMonkey("tablet", PowerGarden.Device.ID, j, PresentationActivity.this);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
	};



	@Override
	protected void createAndSetViews() throws IOException {
		//super.onCreate(savedInstanceState);
		Log.wtf(TAG,"!!!START!!!");
		setContentView(R.layout.activity_presentation);
		
				
		//set up broadcastListener for battery level status
		this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		
		/** setup sign stage updater **/
	    signStageUpdater = new SignStaging();
	    signStageUpdater.setActivity(this);
		
	    /** setup sign stage updater **/
	    SM = new SocketManager();
	    PowerGarden.SM = SM;
	    PowerGarden.loadPrefs(getApplicationContext());
	    
	    Log.d(TAG, "getPrefString: " + PowerGarden.getPrefString("deviceID", null));
	    
	    if(PowerGarden.getPrefString("deviceID", null) == null){
	    	Log.wtf(TAG, "getPref 'deviceID'=null");
	    }else{
	    	PowerGarden.Device.plantType = PowerGarden.getPrefString("plantType", "orange_carrots");
	    	PowerGarden.Device.ID = PowerGarden.getPrefString("deviceID", null);
	    	PowerGarden.Device.host = PowerGarden.getPrefString("hostname", null);
	    	PowerGarden.Device.port = PowerGarden.getPrefString("port", null);
	    	PowerGarden.SM.connectToServer(PowerGarden.Device.host,PowerGarden.Device.port,this);
	    	PowerGarden.bRegistered = true;
	    }
	    
		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.stage_copy);
		
		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		findViewById(R.id.setup_sockets_button).setOnTouchListener(
				mDelayHideTouchListener
				);
		//set up websockets
		findViewById(R.id.setup_sockets_button).setOnClickListener(new Button.OnClickListener(){
			public void onClick(View v){
				Intent setupSockets = new Intent(getApplicationContext(), ConnectSockets.class);
				PresentationActivity.this.startActivity(setupSockets);
			}
		});

		findViewById(R.id.connect_arduino_button).setOnTouchListener(
				mDelayHideTouchListener
				
				);
		//set up arduino
		findViewById(R.id.connect_arduino_button).setOnClickListener(new Button.OnClickListener(){
			public void onClick(View v){	
				
		    	PresentationActivity.super.sendData("setup");
		    	currentViewable_.setState("debug");
			}
		});
		
		
		//-- attach viewable
        currentViewable_ = new PresentationViewable();
        currentViewable_.setActivity(this);
        
        if(PowerGarden.bRegistered){
			// prepare for a progress bar dialog 
			progressBar = new ProgressDialog(contentView.getContext());
			progressBar.setCancelable(true);
			progressBar.setMessage("downloading JSON...");
			progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressBar.show();
	
	        new Thread() {
	        	public void run() {
	        		try {
	        			signStageUpdater.loadDialogue();
	        		} catch (IOException e) {
	        			e.printStackTrace();
	        		} catch (JSONException e) {
	        			e.printStackTrace();
	        		}
	        	}
	    	}.start();
	    	
	        jsonDlHandler = new Handler() { 
		        @Override 
		        public void handleMessage(Message msg) {  //only called when we're done loading all JSON
		        	PowerGarden.audioManager.setupAudio(); //does setup of all sound file types from JSON
		        	setupSoundpool();
		        	
		    		//**** sign staging setup ****//
		    		Timer signScheduling = new Timer();
		    		signScheduling.schedule(signStageUpdater, 10000, 10000); // (task, initial delay, repeated delay
		          
		    		//*** send setup to arduino ***//
		    		PresentationActivity.super.sendData("setup");	
		    		
		        	progressBar.dismiss();
		        } 
	        };
        }
    
//    	while(progressBar.isShowing()){ 
//    		Log.d("PROGRESS BAR", "IS SHOWING");
//    		//sit in this while loop, do not go on until all json is downloaded!
//    	}

		/*** TO BE REMOVED: ***/
//		InputStream is = getResources().openRawResource(R.raw.dialogue);
//		Writer writer = new StringWriter();
//		Reader reader = null;
//		char[] buffer = new char[1024];
//		try {
//			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
//		    int n;
//		    while ((n = reader.read(buffer)) != -1) {
//		        writer.write(buffer, 0, n);
//		    }
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} finally {
//			is.close();
//		}
//		try {
//			PowerGarden.dialogue = new JSONObject(writer.toString());
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	
	protected void downloadJSON(){

	
	}
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		Log.d(TAG, "onPostCreate");
		delayedHide(1500);
	
//		//**** sign staging setup ****//
//		Timer signScheduling = new Timer();
//		signScheduling.schedule(signStageUpdater, 10000, 10000); // (task, initial delay, repeated delay
//      
//		//*** send setup to arduino ***//
//		PresentationActivity.super.sendData("setup");	
	}
	
	private void setupSoundpool(){
		//****** setup soundPool player ******//
		PowerGarden.soundPool = new SoundPool(100, AudioManager.STREAM_MUSIC, 0);
		PowerGarden.soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId,
                    int status) {
                PowerGarden.audioLoaded = true;
                Log.wtf(TAG, "soundpool audioLoaded TRUE, sample: "+Integer.toString(sampleId));
            }
        });
        
        PowerGarden.audioManager.setupAudio(); //does setup of all sound file types from JSON
		
        Log.d(TAG, "numSounds total found: "+Integer.toString(PowerGarden.audioManager.getNumAudioSamples()));
        
        Runnable audioSetup = null;
        audioSetup = new Runnable(){
	
        	public void run(){       
	    	try {
	    		Log.wtf(TAG, "loading audio");
	    		
	    		//first load up the chorusAudio track (to be moved later for song selection)
	    		PowerGarden.chorusAudio = MediaPlayer.create(PresentationActivity.this, R.raw.chorus_1812_all);
	    		
	    		//now load up every soundPool up with associated sound type 
		        for(int j=0; j<PowerGarden.plantAudio_touchRequest.length();j++){
		        		int sound_id = getResources().getIdentifier(PowerGarden.plantAudio_touchRequest.getString(j), "raw", getPackageName());
						PowerGarden.touchRequestAudio.add(PowerGarden.soundPool.load(PresentationActivity.this, sound_id, 1));
		        }
		        
		        for(int j=0; j<PowerGarden.plantAudio_touchResponseGood.length();j++){
	        			int sound_id = getResources().getIdentifier(PowerGarden.plantAudio_touchResponseGood.getString(j), "raw",
	                        getPackageName());
	        			PowerGarden.touchResponseGoodAudio.add(PowerGarden.soundPool.load( PresentationActivity.this, sound_id, 1));
		        }
		        
		        for(int j=0; j<PowerGarden.plantAudio_touchResponseBad.length();j++){
	    			int sound_id = getResources().getIdentifier(PowerGarden.plantAudio_touchResponseBad.getString(j), "raw",
	                    getPackageName());
	    			PowerGarden.touchResponseBadAudio.add(PowerGarden.soundPool.load( PresentationActivity.this, sound_id, 1));
		        }
		        
		        for(int j=0; j<PowerGarden.plantAudio_waterRequest.length();j++){
	    			int sound_id = getResources().getIdentifier(PowerGarden.plantAudio_waterRequest.getString(j), "raw",
	                        getPackageName());
	    			PowerGarden.waterRequestAudio.add(PowerGarden.soundPool.load( PresentationActivity.this, sound_id, 1));
		        }
		        
		        for(int j=0; j<PowerGarden.plantAudio_waterResponseGood.length();j++){
	    			int sound_id = getResources().getIdentifier(PowerGarden.plantAudio_waterResponseGood.getString(j), "raw",
	                        getPackageName());
	    			PowerGarden.waterResponseGoodAudio.add(PowerGarden.soundPool.load( PresentationActivity.this, sound_id, 1));	        
		        }

				
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		};
		if(audioSetup != null){
			PresentationActivity.this.runOnUiThread(audioSetup);	
		}
	}
	
	
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};
	
	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};
	
	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	public void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
		LinearLayout controls = (LinearLayout) findViewById(R.id.fullscreen_content_controls);
		controls.setVisibility(View.INVISIBLE);
	}

	
	public void resetView(){
		Log.d(TAG, "resetView");
		setContentView(R.layout.activity_presentation);
		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.stage_copy);
		wrapper = (FrameLayout) findViewById(R.id.wrapper);
		stageCopy = (TextView) findViewById(R.id.stage_copy);
		twitterHandle = (TextView) findViewById(R.id.twitter_handle);
		twitterHeading = (LinearLayout) findViewById(R.id.twitter_title);
		
		PowerGarden.Device.tweetCopy.add("hello this is a tweet and it is going to be exactly one-hundred forty characters and here it is, the exact number of chars you wanted !!!  .");
		PowerGarden.Device.tweetUsername.add("@QuantifiedPup");
		
		
		setStageBG(); // bare plant background of this plantType
		
		updateStage();
		
		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView, 
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
			int mControlsHeight;
			int mShortAnimTime;

			@Override
			@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
			public void onVisibilityChange(boolean visible) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
					// If the ViewPropertyAnimator API is available
					// (Honeycomb MR2 and later), use it to animate the
					// in-layout UI controls at the bottom of the
					// screen.
					if (mControlsHeight == 0) {
						mControlsHeight = controlsView.getHeight();
					}
					if (mShortAnimTime == 0) {
						mShortAnimTime = getResources().getInteger(
								android.R.integer.config_shortAnimTime);
					}
					controlsView
							.animate()
							.translationY(visible ? 0 : mControlsHeight)
							.setDuration(mShortAnimTime);
				} else {
					// If the ViewPropertyAnimator APIs aren't
					// available, simply show or hide the in-layout UI
					// controls.
					controlsView.setVisibility(visible ? View.VISIBLE
							: View.GONE);
				}

				if (visible && AUTO_HIDE) {
					// Schedule a hide().
					delayedHide(AUTO_HIDE_DELAY_MILLIS);
				}
			}
		});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.setup_sockets_button).setOnTouchListener(
				mDelayHideTouchListener
				
				);
		//set up websockets
		findViewById(R.id.setup_sockets_button).setOnClickListener(new Button.OnClickListener(){
			public void onClick(View v){
				currentViewable_.setState("debugServer");
				Intent setupSockets = new Intent(getApplicationContext(), ConnectSockets.class);
				PresentationActivity.this.startActivity(setupSockets);
			}
		});
		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.connect_arduino_button).setOnTouchListener(
				mDelayHideTouchListener
				
				);
		//set up arduino
		findViewById(R.id.connect_arduino_button).setOnClickListener(new Button.OnClickListener(){
			public void onClick(View v){
				Log.d(TAG, "CLICKED connect arduino button");
		    	PresentationActivity.super.sendData("setup");
		    	currentViewable_.setState("debug");
			}
		});
	}

	
	void updateStage(){//(int deviceStateIndex){
		frameCount++;
		if(frameCount > 3 && PowerGarden.Device.displayMode == PowerGarden.DisplayMode.MessageCopy){
			PowerGarden.Device.displayMode = PowerGarden.DisplayMode.PlantTitle;
			frameCount = 0;
		}
		
		if(PowerGarden.Device.displayMode == PowerGarden.DisplayMode.MessageCopy){
			//*** display whatever is inside PowerGarden.Device.messageCopy ***/
			setStageBG();
			
			twitterHeading.setVisibility(View.INVISIBLE);
			stageCopy.setVisibility(View.VISIBLE);
			
			stageCopy.setText(PowerGarden.Device.messageCopy);
			stageCopy.setPadding(30, 20, 30, 20);
			stageCopy.setShadowLayer(50, .5f, .5f, Color.DKGRAY);
			setTextViewFont(PowerGarden.interstateBold, stageCopy);
			
			int msgLength = PowerGarden.Device.messageCopy.length();
			
			if(msgLength > 80){
				stageCopy.setShadowLayer(50, .5f, .5f, Color.DKGRAY);
				stageCopy.setTextSize(75);//((int)(1/(msgLength*.0004f)));
				stageCopy.setLineSpacing(0, 1);
				stageCopy.setAllCaps(true);

			} else {
				stageCopy.setTextSize(85);//((int)(1/(msgLength*.0004f)));
				stageCopy.setLineSpacing(3, 1);
				stageCopy.setAllCaps(true);
			}
			
			try {
				JSONObject j = new JSONObject();
				j.put("device_id", PowerGarden.Device.ID).put("message", PowerGarden.Device.messageCopy).put("background",PowerGarden.Device.plantType+"_bg.png");
				PowerGarden.SM.sendMonkey("display", PowerGarden.Device.ID.toString(), j, this);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		if(PowerGarden.Device.displayMode == PowerGarden.DisplayMode.Tweet){
			//*** display a tweet ! ***/
			
			twitterHeading.setVisibility(View.VISIBLE);
			stageCopy.setVisibility(View.VISIBLE);
			
			setTextViewFont(PowerGarden.interstateBold, stageCopy);
			setTextViewFont(PowerGarden.interstateBold, twitterHandle);
			wrapper.setBackgroundResource(R.drawable.twitter_bg);
			
			PowerGarden.stateManager.prepareTweet();
			
			String thisHandle = PowerGarden.stateManager.getHandle();
			twitterHandle.setText(thisHandle);
			twitterHandle.setShadowLayer(50, .5f, .5f, Color.DKGRAY);
			twitterHandle.setTextSize(65);//((int)(1/(msgLength*.0004f)));
			twitterHandle.setLineSpacing(3, 1);
			//twitterHandle.setAllCaps(true);
			
			String thisTweet = PowerGarden.stateManager.getTweet();
			int msgLength = thisTweet.length();
			stageCopy.setText(thisTweet);
			stageCopy.setPadding(30, 80, 30, 10); //add padding to top to make the twitter handle fit nicer
			
			if(msgLength > 90){  //if it's a long ass tweet
				stageCopy.setShadowLayer(50, .5f, .5f, Color.DKGRAY);
				stageCopy.setTextSize(70);//((int)(1/(msgLength*.0004f)));
				stageCopy.setLineSpacing(0, 0.9f);
				stageCopy.setAllCaps(true);

			} else {
				stageCopy.setShadowLayer(50, .5f, .5f, Color.DKGRAY);
				stageCopy.setTextSize(90);//((int)(1/(msgLength*.0004f)));
				stageCopy.setLineSpacing(3, 1);
				stageCopy.setAllCaps(true);
			}

			
			PowerGarden.Device.displayMode = PowerGarden.DisplayMode.MessageCopy;
			try {
				JSONObject j = new JSONObject();
				j.put("device_id", PowerGarden.Device.ID).put("message", "@"+thisHandle+" "+thisTweet).put("background","twitter_bg.png");
				PowerGarden.SM.sendMonkey("display", PowerGarden.Device.ID.toString(), j, this);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		
		if(PowerGarden.Device.displayMode == PowerGarden.DisplayMode.PlantTitle){
			//*** show plant title card .png ***//
			twitterHeading.setVisibility(View.INVISIBLE);
			
			if(PowerGarden.Device.plantType.contains(" ")){
				PowerGarden.Device.plantType.replace(" ", "_");
				PowerGarden.Device.plantType = PowerGarden.Device.plantType.toLowerCase();
				
			}
			
			if(PowerGarden.Device.plantType.contains("cherry")){
				wrapper.setBackgroundResource(R.drawable.cherrytomatoes_title);
			} else if(PowerGarden.Device.plantType.contains("beets")){
				wrapper.setBackgroundResource(R.drawable.beets_title);
			} else if(PowerGarden.Device.plantType.contains("celery")){
				wrapper.setBackgroundResource(R.drawable.celery_title);
			} else if(PowerGarden.Device.plantType.contains("tomatoes")){
				wrapper.setBackgroundResource(R.drawable.tomatoes_title);
			} else if(PowerGarden.Device.plantType.contains("orange_carrots")){
				wrapper.setBackgroundResource(R.drawable.orangecarrots_title);
			} else if(PowerGarden.Device.plantType.contains("purple_carrots")){
				wrapper.setBackgroundResource(R.drawable.purplecarrots_title);
			} else if(PowerGarden.Device.plantType.contains("peppers")){
				wrapper.setBackgroundResource(R.drawable.peppers_title);
			}  else if(PowerGarden.Device.plantType == null){
				wrapper.setBackgroundColor(color.default_background);
			} else {
				wrapper.setBackgroundColor(color.default_background);
			}
			stageCopy.setText("");
			
			PowerGarden.Device.displayMode = PowerGarden.DisplayMode.MessageCopy;
			try {
				JSONObject j = new JSONObject();
				j.put("device_id", PowerGarden.Device.ID).put("message", "").put("background",PowerGarden.Device.plantType+"_title.png");
				PowerGarden.SM.sendMonkey("display", PowerGarden.Device.ID.toString(), j, this);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	void setStageBG(){
		if(PowerGarden.Device.plantType.contains("cherry")){
			wrapper.setBackgroundResource(R.drawable.cherrytomato_bg);
		} else if(PowerGarden.Device.plantType.contains("beets")){
			wrapper.setBackgroundResource(R.drawable.beet_bg);
		} else if(PowerGarden.Device.plantType.contains("celery")){
			wrapper.setBackgroundResource(R.drawable.celery_bg);
		} else if(PowerGarden.Device.plantType.contains("tomatoes")){
			wrapper.setBackgroundResource(R.drawable.tomato_bg);
		} else if(PowerGarden.Device.plantType.contains("orange_carrots")){
			wrapper.setBackgroundResource(R.drawable.orange_carrot_bg);
		} else if(PowerGarden.Device.plantType.contains("purple_carrots")){
			wrapper.setBackgroundResource(R.drawable.purple_carrot_bg);
		} else if(PowerGarden.Device.plantType.contains("peppers")){
			wrapper.setBackgroundResource(R.drawable.pepper_bg);
		}  else if(PowerGarden.Device.plantType == null){
			wrapper.setBackgroundColor(color.default_background);
		} else {
			wrapper.setBackgroundColor(color.default_background);
		}	
	}
	
    public static void setTextViewFont(Typeface tf, TextView...params) {
        for (TextView tv : params) {
            tv.setTypeface(tf);
        }
    }
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK ) {
	    	Log.d(TAG,"BACK CLICKED");
	    }
	    return true;
	}
	
	@Override
	public void onStop()
	{
	    unregisterReceiver(mBatInfoReceiver);
	    super.onStop();
	}
}