/* ******************************************************************************************* */
/* Pull in required modules						 								 			   */
/* ******************************************************************************************* */

var io = require('socket.io').listen(9000).set('log level', 2);
var DB = require('./db/index');
var pgtwitter = require('./pgtwitter/index');
// var pump = require('./pump/index');
var colors = require('colors');


var express = require('express');
var app = express();
var server = require('http').createServer(app);
var browserio = require('socket.io').listen(server).set('log level',2);

server.listen(8080);
//app.listen(8080);

colors.setTheme({
  silly: 'rainbow',
  input: 'grey',
  verbose: 'cyan',
  prompt: 'grey',
  info: 'green',
  data: 'grey',
  help: 'cyan',
  warn: 'yellow',
  debug: 'blue',
  error: 'red'
});


/* ******************************************************************************************* */
/* Define globals								 								 			   */
/* ******************************************************************************************* */

var clients = {};
var browsers = {};


//Connect to twitter, pass callback for responding to tweets
//pgtwitter.start(twitterCallback);

/* ******************************************************************************************* */
/* ******************************************************************************************* */


app.use(express.favicon(__dirname+'/public/favicon.ico'));
app.use(express.static(__dirname+'/public'));
//app.use(express.static('../website'));



/* ******************************************************************************************* */
/* ******************************************************************************************* */
var browserClientID = 0;

browserio.sockets.on('connection',function(browserSocket){
	
	var browserConnection = new Connection( ++browserClientID, 'set_id', browserSocket);
	var connectKey = 'browser-'+browserClientID;
	browsers[connectKey]=browserConnection;

	console.log("[BROWSER CONN]".help+" connection.id %s".input, browserConnection.id);

	for(var key in clients) {
		// console.log('Calling deviceInfo() for: ' + clients[key]['device_id']);
		database.deviceInfo(clients[key]['id'], clients[key]['device_id'], function(result) {
			browserSocket.emit('init', result);
		});

		// console.log('Calling settingsForDevice() for: ' + clients[key]['device_id']);
		database.settingsForDevice(clients[key]['id'], clients[key]['device_id'], function(result) {
			browserSocket.emit('settings', result);
		});
	}
	
	//browserSocket.emit('init', clients);
	browserSocket.on('threshold',function(msg){
		console.log(msg);
		clients['client-'+msg.connection_id].socket.emit('threshold',msg);
		browserSocket.broadcast.emit('threshold', msg);
		
		
	});
	browserSocket.on('control',function(msg){
		console.log(msg);
		clients['client-'+msg.connection_id].socket.emit('control',msg);
		browserSocket.broadcast.emit('control', msg);
		
	});

	browserSocket.on('ignore',function(msg){
		console.log(msg);
		clients['client-'+msg.connection_id].socket.emit('ignore',msg);
		browserSocket.broadcast.emit('ignore', msg);
			
		
	});

	browserSocket.on('settings',function(msg){
		// console.log(msg);
		database.updateSettings(msg, function() {
			clients['client-'+msg.connection_id].socket.emit('settings',msg);
			browserSocket.broadcast.emit('settings', msg);
		});
	});
	
	browserSocket.on('disconnect',function(){
		delete browsers['browser'+browserConnection.id];
		console.log("[BROWSER DISCONN]".error+" connection.id %s".input, browserConnection.id);

	});
	
	
});

//Connect to mongo server, store collection references
var database = new DB(browserio.sockets);

/* ******************************************************************************************* */
/* ******************************************************************************************* */
var clientID = 0;


io.sockets.on('connection', function (socket) {
  //io.sockets.emit('this', { will: 'be received by everyone'});
  	var connection = new Connection( ++clientID, 'set_id', socket);
	var connectKey = 'client-'+clientID;
	clients[connectKey]=connection;

	console.log("[NEW CONN]".help+" connection.device_id %s".data,connection.device_id);
	
	//browserio.sockets.emit('device_conn',connectKey);
	
	socket.on('register', function (msg) {
	    console.log('[INBOUND REQUEST]'.warn+' [REGISTER] '.warn + JSON.stringify(msg).input);
	    database.routeRegister(msg,connection);
	  
	});
	
	socket.on('update', function (msg) {
	    console.log('[INBOUND REQUEST]'.warn+' [UPDATE] '.help + JSON.stringify(msg).input);
	    database.routeUpdate(msg,connection);
		browserio.sockets.emit('update',msg);
	});
	
	socket.on('touch',function(msg){
		console.log('[INBOUND REQUEST]'.warn+' [TOUCH] '.info + JSON.stringify(msg).input);
		database.plantTouch(msg,connection);
		browserio.sockets.emit('touch',msg);
	});
	
	socket.on('threshold',function(msg){
		msg.connection_id = connection.id;
		browserio.sockets.emit('threshold',msg);
	});

	socket.on('ignore',function(msg){
		msg.connection_id = connection.id;
		browserio.sockets.emit('ignore',msg);
	});

	socket.on('stream',function(msg){
		msg.connection_id = connection.id;
		browserio.sockets.emit('stream',msg);
	});
	
	socket.on('disconnect', function () {
	  /* io.sockets.emit('user disconnected'); */
  	  
	  console.log("[CLIENT DISCONN]".error+"connection.id %s".input, connection.id);
	  console.log("[CLIENT DISCONN]".error+"connection.device_id %s".input, connection.device_id);
	  
	  var res = {device_id: connection.device_id, connection_id: connection.id};
	  
	  browserio.sockets.emit('device_disconn',res);
	  
	  if(connection.device_id != 'set_id') database.setActive(connection, false);
	  delete clients["client-"+connection.id];
	  //set device to inactive

	});
});

/* ******************************************************************************************* */
/*																							   */
/* ******************************************************************************************* */

function Connection(_id, _device_id,_socket){
	//variables can be altered when  
	//they are within a global object
	
	this.id = _id;
	this.device_id = _device_id;
	this.socket = _socket;	
	this.plant_type;
	this.plant_slug;
}

function twitterCallback(data) {

	// here just for debugging. this will exist further down in the logic
	pump.turnOnSprinklers(15);

	if (data.water) {

		// User is trying to provide water

		/*
		// look up water needs of all devices
		// average most recent moisture reading for each device?
		// determine dry|content|wet for each device and then average those?
		if (PLANTS NEED WATER) {

			// FIGURE OUT HOW LONG TO RUN PUMP FOR
			// thresholds for moisture readings and settings for diff lengths
			// e.g. plants don't need lots of water, run for 5 seconds
			// or plants need lots of water, run for 30 seconds
			var average = CALCULATE_AVERAGE_WATER_NEEDS();

			if (average <= SOME_LOW_THRESHOLD) {
				 var pumpDuration = 5;
			} else if (average <= SOME_HIGH_THRESHOLD) {
				var pumpDuration = 15;
			} else {
				var pumpDuration = 30;
			}

			// ADD TIME FOR PRIMING
			// look up last time we ran pump, add some time if it's been a while
			// stored where? different document type in db.settings?
			var lastTimestamp = QUERY_LAST_SPRINKLER_TIMESTAMP()
			if (lastTimestamp < 10_MINUTES_AGO) {
				pumpDuration += 3;
			}

			// pump.turnOnSprinklers(pumpDuration);

			// TWITTER RESPONSE
			// pgtwitter.updateStatus('message here')


		} else {

			// TWITTER RESPONSE
			// pgtwitter.updateStatus('message here')
	
		}
		*/
		

		if (!data.plants) {
			// No mention of a specific plant
			// Send thanks for water from garden
			console.log('[Twitter Stream] Thanks for water from garden');
			// pgtwitter.updateStatus('message here')
		} else {
			// User mentioned a specific plant
			// Send thanks for water from the plant
			console.log('[Twitter Stream] Thanks for water from ' + data.plants.join(' & '));
			// pgtwitter.updateStatus('message here')
		}

	} else {
		// Not providing water

		if (!data.plants) {
			// No mention of a specific plant
			// Send thanks for attention from garden
			console.log('[Twitter Stream] Thanks for attention from garden');
			// pgtwitter.updateStatus('Thanks for hanging out @' + data.user + '! Watch us grow at http://ustre.am/10WaX', { in_reply_to_status_id: data.id });
		} else {
			// User mentioned a specific plant
			// Send thanks for attention from the plant
			console.log('[Twitter Stream] Thanks for attention from ' + data.plants.join(' & '));
			// pgtwitter.updateStatus('Thanks for the good vibes @' + data.user + '! The ' + data.plants.join(' & ') + ' are loving it!', { in_reply_to_status_id: data.id });
		}
	
	}
}

