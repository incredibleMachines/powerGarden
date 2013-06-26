package com.incredibleMachines.powergarden;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.victorint.android.usb.interfaces.Connectable;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.content.SharedPreferences;

public class ConnectSockets extends Activity implements Connectable {
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
	   static Button mSendUpdate;
	   static Button mSendTouch;
	   static Spinner mplantList;
	   //static ListView mplantsView;
	   
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
	            mSendTouch.setEnabled(true);
	            mSendUpdate.setEnabled(true);
	         }
	      });
	   }
	   
	   private void setupSendButton(){
           mSendMessage.setOnClickListener(new Button.OnClickListener() {
           	public void onClick(View v) {
           		PowerGarden.SM.authDevice(mType.getText().toString(), mID.getText().toString(), Integer.parseInt(mNumPlants.getText().toString()), mplantList.getSelectedItem().toString() , ConnectSockets.this);
           		PowerGarden.Device.plantType = mplantList.getSelectedItem().toString();
           	}
           });
           
           mSendTouch.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
	        	   PowerGarden.SM.plantTouch("touch", mID.getText().toString(), 2, ConnectSockets.this );				
			}
           });
           
           mSendUpdate.setOnClickListener(new Button.OnClickListener(){
        	   public void onClick(View r){
        		   Log.d(TAG, "Update clicked !");
        		   JSONObject j = new JSONObject();
        		   long time = System.currentTimeMillis() / 1000L;
        		   	try {
        		   		//j.put("moisture", 2131).put("temp", 89.75).put("humidity", 66.32).put("light", 3324.32).put("timestamp", time );
        		   		j.put("moisture", PowerGarden.moisture).put("temp", PowerGarden.temp).put("humidity", PowerGarden.hum).put("light", PowerGarden.light).put("timestamp", time );
        		   	} catch (JSONException e) {
        		   		e.printStackTrace();
        		   	}
        		   PowerGarden.Device.plantType = mplantList.getSelectedItem().toString();
        		   PowerGarden.SM.updateData("update", mID.getText().toString(), j, ConnectSockets.this);
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
	            mSendTouch.setEnabled(false);
	            mSendUpdate.setEnabled(false);
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
	    mSendTouch = (Button) findViewById(R.id.sendTouch);
	    mSendUpdate=(Button) findViewById(R.id.sendUpdate);
	    mSendMessage.setEnabled(false);
	    mplantList = (Spinner) findViewById(R.id.type_plants);
	    //mplantsView = (ListView) findViewById(R.id.plants);
	    //mLinearLayout = (LinearLayout)findViewById(R.id.plants);
	    mSettings = getSharedPreferences(PREFS_NAME, 0);
	    
	    ArrayAdapter <CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.plants_array, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    mplantList.setAdapter(adapter);
	    
	    loadPrefs();

	    setButtonConnect();
	    setupSendButton();
		
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
		Runnable myrun = null;
		if(type == PowerGarden.Connected){
			myrun = new Runnable(){
				public void run(){
					mID.setText(PowerGarden.Device.ID);
					saveDevicePrefs();
				}
			};
			
		}
		Log.d(TAG, "Got type: " + type);
		this.runOnUiThread(myrun);
		
	}


}
