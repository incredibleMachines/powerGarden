package com.incredibleMachines.powergarden;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


class SignStaging extends TimerTask {
	String TAG = "SignStaging";
	private PresentationActivity activity_;
	  
	@Override
	public void run() {
		PowerGarden.Device.messageCopy = PowerGarden.stateManager.updateCopy();
		//if(title) PowerGarden.Device.displayMode = PowerGarden.DisplayMode.PlantTitle;
		//PowerGarden.Device.displayMode = PowerGarden.DisplayMode.MessageCopy;
		//title = !title;
		
		Runnable runner = new Runnable(){
			public void run() {
				activity_.updateStage();
			}
		};
		if(runner != null){
			activity_.runOnUiThread(runner);
		}
	}
	
	public void setActivity(Activity activity) {
		
		/** onCreate, only hit once at startup **/
		
		Log.d(TAG, "setActivity");
        if (activity_ == activity) {
        	Log.d(TAG, "activty_ == activity, returning --");
        	return;
        }
		activity_ = (PresentationActivity) activity;
	}
	
	/*** happens onCreate of PresActivity -- load up .json file with all dialogue ***/
	public void loadDialogue() throws IOException, JSONException{
		
		
	    StringBuilder builder = new StringBuilder();
	     
	    try {
			  HttpClient client = new DefaultHttpClient();
			  String url = "http://"+PowerGarden.Device.host+":8080/dialogue?type="+PowerGarden.Device.plantType;
			  Log.wtf("REQUESTING JSON FROM: ", url);
			  HttpGet httpGet = new HttpGet(url);
			   
		      HttpResponse response = client.execute(httpGet);
		      int statusCode = response.getStatusLine().getStatusCode();
		       
		      if (statusCode == 200) {
		        HttpEntity entity = response.getEntity();
		        InputStream content = entity.getContent();
		        InputStreamReader reader1 = new InputStreamReader(content);
		        BufferedReader reader = new BufferedReader(reader1);
		        String line;
		        while ((line = reader.readLine()) != null) {
		          builder.append(line);
		        }
		        JSONObject jobj = new JSONObject(builder.toString());
		        builder = new StringBuilder(jobj.toString());//(jobj.get("status").toString());
			//JSONArray jar = jobj.optJSONArray("friends");
			//builder.append(jar.length());
			  }
			}
			catch(Exception e) {
			  e.printStackTrace();
			  builder.append(e);
			}
			 
			final String str = builder.toString();
			activity_.runOnUiThread(new Runnable() {
			  @Override
			  public void run() {
				Log.wtf("JSON RECEIVED: ", str);
				try {
					PowerGarden.dialogue = new JSONObject(str); //load in all dialogue !
				} catch (JSONException e) {
					e.printStackTrace();
				}
			    Toast.makeText(activity_, str, Toast.LENGTH_LONG).show();
			    Message msg = new Message();
			    msg.arg1 = 0;
			    msg.arg2 = 0;
			    PresentationActivity.jsonDlHandler.sendMessage(msg);
			    //activity_.progressBar.dismiss();
			  }
			});
	  }

		
		/*** old method using dialogue.json file locally ***/
	
//		InputStream is = Parent.getResources().openRawResource(R.raw.dialogue);
//		Writer writer = new StringWriter();
//		Reader reader = null;
//		char[] buffer = new char[1024];
//		try {
//			reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
//		    int n;
//		    while ((n = reader.read(buffer)) != -1) {
//		        writer.write(buffer, 0, n);
//		    }
//		} finally {
//			is.close();
//		}
//		PowerGarden.dialogue = new JSONObject(writer.toString());
		
		//Log.d("LOADDIALOGUE", PowerGarden.dialogue.toString());
}

