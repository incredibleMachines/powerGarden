package com.incredibleMachines.powergarden;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import org.json.JSONObject;

public class SocketManager implements  IOCallback {

	private SocketIO socket;

	
	SocketManager(){
		//connectToServer();
	}
	
	public void connectToServer(String host, String port){
		
		try {
			
			// todo: read this from a text file
			
			socket = new SocketIO();
			//socket.connect("http://thetalkingshoe.com:6090/", this);
			
			socket.connect(host+":"+port+"/", this);
	
			// Sends a string to the server.
			//socket.send("Hello Server");
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onMessage(JSONObject json, IOAcknowledge ack) {
		//try {
			//System.out.println("Server said:" + json.toString(2));
		//} catch (JSONException e) {
	//		e.printStackTrace();
	//	}
	}

	@Override
	public void onMessage(String data, IOAcknowledge ack) {
		//System.out.println("Server said: " + data);
	}

	@Override
	public void onError(SocketIOException socketIOException) {
		//System.out.println("an Error occured");
		socketIOException.printStackTrace();
	}

	@Override
	public void onDisconnect() {
		//System.out.println("Connection terminated.");
	}

	@Override
	public void onConnect() {
		//System.out.println("Connection established");
	}

	@Override
	public void on(String event, IOAcknowledge ack, Object... args) {
		//System.out.println("Server triggered event '" + event + "'");
	}

	
	void closeUpShop(){
		//sendSessionEnd();
	}
}
