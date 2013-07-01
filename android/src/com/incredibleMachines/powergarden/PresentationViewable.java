package com.incredibleMachines.powergarden;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.webkit.WebView.FindListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.*;

import com.incredibleMachines.powergarden.R.color;
import com.victorint.android.usb.interfaces.Connectable;
import com.victorint.android.usb.interfaces.Viewable;

public class PresentationViewable implements Viewable, SeekBar.OnSeekBarChangeListener, Connectable {
	private static String TAG = "PresentationViewable";
	//private Activity activity_;
	private PresentationActivity activity_;
	private int messageLevel_			= 22;
	
    TextView plant1Label;
    TextView plant2Label;
    TextView plant3Label;
   
    TextView plantValView[] = new TextView[8];
    
    SeekBar threshBar[] = new SeekBar[8];    
    int threshVal[] = new int[8];
    
    TextView lightval;
    TextView distanceval;
    TextView tempval;
    TextView humval;
    TextView moistval;
    
    TextView plantCopy;
//
//    Typeface italiaBook;
//    Typeface interstateBold;
    
    int numPlants = 8;
    
    public boolean debugSensors = false;
    public boolean debugServer = false;
    
    Button closeDebug;
    Button audioTest;
    
    boolean triggered_1 = false;
    long trig_1_time = 0;
    boolean triggered_2 = false;
    long trig_2_time = 0;
    boolean triggered_3 = false;
    long trig_3_time = 0;
    final long triggerTime = 2500;
    boolean bHaveData = false;
    String gotData;
    MediaPlayer mp;
    
	@Override
	public void signalToUi(int type, Object data) {
		
//		if(debugServer){
//			
//			
//		}

		if(debugSensors){
		    plantValView[0] = (TextView) activity_.findViewById(R.id.arduino_value_1);
		    plantValView[1] = (TextView) activity_.findViewById(R.id.arduino_value_2);
		    plantValView[2] = (TextView) activity_.findViewById(R.id.arduino_value_3);
		    plantValView[3] = (TextView) activity_.findViewById(R.id.arduino_value_4);
		    plantValView[4] = (TextView) activity_.findViewById(R.id.arduino_value_5);
		    plantValView[5] = (TextView) activity_.findViewById(R.id.arduino_value_6);
		    plantValView[6] = (TextView) activity_.findViewById(R.id.arduino_value_7);
		    plantValView[7] = (TextView) activity_.findViewById(R.id.arduino_value_8);
		    
		    lightval = (TextView) activity_.findViewById(R.id.lightval);
		    distanceval = (TextView) activity_.findViewById(R.id.distanceval);
		    tempval = (TextView) activity_.findViewById(R.id.tempval);
		    humval = (TextView) activity_.findViewById(R.id.humval);
		    moistval = (TextView) activity_.findViewById(R.id.moistval);
		    
		    for (int i=0; i<threshBar.length; i++){
		    	try {
		    		Class res = R.id.class;
		    		String id = "seekBar"+Integer.toString(i+1);
		    		Field field = res.getField(id);
					int resId = field.getInt(null);
					threshBar[i] = (SeekBar) activity_.findViewById(resId);
					threshBar[i].setOnSeekBarChangeListener(this);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
		    }
		} 
	    
		// TODO Auto-generated method stub
		if (type == Viewable.BYTE_SEQUENCE_TYPE) {
    		if (data == null || ((byte[]) data).length == 0) {
    			return;
    		}
    		final byte[] byteArray = (byte[]) data;
    		gotData = new String(byteArray);
    		Log.d(TAG, "Byte DATA: "+gotData);
    		bHaveData = true;
		}else if (type == Viewable.CHAR_SEQUENCE_TYPE) {
    		if (data == null || ((CharSequence) data).length() == 0) {
    			return;
    		}
    		final CharSequence tmpData = (CharSequence) data;
    		Log.d(TAG, "Char DATA: "+tmpData.toString());
    	} else if ( 
    			( type == Viewable.DEBUG_MESSAGE_TYPE || type == Viewable.INFO_MESSAGE_TYPE ) &&
    			( type <= messageLevel_ )
    			) {
    		if (data == null || ((CharSequence) data).length() == 0) {
    			return;
    		}
    		final CharSequence tmpData = (CharSequence) data;
    		Log.d(TAG, "Debug DATA: "+tmpData.toString());
    	}
		if(bHaveData){
			String[] parseData = gotData.split(",");
			String dataType = new String(parseData[0]);
			Log.d(TAG, "GOT DATA MODE = "+dataType);
	//		for(int i=0; i<parseData.length; i++){
	//			Log.d(TAG, "Data: "+ parseData[i]);
	//		}
			//Log.d(TAG, "Is is equal? "+(dataType.equalsIgnoreCase("c")));
			if(dataType.equalsIgnoreCase("c")){
	
				final int plantDisplay[] = new int [8];
				
				for(int i = 0; i<parseData.length-1; i++){
					PowerGarden.Device.plants[i].addValue(Integer.parseInt(parseData[i+1]));
					plantDisplay[i] = PowerGarden.Device.plants[i].getFilteredValue(); //set the final here
					//Log.d(TAG, "cap: "+ Integer.toString(plantDisplay[i]));
				}
	
			    //Log.d(TAG, "before debug");
			    if(debugSensors){
			    	//Log.d(TAG, "Should be debug");
					Runnable runner = new Runnable(){
						public void run() {
							for(int i =0;i<PowerGarden.Device.plants.length;i++){
								if(System.currentTimeMillis() - PowerGarden.Device.plants[i].trig_timestamp > triggerTime){ //if we've hit trigger time max
									PowerGarden.Device.plants[i].triggered = false;
									
								}
								if((PowerGarden.Device.plants[i].getFilteredValue() > PowerGarden.Device.plants[i].threshold) && 
									!PowerGarden.Device.plants[i].triggered){ //if we're over the threshold AND we're not triggered:
										//if(!plants[i].triggered){ 
									PowerGarden.Device.plants[i].triggered = true;
									//sendJson("touch", new Monkey("device_id",PowerGarden.Device.ID), new Monkey("index",i));
									PowerGarden.SM.plantTouch("touch", PowerGarden.Device.ID, i, PowerGarden.Device.plants[i].getFilteredValue(), PresentationViewable.this );
									if(i==7){
										mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.laugh_1);
										mp.start();
									}
										PowerGarden.Device.plants[i].trig_timestamp = System.currentTimeMillis();	
								}
								plantValView[i].setText(String.valueOf(plantDisplay[i]));
								if(PowerGarden.Device.plants[i].triggered) {
									plantValView[i].setTextColor(Color.GREEN);
								} else plantValView[i].setTextColor(Color.WHITE);
							}
						}
					};
					if(runner != null){
						activity_.runOnUiThread(runner);
					}
			    }
			}else if(dataType.equalsIgnoreCase("L")){
				PowerGarden.light = Integer.parseInt(parseData[1]);
				//createJson("light",PowerGarden.light);
				final int light = PowerGarden.light;
				if(debugSensors){
					Runnable runner = new Runnable(){
						public void run() {
							String lightStr = String.valueOf(light);
							lightval.setText(lightStr);
						}
					};
					if(runner != null){
						activity_.runOnUiThread(runner);
					}
			    }
			}else if(dataType.equalsIgnoreCase("M")){
				PowerGarden.moisture = Integer.parseInt(parseData[1]);
				//createJson("moisture",PowerGarden.moisture);
				final int moisture = PowerGarden.moisture;
				if(debugSensors){
					Runnable runner = new Runnable(){
						public void run() {
							String moistStr = String.valueOf(moisture);
							moistval.setText(moistStr);
						}
					};
					if(runner != null){
						activity_.runOnUiThread(runner);
					}
			    }
			}else if(dataType.equalsIgnoreCase("T")){
				Log.d(TAG, "GOT DATA MODE = "+dataType);
				PowerGarden.temp = Integer.parseInt(parseData[1]);
				PowerGarden.hum = Integer.parseInt(parseData[2]);
				//createJson("temperature",PowerGarden.temp, "humidity", PowerGarden.hum);
				final int temp = PowerGarden.temp;
				final int hum = PowerGarden.hum;
				if(debugSensors){
					Runnable runner = new Runnable(){
						public void run() {
							String tempStr = String.valueOf(temp);
							String humStr = String.valueOf(hum);
							tempval.setText(tempStr);
							humval.setText(humStr);
						}
					};
					if(runner != null){
						activity_.runOnUiThread(runner);
					}
			    }
			}else if(dataType.equalsIgnoreCase("R")){
				PowerGarden.distance = Integer.parseInt(parseData[1]);
				//createJson("distance",PowerGarden.distance);
				final int distance = PowerGarden.distance;
				if(debugSensors){
					Runnable runner = new Runnable(){
						public void run() {
							String distanceStr = String.valueOf(distance);
							distanceval.setText(distanceStr);
						}
					};
					if(runner != null){
						activity_.runOnUiThread(runner);
					}
			    }
			}else if(dataType.equalsIgnoreCase("D")){
				//LIGHT, TEMP, HUM, MOIST, RANGE
				Log.d(TAG, "received 'D' ! ");
				PowerGarden.light = Integer.parseInt(parseData[1]);
				PowerGarden.temp = Integer.parseInt(parseData[2]);
				PowerGarden.hum = Integer.parseInt(parseData[3]);
				PowerGarden.moisture = Integer.parseInt(parseData[4]);
				PowerGarden.distance = Integer.parseInt(parseData[5]);
				//objTest(new Object({"light",PowerGarden.light}));
				//objTest(new Monkey("string",49));
				sendJson("update",new Monkey("light",PowerGarden.light),new Monkey("temperature",PowerGarden.temp),new Monkey("humidity",PowerGarden.hum),new Monkey("moisture",PowerGarden.moisture));
				final int distance = PowerGarden.distance;
				if(debugSensors){
					Runnable runner = new Runnable(){
						public void run() {
							distanceval.setText(String.valueOf(PowerGarden.distance));
							lightval.setText(String.valueOf(PowerGarden.light));
							moistval.setText(String.valueOf(PowerGarden.moisture));
							humval.setText(String.valueOf(PowerGarden.hum));
							tempval.setText(String.valueOf(PowerGarden.temp));
						}
					};
					if(runner != null){
						activity_.runOnUiThread(runner);
					}
			    }
			}
			else{
				Log.d(TAG, "RECEIVED ARDUINO DATA MISMATCH");
			}
		}
	}

	private void sendJson(String type, Monkey...monkey ){
		Log.d(TAG, "DEVICE ID: "+PowerGarden.Device.ID + " Registered: " +PowerGarden.bRegistered);
		if(PowerGarden.bRegistered){
		JSONObject j = new JSONObject();
		   long time = System.currentTimeMillis() / 1000L;
		   	try {
		   		Log.d(TAG, "createJson and SENDING:");
		   		for(int i = 0;i<monkey.length;i++){
		   			j.put(monkey[i].key.toString(), monkey[i].value);
		   		}
		   		//Log.d(TAG, name+" "+Integer.toString(value)+ " "+ name2+" "+ Integer.toString(value2)
		   				//+ " "+ name3+" "+Integer.toString(value3)+ " "+ name4+" "+Integer.toString(value4));
		   		//j.put(name, value).put(name2, value2).put(name3, value3).put(name4, value4);
		   				
		   	} catch (JSONException e) {
		   		Log.d(TAG, "CATCH ERROR");
		   		e.printStackTrace();
		   	}
		   //if(PowerGarden.bConnected)
		   PowerGarden.SM.updateData(type, PowerGarden.Device.ID.toString(), j, this);
		}
	}
	@Override
	public void saveState() {
		// TODO Auto-generated method stub

	}

	@Override
	public void readState() {
		// TODO Auto-generated method stub

	}
	
	public void setupDebug(){
		closeDebug = (Button) activity_.findViewById(R.id.close);
		closeDebug.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//activity_.setContentView(R.layout.activity_presentation);
			    debugSensors = false;
				activity_.resetView();
			}
		});
		audioTest = (Button) activity_.findViewById(R.id.audiotest);
		audioTest.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				//audioSetup();
				playAudio();
			}
		});
		//audioSetup();
		
		for(int i=0; i<threshBar.length; i++){
	    	try {
	    		Class res = R.id.class;
	    		String id = "seekBar"+Integer.toString(i+1);
	    		Field field = res.getField(id);
				int resId = field.getInt(null);
				threshBar[i] = (SeekBar) activity_.findViewById(resId);
				threshBar[i].setOnSeekBarChangeListener(this);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			if(PowerGarden.getPrefString("threshVal_"+Integer.toString(i), null) != null ){
				int savedThresh = Integer.parseInt(PowerGarden.getPrefString("threshVal_"+Integer.toString(i), null));
				threshBar[i].setProgress(savedThresh);
			} else threshBar[i].setProgress(50);
		}
	}
		
	

	@Override
	public void setActivity(Activity activity) {
		// TODO Auto-generated method stub
		/** onCreate, only hit once at startup **/
		
		Log.d(TAG, "setActivity");
        if (activity_ == activity) {
        	Log.d(TAG, "activty_ == activity, returning --");
        	return;
        }
		activity_ = (PresentationActivity) activity;
		for(int i = 0; i<numPlants; i++){
			PlantObject tempPlant = new PlantObject();
			PowerGarden.Device.plants[i] = tempPlant;
		}
		
		/*only for the first onCreate*/
		PowerGarden.italiaBook = Typeface.createFromAsset(activity_.getAssets(),"fonts/italiaBook.ttf");
		PowerGarden.interstateBold = Typeface.createFromAsset(activity_.getAssets(), "fonts/Interstate-BoldCondensed.ttf");
		
		plantCopy = (TextView) activity_.findViewById(R.id.fullscreen_content);
		setTextViewFont(PowerGarden.italiaBook, plantCopy);
		//for 'factoids'
		//setTextViewFont(interstateBold, plantCopy);
	}
	
	private void playAudio(){
		// TODO Auto-generated method stub
		//activity_.setContentView(R.layout.activity_presentation);
		//debug = false;
		if(!PowerGarden.bAudioPlaying){
		PowerGarden.bAudioPlaying = true;
		
		if(PowerGarden.Device.plantType.contains("Cherry")){
			audioTest.setText("Stop "+PowerGarden.Device.plantType +" audio");
			mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.cherrytomatoes_audiotest);
			mp.start();
//		} else if(PowerGarden.Device.plantType.contains("Beets")){
//			audioTest.setText("Stop "+PowerGarden.Device.plantType +" audio");
//			mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.beets_audiotest);
//			mp.start();
//		} else if(PowerGarden.Device.plantType.contains("Celery")){
//			audioTest.setText("Play "+PowerGarden.Device.plantType);
//			MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.tomatoes_audiotest);
//			mp.start();
//		} else if(PowerGarden.Device.plantType.contains("Tomatoes")){
//			audioTest.setText("Play "+PowerGarden.Device.plantType);
//			MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.tomatoes_audiotest);
//			mp.start();
//		} else if(PowerGarden.Device.plantType.contains("Orange Carrots")){
//			audioTest.setText("Play "+PowerGarden.Device.plantType);
//			MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.orangecarrot_audiotest);
//			mp.start();
//		} else if(PowerGarden.Device.plantType.contains("Purple Carrots")){
//			audioTest.setText("Play "+PowerGarden.Device.plantType);
//			MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.purplecarrot_audiotest);
//			mp.start();
//		} else if(PowerGarden.Device.plantType.contains("Bell Peppers")){
//			audioTest.setText("Play "+PowerGarden.Device.plantType);
//			MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.bellpeppers_audiotest);
//			mp.start();
//		}  else if(PowerGarden.Device.plantType == null){

		} else {
			audioTest.setText("no audio available");
		}
		}else{
			mp.stop();
		}
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	public static int getResId(String variableName, Context context, Class<?> c) {

	    try {
	        Field idField = c.getDeclaredField(variableName);
	        return idField.getInt(idField);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return -1;
	    } 
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int arg1, boolean arg2) {
		// TODO Auto-generated method stub
		for (int i=0; i<8; i++){
			if(seekBar == threshBar[i]){
				threshVal[i] = arg1;
				PowerGarden.Device.plants[i].threshold = threshVal[i];
		    	try {
		    		Class res = R.id.class;
		    		String id = "bar"+Integer.toString(i+1)+"text";
		    		Field field = res.getField(id);
					int resId = field.getInt(null);
					TextView threshBarText = (TextView)activity_.findViewById(resId);
					threshBarText.setText(Integer.toString(threshVal[i]));
					PowerGarden.savePref("threshVal_"+Integer.toString(i), Integer.toString(threshVal[i])); //set the thresh to sharedPref
					
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		
	}

	@Override
	public void setState(String state) {
		if(state == "debug"){
			Log.d(TAG, "setState: 'debug'");
			activity_.setContentView(R.layout.debug_view);
			debugSensors = true;
			setupDebug();
		}
		if(state == "debugServer"){
			debugServer = true;
		}
	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void connect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendData(CharSequence data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendData(int type, byte[] data) {
		// TODO Auto-generated method stub
		
	}
	
    public static void setTextViewFont(Typeface tf, TextView...params) {
        for (TextView tv : params) {
            tv.setTypeface(tf);
        }
    }

}
