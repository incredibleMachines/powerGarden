package com.incredibleMachines.powergarden;

import android.net.Uri;
import android.widget.TextView;

public class PowerGarden {
	
	//--- this device
	public static class Device{
		static String ID;
		static String connectionID;
		static String PlantNum;
		static String plantType = "Cherry Tomatoes"; //should be populated by prefs	
		//thresholdValues[]
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
