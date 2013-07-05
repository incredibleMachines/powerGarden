package com.incredibleMachines.powergarden.util;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.util.Log;
import android.content.Context;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketConnectionHandler;



public class Websockets {
	static final String TAG = "com.incrediblemachines.powergarden.websockets";
//	static LinearLayout mLinearLayout;
//	private void alert(String message) {
//		 Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
//		 toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
//	     toast.show();
//	}
	public final WebSocket mConnection = new WebSocketConnection();
	
	//final String wsuri = "ws://" + mHostname.getText() + ":" + mPort.getText();
	
	public void start(String hostname, String port) {
		final String wsuri = "ws://"+hostname+":"+port;
	      try {
	          mConnection.connect(wsuri, new WebSocketConnectionHandler() {
	             @Override
	             public void onOpen() {
//	                mStatusline.setText("Status: Connected to " + wsuri);
//	                savePrefs();
//	                mSendMessage.setEnabled(true);
//	                mID.setEnabled(true);
//	                mNumPlants.setEnabled(true);
//	                mType.setEnabled(true);
	             }

	             @Override
	             public void onTextMessage(String payload) {
	                //alert("Got echo: " + payload);
	                try {
	             	   JSONObject incoming = new JSONObject(payload);
	             	   String status =incoming.getString("status"); 
	             	   
	             	   if(status.equals("connected")){
	             		  // alert("connected");
//	             		   mID.setText(incoming.getString("device_id") );
//	             		   mType.setText("update" );
//	             		   SharedPreferences.Editor editor = mSettings.edit();
//	             		   editor.putString("deviceID", incoming.getString("id"));
//	             		   editor.commit();
	             		   
	             	   }else if(status.equals("planted")){
	             		   	
	             		   //JSONObject plantedJSON = incoming.getJSONObject("plant");
	             		  // alert("Planted");
	             		   
//	             		   int i = plantedJSON.getInt("index");
//	             		   PlantData _plant = plantList.get(i);
//	             		   _plant.dbID = plantedJSON.getString("id");
//	             		   _plant.type = plantedJSON.getString("type");
//	             		   _plant.mood = plantedJSON.getString("mood");
//	             		   _plant.idEditText.setText( _plant.dbID );
//	             		   _plant.moodEditText.setText( _plant.mood );
//	             		   _plant.typeEditText.setText( _plant.type );
	             		   //set plant touch to 0 plant length to 0
	             	   }
	             	   
	             	   
	             	   
	                } catch (JSONException e) {
	             	   //TODO Auto-generated catch block
	             	   e.printStackTrace();
	                }
	                
	                
	                
	                
	             }

	             @Override
	             public void onClose(int code, String reason) {
	                //alert("Connection lost.");
//	                mStatusline.setText("Status: Ready.");
//	                setButtonConnect();
//	                mSendMessage.setEnabled(false);
//	                mType.setEnabled(false);
	             }
	          });
	       } catch (WebSocketException e) {

	          Log.d(TAG, e.toString());
	       }

	}
}
