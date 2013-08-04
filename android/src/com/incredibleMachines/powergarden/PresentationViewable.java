package com.incredibleMachines.powergarden;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Messenger;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.TimerTask;

import com.incredibleMachines.powergarden.R.color;
import com.victorint.android.usb.interfaces.Connectable;
import com.victorint.android.usb.interfaces.Viewable;


public class PresentationViewable extends TimerTask implements Connectable, Viewable, SeekBar.OnSeekBarChangeListener  {
	private static String TAG = "PresentationViewable";
	
	private PresentationActivity activity_;
	private int messageLevel_			= 22;
	
    TextView plantValView[] = new TextView[PowerGarden.Device.PlantNum];
    TextView plantDiffView[] = new TextView[PowerGarden.Device.PlantNum];
    SeekBar threshBar[] = new SeekBar[PowerGarden.Device.PlantNum];    
    int threshVal[] = new int[PowerGarden.Device.PlantNum];
    TextView threshBarTextView[] = new TextView[PowerGarden.Device.PlantNum];
    
    TextView lightval;
    TextView distanceval;
    TextView tempval;
    TextView humval;
    TextView moistval;
    TextView streamMode;
    TextView rangeThresh;
    
    //int lastPlantDisplay[] = new int[PowerGarden.Device.PlantNum];
    TextView plantCopy;
  
    public boolean debugSensors = true;
    public boolean debugServer = false;
    
    Button closeDebug;
    Button audioTest;
    
    final long triggerTime = 1750;
    boolean bHaveData = false;
    String gotData;
    
    boolean bSetup = false;
    
    Messenger socketService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;
    /** Some text view we are using to show state information. */
    TextView mCallbackText;
      
    //boolean
	@Override
	public void signalToUi(int type, Object data) {
		Log.d(TAG, "PresentationViewable signalToUi");
		
		Runnable myrun = null;
		if(bSetup){
			if(type == PowerGarden.Settings){
				Log.d(TAG, "from SignalToUi > type .Settings");
				final Object _d = data;
				myrun = new Runnable(){
					public void run(){

//						int index = Integer.valueOf(_d.toString());
//						if (index >= 0){
//							threshBarTextView[ index ].setText( Integer.toString(PowerGarden.Device.plants[ index ].threshold));
//							threshBar[ index ].setProgress(PowerGarden.Device.plants[ index ].threshold);
//						} else rangeThresh.setText(Integer.toString(PowerGarden.Device.distanceThreshold));
					}
				};
			}
			
			if(type == PowerGarden.ThreshChange){
				Log.d(TAG, "from SignalToUi > type .ThreshChange");
				final Object _d = data;
				myrun = new Runnable(){
					public void run(){
						int index = Integer.valueOf(_d.toString());
						if(bSetup){
							if (index >= 0){
								threshBarTextView[ index ].setText( Integer.toString(PowerGarden.Device.plants[ index ].threshold));
								threshBar[ index ].setProgress(PowerGarden.Device.plants[ index ].threshold);
							} else rangeThresh.setText(Integer.toString(PowerGarden.Device.rangeLowThresh));
						}
						PowerGarden.savePref("threshVal_"+Integer.toString(index), Integer.toString(threshVal[index])); //set the thresh to sharedPref
						//sendMonkey("threshold",new Monkey("value",threshVal[index]),new Monkey("type","cap"),new Monkey("plant_index",index),new Monkey("device_id",PowerGarden.Device.ID));
					}
				};
			}
			
			//-- show debug screen
			else if (type == PowerGarden.StreamModeUpdate){
				Log.d(TAG, "from SignalToUi > type .StreamModeUpdate");
				final Object _d = data;
				myrun = new Runnable(){
					String stream_mode = _d.toString();
					public void run(){
						streamMode.setText(stream_mode);
						if (Boolean.valueOf(stream_mode)){
							streamMode.setTextColor(Color.GREEN);
						} else streamMode.setTextColor(Color.DKGRAY);
						if(PowerGarden.Device.datastream_mode == false){
						    debugSensors = false;
							activity_.resetView();
							activity_.mSystemUiHider.hide();
							activity_.delayedHide(100);
						}
					}
				};			
			}
			
			else if (type == PowerGarden.PlantIgnore){
				final int type_ = type;
				Log.d(TAG, "from SignalToUi > type .ThreshChange");
				final Object _d = data;
				
				myrun = new Runnable(){
					public void run(){
						int plantIndex = Integer.valueOf(_d.toString());
						if (PowerGarden.Device.plants[plantIndex].enabled){
							plantValView[plantIndex].setTextColor(Color.DKGRAY);
							threshBarTextView[plantIndex].setTextColor(Color.RED);
							threshBar[plantIndex].setEnabled(false);
						}
						else {
							threshBarTextView[plantIndex].setTextColor(Color.WHITE);
							plantValView[plantIndex].setTextColor(Color.WHITE);
							threshBar[plantIndex].setEnabled(true);
						}
						//mStatusline.setText("server says PLANT IGNORE\n\n " + Integer.toString(type_) + " : "+PowerGarden.serverResponseRaw);
						//saveDevicePrefs();
					}
				};			
			}
		}
		
		//***** arduino data coming in on signalToUi() ******//
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
		if(bHaveData){// && bSetup){
			String[] parseData = gotData.split(",");
			String dataType = new String(parseData[0]);
			Log.d(TAG, "GOT DATA MODE = "+dataType);

			if(dataType.equalsIgnoreCase("c")){ /** capacitance touch received from arduino **/
	
				final int plantDisplay[] = new int [8];
				final int plantDiffDisplay[] = new int [8];
				int delta = 0;
				int val = 0;
				//updateView(); //quick refresh of all PowerGarden.statics
				
				for(int i = 0; i<parseData.length-1; i++){
					val = Integer.parseInt(parseData[i+1]);
					
					plantDisplay[i] = val;
					
					Log.d("plantDisplay[i]", String.valueOf(val));
					
					PowerGarden.Device.plants[i].capCalculator.addValue(val);
					
					delta = PowerGarden.Device.plants[i].capCalculator.getDelta();
					
					if(i < 2 || i == 7) { //only have delta enabled on 0, 1, 7
						plantDiffDisplay[i] = delta;
						Log.d("plantDIFFDisplay[i]", String.valueOf(delta));
					}
					//PowerGarden.Device.plants[i].addValue(Integer.parseInt(parseData[i+1]));
					//plantDisplay[i] = PowerGarden.Device.plants[i].getFilteredValue(); //set the final here
					//Log.d(TAG, "cap: "+ Integer.toString(plantDisplay[i]));
				}
	
			    //if(debugSensors){
			    	//Log.d(TAG, "Should be debug");
					final int currDelta = delta;
					final int currVal = val;
					Runnable runner = new Runnable(){
						public void run() {
							for(int i =0;i<PowerGarden.Device.plants.length;i++){
								
								if(PowerGarden.Device.plants[i].enabled){ //only if this plant is enabled
									
									if(System.currentTimeMillis() - PowerGarden.Device.plants[i].touchedTimestamp > triggerTime){ //if we've hit trigger time max
										PowerGarden.Device.plants[i].triggered = false;
									}
									
									if((plantDiffDisplay[i] > PowerGarden.Device.plants[i].threshold) && 
											!PowerGarden.Device.plants[i].triggered){ //if we're over the threshold AND we're not triggered:
										
										Log.d(TAG, "currDelta over threshold : "+Integer.toString(currDelta));
										//play correct sound for this plant here
										PowerGarden.audioManager.playSound(i);
										
										//*** TOUCHED ***//
										PowerGarden.Device.plants[i].triggered = true; //set triggered to true for this plant
										PowerGarden.Device.plants[i].touchedTimestamp = System.currentTimeMillis(); //record timestamp
										PowerGarden.Device.plants[i].touchStamps.add(PowerGarden.Device.plants[i].touchedTimestamp); //add timestamp to overall touchStamps vector
										PowerGarden.stateManager.updatePlantStates(); //check this plant's state
										
										//******* for now
										PowerGarden.SM.plantTouch("touch", PowerGarden.Device.ID, i, plantDiffDisplay[i], PowerGarden.Device.plants[i].state, PowerGarden.Device.plants[i].touchStamps.size(), PresentationViewable.this );
										//*******	
										if (PowerGarden.stateManager.updateDeviceState() ){ // returns true if changed
											PowerGarden.Device.messageCopy = PowerGarden.stateManager.updateCopy();
											//activity_.resetView(); //now done in timerTask run() method
										}
									}
											
									if(bSetup){
										if(PowerGarden.Device.plants[i].triggered)
											plantValView[i].setTextColor(Color.GREEN);
										else plantValView[i].setTextColor(Color.WHITE);
										plantValView[i].setText(String.valueOf(plantDisplay[i]));
										
										if( i < 2 || i == 7) {
											if(PowerGarden.Device.plants[i].triggered){
												plantDiffView[i].setTextColor(Color.GREEN);
											} else {
												plantDiffView[i].setTextColor(Color.WHITE);												
											}
											plantDiffView[i].setText(String.valueOf(plantDiffDisplay[i]));
										}
									}
								}
							}
						}
					};
					if(runner != null){
						activity_.runOnUiThread(runner);
					}
			}
			
			//--- VIBRATION SENSOR DATA
			else if(dataType.equalsIgnoreCase("V")){ 
				
				final int plantDisplay[] = new int [8];
				int val = 0;
				//updateView(); //quick refresh of all PowerGarden.statics
				
				for(int i = 0; i<parseData.length-1; i++){
					
					if(PowerGarden.Device.plants[i+4].enabled){ //only if this plant is enabled
						val = Integer.parseInt(parseData[i+1]);
						plantDisplay[i+4] = val; //going to display on 4,5,6
						Log.d("plantVib"+Integer.toString((i+4)), String.valueOf(val));
					}
				}
				
				if(bSetup){ //update the debug screen if it's on
					Runnable runner = new Runnable(){
						public void run() {

							for(int i=0; i<3; i++){ //display vibration on plants 4, 5, 6
								
								if(System.currentTimeMillis() - PowerGarden.Device.plants[i+4].touchedTimestamp > triggerTime){ //if we've hit trigger time max
									PowerGarden.Device.plants[i+4].triggered = false;
								}
								
								if((plantDisplay[i+4] < 10) && !PowerGarden.Device.plants[i+4].triggered){ //if we're over the threshold AND we're not triggered:
									
									Log.d(TAG, "currVIB over threshold : "+Integer.toString(plantDisplay[i+4]));
									//play correct sound for this plant here
									PowerGarden.audioManager.playSound(i+4);
									
									//*** TOUCHED ***//
									PowerGarden.Device.plants[i+4].triggered = true; //set triggered to true for this plant
									PowerGarden.Device.plants[i+4].touchedTimestamp = System.currentTimeMillis(); //record timestamp
									PowerGarden.Device.plants[i+4].touchStamps.add(PowerGarden.Device.plants[i+4].touchedTimestamp); //add timestamp to overall touchStamps vector
									PowerGarden.stateManager.updatePlantStates(); //check this plant's state
									PowerGarden.SM.plantTouch("touch", PowerGarden.Device.ID, i+4, plantDisplay[i+4], PowerGarden.Device.plants[i+4].state, PowerGarden.Device.plants[i+4].touchStamps.size(), PresentationViewable.this );
									//*******	
									if (PowerGarden.stateManager.updateDeviceState() ){ // returns true if changed
										PowerGarden.Device.messageCopy = PowerGarden.stateManager.updateCopy();
										//activity_.resetView(); //now done in timerTask run() method
									}
								}
								
								if(debugSensors){
									if(PowerGarden.Device.plants[i+4].triggered)
										plantValView[i+4].setTextColor(Color.GREEN);
									else plantValView[i+4].setTextColor(Color.WHITE);
									plantValView[i+4].setText(String.valueOf(plantDisplay[i+4]));
								}
							}
						}
					};
					if(runner != null){
						activity_.runOnUiThread(runner);
					}
			    }
			}
			
			else if(dataType.equalsIgnoreCase("L")){
				PowerGarden.Device.light = Integer.parseInt(parseData[1]);
				//createJson("light",PowerGarden.light);
				final int light = PowerGarden.Device.light;
				if(debugSensors&& bSetup){
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
			}
			
			else if(dataType.equalsIgnoreCase("M")){
				PowerGarden.Device.moisture = Integer.parseInt(parseData[1]);
				//createJson("moisture",PowerGarden.moisture);
				final int moisture = PowerGarden.Device.moisture;
				if(debugSensors&& bSetup){
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
				PowerGarden.Device.temp = Integer.parseInt(parseData[1]);
				PowerGarden.Device.hum = Integer.parseInt(parseData[2]);
				//createJson("temperature",PowerGarden.temp, "humidity", PowerGarden.hum);
				final int temp = PowerGarden.Device.temp;
				final int hum = PowerGarden.Device.hum;
				if(debugSensors && bSetup){
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
			}else if(dataType.equalsIgnoreCase("R")){ // RANGE
				PowerGarden.Device.distance = Integer.parseInt(parseData[1]);
				Log.d("distqnce: ", Integer.toString(PowerGarden.Device.distance) );
				//createJson("distance",PowerGarden.distance);
//				if(PowerGarden.Device.distance > PowerGarden.Device.rangeLowThresh && )
//					PowerGarden.Device.rangeActive = true;
//				if(System.currentTimeMillis() - PowerGarden.Device.lastRangeHitTime > 3000){
//					PowerGarden.Device.rangeActive = true;
//				}
				
				if(PowerGarden.stateManager.updateDeviceState()){
					//activity_.updateStage(PowerGarden.deviceStateIndex);
					activity_.updateStage();
				}
				
				if(PowerGarden.Device.datastream_mode)
					PowerGarden.SM.updateData("update", PowerGarden.Device.ID, this);
				
				final int distance = PowerGarden.Device.distance;
				if(debugSensors && bSetup){
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
				PowerGarden.Device.light = Integer.parseInt(parseData[1]);
				PowerGarden.Device.temp = Integer.parseInt(parseData[2]);
				PowerGarden.Device.hum = Integer.parseInt(parseData[3]);
				PowerGarden.Device.moisture = Integer.parseInt(parseData[4]);
				PowerGarden.Device.distance = Integer.parseInt(parseData[5]);
				//objTest(new Object({"light",PowerGarden.light}));
				//objTest(new Monkey("string",49));
				sendUpdateData("update",new Monkey("light",PowerGarden.Device.light),new Monkey("distance",PowerGarden.Device.distance),new Monkey("temperature",PowerGarden.Device.temp),new Monkey("humidity",PowerGarden.Device.hum),new Monkey("moisture",PowerGarden.Device.moisture));
				PowerGarden.stateManager.updatePlantStates();
				PowerGarden.stateManager.updateDeviceState();
				
				PowerGarden.SM.updateData("update", PowerGarden.Device.ID,  this);
				final int distance = PowerGarden.Device.distance;
				if(debugSensors && bSetup){
					Runnable runner = new Runnable(){
						public void run() {
							distanceval.setText(String.valueOf(PowerGarden.Device.distance));
							lightval.setText(String.valueOf(PowerGarden.Device.light));
							moistval.setText(String.valueOf(PowerGarden.Device.moisture));
							humval.setText(String.valueOf(PowerGarden.Device.hum));
							tempval.setText(String.valueOf(PowerGarden.Device.temp));
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
		if(myrun != null){
			activity_.runOnUiThread(myrun);	
		}
	}

	
	public void sendUpdateData(String type, Monkey...monkey ){
		Log.d(TAG, "DEVICE ID: "+PowerGarden.Device.ID + " Registered: " +PowerGarden.bRegistered);
		if(PowerGarden.bRegistered){
		JSONObject j = new JSONObject();
		   long time = System.currentTimeMillis() / 1000L;
		   	try {
		   		Log.d(TAG, "createJson and SENDING:");
		   		for(int i = 0;i<monkey.length;i++){
		   			j.put(monkey[i].key.toString(), monkey[i].value);
		   		}	
		   	} catch (JSONException e) {
		   		Log.d(TAG, "CATCH ERROR");
		   		e.printStackTrace();
		   	}
		   //if(PowerGarden.bConnected)
		   PowerGarden.SM.updateData(type, PowerGarden.Device.ID.toString(), this);
		}
	}
	
	
	@Override
	public void sendMonkey(String type, Monkey...monkey ){
		Log.d(TAG, "DEVICE ID: "+PowerGarden.Device.ID + " Registered: " +PowerGarden.bRegistered);
		if(PowerGarden.bRegistered){
		JSONObject j = new JSONObject();
		   long time = System.currentTimeMillis() / 1000L;
		   	try {
		   		Log.d(TAG, "createJson and SENDING:");
		   		for(int i = 0;i<monkey.length;i++){
		   			j.put(monkey[i].key.toString(), monkey[i].value);
		   		}
		   				
		   	} catch (JSONException e) {
		   		Log.d(TAG, "CATCH ERROR");
		   		e.printStackTrace();
		   	}
		   PowerGarden.SM.sendMonkey(type, PowerGarden.Device.ID.toString(), j, this);
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
	    	
	    	plantDiffView[0] = (TextView) activity_.findViewById(R.id.plant_1_diff);
	    	plantDiffView[1] = (TextView) activity_.findViewById(R.id.plant_2_diff);
	    	plantDiffView[7] = (TextView) activity_.findViewById(R.id.plant_7_diff);
	    	
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
							sendMonkey("ignore",new Monkey("device_id",PowerGarden.Device.ID),new Monkey("plant_index",thisOne),new Monkey("ignore",!PowerGarden.Device.plants[thisOne].enabled));
						}
						else { 
							threshBarTextView[thisOne].setTextColor(Color.WHITE);
							plantValView[thisOne].setTextColor(Color.WHITE);
							sendMonkey("ignore",new Monkey("device_id",PowerGarden.Device.ID),new Monkey("plant_index",thisOne),new Monkey("ignore",!PowerGarden.Device.plants[thisOne].enabled));
							
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
	    streamMode = (TextView) activity_.findViewById(R.id.stream_mode);
	    rangeThresh = (TextView) activity_.findViewById(R.id.range_thresh);
	    
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
				//int tempAudioFile = (int) (Math.random()*8);
				PowerGarden.audioManager.playSound(-1); //test code
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
		
		/** onCreate, only hit once at startup **/
		
		PowerGarden.SM.registerViewableCallback(PresentationViewable.this);
		
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
		
		
		
//		plantCopy = (TextView) activity_.findViewById(R.id.stage_copy);
//		setTextViewFont(PowerGarden.italiaBook, plantCopy);
		
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
					sendMonkey("threshold",new Monkey("value",threshVal[i]),new Monkey("type","cap"),new Monkey("plant_index",i),new Monkey("device_id",PowerGarden.Device.ID));
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
		Log.d(TAG, "setState");
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

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
