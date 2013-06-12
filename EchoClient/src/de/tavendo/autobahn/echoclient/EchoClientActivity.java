/******************************************************************************
 *
 *  Copyright 2011 Tavendo GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package de.tavendo.autobahn.echoclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketConnectionHandler;

public class EchoClientActivity extends Activity {

   static final String TAG = "de.tavendo.autobahn.echo";
   private static final String PREFS_NAME = "AutobahnAndroidEcho";

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
   final ArrayList<PlantData> plantList = new ArrayList<PlantData>();

   private SharedPreferences mSettings;

   private void alert(String message) {
      Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
      toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 0);
      toast.show();
   }

   private void loadPrefs() {

      mHostname.setText(mSettings.getString("hostname", ""));
      mPort.setText(mSettings.getString("port", "9000"));
      mID.setText(mSettings.getString("deviceID", "set_id"));
      mNumPlants.setText(mSettings.getString("numPlants",""+plantList.size()));
      
   }

   private void saveDevicePrefs(){
	   SharedPreferences.Editor editor = mSettings.edit();
	   editor.putString("deviceID", mID.getText().toString());
	   editor.putString("numPlants", mNumPlants.getText().toString());
	   editor.commit();
   }
 
   private void savePrefs() {

      SharedPreferences.Editor editor = mSettings.edit();
      editor.putString("hostname", mHostname.getText().toString());
      editor.putString("port", mPort.getText().toString());
      editor.putString("deviceID", mID.getText().toString());
      editor.putString("numPlants", mNumPlants.getText().toString());
      editor.commit();
   }

   private void setButtonConnect() {
      mHostname.setEnabled(true);
      mPort.setEnabled(true);
      mStart.setText("Connect");
      mStart.setOnClickListener(new Button.OnClickListener() {
         public void onClick(View v) {
            start();
         }
      });
   }

   private void setButtonDisconnect() {
      mHostname.setEnabled(false);
      mPort.setEnabled(false);
      mStart.setText("Disconnect");
      mType.setText("connect");
      mStart.setOnClickListener(new Button.OnClickListener() {
         public void onClick(View v) {
            mConnection.disconnect();
         }
      });
   }

   private final WebSocket mConnection = new WebSocketConnection();

   private void start() {

      final String wsuri = "ws://" + mHostname.getText() + ":" + mPort.getText();

      mStatusline.setText("Status: Connecting to " + wsuri + " ..");

      setButtonDisconnect();

      try {
         mConnection.connect(wsuri, new WebSocketConnectionHandler() {
            @Override
            public void onOpen() {
               mStatusline.setText("Status: Connected to " + wsuri);
               savePrefs();
               mSendMessage.setEnabled(true);
               mID.setEnabled(true);
               mNumPlants.setEnabled(true);
               mType.setEnabled(true);
            }

            @Override
            public void onTextMessage(String payload) {
               //alert("Got echo: " + payload);
               try {
            	   JSONObject incoming = new JSONObject(payload);
            	   String status =incoming.getString("status"); 
            	   
            	   if(status.equals("connected")){
            		   alert("connected");
            		   mID.setText(incoming.getString("device_id") );
            		   mType.setText("update" );
            		   SharedPreferences.Editor editor = mSettings.edit();
            		   editor.putString("deviceID", incoming.getString("id"));
            		   editor.commit();
            		   
            	   }else if(status.equals("planted")){
            		   	
            		   JSONObject plantedJSON = incoming.getJSONObject("plant");
            		   alert("Planted");
            		   
            		   int i = plantedJSON.getInt("index");
            		   PlantData _plant = plantList.get(i);
            		   _plant.dbID = plantedJSON.getString("id");
            		   _plant.type = plantedJSON.getString("type");
            		   _plant.mood = plantedJSON.getString("mood");
            		   _plant.idEditText.setText( _plant.dbID );
            		   _plant.moodEditText.setText( _plant.mood );
            		   _plant.typeEditText.setText( _plant.type );
            		   //set plant touch to 0 plant length to 0
            	   }
            	   
            	   
            	   
               } catch (JSONException e) {
            	   //TODO Auto-generated catch block
            	   e.printStackTrace();
               }
               
               
               
               
            }

            @Override
            public void onClose(int code, String reason) {
               alert("Connection lost.");
               mStatusline.setText("Status: Ready.");
               setButtonConnect();
               mSendMessage.setEnabled(false);
               mType.setEnabled(false);
            }
         });
      } catch (WebSocketException e) {

         Log.d(TAG, e.toString());
      }
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {

      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);

      mHostname = (EditText) findViewById(R.id.hostname);
      mPort = (EditText) findViewById(R.id.port);
      mStatusline = (TextView) findViewById(R.id.statusline);
      mStart = (Button) findViewById(R.id.start);
      mType = (EditText) findViewById(R.id.type);
      mID = (EditText) findViewById(R.id.device_id);
      mNumPlants = (EditText) findViewById(R.id.num_plants);
      mSendMessage = (Button) findViewById(R.id.sendMsg);
      //mplantsView = (ListView) findViewById(R.id.plants);
      mLinearLayout = (LinearLayout)findViewById(R.id.plants);
      mSettings = getSharedPreferences(PREFS_NAME, 0);
      
      loadPrefs();
      
      setButtonConnect();
      mSendMessage.setEnabled(false);
      mType.setEnabled(false);
      mID.setEnabled(false);
      mNumPlants.setEnabled(false);
  	  
      int numPlants = Integer.parseInt(mNumPlants.getText().toString());

      for (int i = 0; i < numPlants; ++i) {
        //list.add("Item: "+i);
          TextView mPlantText = new TextView(this);
          mPlantText.setText("Plant "+i+": ID/ Type/ Touch/ Mood/ Update/ Delete");
          mLinearLayout.addView(mPlantText);
          
    	  LinearLayout thisPlantLayout = new LinearLayout(this);
          final PlantData thePlant = new PlantData();
          
          thePlant.idEditText = new EditText(this);
          thePlant.typeEditText = new EditText(this);
          thePlant.moodEditText= new EditText(this);
          thePlant.touchEditText = new EditText(this);
          thePlant.submit = new Button(this);
          thePlant.delete = new Button(this);
          thePlant.typeEditText.setText("Type");
          thePlant.idEditText.setText("ID");
          thePlant.moodEditText.setText("Mood");
          thePlant.touchEditText.setText("0");
          thePlant.submit.setText("Update");
          thePlant.delete.setText("Delete");
       
          thePlant.dbID = "unknown";
          thePlant.type= "unknown";
          thePlant.index = i;
          
          thePlant.idEditText.setId(getResources().getIdentifier("plant_id_%i",Integer.toString(i), getPackageName()));
          thePlant.typeEditText.setId(getResources().getIdentifier("plant_type_%i", Integer.toString(i), getPackageName()));
          thePlant.moodEditText.setId(getResources().getIdentifier("plant_mood_%i", Integer.toString(i), getPackageName()));
          thePlant.touchEditText.setId(getResources().getIdentifier("plant_touch_%i", Integer.toString(i), getPackageName()));
          thePlant.submit.setId(getResources().getIdentifier("plant_submit_%i", Integer.toString(i), getPackageName()));
          thePlant.delete.setId(getResources().getIdentifier("plant_delete_%i", Integer.toString(i), getPackageName()));
                    
          plantList.add(thePlant);
          
          thisPlantLayout.addView(thePlant.idEditText);
          thisPlantLayout.addView(thePlant.typeEditText);
          thisPlantLayout.addView(thePlant.touchEditText);
          thisPlantLayout.addView(thePlant.moodEditText);
          thisPlantLayout.addView(thePlant.submit);
          thisPlantLayout.addView(thePlant.delete);
          
          thePlant.submit.setOnClickListener(new Button.OnClickListener(){

			@Override
			public void onClick(View v) {
				
				String message = "{\"type\": \"update\", \"device_id\":\""+mID.getText().toString()+"\", \"plants\": [";    
        		message+= "{ \"id\": \""+ thePlant.idEditText.getText().toString()+ "\", \"index\":\""+thePlant.index+"\",\"mood\": \""+thePlant.moodEditText.getText().toString() +"\" ,\"type\": \""+thePlant.typeEditText.getText().toString()  +"\", \"touch\": { \"count\": "+thePlant.touchEditText.getText().toString()+", \"length\": 212 } }";
				message+="]}";
	            mConnection.sendTextMessage(message);

			}
        	  
        	  
          });
          
          mLinearLayout.addView(thisPlantLayout);
      }

      mSendMessage.setOnClickListener(new Button.OnClickListener() {
         public void onClick(View v) {
        	//mType set only for debugging purposes 
        	//will need logic to support debugging
        	String message = "{ \"type\":\""+mType.getText().toString() +"\", \"device_id\": \""+ mID.getText().toString()+"\", \"plants\" : [ ";

        	int c = 0;
        	for(PlantData p: plantList){	
        		message+= "{ \"id\": \""+ p.dbID+ "\", \"index\":\""+c+"\", \"mood\": \""+p.moodEditText.getText().toString() +"\" ,\"type\": \""+p.type+"\", \"touch\": { \"count\": "+p.touchEditText.getText().toString()+", \"length\": 1233 } }";
        		if( ++c != plantList.size() ) message += ","; //if its not the last item
        	
        	}
        	
        	message +=" ] }";
        	
        	
            mConnection.sendTextMessage(message);
            saveDevicePrefs();
         }
      });
   }

   @Override
   protected void onDestroy() {
       super.onDestroy();
       if (mConnection.isConnected()) {
          mConnection.disconnect();
       }
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
       MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.main_menu, menu);
       return true;
   }
   
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case R.id.quit:
            finish();
            break;
         default:
            return super.onOptionsItemSelected(item);
      }
      return true;
   }


   public class PlantData{
	   
	   EditText idEditText;
	   EditText typeEditText;
	   EditText moodEditText;
	   EditText touchEditText;
	   Button submit;
	   Button delete;
	   int index;
	   
	   String dbID;
	   String type;
	   String mood;
	   
	   //keep track of plant touches until sending
	   int touch;
	   long length;
	   
   }
   
}
