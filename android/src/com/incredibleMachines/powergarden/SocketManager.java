package com.incredibleMachines.powergarden;

import java.net.MalformedURLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;


import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import com.incredibleMachines.powergarden.PowerGarden.Device;
import com.victorint.android.usb.interfaces.Connectable;


import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class SocketManager extends TimerTask  implements  IOCallback, Connectable {

	private SocketIO socket;
	private static long connectTimeout  = 5000;

	private static String TAG = "SocketManager";
	private Connectable callbackActivity;
	private Connectable presentationCallback;
	//final private Timer backgroundTimer = new Timer("backgroundTimer");
	private boolean firstConnect = true;

	SocketManager(){
		//connectToServer();
	}
	
	public void registerViewableCallback (Connectable _callback){
		presentationCallback = _callback;
	}
	
	public void authDevice (String type, String device_id, int num_plants, String plant_type, Connectable _callback){
		Log.wtf(TAG, "authDevice");
		callbackActivity = _callback;
		try {

			socket.emit(type, 
				 new JSONObject().put("device_id", device_id).put("plant_type", plant_type).put("num_plants", num_plants)
					);
			} catch (Exception e) {
				e.printStackTrace();
			}
		
	}
	public void authDevice(String type, String device_id, Connectable _callback){
		Log.wtf(TAG, "authDevice");
		callbackActivity = _callback;
		try {

			socket.emit(type, 
				 new JSONObject().put("device_id", device_id)
					);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	
	public void plantTouch(String type, String device_id, int plant_index, int cap_val, String state, long numTouches, Connectable _callback){
		callbackActivity = _callback;
		String thisState = PowerGarden.Device.plants[plant_index].state;
		Log.d("touch state for plantindex "+Integer.toString(plant_index)+": ", thisState);
		
		try{
			socket.emit(type, 
					new JSONObject().put("device_id", device_id).put("plant_index", plant_index).put("cap_val", cap_val).put("state", thisState).put("count", numTouches) //added capval here
					);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void updateData(String type, String device_id, Connectable _callback){
		callbackActivity = _callback;
		JSONObject data = new JSONObject();
		JSONArray plants = new JSONArray();
		JSONObject state = new JSONObject();
		
		try{
			for(int i=0; i<PowerGarden.Device.PlantNum; i++){
				JSONObject thisPlant = new JSONObject();
				thisPlant.put("state", PowerGarden.Device.plants[i].state).put("index",i).put("count", PowerGarden.Device.plants[i].touchStamps.size());
				plants.put(thisPlant);
			}
			data.put("moisture", PowerGarden.Device.moisture).put("temp", PowerGarden.Device.temp).put("humidity", PowerGarden.Device.hum).put("light", PowerGarden.Device.light).put("range",PowerGarden.Device.distance);
			state.put("moisture", PowerGarden.stateManager.getDeviceMoisture()).put("touch", StateManager.getDeviceState()).put("plants", plants);
			socket.emit(type, 
					new JSONObject().put("device_id", device_id).put("plant_type", PowerGarden.Device.plantType).put("data", data).put("state", state)
					);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void sendMonkey(String type, String device_id, JSONObject json, Connectable _callback){
		callbackActivity = _callback;
		Log.d("SENDING SOCKET EVENT: ", type+": "+ json.toString());
		
		try{
			socket.emit(type, json);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void connectToServer(String host, String port, Connectable _callback){
		callbackActivity = _callback;
		try {
			Log.wtf(TAG, "connectToServer");
			Log.d(TAG, "callbackActivity: " + _callback.toString());
			
			PowerGarden.Device.host = host;
			PowerGarden.Device.port = port;
			
			//todo: read this from a text file
				socket = new SocketIO();
				socket.connect("http://"+host+":"+port+"/", this);
				//backgroundTimer.schedule(this, connectTimeout);
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onMessage(JSONObject json, IOAcknowledge ack) {
		Log.d(TAG,"On Message: ");
		try {
			System.out.println("Server said:" + json.toString(2));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(String data, IOAcknowledge ack) {
		System.out.println("Server said: " + data);
	}

	@Override
	public void onError(SocketIOException socketIOException) {
		System.out.println("an Error occured");
		socketIOException.printStackTrace();

		
		if(socketIOException.getMessage().equals("1+0")){
			Log.d(TAG, "hit onError '1+0'");
			PowerGarden.bConnected = false;
		}
		if(socketIOException.getMessage().equals("Timeout Error")){
			PowerGarden.bConnected = false;
		}
		if(socketIOException.getMessage().equals("Error while handshaking")){
			PowerGarden.bConnected = false;

		}
		
/*		if(!PowerGarden.bConnected){ //--- if one of these three errors happened, try reconnecting
			socket = new SocketIO();
			try {
				socket.connect("http://"+PowerGarden.Device.host+":"+PowerGarden.Device.port+"/", this);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
*/
		
		//--- regardless, onError, restart connection 
		socket = new SocketIO();
		try {
			socket.connect("http://"+PowerGarden.Device.host+":"+PowerGarden.Device.port+"/", this);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onDisconnect() {
		System.out.println("Connection terminated.");
	}

	@Override
	public void onConnect() {
		//System.out.println("Connection established");
		Log.d(TAG, "onConnect callback");
		callbackActivity.signalToUi(PowerGarden.SocketConnected, null);
		PowerGarden.bConnected = true;
	}

	@Override 
	public void on(String event, IOAcknowledge ack, Object... args) { //incoming from server
		PowerGarden.bConnected = true;
		System.out.println("Server triggered event '" + event + "'");
//		Log.d(TAG, "INCOMING MESSAGE: "+args[0].toString());
		PowerGarden.serverResponseRaw = args[0].toString();
		JSONObject j;

		try {
			j = new JSONObject(args[0].toString() );
			
			//**** register ****//
			if(event.equals("register")){
				if(PowerGarden.Device.ID == null){
					PowerGarden.Device.ID = j.getString("device_id");
					Log.d(TAG, "Device.ID now set to: "+ PowerGarden.Device.ID);
					PowerGarden.savePref("deviceID",PowerGarden.Device.ID);
				}else{
					//SOMETHING IS FUCKED
					PowerGarden.Device.ID = j.getString("device_id");
					PowerGarden.savePref("deviceID",PowerGarden.Device.ID);
				}
				callbackActivity.signalToUi(PowerGarden.Registered, PowerGarden.Device.ID);	
			}
			
			else if(event.equals("threshold")){
				String thisType; //range or capacitive
				int thisValue = 0;;
				int thisPlantIndex = 0; //only needed for capacitive
				if(j.has("value")){
					thisValue = Integer.valueOf(j.getString("value"));
					if(j.has("plant_index")){
						thisPlantIndex = Integer.valueOf(j.getString("plant_index"));
					}
					if(j.has("type")){
						thisType = j.getString("type");
						//Log.d(TAG, "type = "+thisType);
						if(thisType.contentEquals("cap")){
							Log.d(TAG, "CAP thresh update: thisValue = "+Integer.toString(thisValue));
							PowerGarden.Device.plants[thisPlantIndex].threshold = thisValue;
							PowerGarden.savePref("threshVal_"+Integer.toString(thisPlantIndex), Integer.toString(PowerGarden.Device.plants[thisPlantIndex].threshold));
							
							callbackActivity.signalToUi(PowerGarden.ThreshChange, thisPlantIndex);
							if(presentationCallback != null) presentationCallback.signalToUi(PowerGarden.ThreshChange, thisPlantIndex);
							
						} else {
							Log.d(TAG, "RANGE thresh update: thisValue = "+Integer.toString(thisValue));
							PowerGarden.Device.rangeLowThresh = thisValue;
							PowerGarden.savePref("rangeThresh", Integer.toString(PowerGarden.Device.rangeLowThresh));
							
							callbackActivity.signalToUi(PowerGarden.ThreshChange, thisType);
							if(presentationCallback != null) presentationCallback.signalToUi(PowerGarden.ThreshChange, "-1");
						}
					}
				}
				//callbackActivity.signalToUi(PowerGarden.ThreshChange, PowerGarden.Device.ID);
			}
			
			//**** CHORUS CONTROL ****//
			else if(event.equals("chorus")){
				//Log.d(TAG, "recieved CHORUS CONTROL: "+ j.toString());
				if(j.has("start_time")){
					  
					try { //check to see if we have a bool for start_time
						Boolean thisone = j.getBoolean("start_time"); // if it's a bool, it's FALSE aka stop audio
						Log.d(TAG, "start_time == false");
						callbackActivity.signalToUi(PowerGarden.SetChorusTime, "stop");
						if(presentationCallback != null) presentationCallback.signalToUi(PowerGarden.SetChorusTime, "stop");
					}
					
					catch(JSONException e){ //no bool, it's a UNIX timestamp
						String startTimeString = j.getString("start_time");

						long startTime = Long.valueOf(startTimeString); 
						Log.d(TAG, "chorus start time set to: "+ Long.toString(startTime));
						
						long currentTime = System.currentTimeMillis();
						Log.d(TAG, "current system time: "+ Long.toString(currentTime));
						
						long diff = startTime - currentTime;
						Log.d(TAG, "diff in time: "+ Long.toString(diff));
						
						PowerGarden.Device.chorus_start_time = diff;
	
						if(j.has("file_name")){
							Log.d(TAG, "chorus file_name: "+j.getString("file_name"));
							PowerGarden.Device.chorus_filename = j.getString("file_name");
						}

						callbackActivity.signalToUi(PowerGarden.SetChorusTime, PowerGarden.Device.ID);
						if(presentationCallback != null) presentationCallback.signalToUi(PowerGarden.SetChorusTime, PowerGarden.Device.chorus_start_time);
					}
				}
			}
			
			//**** TABLET CONTROL ****//
			else if(event.equals("tablet")){
				Log.d(TAG, "recieved TABLET CONTROL: "+ j.toString());
				if(j.has("brightness")){
					Log.d(TAG, "brightness set to: "+j.getString("brightness"));
					PowerGarden.Device.tablet_brightness = (Integer.valueOf(j.getString("brightness")))/100.f;
					}
				if(j.has("volume")){
					Log.d(TAG, "volume set to: "+j.getString("volume"));
					PowerGarden.Device.tablet_volume = Integer.valueOf(j.getString("volume"));
				}
				callbackActivity.signalToUi(PowerGarden.TabletSettings, PowerGarden.Device.ID);
				if(presentationCallback != null) presentationCallback.signalToUi(PowerGarden.TabletSettings, PowerGarden.Device.ID);
			}

			
			//**** stream control update ****//
			else if(event.equals("firehose")){
				
				int thisPlantIndex = 0;
				//if(j.has("firehose")){
					PowerGarden.Device.datastream_mode = j.getBoolean("stream");//Boolean.parseBoolean(j.getString("stream"));
					Log.d(TAG, "datastream_mode set to: "+j.getString("stream"));
					callbackActivity.signalToUi(PowerGarden.StreamModeUpdate, PowerGarden.Device.ID);
					if(presentationCallback != null) presentationCallback.signalToUi(PowerGarden.StreamModeUpdate, PowerGarden.Device.datastream_mode);
				//}
			}
			
			
			//**** enable/disable plant touches by index ****//
			else if(event.equals("ignore")){
				
				int thisPlantIndex = 0;
				if(j.has("ignore")){
					if(j.has("plant_index")){
						thisPlantIndex = Integer.valueOf(j.getString("plant_index"));
						PowerGarden.Device.plants[thisPlantIndex].enabled = Boolean.valueOf(j.getString("ignore"));
						PowerGarden.savePref("plantIgnore_"+Integer.toString(thisPlantIndex), Integer.toString(PowerGarden.Device.plants[thisPlantIndex].threshold));
						Log.d(TAG, "set ignore to " + j.getString("ignore")+" on plant "+j.getString("plant_index"));
					}
					callbackActivity.signalToUi(PowerGarden.PlantIgnore, thisPlantIndex);
					if(presentationCallback != null) presentationCallback.signalToUi(PowerGarden.PlantIgnore, thisPlantIndex);
				}
			}
			

			//**** display tweet ****//
			else if(event.equals("tweet")){

				if(j.has("text")){
					if(j.has("user_name")){
						String name = j.getString("user_name");
						PowerGarden.Device.tweetUsername.add("@"+name);
						String text = j.getString("text");
						PowerGarden.Device.tweetCopy.add(text);
						PowerGarden.Device.displayMode = PowerGarden.DisplayMode.Tweet;
						Log.wtf(TAG,"TWEET RECEIVED: ");
						Log.wtf(TAG, PowerGarden.Device.tweetCopy.lastElement());
					}
				}
				if(j.has("plant_type")){
					String text = j.getString("text");
					//don't believe we'll use this.
				}
				if(j.has("water")){
					Boolean isWatering = Boolean.valueOf(j.getString("water"));
					Log.d("water: ", j.getString("water"));
//					PowerGarden.Device.isWatering = isWatering; //not using this at the moment.
					//if this is a watering tweet, garden is watering, let's play water sounds.
					if(isWatering){ 
						PowerGarden.audioManager.playSound(-4); //only sound triggered by a tweet event !
						Log.d(TAG, "isWatering == TRUE");
					}
				}

				callbackActivity.signalToUi(PowerGarden.DisplayTweet, PowerGarden.Device.ID);
				if(presentationCallback != null) presentationCallback.signalToUi(PowerGarden.DisplayTweet, PowerGarden.Device.ID);
			}


			//**** settings ****//
			else if(event.equals("settings")){
				
				if(j.has("humidity")){
					JSONObject humidity = new JSONObject();
					humidity = j.getJSONObject("humidity");
				
					PowerGarden.savePref("humidity_active", humidity.getString("active"));
					PowerGarden.savePref("humidity_thres_high", humidity.getString("high"));
					PowerGarden.savePref("humidity_thres_low", humidity.getString("low"));
				}
				
				if(j.has("temp")){
					JSONObject temp = new JSONObject();
					temp = j.getJSONObject("temp");
				
					PowerGarden.savePref("temp_active", temp.getString("active"));
					PowerGarden.savePref("temp_thres_high", temp.getString("high"));
					PowerGarden.savePref("temp_thres_low", temp.getString("low"));
				}
				
				if(j.has("moisture")){
					JSONObject moisture = new JSONObject();
					moisture = j.getJSONObject("moisture");
				
					PowerGarden.savePref("moisture_active", moisture.getString("active"));
					PowerGarden.savePref("moisture_thres_high", moisture.getString("high"));
					PowerGarden.savePref("moisture_thres_low", moisture.getString("low"));
					
					PowerGarden.Device.moistureActive = Boolean.valueOf(moisture.getString("active"));
					PowerGarden.Device.moistureHighThresh = Integer.parseInt(moisture.getString("high"));
					PowerGarden.Device.moistureLowThresh = Integer.parseInt(moisture.getString("low"));				
				}

				if(j.has("light")){
					JSONObject light = new JSONObject();
					light = j.getJSONObject("light");
					PowerGarden.savePref("light_active", light.getString("active"));
					PowerGarden.savePref("light_thres_high", light.getString("high"));
					PowerGarden.savePref("light_thres_low", light.getString("low"));
					
					PowerGarden.Device.lightActive = Boolean.valueOf(light.getString("active"));
					PowerGarden.Device.lightHighThresh = Integer.parseInt(light.getString("high"));
					PowerGarden.Device.lightLowThresh = Integer.parseInt(light.getString("low"));	
				}
				
				if(j.has("touch")){
					
					JSONObject touch = new JSONObject();
					touch = j.getJSONObject("touch");
				
					PowerGarden.savePref("touch_active", touch.getString("active"));
					PowerGarden.savePref("touch_thres_high", touch.getString("high"));
					PowerGarden.savePref("touch_thres_low", touch.getString("low"));
					PowerGarden.savePref("touch_window", touch.getString("window"));
					
					PowerGarden.Device.touchActive = Boolean.valueOf(touch.getString("active"));
					PowerGarden.Device.touchHighThresh = Integer.parseInt(touch.getString("high"));
					PowerGarden.Device.touchLowThresh = Integer.parseInt(touch.getString("low"));
					PowerGarden.Device.touchWindow = Integer.parseInt(touch.getString("window"));
				}
				
				if(j.has("range")){
					JSONObject range = new JSONObject();
					range = j.getJSONObject("range");
					
					PowerGarden.savePref("range_active", range.getString("active"));
					PowerGarden.savePref("range_thres_low", range.getString("low"));
				
					PowerGarden.Device.rangeActive = Boolean.valueOf(range.getString("active"));
					PowerGarden.Device.rangeLowThresh = Integer.parseInt(range.getString("low"));
				}
				
				if(j.has("duration")){
					//some sort of text display duration setting....
				}
				
				
				callbackActivity.signalToUi(PowerGarden.Settings, PowerGarden.Device.ID);
				if(presentationCallback != null) presentationCallback.signalToUi(PowerGarden.Settings, PowerGarden.Device.ID);
			}
			
			//**** touched ****//  DEPRECATED, never used
			else if(event.equals("touch")){
				
				if(j.has("mood")){
					if(j.has("index")){
						//Log.wtf(TAG, "mood set to: " + j.getString("mood"));
						PowerGarden.Device.plants[Integer.parseInt(j.getString("index"))].state = j.getString("mood");
					}
				}
				callbackActivity.signalToUi(PowerGarden.Touched, PowerGarden.Device.ID);
			}
			
			else {
				
				callbackActivity.signalToUi(PowerGarden.Unrecognized, PowerGarden.Device.ID);
			}
			
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	void closeUpShop(){
		
		Log.e(TAG, "closeup shop");
		socket.disconnect();
	}

	@Override
	public void run() {
		Log.d("TIMER", "hit run()");
		// TODO Auto-generated method stub
		//if(!firstConnect){
			Log.e(TAG,"CONNECT TIMED OUT");
			//socket.disconnect();
			//connectToServer(PowerGarden.Device.host,PowerGarden.Device.port,callbackActivity);
			//PowerGarden.SM.authDevice("register", PowerGarden.Device.ID, PowerGarden.Device.PlantNum, PowerGarden.Device.plantType, this);
		//} else firstConnect = false;
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
	public void close() {
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

	@Override
	public void signalToUi(int type, Object data) {
		// TODO Auto-generated method stub
		
	}
}
