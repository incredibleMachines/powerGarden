package com.incredibleMachines.powergarden;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.widget.TextView;

public class PowerGarden {
	
	public static int HAPPY = 1;
	public static int DRY = 2;
	public static int UNHAPPY = 3;
	public static SharedPreferences mSettings;
	private static final String PREFS_NAME = "PowerGarden";
	
	public static void loadPrefs(Context context){
		mSettings = context.getSharedPreferences(PREFS_NAME, 0);
	}
	public static String getPrefString(String name, String defString){
		return mSettings.getString(name,defString);
	}
	public static void savePref(String name, String value){
		SharedPreferences.Editor editor = mSettings.edit();
		editor.putString(name,value);
		editor.commit();
	}
	SharedPreferences.Editor editor = PowerGarden.mSettings.edit();
	
	
	//--- this device
	public static class Device{
		static String ID;
		static String connectionID;
		static String PlantNum;
		static String plantType = "Cherry Tomatoes"; //should be populated by prefs	
		static PlantObject plants[] = new PlantObject[8];
	}
	
	//--- global socket vars
	public static SocketManager SM;
	public static int Connected = 1;
	public static int Disconnected = 2;
	public static int Planted = 3;

	//--- global sensors
	public static int temp;
	public static int hum;
	public static int light;
	public static int moisture;
	public static int distance;
	
	//--- app status
	public static boolean bAudioPlaying = false;
}