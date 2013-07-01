package com.incredibleMachines.powergarden;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import com.victorint.android.usb.interfaces.Connectable;


import android.app.Activity;
import android.util.Log;

public class SocketManager implements  IOCallback {

	private SocketIO socket;

	private static String TAG = "SocketManager";
	private Connectable callbackActivity;
	
	SocketManager(){
		//connectToServer();
	}
	public void authDevice (String type, String device_id, int num_plants, String plant_type, Connectable _callback){
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
		callbackActivity = _callback;
		try {

			socket.emit(type, 
				 new JSONObject().put("device_id", device_id)
					);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	
	public void plantTouch(String type, String device_id, int plant_index, int cap_val, Connectable _callback){
		callbackActivity = _callback;
		try{
			socket.emit(type, 
					new JSONObject().put("device_id", device_id).put("plant_index", plant_index).put("cap_val", cap_val)
					);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void updateData(String type, String device_id, JSONObject json, Connectable _callback){
		callbackActivity = _callback;
		
		try{
			socket.emit(type, 
					new JSONObject().put("device_id", device_id).put("data", json)
					);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void connectToServer(String host, String port, Connectable _callback){
		callbackActivity = _callback;
		try {
			Log.d(TAG, "connectToServer");
			Log.d(TAG, _callback.toString());
			
			//todo: read this from a text file
				socket = new SocketIO();
				socket.connect("http://"+host+":"+port+"/", this);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onMessage(JSONObject json, IOAcknowledge ack) {
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
	}

	@Override
	public void on(String event, IOAcknowledge ack, Object... args) {
		
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
			
			//**** update ****//
			else if(event.equals("update")){
				try {
					//JSONObject j = new JSONObject(args[0].toString() );
					if(!PowerGarden.Device.ID.contentEquals(j.getString("device_id"))){
						Log.e(TAG, "DEVICE ID MISMATCH");
						Log.e(TAG, "PowerGarden.Device.ID: "+ PowerGarden.Device.ID);
						Log.e(TAG, "incoming Device.ID: "+ j.getString("device_id"));
						
					}
					//PowerGarden.Device.ID = j.getString("device_id");
					PowerGarden.Device.deviceMood = j.getString("mood");
					PowerGarden.Device.messageCopy = j.getString("message");
//					PowerGarden.savePref("deviceID",PowerGarden.Device.ID);
//					System.out.println(PowerGarden.Device.ID);
					
					if(j.has("message")){
						PowerGarden.Device.messageCopy = j.getString("message");
						Log.d(TAG, "received 'message'");
						Log.d(TAG, PowerGarden.Device.messageCopy);
						callbackActivity.signalToUi(PowerGarden.MessageUpdated, PowerGarden.Device.messageCopy);
					} else Log.wtf(TAG, "NO 'message' received");
					
					if(j.has("mood")){
						//do something yo
					}
					
					callbackActivity.signalToUi(PowerGarden.Updated, PowerGarden.Device.ID);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//**** touched ****//
			else if(event.equals("touch")){
				
				if(j.has("mood")){
					if(j.has("index")){
						Log.wtf(TAG, "mood set to: " + j.getString("mood"));
						PowerGarden.Device.plants[Integer.parseInt(j.getString("index"))].mood = j.getString("mood");
					}
				}
				callbackActivity.signalToUi(PowerGarden.Touched, PowerGarden.Device.ID);
			}
			
			//**** control message ****  *** FUTURE ***//
			else if(event.equals("control")){
				//https://docs.google.com/a/incrediblemachines.net/document/d/1ue7jnC6fR7SFgvZNny9MtHGbP_Sm8F-FrgUELhT9OKo/edit
				if(j.has("distance_thresh")){
					PowerGarden.Device.distanceThreshold = Integer.parseInt(j.getString("distance_thresh"));
				}
				if(j.has("cap_thresh")){
					JSONObject thresholds = new JSONObject();
					thresholds = j.getJSONObject("cap_thresh");
					for(int i=0; i<PowerGarden.Device.PlantNum; i++){
						if (thresholds.has(Integer.toString(i))){
							PowerGarden.Device.plants[i].threshold = Integer.parseInt(thresholds.getString(Integer.toString(i)));
						}
					}
				}
				if(j.has("datastream_mode")){
					PowerGarden.Device.datastream_mode = Boolean.parseBoolean(j.getString("datastream_mode"));
				}
				
				//**** NEEDS TO BE IMPLEMENTED HERE:
				//callbackActivity.signalToUi(PowerGarden.ControlChange, PowerGarden.Device);
			}
			
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	

	/**** LAST WORKING VERSION ****/
//	public void on(String event, IOAcknowledge ack, Object... args) {
//		
//		System.out.println("Server triggered event '" + event + "'");
//		Log.d(TAG, "INCOMING MESSAGE: "+args[0].toString());
//		PowerGarden.serverResponseRaw = args[0].toString();
//		JSONObject j;
////		try {
////			//j = new JSONObject("{null}");
////		} catch (JSONException e2) {
////			// TODO Auto-generated catch block
////			e2.printStackTrace();
////		}
//		try {
//			j = new JSONObject(args[0].toString() );
//		} catch (JSONException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
//		if(event.equals("register")){
//			//System.out.println("HERE");
//			//PowerGarden.Device.ID = j.getString("device_id");
//			try {
//				//JSONObject j = new JSONObject(args[0].toString() );
//				if(PowerGarden.Device.ID == null){
//					PowerGarden.Device.ID = j.getString("device_id");
//					PowerGarden.savePref("deviceID",PowerGarden.Device.ID);
//				}else{
//					//SOMETHING IS FUCKED
//					PowerGarden.Device.ID = j.getString("device_id");
//					PowerGarden.savePref("deviceID",PowerGarden.Device.ID);
//				}
//				
//				//System.out.println(PowerGarden.Device.ID);
//			
//				callbackActivity.signalToUi(PowerGarden.Registered, PowerGarden.Device.ID);
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//			//System.out.println(args[0]);
//			//System.out.println(String.valueOf(args));
//			//System.out.println(args.toString());
//			
//		}else if(event.equals("planted")){
//			System.out.println(args.toString());
//		}else if(event.equals("update")){
//			try {
//				//JSONObject j = new JSONObject(args[0].toString() );
//				if(!PowerGarden.Device.ID.contentEquals(j.getString("device_id"))){
//					Log.e(TAG, "DEVICE ID MISMATCH");
//					Log.e(TAG, "PowerGarden.Device.ID: "+ PowerGarden.Device.ID);
//					Log.e(TAG, "incoming Device.ID: "+ j.getString("device_id"));
//					
//				}
//				//PowerGarden.Device.ID = j.getString("device_id");
//				PowerGarden.Device.deviceMood = j.getString("mood");
//				PowerGarden.Device.messageCopy = j.getString("message");
////				PowerGarden.savePref("deviceID",PowerGarden.Device.ID);
////				System.out.println(PowerGarden.Device.ID);
//				
//				if(j.has("message")){
//					PowerGarden.Device.messageCopy = j.getString("message");
//					Log.wtf(TAG, "received 'message'");
//					Log.wtf(TAG, PowerGarden.Device.messageCopy);
//					callbackActivity.signalToUi(PowerGarden.MessageUpdated, PowerGarden.Device.messageCopy);
//				} else Log.wtf(TAG, "NO 'message' received");
//				
//				if(j.has("mood")){
//					//do something yo
//				}
//				
//				
//				callbackActivity.signalToUi(PowerGarden.Updated, PowerGarden.Device.ID);
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		
//	}

	
	void closeUpShop(){
		//sendSessionEnd();
		socket.disconnect();
	}
}
