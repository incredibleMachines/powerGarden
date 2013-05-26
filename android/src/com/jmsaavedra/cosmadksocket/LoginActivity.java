// ServiceADKActivity.java
// ---------------------------
// RobotGrrl.com
// November 29, 2011

package com.jmsaavedra.cosmadksocket;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;
import com.jmsaavedra.cosmadksocket.ADKService;
import com.jmsaavedra.cosmadksocket.LoginActivity;
import com.jmsaavedra.cosmadksocket.ServiceADKApplication;
import com.jmsaavedra.cosmadksocket.CosmParser;

import com.jmsaavedra.cosmadksocket.R;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity implements Runnable{
    /** Called when the activity is first created. */
    
	private static final String TAG = "LoginActivity";
	
	   private final WebSocketConnection mConnection = new WebSocketConnection();
	   
	   TextView datastreamId;
	   TextView datastreamId2;
	   TextView currentValue;
	   TextView currentValue2;
	   
	   TextView analogValIn;
	   TextView analog2ValIn;
	   
	   Button counter;
	   
	   JSONObject dataIn;
	   JSONObject dataToSend;
	   JSONArray datastreams;
	   
	   CosmParser parser;
	   

	   public int currentCount;
	   
	   int arduinoVal;
	   
	   String FEEDID = "81331";
	   String APIKEY = "_sG8TKAucC_cY02I8FIjYzkZkv-SAKxWWGZDWVh2eDlJbz0g";
	   

	boolean debug = false;
	
	/*** service stuff ***/
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
	Thread mThread;
	TextView fileDescText, inputStreamText, outputStreamText, accessoryText;
	private Handler mHandler = new Handler();	
	/*** end service stuff ***/
	
    
    private boolean slideTimeGo = false;

    Typeface ExistenceLightOtf;
    Typeface Museo300Regular;
    Typeface Museo500Regular;
    Typeface Museo700Regular;
   
	@Override
    public void onCreate(Bundle savedInstanceState) {
		
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate");
                
        setContentView(R.layout.activity_socket);
        /******* service stuff ******/
        mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		if (getLastNonConfigurationInstance() != null) {
			mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}
		Log.e(TAG, "Hellohello!");	
		startService(new Intent(this, ADKService.class));
		/***** end service stuff *****/      		
		
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        
	    ExistenceLightOtf = Typeface.createFromAsset(getAssets(),"fonts/Existence-Light.ttf");
	    Museo300Regular = Typeface.createFromAsset(getAssets(),"fonts/Museo300-Regular.otf");
	    Museo500Regular = Typeface.createFromAsset(getAssets(),"fonts/Museo500-Regular.otf");
	    Museo700Regular = Typeface.createFromAsset(getAssets(),"fonts/Museo700-Regular.otf");
	    
	    Log.d(TAG,"...instantiateButtons");
	    counter = (Button) findViewById(R.id.btn_counter);
	    counter.setOnClickListener(mCounter);
	    
	      
	      datastreamId = (TextView) findViewById(R.id.datastream_id_1);
	      currentValue = (TextView) findViewById(R.id.current_value_1);
	      datastreamId2 = (TextView) findViewById(R.id.datastream_id_2);
	      currentValue2 = (TextView) findViewById(R.id.current_value_2);
	      
	      analogValIn = (TextView) findViewById(R.id.arduino_value_1);
	      
	      analog2ValIn = (TextView) findViewById(R.id.arduino_value_2);
	    
    	Resources res = getResources();
    	
    	
    	start();
    }

	private void start() {
		   
	      final String wsuri = "ws://api.cosm.com:8080";
	      
	      try {
	         mConnection.connect(wsuri, new WebSocketHandler() {
	 
	            @Override
	            public void onOpen() {
	               Log.d(TAG, "Status: Connected to " + wsuri);
	               
	               //open subscription to this feed's socket
	               //mConnection.sendTextMessage("{\"method\" : \"subscribe\",\"resource\" : \"/feeds/"+FEEDID+"\",\"headers\" :{\"X-ApiKey\" : \""+APIKEY+"\"},\"token\" : \"0xabcdef\"}");
	               
	               //initial pull of current datastreams
	               //qamConnection.sendTextMessage("{\"method\":\"get\",\"resource\":\"/feeds/"+FEEDID+"9m  \",\"headers\":{\"X-ApiKey\":\""+APIKEY+"\"},\"token\":\"0xabcdef\"}");       
	            }
	 
	            @Override
	            public void onTextMessage(String payload) {
	               Log.d(TAG, "Got echo: " + payload);
	               try {
	            	   JSONObject rawIn = new JSONObject(payload);
	            	   if(rawIn.has("body")){
		            	   dataIn = new JSONObject();
		            	   dataIn = rawIn.getJSONObject("body");
		            	   parser = new CosmParser(dataIn);			
		            	   updateTextViews();  
	            	   }
					
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	               
	            }
	 
	            @Override
	            public void onClose(int code, String reason) {
	               Log.d(TAG, "Connection lost.");
	            }
	         });
	      } catch (WebSocketException e) {
	 
	         Log.d(TAG, e.toString());
	      }
	   }
 
 public void pullData(){
	   
	//   mConnection.sendTextMessage("{'method':'get','resource':'/feeds/81331/datastreams/0','headers':{'X-ApiKey':'_sG8TKAucC_cY02I8FIjYzkZkv-SAKxWWGZDWVh2eDlJbz0g'},'token':'0xabcdef'}");
 }
 
 public void updateTextViews(){
	   
	   try {
		Log.d(TAG, String.valueOf(parser.getDataStreamCount()));
		Log.d(TAG, parser.getStreamId(0));
		String thisDatastreamId = parser.getStreamId(0);
		String thisDatastreamId2 = parser.getStreamId(1);
		datastreamId.setText("Moisture");
		datastreamId2.setText("Temperature");
		
		   
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		   
	   
	   currentCount = (int) parser.getStreamCurrVal(0);
	   String datastreamValue = String.valueOf(parser.getStreamCurrVal(0));
	   currentValue.setText(datastreamValue);
	   
	   String datastreamValue2 = String.valueOf(parser.getStreamCurrVal(1));
	   currentValue2.setText(datastreamValue2);
	   
	   double lat = parser.latitude;
	   double lon = parser.longitude;
	   Log.d(TAG, String.valueOf(lat));
	   Log.d(TAG, String.valueOf(lon));
 }
 
	//---- when counter button is pressed
 OnClickListener mCounter = new OnClickListener(){
 	public void onClick(View v) {
 		Log.d(TAG,"mCounter.onClick");
 		
 		String datastream = "counter";
 		currentCount++;
 		sendPress('Y');
 		
 		/*********** THIS ONE: *****/
 		//mConnection.sendTextMessage("{\"method\":\"put\",\"resource\":\"/feeds/"+FEEDID+"\",\"headers\" : {\"X-ApiKey\" : \""+APIKEY+"\"},\"body\":{\"version\":\"1.0.0\",\"datastreams\":[{\"id\":\"counter\",\"current_value\":\""+String.valueOf(currentCount)+"\"},{\"id\":\"streamId2\",\"current_value\":\"22\"}]}}");
 		}	
 	};
	
    @Override
	public Object onRetainNonConfigurationInstance() {
		if (mAccessory != null) {
			return mAccessory;
		} else {
			return super.onRetainNonConfigurationInstance();
		}
	}
    
    @Override
    protected void onNewIntent(Intent intent){
        Log.d(TAG,"onNewIntent");

       
    } 

	/****** ADK stuff!! *****/
	  @Override
		public void onResume() {
	    	Resources res = getResources();
			super.onResume();
			//setContentView(R.layout.login_page);
			try {
				ADKService.self.stopUpdater();
			} catch(Exception e) {
				Log.d(TAG, "Stopping the updater failed");
			}
			Intent intent = getIntent();
			if (mInputStream != null && mOutputStream != null) {
				Log.v(TAG, "input and output stream weren't null!");
				enableControls(true);
				return;
			}
			UsbAccessory[] accessories = mUsbManager.getAccessoryList();
			Log.v(TAG, "all the accessories: " + accessories);
			UsbAccessory accessory = (accessories == null ? null : accessories[0]);
			if (accessory != null) {
				if (mUsbManager.hasPermission(accessory)) {
					Log.v(TAG, "mUsbManager does have permission");
					openAccessory(accessory);
				} else {
					Log.v(TAG, "mUsbManager did not have permission");
					synchronized (mUsbReceiver) {
						if (!mPermissionRequestPending) {
							mUsbManager.requestPermission(accessory,
									mPermissionIntent);
							mPermissionRequestPending = true;
						}
					}
				}
			} else {
				Log.d(TAG, "mAccessory is null");
				Log.d(TAG, "NO ACCESSORY ATTACHED");
				//alertDialog.setMessage("Please Attach the Registration System to this Tablet and Login");
				//alertDialog.show();
				if(!debug){
					Log.d(TAG, "no device attached!!");
					//setContentView(R.layout.no_device);
				}
		        
			}
			// Let's update the textviews for easy debugging here...
			//updateTextViews();
		}
	    
	    @Override
		public void onPause() {
	    	Log.v(TAG, "onPause");
	    	//closeAccessory();
	    	try {
	    		ADKService.self.startUpdater();
			} catch(Exception e) {		
			} 	
	        Log.v(TAG, "done, now pause");
			super.onPause();
		}

		@Override
		public void onDestroy() {
			Log.v(TAG, "onDestroy");
			unregisterReceiver(mUsbReceiver);
			super.onDestroy();
		}
		
		@Override
		protected void onStop() {
		      super.onStop();
		}

		//@Override
		public void run() {
			int ret = 0; //number of bytes returned
			byte[] buffer = new byte[16384]; //holds all bytes returned
			int i;
			int iValue = 0;
			String thisBracelet = "";
			int slideTime1 = 0;
			int slideTime1b = 0;
			int slideTime1c = 0;
			int slideTime2 = 0;
			int slideTime3 = 0;
			int slideCtr = 0;
			//while (ret >= 0) {
			while(true){
				try {
					ret = mInputStream.read(buffer);
					Log.d(TAG, "ret =" + String.valueOf(ret));
				} catch (IOException e) {
					break;
				}
				thisBracelet = "";
				i = 0;
				byte[] sValue1 = new byte [8]; //will hold slide values from buffer
				byte[] sValue2 = new byte [8]; //will hold slide values from buffer
				byte[] sValue3 = new byte [8]; //will hold slide values from buffer
				
				int one = 0;
				int two = 0;
				int three = 0;
				byte crc = 0;
				
				
				
				if(!slideTimeGo){ /* for when we are reading any RFID tag */
					while (i < ret) {
						int len = ret - i;
						Log.v(TAG, "Read: " + buffer[i]);	
						final int val = (int)buffer[i];
						byte[] bValue = new byte [ret];
						bValue[i] = (byte)buffer[i];
						iValue = (int)buffer[i];
						Log.d(TAG, "currBuffer: "+String.valueOf((int)i));
						Log.d(TAG, Integer.toHexString(iValue));
						thisBracelet = thisBracelet.concat(Integer.toHexString(iValue));
						Log.d(TAG, "thisBracelet: "+thisBracelet);
						i++;
					}
				} else { /* for when we are receiving slide data */
					while (i < ret){
						
						crc = (byte)((0x00FF & buffer[0]) ^ (0x00FF & buffer[1]) ^ (0x00FF &
								buffer[2]) ^ (0x00FF & buffer[3]) ^ (0x00FF & buffer[4]) ^ (0x00FF & buffer[5]));
								// calculate the "CRC"
						
						if (buffer[6] == crc) {  // compare to the received "CRC" - if it matches we'll trust that the received data is good
							
							one = ((int)(buffer[0] & 0x00FF) * 256) + (int)(buffer[1] & 0x00FF);
							two = ((int)(buffer[2] & 0x00FF) * 256) + (int)(buffer[3] & 0x00FF);
							three = ((int)(buffer[4] & 0x00FF) * 256) + (int)(buffer[5] & 0x00FF);
						}
						i++;	
					}
				}
				
				one = ((int)buffer[0] & 0x00FF);
				two = ((int)buffer[1] & 0x00FF);
				
				/* values need to be final before passing to handler */
				final int fRet = ret;
				//final byte[] finalVals1 = sValue1;	//copy array into a final byte[]
				//final byte[] finalVals2 = sValue2; //copy array into a final byte[]
				//final byte[] finalVals3 = sValue3; //copy array into a final byte[]
				
				final int tOne = one;
				final int tTwo = two;
				final int tThree = three;
				final byte fCrc = crc;
				final String fThisBracelet = thisBracelet;
				mHandler.post(new Runnable() {
					
					public void run() {
						String analogInVal = String.valueOf(tOne);
						String analog2InVal = String.valueOf(tTwo);
						analogValIn.setText(analogInVal);
						analog2ValIn.setText(analog2InVal);
						Log.d(TAG,analogInVal);
						mConnection.sendTextMessage("{\"method\":\"put\",\"resource\":\"/feeds/81331\",\"headers\" : {\"X-ApiKey\" : \"_sG8TKAucC_cY02I8FIjYzkZkv-SAKxWWGZDWVh2eDlJbz0g\"},\"body\":{\"version\":\"1.0.0\",\"datastreams\":[{\"id\":\"potentiometer\",\"current_value\":\""+analogInVal+"\"},{\"id\":\"streamId2\",\"current_value\":\"22\"}]}}");
				 		
						sendPress('Y');
					}
				});
//				switch (buffer[i]) {
//					default:
//						Log.d(TAG, "unknown msg: " + buffer[i]);
//						i = len;
//						break;
//					}
			}
		}

	    // ------------
	    // ADK Handling
		// ------------
		
		private void openAccessory(UsbAccessory accessory) {
			
			Log.e(TAG, "openAccessory: " + accessory);
			Log.d(TAG, "this is mUsbManager: " + mUsbManager);
			mFileDescriptor = mUsbManager.openAccessory(accessory);
			
			Log.d(TAG, "Tried to open");
			
			if (mFileDescriptor != null) {
				mAccessory = accessory;
				FileDescriptor fd = mFileDescriptor.getFileDescriptor();
				mInputStream = new FileInputStream(fd);
				mOutputStream = new FileOutputStream(fd);
				mThread = new Thread(null, this, "DemoKit"); // meep
				mThread.start(); // meep
				Log.d(TAG, "accessory opened");
//				setContentView(R.layout.login_page);
//				if(currPage.equals("login"))sendPress('Y');
				enableControls(true);
			} else {
				Log.d(TAG, "accessory open fail");
				
					Log.d(TAG, "NO ACCESSORY ATTACHED");
					//alertDialog.setMessage("Please Attach the Registration System to this Tablet and Login");
					//alertDialog.show();
				
				enableControls(false);
			}
		}
		
		private void closeAccessory() {
			Log.e(TAG, "closing accessory");
			try {
				if (mFileDescriptor != null) {
					mFileDescriptor.close();
				}
			} catch (IOException e) {
			} finally {
				mFileDescriptor = null;
				mAccessory = null;
			}
			enableControls(false);
		}

		public void sendCommand(byte command, byte target, int value) {
			Log.e(TAG,"sendCommand hit");
			byte[] buffer = new byte[3];
			if (value > 255)
				value = 255;

			buffer[0] = command;
			buffer[1] = target;
			buffer[2] = (byte) value;
			if (mOutputStream != null && buffer[1] != -1) {
				try {
					mOutputStream.write(buffer);
				} catch (IOException e) {
					Log.e(TAG, "write failed", e);
				}
			}
		}
		
	    public void sendPress(char c) {
			Log.d(TAG, "sendPress hit: ");
			Log.d(TAG, String.valueOf(c));
	    	byte[] buffer = new byte[2];
			buffer[0] = (byte)'!';
			buffer[1] = (byte)c;
				
			if (mOutputStream != null) {
				try {
					mOutputStream.write(buffer);
				} catch (IOException e) {
					Log.e(TAG, "write failed", e);
				}
			}
		}
	    
		public boolean adkConnected() {
	    	//if(mInputStream != null && mOutputStream != null) return true;
	    	if(mFileDescriptor != null) return true;
	    	return false;
	    }
			
		private void enableControls(boolean b) {
			((ServiceADKApplication) getApplication()).setInputStream(mInputStream);
			((ServiceADKApplication) getApplication()).setOutputStream(mOutputStream);
			((ServiceADKApplication) getApplication()).setFileDescriptor(mFileDescriptor);
			((ServiceADKApplication) getApplication()).setUsbAccessory(mAccessory);
			//updateTextViews();
			
			if(!b) {
				try {
		    		ADKService.self.stopUpdater();
				} catch(Exception e) {
					
				}
			}
			//sendPress('X');
		}
	    
//	    private void updateTextViews() {
//
//	    	Log.v(TAG, "updated text views");    	
//	    }

		private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (ACTION_USB_PERMISSION.equals(action)) {
					synchronized (this) {
						UsbAccessory accessory = UsbManager.getAccessory(intent);
						if (intent.getBooleanExtra(
								UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
							openAccessory(accessory);
						} else {
							Log.d(TAG, "permission denied for accessory "
									+ accessory);
						}
						mPermissionRequestPending = false;
					}
				} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (accessory != null && accessory.equals(mAccessory)) {
						closeAccessory();
					}
				}
			}
		};
		
    //---- methods for setting fonts
    public static void setTextViewFont(Typeface tf, TextView...params) {
        for (TextView tv : params) {
            tv.setTypeface(tf);
        }
    } 
    public static void setEditTextFont(Typeface tf, EditText...params) {
        for (EditText tv : params) {
            tv.setTypeface(tf);
        }
    }  
    public static void setButtonFont(Typeface tf, Button...params) {
        for (Button tv : params) {
            tv.setTypeface(tf);
        }
    }
    
    //----- check if tablet is connected to internet!
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager 
              = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }
    
    //----- disable back button
	@Override
	public void onBackPressed() {
		//do nothing
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		//getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
	}
	
//	@Override
//	  public boolean onTouchEvent(MotionEvent event){
//		try {
//			
//			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//			Thread.sleep(250);
//			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return debug;
//	}
}
