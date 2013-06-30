package com.incredibleMachines.powergarden;

import com.incredibleMachines.powergarden.R.color;
import com.incredibleMachines.powergarden.util.SystemUiHider;
import com.victorint.android.usb.interfaces.Connectable;
import com.victorint.android.usb.interfaces.Viewable;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
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
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	
	private static String TAG = "PresentationActivity";
	SocketManager SM;
	
	FrameLayout wrapper;
	TextView plantCopy;
	
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
		if(type == PowerGarden.SocketConnected){
			PowerGarden.bConnected = true;
			PowerGarden.Device.plantType = PowerGarden.getPrefString("plantType", null);
			PowerGarden.Device.PlantNum = Integer.parseInt(PowerGarden.getPrefString("numPlants", null));
			PowerGarden.SM.authDevice("register", PowerGarden.Device.ID, PowerGarden.Device.PlantNum, PowerGarden.Device.plantType, this);
			//PowerGarden.SM.authDevice("register", PowerGarden.Device.ID, this);
		}else if(type == PowerGarden.Registered){
			PowerGarden.bRegistered = true;
		}else if(type == PowerGarden.MessageUpdated){
			plantCopy.setText(PowerGarden.Device.messageCopy);
		}
	}
	
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
	    if(PowerGarden.getPrefString("deviceID", null) == null){
	    	//offline/unregistered
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

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
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
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	
	public void resetView(){
		setContentView(R.layout.activity_presentation);
		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);
		wrapper = (FrameLayout) findViewById(R.id.wrapper);
		plantCopy = (TextView) findViewById(R.id.fullscreen_content);
		
		//plantCopy.setText(PowerGarden.Device.plantType.toString().toLowerCase());
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
