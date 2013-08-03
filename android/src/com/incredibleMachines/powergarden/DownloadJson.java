package com.incredibleMachines.powergarden;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class DownloadJson extends Thread{

	//Runnable getJsonDialog = new Runnable(){

//		String thisJson = null;
//		@Override
//		public void run(){
//			//public String readTwitterFeed() {
//				StringBuilder builder = new StringBuilder();
//			    HttpClient client = new DefaultHttpClient();
//			    HttpGet httpGet = new HttpGet("http://twitter.com/statuses/user_timeline/vogella.json");
//			    try {
//			      HttpResponse response = client.execute(httpGet);
//			      StatusLine statusLine = response.getStatusLine();
//			      int statusCode = statusLine.getStatusCode();
//			      if (statusCode == 200) {
//			        HttpEntity entity = response.getEntity();
//			        InputStream content = entity.getContent();
//			        BufferedReader reader = new BufferedReader(new InputStreamReader(content));
//			        String line;
//			        while ((line = reader.readLine()) != null) {
//			          builder.append(line);
//			        }
//			      } else {
//			        Log.wtf(SignStaging.class.toString(), "Failed to download file");
//			      }
//			    } catch (ClientProtocolException e) {
//			      e.printStackTrace();
//			    } catch (IOException e) {
//			      e.printStackTrace();
//			    }
//			    
//			    thisJson = builder.toString();
//			    //return thisJson;
//			    //rawJson = builder.toString();
//			    //return builder.toString();
//	//	}
//		
//	};
	
	
	
//	if (getJsonDialog != null){
//		getJsonDialog.run();
//		//activity_.runOnUiThread(getJsonDialog);
//	}
//	return TAG;
}
