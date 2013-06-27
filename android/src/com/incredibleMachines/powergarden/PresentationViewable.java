package com.incredibleMachines.powergarden;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.util.Log;
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
   
    TextView plantVal_1;
    TextView plantVal_2;
    TextView plantVal_3;
    TextView plantVal_4;
    TextView plantVal_5;
    TextView plantVal_6;
    TextView plantVal_7;
    TextView plantVal_8;
    
    TextView lightval;
    TextView distanceval;
    TextView tempval;
    TextView humval;
    TextView moistval;
    
    TextView plantCopy;
    
    SeekBar bar1;
    SeekBar bar2;
    SeekBar bar3;
    SeekBar bar4;
    SeekBar bar5;
    SeekBar bar6;
    SeekBar bar7;
    SeekBar bar8;
    
    Typeface italiaBook;
    Typeface interstateBold;
    
    PlantObject plants[] = new PlantObject[8];
    int numPlants = 8;
    
    int bar1Val = 1000;
    int bar2Val = 1000;
    int bar3Val = 1000;
    int bar4Val = 1000;
    int bar5Val = 1000;
    int bar6Val = 1000;
    int bar7Val = 1000;
    int bar8Val = 1000;
    
    public boolean debug = false;
    
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
    //int [] arrayToSort = {0,0,0,0,0,0,0,0,0,0};
    
	@Override
	public void signalToUi(int type, Object data) {

		int temp1;
		int temp2;
		int temp3;
		if(debug){
	    plantVal_1 = (TextView) activity_.findViewById(R.id.arduino_value_1);
	    plantVal_2 = (TextView) activity_.findViewById(R.id.arduino_value_2);
	    plantVal_3 = (TextView) activity_.findViewById(R.id.arduino_value_3);
	    plantVal_4 = (TextView) activity_.findViewById(R.id.arduino_value_4);
	    plantVal_5 = (TextView) activity_.findViewById(R.id.arduino_value_5);
	    plantVal_6 = (TextView) activity_.findViewById(R.id.arduino_value_6);
	    plantVal_7 = (TextView) activity_.findViewById(R.id.arduino_value_7);
	    plantVal_8 = (TextView) activity_.findViewById(R.id.arduino_value_8);
	    
	    lightval = (TextView) activity_.findViewById(R.id.lightval);
	    distanceval = (TextView) activity_.findViewById(R.id.distanceval);
	    tempval = (TextView) activity_.findViewById(R.id.tempval);
	    humval = (TextView) activity_.findViewById(R.id.humval);
	    moistval = (TextView) activity_.findViewById(R.id.moistval);
	    
//	    plantVal_2 = (TextView) activity_.findViewById(R.id.arduino_value_2);
//	    plantVal_3 = (TextView) activity_.findViewById(R.id.arduino_value_3);
//	    
//	    plant1Label = (TextView) activity_.findViewById(R.id.arduino);
//	    plant2Label = (TextView) activity_.findViewById(R.id.arduino2);
//	    plant3Label = (TextView) activity_.findViewById(R.id.arduino3);
	    bar1 = (SeekBar) activity_.findViewById(R.id.seekBar1);
	    bar2 = (SeekBar) activity_.findViewById(R.id.seekBar2);
	    bar3 = (SeekBar) activity_.findViewById(R.id.seekBar3);
	    bar4 = (SeekBar) activity_.findViewById(R.id.seekBar4);
	    bar5 = (SeekBar) activity_.findViewById(R.id.seekBar5);
	    bar6 = (SeekBar) activity_.findViewById(R.id.seekBar6);
	    bar7 = (SeekBar) activity_.findViewById(R.id.seekBar7);
	    bar8 = (SeekBar) activity_.findViewById(R.id.seekBar8);
//	    
	    bar1.setOnSeekBarChangeListener(this);
	    bar2.setOnSeekBarChangeListener(this);
	    bar3.setOnSeekBarChangeListener(this);
	    bar4.setOnSeekBarChangeListener(this);
	    bar5.setOnSeekBarChangeListener(this);
	    bar6.setOnSeekBarChangeListener(this);
	    bar7.setOnSeekBarChangeListener(this);
	    bar8.setOnSeekBarChangeListener(this);
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
		//MF1.medianFilterAddValue(Integer.parseInt(parseData[1]));
		//int tOne_;
		//Log.d(TAG, "GOT DATA MODE = "+dataType);
		//Log.d(TAG, "Is is equal? "+(dataType.equalsIgnoreCase("c")));
		if(dataType.equalsIgnoreCase("c")){
			//tOne_ = 5;
			for(int i = 0; i<parseData.length-1; i++){
				plants[i].addValue(Integer.parseInt(parseData[i+1]));
			}
			final int plantDisplay1 = plants[0].getFilteredValue();
			final int plantDisplay2 = plants[1].getFilteredValue();
			final int plantDisplay3 = plants[2].getFilteredValue();
			final int plantDisplay4 = plants[3].getFilteredValue();
			final int plantDisplay5 = plants[4].getFilteredValue();
		    final int plantDisplay6 = plants[5].getFilteredValue();
		    final int plantDisplay7 = plants[6].getFilteredValue();
		    final int plantDisplay8 = plants[7].getFilteredValue();
		    //Log.d(TAG, "before debug");
		    if(debug){
		    	//Log.d(TAG, "Should be debug");
				Runnable runner = new Runnable(){
					public void run() {
						for(int i =0;i<plants.length;i++){
							if(System.currentTimeMillis() - plants[i].trig_timestamp > triggerTime){
								plants[i].triggered = false;
							}
							if((plants[i].getFilteredValue() > plants[i].threshold) && !plants[i].triggered){
								//if(!plants[i].triggered){ 
									plants[i].triggered = true;
									if(i==7){
									MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.laugh_1);
									mp.start();
									}
									plants[i].trig_timestamp = System.currentTimeMillis();	
//									if(i == 7){
//										plantVal_8.setTextColor(Color.GREEN);
//									}
								//}
							}
						}
						if(plants[7].triggered){
							plantVal_8.setTextColor(Color.GREEN);
						}
						else{
							plantVal_8.setTextColor(Color.WHITE);
						}

						String val1 = String.valueOf(plantDisplay1);
						String val2 = String.valueOf(plantDisplay2);
						String val3 = String.valueOf(plantDisplay3);
						String val4 = String.valueOf(plantDisplay4);
						String val5 = String.valueOf(plantDisplay5);
						String val6 = String.valueOf(plantDisplay6);
						String val7 = String.valueOf(plantDisplay7);
						String val8 = String.valueOf(plantDisplay8);
						plantVal_1.setText(val1);
						plantVal_2.setText(val2);
						plantVal_3.setText(val3);
						plantVal_4.setText(val4);
						plantVal_5.setText(val5);
						plantVal_6.setText(val6);
						plantVal_7.setText(val7);
						plantVal_8.setText(val8);
					}
				};
				if(runner != null){
					activity_.runOnUiThread(runner);
				}
		    }
		}else if(dataType.equalsIgnoreCase("L")){
			PowerGarden.light = Integer.parseInt(parseData[1]);
			createJson("light",PowerGarden.light);
			final int light = PowerGarden.light;
			if(debug){
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
			createJson("moisture",PowerGarden.moisture);
			final int moisture = PowerGarden.moisture;
			if(debug){
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
			createJson("temperature",PowerGarden.temp, "humidity", PowerGarden.hum);
			final int temp = PowerGarden.temp;
			final int hum = PowerGarden.hum;
			if(debug){
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
			createJson("distance",PowerGarden.distance);
			final int distance = PowerGarden.distance;
			if(debug){
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
		}else{
			
		}

		}

	}
	
	private void createJson(String name1, int value1){
		if(PowerGarden.Device.ID != null){
		 JSONObject j = new JSONObject();
		   long time = System.currentTimeMillis() / 1000L;
		   	try {
		   		j.put(name1, value1).put("timestamp", time );
		   	} catch (JSONException e) {
		   		e.printStackTrace();
		   	}
		   
		   PowerGarden.SM.updateData("update", PowerGarden.Device.ID.toString(), j, this);
		}
	}
	private void createJson(String name1, int value1, String name2, int value2){
		if(PowerGarden.Device.ID != null){
		JSONObject j = new JSONObject();
		   long time = System.currentTimeMillis() / 1000L;
		   	try {
		   		j.put(name1, value1).put(name2, value2).put("timestamp", time );
		   	} catch (JSONException e) {
		   		e.printStackTrace();
		   	}
		   
		   PowerGarden.SM.updateData("update", PowerGarden.Device.ID.toString(), j, this);
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
				//debug = false;
				activity_.resetView();
			}
			
		});
		audioTest = (Button) activity_.findViewById(R.id.audiotest);
		audioTest.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//activity_.setContentView(R.layout.activity_presentation);
				//debug = false;
				if(PowerGarden.Device.plantType.contains("Cherry")){
					audioTest.setText("Play "+PowerGarden.Device.plantType);
					MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.cherrytomatoes_audiotest);
					mp.start();
				} else if(PowerGarden.Device.plantType.contains("Beets")){
					audioTest.setText("Play "+PowerGarden.Device.plantType);
					MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.beets_audiotest);
					mp.start();
				} else if(PowerGarden.Device.plantType.contains("Celery")){
					audioTest.setText("Play "+PowerGarden.Device.plantType);
					MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.tomatoes_audiotest);
					mp.start();
				} else if(PowerGarden.Device.plantType.contains("Tomatoes")){
					audioTest.setText("Play "+PowerGarden.Device.plantType);
					MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.tomatoes_audiotest);
					mp.start();
				} else if(PowerGarden.Device.plantType.contains("Orange Carrots")){
					audioTest.setText("Play "+PowerGarden.Device.plantType);
					MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.orangecarrot_audiotest);
					mp.start();
				} else if(PowerGarden.Device.plantType.contains("Purple Carrots")){
					audioTest.setText("Play "+PowerGarden.Device.plantType);
					MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.purplecarrot_audiotest);
					mp.start();
				} else if(PowerGarden.Device.plantType.contains("Bell Peppers")){
					audioTest.setText("Play "+PowerGarden.Device.plantType);
					MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.bellpeppers_audiotest);
					mp.start();
				}  else if(PowerGarden.Device.plantType == null){

				} else {

				}

			}
			
		});
	}
	

	@Override
	public void setActivity(Activity activity) {
		// TODO Auto-generated method stub
		Log.d(TAG, "setActivity");
        if (activity_ == activity) {
        	Log.d(TAG, "activty_ == activity, returning --");
        	return;
        }
		activity_ = (PresentationActivity) activity;
		for(int i = 0; i<numPlants; i++){
			PlantObject tempPlant = new PlantObject();
			plants[i] = tempPlant;
		}
		italiaBook = Typeface.createFromAsset(activity_.getAssets(),"fonts/italiaBook.ttf");
		interstateBold = Typeface.createFromAsset(activity_.getAssets(), "fonts/Interstate-BoldCondensed.ttf");
		
		plantCopy = (TextView) activity_.findViewById(R.id.fullscreen_content);
		setTextViewFont(italiaBook, plantCopy);
		
		//for 'factoids'
		//setTextViewFont(interstateBold, plantCopy);
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
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		// TODO Auto-generated method stub
		if(arg0 == bar1){
			bar1Val = arg1;
			plants[0].threshold = bar1Val;
			TextView bar1text = (TextView)activity_.findViewById(R.id.bar1text);
			bar1text.setText(Integer.toString(bar1Val));
		}else if(arg0 == bar2){
			bar2Val = arg1;
			plants[1].threshold = bar2Val;
			TextView bar2text = (TextView)activity_.findViewById(R.id.bar2text);
			bar2text.setText(Integer.toString(bar2Val));
		}else if(arg0 == bar4){
			bar3Val = arg1;
			plants[2].threshold = bar3Val;
			TextView bar3text = (TextView)activity_.findViewById(R.id.bar3text);
			bar3text.setText(Integer.toString(bar3Val));
		}else if(arg0 == bar4){
			bar4Val = arg1;
			plants[3].threshold = bar4Val;
			TextView bar4text = (TextView)activity_.findViewById(R.id.bar4text);
			bar4text.setText(Integer.toString(bar4Val));
		}else if(arg0 == bar5){
			bar5Val = arg1;
			plants[4].threshold = bar5Val;
			TextView bar5text = (TextView)activity_.findViewById(R.id.bar5text);
			bar5text.setText(Integer.toString(bar5Val));
		}else if(arg0 == bar6){
			plants[5].threshold = bar6Val;
			bar6Val = arg1;
			TextView bar6text = (TextView)activity_.findViewById(R.id.bar6text);
			bar6text.setText(Integer.toString(bar6Val));
		}else if(arg0 == bar7){
			bar7Val = arg1;
			plants[6].threshold = bar7Val;
			TextView bar7text = (TextView)activity_.findViewById(R.id.bar7text);
			bar7text.setText(Integer.toString(bar7Val));
		}else if(arg0 == bar8){
			bar8Val = arg1;
			plants[7].threshold = bar8Val;
			TextView bar8text = (TextView)activity_.findViewById(R.id.bar8text);
			bar8text.setText(Integer.toString(bar8Val));
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setState(String state) {
		// TODO Auto-generated method stub
		if(state == "debug"){
			activity_.setContentView(R.layout.debug_view);
			debug = true;
			setupDebug();
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
