package com.incredibleMachines.powergarden;

import android.net.Uri;
import android.widget.TextView;

public class PowerGarden {

	public static SocketManager SM;
	public static int Connected = 1;
	public static int Disconnected = 2;
	public static int Planted = 3;
	
	
	public static class Device{
		
		static String ID;
		static String connectionID;
		static String PlantNum;
		static String plantType = "Cherry Tomatoes"; //should be populated by prefs
		
	}
	
	public static int temp;
	public static int hum;
	public static int light;
	public static int moisture;
	public static int distance;
}
