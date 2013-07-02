package com.incredibleMachines.powergarden;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;

public class PowerGarden {

    static Typeface italiaBook;
    static Typeface interstateBold;
	
    
	public static int HAPPY = 1;
	public static int DRY = 2;
	public static int UNHAPPY = 3;

	//--- audio
	public static final int numSounds = 8;
	public static int[] cherryTomatoesAudio = new int [numSounds];
	public static boolean audioLoaded = false;
	
	
	//--- shared preferences
	public static SharedPreferences mSettings;
	SharedPreferences.Editor editor = PowerGarden.mSettings.edit();
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
		
	
	//--- this device
	public static class Device{
		static String ID = "set_id";
		static String connectionID;
		static int PlantNum = 8;
		static String plantType = "Cherry Tomatoes"; //should be populated by prefs	
		static PlantObject plants[] = new PlantObject[PlantNum];
		static String host;
		static String port;
		static String deviceMood;
		static String messageCopy = plantType;
		static int distanceThreshold; //distance at which we'll send a PING to server
		static boolean datastream_mode = false; //when this is true, we will stream all data as it comes in, to server.
	}
	
	//--- global socket vars
	public static SocketManager SM;
	public static int SocketConnected = 90;
	public static int Connected = 91;
	public static int Disconnected = 92;
	public static int Registered = 93;
	public static int Updated = 94;
	public static int MessageUpdated = 95;
	public static int Touched = 96;
	public static int ThreshChange = 97;
	public static int StreamModeUpdate = 98;
	public static int PlantIgnore = 99;
	public static int Unrecognized = 100;
	public static String serverResponseRaw;

	//--- global sensors
	public static int temp = 0;
	public static int hum = 0;
	public static int light = 0;
	public static int moisture = 0;
	public static int distance = 0;
	
	//--- app status
	public static boolean bAudioPlaying = false;
	public static boolean bConnected = false;
	public static boolean bRegistered = false;
	static boolean sendFakeNumbersOnUpdateButtonClick = true; ///**** IS YOUR NAME CHRIS PIUGGI? ****///
}
