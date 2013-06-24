package com.incredibleMachines.powergarden;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.*;

import com.victorint.android.usb.interfaces.Viewable;

public class PresentationViewable implements Viewable, SeekBar.OnSeekBarChangeListener {
	private static String TAG = "PresentationViewable";
	private Activity activity_;
	private int messageLevel_			= 22;
    TextView plant1Label;
    TextView plant2Label;
    TextView plant3Label;
   
    TextView plantVal_1;
    TextView plantVal_2;
    TextView plantVal_3;
    SeekBar bar1;
    SeekBar bar2;
    SeekBar bar3;
    PlantObject plants[] = new PlantObject[8];
    int numPlants = 8;
    int bar1Val;
    MedianFilter MF1 = new MedianFilter();
    MedianFilter MF2 = new MedianFilter();
    MedianFilter MF3 = new MedianFilter();
    
    

    
    boolean triggered_1 = false;
    long trig_1_time = 0;
    boolean triggered_2 = false;
    long trig_2_time = 0;
    boolean triggered_3 = false;
    long trig_3_time = 0;
    final long triggerTime = 2000;
    boolean bHaveData = false;
    String gotData;
    //int [] arrayToSort = {0,0,0,0,0,0,0,0,0,0};
    
	@Override
	public void signalToUi(int type, Object data) {

		int temp1;
		int temp2;
		int temp3;
	    plantVal_1 = (TextView) activity_.findViewById(R.id.arduino_value_1);
	    plantVal_2 = (TextView) activity_.findViewById(R.id.arduino_value_2);
	    plantVal_3 = (TextView) activity_.findViewById(R.id.arduino_value_3);
	    
	    plant1Label = (TextView) activity_.findViewById(R.id.arduino);
	    plant2Label = (TextView) activity_.findViewById(R.id.arduino2);
	    plant3Label = (TextView) activity_.findViewById(R.id.arduino3);
	    bar1 = (SeekBar) activity_.findViewById(R.id.seekBar1);
	    bar2 = (SeekBar) activity_.findViewById(R.id.seekBar2);
	    bar3 = (SeekBar) activity_.findViewById(R.id.seekBar3);
	    
	    bar1.setOnSeekBarChangeListener(this);
	    bar2.setOnSeekBarChangeListener(this);
	    bar3.setOnSeekBarChangeListener(this);
		// TODO Auto-generated method stub
		if (type == Viewable.BYTE_SEQUENCE_TYPE) {
    		if (data == null || ((byte[]) data).length == 0) {
    			return;
    		}
    		final byte[] byteArray = (byte[]) data;
    		gotData = new String(byteArray);
    		Log.d(TAG, "Byte DATA: "+gotData);
    		bHaveData = true;
		}else if (type == Viewable.CHAR_SEQUENCE_TYPE) {
    		if (data == null || ((CharSequence) data).length() == 0) {
    			return;
    		}
    		final CharSequence tmpData = (CharSequence) data;
    		Log.d(TAG, "Char DATA: "+tmpData.toString());
    	} else if ( 
    			( type == Viewable.DEBUG_MESSAGE_TYPE || type == Viewable.INFO_MESSAGE_TYPE ) &&
    			( type <= messageLevel_ )
    			) {
    		if (data == null || ((CharSequence) data).length() == 0) {
    			return;
    		}
    		final CharSequence tmpData = (CharSequence) data;
    		Log.d(TAG, "Debug DATA: "+tmpData.toString());
    	}
		if(bHaveData){
		String[] parseData = gotData.split(",");
		MF1.medianFilterAddValue(Integer.parseInt(parseData[1]));
		final int tOne = MF1.medianFilterGetMedian();
		final int tTwo = Integer.parseInt(parseData[2]);
		final int tThree = Integer.parseInt(parseData[3]);
		Runnable runner = new Runnable(){
		public void run() {
			
			if(System.currentTimeMillis() - trig_1_time > triggerTime){
				triggered_1 = false;
			}
			if(System.currentTimeMillis() - trig_2_time > triggerTime){
				triggered_2 = false;
			}
			if(System.currentTimeMillis() - trig_3_time > triggerTime){
				triggered_3 = false;
			}
			
			if(tOne > bar1Val){
				if(!triggered_1){ 
					MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.laugh_1);
					mp.start();
					triggered_1 = true;
					trig_1_time = System.currentTimeMillis();	
				}
			} 
			if(triggered_1){
				plant1Label.setTextColor(Color.GREEN);
				plantVal_1.setTextColor(Color.GREEN);
			}
			else{
				plant1Label.setTextColor(Color.WHITE);
				plantVal_1.setTextColor(Color.WHITE);
			}
			
			
//			if(tTwo > 0){
//				if(!triggered_2){
////					MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.laugh_2);
////					mp.start();
//					triggered_2 = true;
//					trig_2_time = System.currentTimeMillis();
//				}
//			} 
//			if(triggered_2){
//				plant2Label.setTextColor(Color.GREEN);
//				plantVal_2.setTextColor(Color.GREEN);
//			}
//			else{
//				plant2Label.setTextColor(Color.WHITE);
//				plantVal_2.setTextColor(Color.WHITE);
//			}
//			
//			
//			if(tThree > 0){
//				if(!triggered_3){
////					MediaPlayer mp = MediaPlayer.create(activity_.getApplicationContext(), R.raw.hey_1);
////					mp.start();
//					triggered_3 = true;
//					trig_3_time = System.currentTimeMillis();
//				}
//			} 
//			if(triggered_3){
//				plant3Label.setTextColor(Color.GREEN);
//				plantVal_3.setTextColor(Color.GREEN);
//			}
//			else{
//				plant3Label.setTextColor(Color.WHITE);
//				plantVal_3.setTextColor(Color.WHITE);
//			}
			
			
			String analogInVal = String.valueOf(tOne);
			String analog2InVal = String.valueOf(tTwo);
			String analog3InVal = String.valueOf(tThree);
			
			plantVal_1.setText(analogInVal);
			plantVal_2.setText(analog2InVal);
			plantVal_3.setText(analog3InVal);
		}
		};
		if(runner != null){
			activity_.runOnUiThread(runner);
		}
		}

	}
	
	@Override
	public void saveState() {
		// TODO Auto-generated method stub

	}

	@Override
	public void readState() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setActivity(Activity activity) {
		// TODO Auto-generated method stub
        if (activity_ == activity) {
        	return;
        }
		activity_ = activity;
		for(int i = 0; i<numPlants; i++){
			PlantObject tempPlant = new PlantObject();
			plants[i] = tempPlant;
		}

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}


	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		// TODO Auto-generated method stub
		if(arg0 == bar1){
			bar1Val = arg1;
			TextView bar1text = (TextView)activity_.findViewById(R.id.bar1text);
			bar1text.setText(Integer.toString(bar1Val));
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		// TODO Auto-generated method stub
		
	}

}
