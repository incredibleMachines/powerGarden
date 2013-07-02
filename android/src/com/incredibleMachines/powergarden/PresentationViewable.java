package com.incredibleMachines.powergarden;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.reflect.Field;

import com.incredibleMachines.powergarden.R.color;
import com.victorint.android.usb.interfaces.Connectable;
import com.victorint.android.usb.interfaces.Viewable;


public class PresentationViewable implements Viewable, SeekBar.OnSeekBarChangeListener, Connectable {
	private static String TAG = "PresentationViewable";
	//private Activity activity_;
	private PresentationActivity activity_;
	private int messageLevel_			= 22;
	
    TextView plantValView[] = new TextView[PowerGarden.Device.PlantNum];
    SeekBar threshBar[] = new SeekBar[PowerGarden.Device.PlantNum];    
    int threshVal[] = new int[PowerGarden.Device.PlantNum];
    TextView threshBarTextView[] = new TextView[PowerGarden.Device.PlantNum];
    
    TextView lightval;
    TextView distanceval;
    TextView tempval;
    TextView humval;
    TextView moistval;
    
    TextView plantCopy;
  
    public boolean debugSensors = false;
    public boolean debugServer = false;
    
    Button closeDebug;
    Button audioTest;
    
    final long triggerTime = 2500;
    boolean bHaveData = false;
    String gotData;
    
    boolean bSetup = false;
    void updateView(){
    	Log.wtf(TAG, "update sensor View");
//    	
    	if(!bSetup)setupDebug();
    
	    Runnable runner = new Runnable(){
			public void run() {
	//    	
			//update debug window
				for(int i=0; i<PowerGarden.Device.PlantNum; i++){
					//PowerGarden.Device.plants[i].threshold = threshVal[i];
//			    	try {
//			    		Class res = R.id.class;
//			    		String id = "bar"+Integer.toString(i+1)+"text";
//			    		Field field = res.getField(id);
//						int resId = field.getInt(null);
//						TextView threshBarText = (TextView)activity_.findViewById(resId);
//						threshBarText.setText(Integer.toString(threshVal[i]));
//						PowerGarden.savePref("threshVal_"+Integer.toString(i), Integer.toString(threshVal[i])); //set the thresh to sharedPref
//						
//					} catch (Exception e) {
//						e.printStackTrace();
//					} 
			    	try {
			    		Class res = R.id.class;
			    		String id = "bar"+Integer.toString(i+1)+"text";
			    		Field field = res.getField(id);
						int resId = field.getInt(null);
						threshBarTextView[i] = (TextView)activity_.findViewById(resId);
						threshBarTextView[i].setText(Integer.toString(PowerGarden.Device.plants[i].threshold));
						PowerGarden.savePref("threshVal_"+Integer.toString(i), Integer.toString(PowerGarden.Device.plants[i].threshold));
						//PowerGarden.savePref("plantIgnore_"+Integer.toString(i), Integer.toString(PowerGarden.Device.plants[i].threshold));
						
						final int thisOne = i;	
						if (PowerGarden.Device.plants[thisOne].enabled){
							plantValView[thisOne].setTextColor(color.darkgrey);
							threshBarTextView[thisOne].setTextColor(Color.RED);
						}
						else {
							threshBarTextView[thisOne].setTextColor(Color.WHITE);
							plantValView[thisOne].setTextColor(Color.WHITE);
						}
						
												
						threshBarTextView[i].setOnClickListener(new TextView.OnClickListener(){
							@Override
							public void onClick(View v) {
								PowerGarden.Device.plants[thisOne].enabled = !PowerGarden.Device.plants[thisOne].enabled;
								PowerGarden.savePref("plantEnabled_"+Integer.toString(thisOne), String.valueOf(PowerGarden.Device.plants[thisOne].enabled)); //set the thresh to sharedPref
								if (!PowerGarden.Device.plants[thisOne].enabled){
									threshBarTextView[thisOne].setTextColor(Color.RED);
									plantValView[thisOne].setTextColor(color.darkgrey);
								}
								else { 
									threshBarTextView[thisOne].setTextColor(Color.WHITE);
									plantValView[thisOne].setTextColor(Color.WHITE);
								}
								threshBar[thisOne].setEnabled(PowerGarden.Device.plants[thisOne].enabled);
							}
						});
						
					} catch (Exception e) {
						e.printStackTrace();
					} 
					//threshBar[i].setProgress(PowerGarden.Device.plants[i].threshold);
					//threshBarTextView[i].setText(PowerGarden.Device.plants[i].threshold);
				}
			}
    	};
		//if(runner != null){
			activity_.runOnUiThread(runner);
		//}
    }
    

    
    //boolean
	@Override
	public void signalToUi(int type, Object data) {
		
		
		// TODO Auto-generated method stub
		if (type == Viewable.BYTE_SEQUENCE_TYPE) {
    		if (data == null || ((byte[]) data).length == 0) {
    			return;
    		}
    		final byte[] byteArray = (byte[]) data;
    		gotData = new String(byteArray);
    		Log.d(TAG, "Byte DATA: "+gotData);
    		bHaveData = true;
		} else if (type == Viewable.CHAR_SEQUENCE_TYPE) {
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
		
		//--- we got arduino data
		if(bHaveData){
			String[] parseData = gotData.split(",");
			String dataType = new String(parseData[0]);
			Log.d(TAG, "GOT DATA MODE = "+dataType);

			if(dataType.equalsIgnoreCase("c")){ /** capacitance touch received from arduino **/
	
				final int plantDisplay[] = new int [8];
				
				updateView(); //quick refresh of all PowerGarden.statics
				
				for(int i = 0; i<parseData.length-1; i++){
					PowerGarden.Device.plants[i].addValue(Integer.parseInt(parseData[i+1]));
					plantDisplay[i] = PowerGarden.Device.plants[i].getFilteredValue(); //set the final here
					//Log.d(TAG, "cap: "+ Integer.toString(plantDisplay[i]));
				}
	
			    //if(debugSensors){
			    	//Log.d(TAG, "Should be debug");
					Runnable runner = new Runnable(){
						public void run() {
							for(int i =0;i<PowerGarden.Device.plants.length;i++){
								
								if(PowerGarden.Device.plants[i].enabled){ //only if this plant is enabled
									
									if(System.currentTimeMillis() - PowerGarden.Device.plants[i].trig_timestamp > triggerTime){ //if we've hit trigger time max
										PowerGarden.Device.plants[i].triggered = false;
									}
									
									if((PowerGarden.Device.plants[i].getFilteredValue() > PowerGarden.Device.plants[i].threshold) && 
										!PowerGarden.Device.plants[i].triggered){ //if we're over the threshold AND we're not triggered:
										
										PowerGarden.Device.plants[i].triggered = true;
										
										//play correct sound for this plant here
										activity_.playAudio(i); //right now just a single array of "cherrytomato_audio[i]"
										
										//sendJson("touch", new Monkey("device_id",PowerGarden.Device.ID), new Monkey("index",i)); //let's save this for when we get crazy
										PowerGarden.SM.plantTouch("touch", PowerGarden.Device.ID, i, PowerGarden.Device.plants[i].getFilteredValue(), PresentationViewable.this );
										PowerGarden.Device.plants[i].trig_timestamp = System.currentTimeMillis();	
									}
									
									plantValView[i].setText(String.valueOf(plantDisplay[i]));
									
									
									if(PowerGarden.Device.datastream_mode == true) //this might be getting crazy
										PowerGarden.SM.plantTouch("touch", PowerGarden.Device.ID, i, PowerGarden.Device.plants[i].getFilteredValue(), PresentationViewable.this );
									
									if(PowerGarden.Device.plants[i].triggered) {
										plantValView[i].setTextColor(Color.GREEN);
									} else plantValView[i].setTextColor(Color.WHITE);
								}
							}
						}
					};
					if(runner != null){
						activity_.runOnUiThread(runner);
					}
			    //}
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
				Log.e(TAG, "RECEIVED ARDUINO DATA MISMATCH");
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
		Log.d(TAG, "setupDebug");
		//--- setup plant value views
		for(int i=0; i<PowerGarden.Device.PlantNum; i++){
	    	try {
	    		Class res = R.id.class;
	    		String id = "arduino_value_"+Integer.toString(i+1);
	    		Field field = res.getField(id);
				int resId = field.getInt(null);
				plantValView[i] = (TextView) activity_.findViewById(resId);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
	    	try {
	    		Class res = R.id.class;
	    		String id = "bar"+Integer.toString(i+1)+"text";
	    		Field field = res.getField(id);
				int resId = field.getInt(null);
				threshBarTextView[i] = (TextView)activity_.findViewById(resId);
				//threshBarTextView[i].setText(Integer.toString(PowerGarden.Device.plants[i].threshold));
				
				if (!PowerGarden.Device.plants[i].enabled){
					threshBarTextView[i].setTextColor(Color.RED);
					plantValView[i].setTextColor(color.darkgrey);
				}
				else { 
					threshBarTextView[i].setTextColor(Color.WHITE);
					plantValView[i].setTextColor(Color.WHITE);
				}
				threshBar[i].setEnabled(PowerGarden.Device.plants[i].enabled);
				
				final int thisOne = i;
				threshBarTextView[i].setOnClickListener(new TextView.OnClickListener(){
					@Override
					public void onClick(View v) {
						PowerGarden.Device.plants[thisOne].enabled = !PowerGarden.Device.plants[thisOne].enabled;
						PowerGarden.savePref("plantEnabled_"+Integer.toString(thisOne), String.valueOf(PowerGarden.Device.plants[thisOne].enabled)); //set the thresh to sharedPref
						if (!PowerGarden.Device.plants[thisOne].enabled){
							threshBarTextView[thisOne].setTextColor(Color.RED);
							plantValView[thisOne].setTextColor(color.darkgrey);
						}
						else { 
							threshBarTextView[thisOne].setTextColor(Color.WHITE);
							plantValView[thisOne].setTextColor(Color.WHITE);
						}
						threshBar[thisOne].setEnabled(PowerGarden.Device.plants[thisOne].enabled);
					}
				});
				
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}

		//--- setup sensor values
	    lightval = (TextView) activity_.findViewById(R.id.lightval);
	    //lightval.setText(Integer.toString(PowerGarden.light));
	    distanceval = (TextView) activity_.findViewById(R.id.distanceval);
	    //distanceval.setText(Integer.toString(PowerGarden.distance));
	    tempval = (TextView) activity_.findViewById(R.id.tempval);
	    //tempval.setText(Integer.toString(PowerGarden.temp));
	    humval = (TextView) activity_.findViewById(R.id.humval);
	    //humval.setText(Integer.toString(PowerGarden.hum));
	    moistval = (TextView) activity_.findViewById(R.id.moistval);
	    //moistval.setText(Integer.toString(PowerGarden.moisture));
	    
	    //--- setup buttons
		closeDebug = (Button) activity_.findViewById(R.id.close);
		closeDebug.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
			    debugSensors = false;
				activity_.resetView();
			}
		});
		audioTest = (Button) activity_.findViewById(R.id.audiotest);
		audioTest.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				int tempAudioFile = (int) (Math.random()*8);
				activity_.playAudio(tempAudioFile);
			}
		});
		
		//--- setup threshBars
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
	    	//Log.d(TAG, PowerGarden.getPrefString("threshVal_"+Integer.toString(i),null));
			if(PowerGarden.getPrefString("threshVal_"+Integer.toString(i), null) != null ){
				int savedThresh = Integer.parseInt(PowerGarden.getPrefString("threshVal_"+Integer.toString(i), null));
				threshBar[i].setProgress(savedThresh);
			} else threshBar[i].setProgress(1000);
		}
		bSetup = true;
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
		for(int i = 0; i<PowerGarden.Device.PlantNum; i++){
			PlantObject tempPlant = new PlantObject();
			PowerGarden.Device.plants[i] = tempPlant;
		}
		
		/*only for the first onCreate*/
		PowerGarden.italiaBook = Typeface.createFromAsset(activity_.getAssets(),"fonts/italiaBook.ttf");
		PowerGarden.interstateBold = Typeface.createFromAsset(activity_.getAssets(), "fonts/Interstate-BoldCondensed.ttf");
		
		plantCopy = (TextView) activity_.findViewById(R.id.fullscreen_content);
		setTextViewFont(PowerGarden.italiaBook, plantCopy);
		
		//for 'factoids':
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
