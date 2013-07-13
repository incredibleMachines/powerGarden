/*

 Incredible Machines
 The Power Garden
 Summer 2013
 
 Pump relay controller code
 
 */

#include <WiFlyHQ.h>
WiFly wifly;

// Network settings
const char mySSID[] = "localnet";
const char myPassword[] = "ep%9JuC13";

// Server settings
char site[] = "10.0.1.2";
int port = 9001;

// Pins for devices
int wiflyPin = 11;
int sprinklerPin = 6;

// Start with sprinklers off
boolean sprinklerState = false;

// Timing variables
unsigned long turnOffTime;
int reconnectInterval = 30 * 1000;


void setup()
{
  char buf[32];

  // Pull pin 11 high, necessary for Visualight board
  pinMode(wiflyPin, OUTPUT);
  digitalWrite(wiflyPin, HIGH);

  // Set up sprinkler relay pin
  pinMode(sprinklerPin, OUTPUT);
  digitalWrite(sprinklerPin, !sprinklerState);

  // Start serial connections
  Serial.begin(115200);
  Serial1.begin(9600);

  // Start serial connection to wifly
  if (!wifly.begin(&Serial1, &Serial)) {
    Serial.println("Failed to start wifly");
  }

  // Join wifi network if not already associated
  if (!wifly.isAssociated()) {
    // Setup the WiFly to connect to a wifi network
    Serial.println("Joining network");
    wifly.setSSID(mySSID);
    wifly.setPassphrase(myPassword);
    wifly.setJoin(WIFLY_WLAN_JOIN_AUTO);
    wifly.enableDHCP();
    wifly.save();

    if (wifly.join()) {
      Serial.println("Joined wifi network");
    } 
    else {
      Serial.println("Failed to join wifi network");
    }
  } 
  else {
    Serial.println("Already joined network");
  }

  Serial.print("MAC: ");
  Serial.println(wifly.getMAC(buf, sizeof(buf)));
  Serial.print("IP: ");
  Serial.println(wifly.getIP(buf, sizeof(buf)));
  Serial.print("Netmask: ");
  Serial.println(wifly.getNetmask(buf, sizeof(buf)));
  Serial.print("Gateway: ");
  Serial.println(wifly.getGateway(buf, sizeof(buf)));


  wifly.setDeviceID("Wifly-WebClient");

  // Close old connection if active
  if (wifly.isConnected()) {
    Serial.println("Old connection active. Closing");
    wifly.close();
  }

  // Connect initially before we hit the run loop
  if (wifly.open(site, port)) {
    Serial.print("Connected to ");
    Serial.println(site);
  } 
  else {
    Serial.println("Failed to connect");
  }
}

void loop() {

  // Check connection
  // Apparently wifly.isAssociated is ALWAYS returning false, so just ignore this check
  //  if(wifly.isAssociated()) {
  if (wifly.isConnected() == false) {
    Serial.println("Not connected to server, trying to connect...");
    if (wifly.open(site, port)) {
      Serial.println("Opened connection.");
    } 
    else {
      Serial.println("Failed to open connection.");
      delay(reconnectInterval);
    }
  }
  //  } 
  //  else {
  //
  //    // Setup the WiFly to connect to a wifi network
  //    Serial.println("Run loop, not associated. Joining network.");
  //    wifly.setSSID(mySSID);
  //    wifly.setPassphrase(myPassword);
  //    wifly.setJoin(WIFLY_WLAN_JOIN_AUTO);
  //    wifly.enableDHCP();
  //
  //    if (wifly.join()) {
  //      Serial.println("Joined wifi network");
  //    } 
  //    else {
  //      Serial.println("Failed to join wifi network");
  //    }
  //
  //    if (wifly.isConnected()) {
  //      Serial.println("Old connection active. Closing");
  //      wifly.close();
  //    }
  //
  //    if (wifly.open(site, port)) {
  //      Serial.print("Connected to ");
  //      Serial.println(site);
  //    } 
  //    else {
  //      Serial.println("Failed to connect");
  //    }
  //
  //    delay(1000);
  //  }

  if (wifly.available() > 0) {

    // Expecting a message like 1,15000 or 0,0
    // So call parseInt() twice to grab each value
    int state = wifly.parseInt();
    unsigned long duration = wifly.parseInt();

    //    Serial.print("State: "); 
    //    Serial.println(state);
    //    Serial.print("Duration: "); 
    //    Serial.println(duration);

    if (state == 1) {
      Serial.print("Turning on sprinklers for ");
      Serial.print(duration / 1000);
      Serial.println(" seconds.");

      // Flip our bool and set the timestamp for a future date to turn off
      sprinklerState = true;
      turnOffTime = millis() + duration;
    } 
    else {
      Serial.println("Turning off sprinklers (server-initiated).");

      // Set bool to false and set turn off timestamp to a time in the past
      // to force the sprinklers off
      sprinklerState = false;
      turnOffTime = millis() - 1;
    }
  }

  // Check if sprinklers are on and we've passed the turn off time
  if (sprinklerState && (millis() >= turnOffTime)) {
    Serial.println("Turning off sprinklers (self-initiated).");
    sprinklerState = false;
  }

  // Write the inverse of our bool since the relay is inverted
  digitalWrite(sprinklerPin, !sprinklerState);

  if (Serial.available() > 0) {
    wifly.write(Serial.read());
  }

}







