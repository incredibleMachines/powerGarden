package com.incredibleMachines.powergarden;

import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.content.SharedPreferences;

public class ConnectSockets extends Activity {
	   private static final String TAG = "ConnectSocketsView";
	   private static final String PREFS_NAME = "PowerGarden";
	   static EditText mHostname;
	   static EditText mPort;
	   static TextView mStatusline;
	   static Button mStart;

	   static EditText mType;
	   static EditText mID;
	   static EditText mNumPlants;
	   static Button mSendMessage;
	   static ListView mplantsView;
	   static LinearLayout mLinearLayout;
	   //final ArrayList<PlantData> plantList = new ArrayList<PlantData>();
	   

	   
	   private SharedPreferences mSettings;

	   private void alert(String message) {
	      Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
	      toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
	      toast.show();
	   }

	   private void loadPrefs() {

	      mHostname.setText(mSettings.getString("hostname", "192.168.1.0"));
	      mPort.setText(mSettings.getString("port", "9001"));
	      mID.setText(mSettings.getString("deviceID", "set_id"));
	      mNumPlants.setText(mSettings.getString("numPlants","8"));
	      
	   }

	   private void saveDevicePrefs(){
		   SharedPreferences.Editor editor = mSettings.edit();
		   editor.putString("deviceID", mID.getText().toString());
		   editor.putString("numPlants", mNumPlants.getText().toString());
		   editor.commit();
	   }
	 
	   private void saveServerPrefs() {

	      SharedPreferences.Editor editor = mSettings.edit();
	      editor.putString("hostname", mHostname.getText().toString());
	      editor.putString("port", mPort.getText().toString());
	      //editor.putString("deviceID", mID.getText().toString());
	      //editor.putString("numPlants", mNumPlants.getText().toString());
	      editor.commit();
	   }

	   private void setButtonConnect() {
	      mHostname.setEnabled(true);
	      mPort.setEnabled(true);
	      mStart.setText("Connect");
	      mStart.setOnClickListener(new Button.OnClickListener() {
	         public void onClick(View v) {
	        	Log.d(TAG, "Clicked Connect");
	            PowerGarden.SM.connectToServer(mHostname.getText().toString(), mPort.getText().toString());
	            saveServerPrefs();
	            setButtonDisconnect();
	            mSendMessage.setEnabled(true);
	         }
	      });
	   }
	   
	   private void setupSendButton(){
           mSendMessage.setOnClickListener(new Button.OnClickListener() {
           	public void onClick(View v) {
           		PowerGarden.SM.authDevice(mType.getText().toString(), mID.getText().toString());
           	}
           });
	   }
	   private void setButtonDisconnect() {
	      mHostname.setEnabled(false);
	      mPort.setEnabled(false);
	      mStart.setText("Disconnect");
	      mType.setText("register");
	      mStart.setOnClickListener(new Button.OnClickListener() {
	         public void onClick(View v) {
	            PowerGarden.SM.closeUpShop();
	            mSendMessage.setEnabled(false);
	            setButtonConnect();
	         }
	      });
	   }
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connect_sockets);
		Log.d(TAG, "ConnectSockets Start");
		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(R.id.fullscreen_content);
		
	    mHostname = (EditText) findViewById(R.id.hostname);
	    mPort = (EditText) findViewById(R.id.port);
	    mStatusline = (TextView) findViewById(R.id.statusline);
	    mStart = (Button) findViewById(R.id.start);
	    mType = (EditText) findViewById(R.id.type);
	    mID = (EditText) findViewById(R.id.device_id);
	    mNumPlants = (EditText) findViewById(R.id.num_plants);
	    mSendMessage = (Button) findViewById(R.id.sendMsg);
	    mSendMessage.setEnabled(false);
	    //mplantsView = (ListView) findViewById(R.id.plants);
	    //mLinearLayout = (LinearLayout)findViewById(R.id.plants);
	    mSettings = getSharedPreferences(PREFS_NAME, 0);

	    
	    loadPrefs();

	    setButtonConnect();
	    setupSendButton();
		
	}


}
