package com.incredibleMachines.powergarden;

import com.incredibleMachines.powergarden.R.color;
import com.incredibleMachines.powergarden.util.SystemUiHider;
import com.victorint.android.usb.interfaces.Connectable;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class PresentationActivity extends UsbActivity implements Connectable{

	private static final boolean AUTO_HIDE = true;			//show/hide menu vars
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
	private static final boolean TOGGLE_ON_CLICK = true;
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;
	private SystemUiHider mSystemUiHider;
	
	private static String TAG = "PresentationActivity";
	
	SocketManager SM;
	
	FrameLayout wrapper;
	TextView plantCopy;
	
	//SOUNDPOOL STUFFS
	private SoundPool soundPool;
	
	@Override
	public void onResume(){
		super.onResume();
		//setContentView(R.layout.activity_presentation);
		wrapper = (FrameLayout) findViewById(R.id.wrapper);
		plantCopy = (TextView) findViewById(R.id.fullscreen_content);
		
		//plantCopy.setText(PowerGarden.Device.plantType.toString().toLowerCase());
		plantCopy.setText(PowerGarden.Device.messageCopy);
		setTextViewFont(PowerGarden.italiaBook, plantCopy);
//		plantCopy.setText(PowerGarden.Device.plantType.toString().toLowerCase());
		
		if(PowerGarden.Device.plantType.contains("Cherry")){
			wrapper.setBackgroundResource(R.drawable.cherrytomato_bg);
		} else if(PowerGarden.Device.plantType.contains("Beets")){
			wrapper.setBackgroundResource(R.drawable.beet_bg);
		} else if(PowerGarden.Device.plantType.contains("Celery")){
			wrapper.setBackgroundResource(R.drawable.celery_bg);
		} else if(PowerGarden.Device.plantType.contains("Tomatoes")){
			wrapper.setBackgroundResource(R.drawable.tomato_bg);
		} else if(PowerGarden.Device.plantType.contains("Orange Carrots")){
			wrapper.setBackgroundResource(R.drawable.orange_carrot_bg);
		} else if(PowerGarden.Device.plantType.contains("Purple Carrots")){
			wrapper.setBackgroundResource(R.drawable.purple_carrot_bg);
		} else if(PowerGarden.Device.plantType.contains("Bell Peppers")){
			wrapper.setBackgroundResource(R.drawable.pepper_bg);
		}  else if(PowerGarden.Device.plantType == null){
			wrapper.setBackgroundColor(color.default_background);
		} else {
			wrapper.setBackgroundColor(color.default_background);
		}
	}
	
	@Override
	public void signalToUi(int type, Object data){
		super.signalToUi(type, data);
		Log.d(TAG, "GOT CALLBACK: " + Integer.toString(type));
		if(data != null) Log.d(TAG, "DATA: " + data.toString());

		
		/*** connected ***/
		if(type == PowerGarden.SocketConnected){
			PowerGarden.bConnected = true;
			PowerGarden.Device.plantType = PowerGarden.getPrefString("plantType", null);
			PowerGarden.Device.PlantNum = Integer.parseInt(PowerGarden.getPrefString("numPlants", null));
			PowerGarden.SM.authDevice("register", PowerGarden.Device.ID, PowerGarden.Device.PlantNum, PowerGarden.Device.plantType, this);
			//PowerGarden.SM.authDevice("register", PowerGarden.Device.ID, this);
		}
		
		/*** registered ***/
		else if(type == PowerGarden.Registered){
			PowerGarden.bRegistered = true;
		}
		
		/*** message updated ***/
		else if(type == PowerGarden.MessageUpdated){
			plantCopy.setText(PowerGarden.Device.messageCopy);
		}
		
		else if(type == PowerGarden.ThreshChange){
			Log.d(TAG, "presViewable signalToUi recevievd THRESHCHANGE");
			//updateThresholds( data.toString() );
		}
	}
	
//    void updateThresholds(String j){
//    	Log.wtf(TAG, "updateThresh RECVD  " + j);
//		//update debug window
//		for(int i=0; i<PowerGarden.Device.PlantNum; i++){
////			threshBar[i].setProgress(PowerGarden.Device.plants[i].threshold);
////			threshBarTextView[i].setText(PowerGarden.Device.plants[i].threshold);
//		}
//    }
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if (keyCode == KeyEvent.KEYCODE_BACK ) {
	        //do your stuff
	    	Log.d(TAG,"BACK CLICKED");
	    	//activity_.resetView();
	    	//currentViewable_.debug = false;
	    }
	    return true;
	}

	@Override
	protected void createAndSetViews() {
		//super.onCreate(savedInstanceState);
		Log.d(TAG,"!!!START!!!");
		setContentView(R.layout.activity_presentation);
		
		
	    SM = new SocketManager();
	    PowerGarden.SM = SM;
	    PowerGarden.loadPrefs(getApplicationContext());
	    
	    Log.wtf(TAG, "getPrefString: " + PowerGarden.getPrefString("deviceID", null));
	    
	    if(PowerGarden.getPrefString("deviceID", null) == null){
	    	//offline/unregistered
	    	Log.wtf(TAG, "getPref 'deviceID'=null");
	    }else{
	    	PowerGarden.Device.ID = PowerGarden.getPrefString("deviceID", null);
	    	PowerGarden.Device.host = PowerGarden.getPrefString("hostname", null);
	    	PowerGarden.Device.port = PowerGarden.getPrefString("port", null);
	    	PowerGarden.SM.connectToServer(PowerGarden.Device.host,PowerGarden.Device.port,this);
	    }
	    
		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);
		
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
		    	PresentationActivity.super.sendData("setup");
		    	currentViewable_.setState("debug");
			}
		});
        currentViewable_ = new PresentationViewable();
        currentViewable_.setActivity(this);
        
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		delayedHide(100);
		
		//****** setup soundPool player ******//
		
        //this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		soundPool = new SoundPool(8, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId,
                    int status) {
                PowerGarden.audioLoaded = true;
                Log.wtf(TAG, "soundpool audioLoaded TRUE");
            }
        });
        
        PowerGarden.cherryTomatoesAudio[0] =  soundPool.load(this,R.raw.cherrytomatoes_0,1);
        PowerGarden.cherryTomatoesAudio[1] =  soundPool.load(this,R.raw.cherrytomatoes_1,1);
        PowerGarden.cherryTomatoesAudio[2] =  soundPool.load(this,R.raw.cherrytomatoes_2,1);
        PowerGarden.cherryTomatoesAudio[3] =  soundPool.load(this,R.raw.cherrytomatoes_3,1);
        PowerGarden.cherryTomatoesAudio[4] =  soundPool.load(this,R.raw.cherrytomatoes_4,1);
        PowerGarden.cherryTomatoesAudio[5] =  soundPool.load(this,R.raw.cherrytomatoes_5,1);
        PowerGarden.cherryTomatoesAudio[6] =  soundPool.load(this,R.raw.cherrytomatoes_6,1);
        PowerGarden.cherryTomatoesAudio[7] =  soundPool.load(this,R.raw.cherrytomatoes_7,1);
        
        //TODO: have them load by dynamic resID:
        
//        for(int i=0; i<PowerGarden.numSounds; i++){
//	    	try {
//	    		Class res = R.id.class;
//	    		String id = "cherrytomatoes_"+Integer.toString(i+1);
//	    		Field field = res.getField(id);
//				int resId = field.getInt(null);
//				
//				PowerGarden.cherryTomatoesAudio[i] = soundPool.load(this,resId,1);
//				
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} 
//      }
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
	
	public void playAudio(int sound){
//		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (PowerGarden.audioLoaded) {
        	
        	//.play(audio[i], leftVol, rightVol, priority, loop, rate);
        	soundPool.play(PowerGarden.cherryTomatoesAudio[sound], 1.0f, 1.0f, 1, 0, 1f);             
        	Log.d(TAG, "Played sound "+Integer.toString(sound));
        } else Log.d(TAG, "audio NOT loaded yet");
	}
	

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	
	public void resetView(){
		setContentView(R.layout.activity_presentation);
		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);
		wrapper = (FrameLayout) findViewById(R.id.wrapper);
		//plantCopy = (TextView) findViewById(R.id.fullscreen_content);
		
		plantCopy.setText(PowerGarden.Device.messageCopy);
		setTextViewFont(PowerGarden.italiaBook, plantCopy);
		
		if(PowerGarden.Device.plantType.contains("Cherry")){
			wrapper.setBackgroundResource(R.drawable.cherrytomato_bg);
		} else if(PowerGarden.Device.plantType.contains("Beets")){
			wrapper.setBackgroundResource(R.drawable.beet_bg);
		} else if(PowerGarden.Device.plantType.contains("Celery")){
			wrapper.setBackgroundResource(R.drawable.celery_bg);
		} else if(PowerGarden.Device.plantType.contains("Tomatoes")){
			wrapper.setBackgroundResource(R.drawable.tomato_bg);
		} else if(PowerGarden.Device.plantType.contains("Orange Carrots")){
			wrapper.setBackgroundResource(R.drawable.orange_carrot_bg);
		} else if(PowerGarden.Device.plantType.contains("Purple Carrots")){
			wrapper.setBackgroundResource(R.drawable.purple_carrot_bg);
		} else if(PowerGarden.Device.plantType.contains("Bell Peppers")){
			wrapper.setBackgroundResource(R.drawable.pepper_bg);
		}  else if(PowerGarden.Device.plantType == null){
			wrapper.setBackgroundColor(color.default_background);
		} else {
			wrapper.setBackgroundColor(color.default_background);
		}
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
    public static void setTextViewFont(Typeface tf, TextView...params) {
        for (TextView tv : params) {
            tv.setTypeface(tf);
        }
    }
}
