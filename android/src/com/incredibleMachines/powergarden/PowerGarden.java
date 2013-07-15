package com.incredibleMachines.powergarden;

import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.SoundPool;

public class PowerGarden {

    static Typeface italiaBook;
    static Typeface interstateBold;
    
    public static StateManager stateManager = new StateManager();
    public static AudioManager audioManager = new AudioManager();
   
	
    public static String[] plantState = {"lonely", "content", "worked_up"};
    public static int plantStateIndex = 0;
    public static String[] deviceState = {"low", "medium", "high"};
    public static int deviceStateIndex = 0;
    
    public static JSONObject dialogue;// = new JSONObject();
    public static String[] copyType = {"touchRequest", "touchResponseGood", "touchResponseBad", 
    									"waterRequest", "waterResponseGood", "waterResponseBad"};
    
    //public static long plantStateChangeDur = 30000L;
        
	//--- audio
    public static SoundPool soundPool;
	public static int numSounds = 8;
	static JSONArray allPlantAudio = new JSONArray();
	static JSONArray plantAudio_touchRequest = new JSONArray(); 		 //holds audio file names
	static Vector <Integer> touchRequestAudio = new Vector <Integer> (); //holds sound pool player refs
	static JSONArray plantAudio_touchResponseGood = new JSONArray();
	static Vector <Integer> touchResponseGoodAudio = new Vector <Integer> ();
	static JSONArray plantAudio_touchResponseBad = new JSONArray();
	static Vector <Integer> touchResponseBadAudio = new Vector <Integer> ();
	static JSONArray plantAudio_waterRequest = new JSONArray();
	static Vector <Integer> waterRequestAudio = new Vector <Integer> ();
	static JSONArray plantAudio_waterResponseGood = new JSONArray();
	static Vector <Integer> waterResponseGoodAudio = new Vector <Integer> ();
	static JSONArray plantAudio_waterResponseBad = new JSONArray();
	static Vector <Integer> waterResponseBadAudio = new Vector <Integer> ();
	//public static Vector <Integer>[] audio = new Vector <Integer>()[6]; //future?
	//static Vector <Vector <Integer>> audio;
	//static Vector <Integer>[] audio = {touchRequestAudio, touchResponseGoodAudio};
	
	//public static int[] plantAudio = new int [numSounds];
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
	

	// event types
	public static int Registered = 93;
	public static int Updated = 94;
	public static int MessageUpdated = 95;
	public static int Touched = 96;
	public static int ThreshChange = 97;
	public static int StreamModeUpdate = 98;
	public static int PlantIgnore = 99;
	public static int DisplayTweet = 100;
	public static int Settings = 101;
	public static int Unrecognized = 102;
			
	//--- app status
	public static boolean bAudioPlaying = false;
	public static boolean bConnected = false;
	public static boolean bRegistered = false;
	static boolean sendFakeNumbersOnUpdateButtonClick = true; ///**** IS YOUR NAME CHRIS PIUGGI? --YES****///
	
	//--- global socket vars
	public static SocketManager SM;
	public static String serverResponseRaw;
	// socket status
	public static int SocketConnected = 90;
	public static int Connected = 91;
	public static int Disconnected = 92;
	
	//-------------------------//	
	//****** this device ******//
	//-------------------------//
	
	public static class Device{
		//--- connection stuff
		static String ID = "set_id";
		static String connectionID;
		static String host;
		static String port;
	    static String deviceState = "low";
		
		//--- plants
		static int PlantNum = 8;
		static String plantType = "cherry_tomatoes"; //should be populated by prefs	
		static PlantObject plants[] = new PlantObject[PlantNum];
		static boolean isWatering = false;
		
		static boolean datastream_mode = false; //when this is true, we will stream all data as it comes in, straight to server.
		
		//--- tweets, screen stuff
		static Vector <String> tweetCopy = new Vector<String>();
		static Vector <String> tweetUsername = new Vector<String>();;
		static String messageCopy = "Welcome to @ThePowerGarden!";
		public static int displayMode = PowerGarden.DisplayMode.MessageCopy;
		
		//--- global device sensors
		public static int temp = 0;
		public static int hum = 0;
		public static int light = 0;
		public static int moisture = 0;
		public static int distance = 0;
		public static int lastDistance = 0;
		public static int totalNumPlantTouches = 0; //running total of all plant touches on DEVICE in a period at any given time
		
		//--- thresholds
		//static int distanceThreshold; //distance at which we'll send a PING to server
		static boolean moistureActive = Boolean.valueOf(PowerGarden.getPrefString("moisture_active","true"));
		static int moistureLowThresh =  Integer.parseInt(PowerGarden.getPrefString("moisture_low","800"));
		static int moistureHighThresh = Integer.parseInt(PowerGarden.getPrefString("moisture_high","800"));
		
		static boolean tempActive = Boolean.valueOf(PowerGarden.getPrefString("temp_active","true"));
		static int tempLowThresh = Integer.parseInt(PowerGarden.getPrefString("temp_low","25"));;
		static int tempHighThresh = Integer.parseInt(PowerGarden.getPrefString("temp_high","45"));;
		
		static boolean humidityActive = Boolean.valueOf(PowerGarden.getPrefString("humidity_active","true"));
		static int humidityLowThresh = Integer.parseInt(PowerGarden.getPrefString("humidity_low","60"));
		static int humidityHighThresh = Integer.parseInt(PowerGarden.getPrefString("humidity_high","85"));
		
		static boolean lightActive = Boolean.valueOf(PowerGarden.getPrefString("light_active","true"));
		static int lightLowThresh = Integer.parseInt(PowerGarden.getPrefString("light_low","200"));
		static int lightHighThresh = Integer.parseInt(PowerGarden.getPrefString("light_high","85"));
		
		static boolean touchActive = Boolean.valueOf(PowerGarden.getPrefString("touch_active","true"));
		static int touchLowThresh = Integer.parseInt(PowerGarden.getPrefString("touch_low","1"));
		static int touchHighThresh = Integer.parseInt(PowerGarden.getPrefString("touch_high","14"));
		static int touchWindow = Integer.parseInt(PowerGarden.getPrefString("touch_window","30"));

		
		static boolean rangeActive = Boolean.valueOf(PowerGarden.getPrefString("range_active","true"));
		static int rangeLowThresh = Integer.parseInt(PowerGarden.getPrefString("range_low","50"));
		static long lastRangeHitTime = 0;

	}
	
	public static class DisplayMode{ // different display mode codes
		final static int PlantTitle = 111;
		final static int MessageCopy = 222;
		final static int Tweet = 333;
	}
	
	
}
