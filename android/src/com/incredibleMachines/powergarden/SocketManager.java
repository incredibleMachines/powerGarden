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
	
	public void plantTouch(String type, String device_id, int plant_index, Connectable _callback){
		callbackActivity = _callback;
		try{
			socket.emit(type, 
					new JSONObject().put("device_id", device_id).put("plant_index", plant_index)
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
		Log.d(TAG, "INCOMING MESSAGE: "+args[0].toString());

		if(event.equals("register")){
			//System.out.println("HERE");
			
			try {
				JSONObject j = new JSONObject(args[0].toString() );
				if(PowerGarden.Device.ID == null){
					PowerGarden.Device.ID = j.getString("device_id");
					PowerGarden.savePref("deviceID",PowerGarden.Device.ID);
				}else{
					//SOMETHING IS FUCKED
					PowerGarden.Device.ID = j.getString("device_id");
					PowerGarden.savePref("deviceID",PowerGarden.Device.ID);
				}
				//System.out.println(PowerGarden.Device.ID);
				callbackActivity.signalToUi(PowerGarden.Registered, PowerGarden.Device.ID);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println(args[0]);
			
			
			//System.out.println(String.valueOf(args));
			
			//System.out.println(args.toString());
		}else if(event.equals("planted")){
			System.out.println(args.toString());
		}else if(event.equals("update")){
			try {
				JSONObject j = new JSONObject(args[0].toString() );
				if(PowerGarden.Device.ID != j.getString("device_id")){
					Log.e(TAG, "DEVICE ID MISMACTCH");
				}
				//PowerGarden.Device.ID = j.getString("device_id");
				PowerGarden.Device.deviceMood = j.getString("mood");
				PowerGarden.Device.messageCopy = j.getString("message");
//				PowerGarden.savePref("deviceID",PowerGarden.Device.ID);
//				System.out.println(PowerGarden.Device.ID);
				callbackActivity.signalToUi(PowerGarden.Updated, PowerGarden.Device.ID);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	
	void closeUpShop(){
		//sendSessionEnd();
		socket.disconnect();
	}
}
