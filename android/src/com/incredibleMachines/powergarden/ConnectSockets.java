package com.incredibleMachines.powergarden;

import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import com.victorint.android.usb.interfaces.Connectable;

import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;

public class ConnectSockets extends Activity implements Connectable {
	   private static final String TAG = "ConnectSockets Activity";
	   static EditText mHostname;
	   static EditText mPort;
	   static TextView mStatusline;
	   static Button mStart;

	   //static EditText mType;
	   static EditText mID;
	   static EditText mNumPlants;
	   static Button mSendMessage;
	   static Button mSendUpdate;
	   static Button mSendTouch;
	   static Spinner mplantList;
	   
	   static LinearLayout mLinearLayout;

	   private void loadPrefs() {

	      mHostname.setText(PowerGarden.getPrefString("hostname", "10.0.1.2"));
	      mPort.setText(PowerGarden.getPrefString("port", "9000"));
	      
	      //if(PowerGarden.getPrefString("deviceID", "set_id") != null){
	      mID.setText(PowerGarden.getPrefString("deviceID", "set_id"));
	      
	      //String plantIndex[] = {"cherry_tomatoes","tomatoes","orange_carrots","purple_carrots", "beets", "celery","peppers"}; //list in the order of the spinner
	      ArrayAdapter myAdap = (ArrayAdapter) mplantList.getAdapter(); //cast to an ArrayAdapter
	      int spinnerIndex = myAdap.getPosition(PowerGarden.Device.plantType);
	      mplantList.setSelection(spinnerIndex);
	    	  
	      mNumPlants.setText(PowerGarden.getPrefString("numPlants","8"));  
	   }

	   private void saveDevicePrefs(){
		   //PowerGarden.savePref("deviceID", mID.getText().toString());
		   //PowerGarden.savePref("numPlants", mNumPlants.getText().toString());
	   }
	   private void savePlantPrefs(){
		   //PowerGarden.savePref("deviceID", mID.getText().toString());
		   PowerGarden.savePref("numPlants", mNumPlants.getText().toString());
		   PowerGarden.savePref("plantType", mplantList.getSelectedItem().toString());
	   }

	   private void saveServerPrefs() {

	      //SharedPreferences.Editor editor = PowerGarden.mSettings.edit();
	      PowerGarden.savePref("hostname", mHostname.getText().toString());
	      PowerGarden.savePref("port", mPort.getText().toString());
	      //editor.putString("deviceID", mID.getText().toString());
	      //editor.putString("numPlants", mNumPlants.getText().toString());
	      //editor.commit();
	   }

	   private void setButtonConnect() {
		   if(!PowerGarden.bConnected){
			   Log.d(TAG,"PowerGarden.bConnected = FALSE");
			   mHostname.setEnabled(true);
			   mPort.setEnabled(true);
			   mStart.setText("Connect");
		   } 
//		   else {
//			   Log.d(TAG,"PowerGarden.bConnected = TRUE");
//			   mHostname.setEnabled(false);
//			   mPort.setEnabled(false);
//			   mStart.setText("Disconnect");  
//		   }
	      mStart.setOnClickListener(new Button.OnClickListener() {
	         public void onClick(View v) {
	        	Log.d(TAG, "Clicked Connect");
	            PowerGarden.SM.connectToServer(mHostname.getText().toString(), mPort.getText().toString(),ConnectSockets.this);
	            saveServerPrefs();
	            setButtonDisconnect();
	            mSendMessage.setEnabled(true);
	            mSendTouch.setEnabled(true);
	            mSendUpdate.setEnabled(true);
	         }
	      });
	   }
	   
	   private void setupSendButton(){ //*** REGISTER BUTTON ***//
           mSendMessage.setOnClickListener(new Button.OnClickListener() {
           	public void onClick(View v) {
           		
           		PowerGarden.Device.plantType = mplantList.getSelectedItem().toString();
           		PowerGarden.Device.PlantNum = Integer.parseInt(mNumPlants.getText().toString());
           		savePlantPrefs();
           		//PowerGarden.SM.authDevice("register", PowerGarden.Device.ID, PowerGarden.Device.PlantNum.toString(), mplantList.getSelectedItem().toString() , ConnectSockets.this);
           		PowerGarden.SM.authDevice("register", PowerGarden.Device.ID, PowerGarden.Device.PlantNum, PowerGarden.Device.plantType, ConnectSockets.this);
           		//PowerGarden.Device.plantType = mplantList.getSelectedItem().toString();
           	}
           });
           
           mSendTouch.setOnClickListener(new Button.OnClickListener() {
        	   public void onClick(View v) {
		        	   //PowerGarden.SM.plantTouch("touch", mID.getText().toString(), 2, ConnectSockets.this );
					int plantNum = (int) (Math.random()*8);
					//int plantNum = 1;
					
					//*** TOUCHED ***//
					PowerGarden.Device.plants[plantNum].triggered = true; //set triggered to true for this plant
					
					PowerGarden.Device.plants[plantNum].touchedTimestamp = System.currentTimeMillis(); //record timestamp
					
					PowerGarden.Device.plants[plantNum].touchStamps.add(PowerGarden.Device.plants[plantNum].touchedTimestamp); //add timestamp to overall touchStamps vector
					
					PowerGarden.stateManager.updatePlantStates(); //check this plant's state
					
					//sendJson("touch", new Monkey("device_id",PowerGarden.Device.ID), new Monkey("index",i)); //let's save this for when we get crazy
					PowerGarden.SM.plantTouch("touch", PowerGarden.Device.ID, plantNum, PowerGarden.Device.plants[plantNum].getFilteredValue(), PowerGarden.Device.plants[plantNum].state, PowerGarden.Device.plants[plantNum].touchStamps.size(), ConnectSockets.this );
					//PowerGarden.SM.plantTouch("touch", PowerGarden.Device.ID, i, PowerGarden.Device.plants[i].getFilteredValue(), "worked_up", 88, PresentationViewable.this );	
					if (PowerGarden.stateManager.updateDeviceState() ){
						PowerGarden.Device.messageCopy = PowerGarden.stateManager.updateCopy();
						//activity_.resetView(); //now done in timerTask run() method
					}
					
					PowerGarden.audioManager.playSound(plantNum);
				}
           });
           
           mSendUpdate.setOnClickListener(new Button.OnClickListener(){
        	   public void onClick(View r){
        		   Log.d(TAG, "Update clicked !");
        		   PowerGarden.stateManager.updatePlantStates();
        		   JSONObject j = new JSONObject();
        		   long time = System.currentTimeMillis() / 1000L;
        		   	try {
        		   		if(PowerGarden.sendFakeNumbersOnUpdateButtonClick){ //*** IS YOUR NAME CHRIS PIUGGI? **//
            		   		Random generator = new Random(); 
            		   		int tempMoist = generator.nextInt(99) + 5; 
            		   		int tempTemp = generator.nextInt(35) + 20;
            		   		int tempHumidity = generator.nextInt(99) + 5;
            		   		int tempLight = generator.nextInt(500) + 0;
        		   			j.put("moisture", tempMoist).put("temp", tempTemp).put("humidity", tempHumidity).put("light", tempLight).put("timestamp", time );
        		   		}
        		   		else {
        		   			j.put("moisture", PowerGarden.Device.moisture).put("temp", PowerGarden.Device.temp).put("humidity", PowerGarden.Device.hum).put("light", PowerGarden.Device.light).put("timestamp", time );
        		   		}
        		   	} catch (JSONException e) {
        		   		e.printStackTrace();
        		   	}
        		   PowerGarden.Device.plantType = mplantList.getSelectedItem().toString();
        		   PowerGarden.SM.updateData("update", PowerGarden.Device.ID, ConnectSockets.this);
        	   } 
           });
	   }
	   private void setButtonDisconnect() {
	      mHostname.setEnabled(false);
	      mPort.setEnabled(false);
	      mStart.setText("Disconnect");
	      PowerGarden.bConnected = false;
	      mStart.setOnClickListener(new Button.OnClickListener() {
	         public void onClick(View v) {
	            PowerGarden.SM.closeUpShop();
	            mSendMessage.setEnabled(false);
	            mSendTouch.setEnabled(false);
	            mSendUpdate.setEnabled(false);
	            mStart.setText("Connect");
	            setButtonConnect();
	         }
	      });
	   }
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connect_sockets);
		Log.d(TAG, "ConnectSockets onCreate");
		Log.d(TAG, "bConnected= " + String.valueOf(PowerGarden.bConnected));
		//final View controlsView = findViewById(R.id.fullscreen_content_controls);
		//final View contentView = findViewById(R.id.fullscreen_content);
		
	    mHostname = (EditText) findViewById(R.id.hostname);
	    mPort = (EditText) findViewById(R.id.port);
	    mStatusline = (TextView) findViewById(R.id.statusline);
	    mStart = (Button) findViewById(R.id.start);
	    //mType = (EditText) findViewById(R.id.type);
	    mID = (EditText) findViewById(R.id.device_id);
	    mNumPlants = (EditText) findViewById(R.id.num_plants);
	    mSendMessage = (Button) findViewById(R.id.sendMsg);
	    mSendTouch = (Button) findViewById(R.id.sendTouch);
	    mSendUpdate=(Button) findViewById(R.id.sendUpdate);
	    
	    mplantList = (Spinner) findViewById(R.id.type_plants);
	    //mplantsView = (ListView) findViewById(R.id.plants);
	    //mLinearLayout = (LinearLayout)findViewById(R.id.plants);
	    //PowerGarden.mSettings = getSharedPreferences(PREFS_NAME, 0);
	    
	    ArrayAdapter <CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.plants_array, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    mplantList.setAdapter(adapter);
	    
	    loadPrefs();
	    
	    if(!PowerGarden.bConnected){
	    	setButtonConnect();
	    	mSendMessage.setEnabled(false);
	    	
	    } else setButtonDisconnect();
	    setupSendButton();
	   // }
	}

	@Override
	public boolean isConnected() {
		return false;
	}

	@Override
	public void connect() {

	}

	@Override
	public void disconnect() {
		
	}

	@Override
	public void close() {
		
	}

	@Override
	public void sendData(CharSequence data) {
		
	}

	@Override
	public void sendData(int type, byte[] data) {
		
	}

	@Override
	public void signalToUi(int type, Object data) {
		Log.d(TAG, "ConnectSockets signalToUi");

		Runnable myrun = null;
		if(type == PowerGarden.SocketConnected){
			myrun = new Runnable(){
				public void run(){
					mID.setText(PowerGarden.Device.ID);
					mStatusline.setText("CONNECTED TO HOST: "+PowerGarden.Device.host +"\n"+ "ON PORT "+ PowerGarden.Device.port);
					saveDevicePrefs();
				}
			};
		}
		
		else if(type == PowerGarden.Registered){
			Log.wtf(TAG, "from SignalToUi > type .Registered");
			//mID.setText(PowerGarden.Device.ID);
			myrun = new Runnable(){
				public void run(){
					mID.setText(PowerGarden.Device.ID);
					mStatusline.setText("server says REGISTERED\n\n " + PowerGarden.serverResponseRaw);
					//saveDevicePrefs();
				}
			};
		}
		
		else if(type == PowerGarden.Updated){
			Log.d(TAG, "from SignalToUi > type .Updated");
			//mStatusline.setText(PowerGarden.serverResponseRaw);
			
			myrun = new Runnable(){
				public void run(){
					mStatusline.setText("server says UPDATED\n\n "+PowerGarden.serverResponseRaw);
					//saveDevicePrefs();
				}
			};
		}
		
		else if(type == PowerGarden.Touched){
			Log.d(TAG, "from SignalToUi > type .Touched");
			myrun = new Runnable(){
				public void run(){
					mStatusline.setText("server says TOUCHED\n\n "+PowerGarden.serverResponseRaw);
					//saveDevicePrefs();
				}
			};
		}

		else if(type == PowerGarden.ThreshChange){
			Log.d(TAG, "from SignalToUi > type .ThreshChange");
			final Object _d = data;
			myrun = new Runnable(){
				public void run(){
					mStatusline.setText("server says THRESHOLD UPDATE "+_d.toString()+"\n\n"+PowerGarden.serverResponseRaw);
					//saveDevicePrefs();
				}
			};
		}
		
		else if (type == PowerGarden.StreamModeUpdate){
			final int type_ = type;
			myrun = new Runnable(){
				
				public void run(){
					mStatusline.setText("server says STREAM MODE UPDATE\n\n " + Integer.toString(type_) + " : "+PowerGarden.serverResponseRaw);
					//saveDevicePrefs();
				}
			};			
		}
		
		else if (type == PowerGarden.PlantIgnore){
			final int type_ = type;
			myrun = new Runnable(){
				
				public void run(){
					mStatusline.setText("server says PLANT IGNORE\n\n " + Integer.toString(type_) + " : "+PowerGarden.serverResponseRaw);
					//saveDevicePrefs();
				}
			};			
		}
		
		else if (type == PowerGarden.Settings){
			final int type_ = type;
			myrun = new Runnable(){
				
				public void run(){
					mStatusline.setText("server says SETTINGS UPDATE\n\n " + Integer.toString(type_) + " : "+PowerGarden.serverResponseRaw);
					//saveDevicePrefs();
				}
			};			
		}
		
		else if(type == PowerGarden.Unrecognized){ //impossibility?
			final int type_ = type;
			myrun = new Runnable(){
				
				public void run(){
					mStatusline.setText("server says TYPE UNRECOGNIZED\n\n " + Integer.toString(type_) + " : "+PowerGarden.serverResponseRaw);
					//saveDevicePrefs();
				}
			};
		}
		 
		Log.d(TAG, "Got type: " + type);
		this.runOnUiThread(myrun);	
	}
}
