package com.incrediblemachines.powergarden;

import java.io.IOException;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

public class GardenMedia {

	public void audioPlayer(int plantID, Context context){
		
		String path = "android.resource://com.incrediblemachines.powergarden/raw"; 
		String fileName = null;
		
		if (plantID == 1){
			fileName = "laugh_1";
		} 
		else if (plantID == 2){
			fileName = "laugh_2";
		} 
		else if (plantID == 3){
			fileName = "hey_1";
		}
		
		Uri audioUri = Uri.parse("R.raw."+fileName);
		
		MediaPlayer mp = MediaPlayer.create(context, audioUri);
		mp.start();
		
		
//	    ---- another way to do the same thing:
//	    MediaPlayer mp = new MediaPlayer();
//	 
//	    try {
//	        mp.setDataSource(path+"/"+fileName);
//	    } catch (IllegalArgumentException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//	    } catch (IllegalStateException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//	    } catch (IOException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//	    }
//	    try {
//	        mp.prepare();
//	    } catch (IllegalStateException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//	    } catch (IOException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//	    }
//	    mp.start();
	}
}
