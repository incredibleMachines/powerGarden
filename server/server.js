/* ******************************************************************************************* */
/* Pull in required modules						 								 			   */
/* ******************************************************************************************* */

var io = require('socket.io').listen(9000).set('log level', 2);
var DB = require('./db/index');
var pgtwitter = require('./pgtwitter/index');
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


/* app.use(express.favicon()); */
app.use(express.static(__dirname+'/public'));



/* ******************************************************************************************* */
/* ******************************************************************************************* */
var browserClientID = 0;

browserio.sockets.on('connection',function(browserSocket){
	
	console.log('connection incoming');
	var browserConnection = new Connection( ++browserClientID, 'set_id', browserSocket);
	var connectKey = 'browser-'+browserClientID;
	browsers[connectKey]=browserConnection;
	
	
	for(var key in clients){
	
		browserSocket.emit('init', {connection_id: clients[key]['id'], device_id: clients[key]['device_id']});	
	}
	//browserSocket.emit('init', clients);
	browserSocket.on('threshold',function(msg){
		console.log(msg);
		clients['client-'+msg.connection_id].socket.emit('threshold',msg);
		
	});
	browserSocket.on('control',function(msg){
		console.log(msg);
		clients['client-'+msg.connection_id].socket.emit('control',msg);
	});
	browserSocket.on('ignore',function(msg){
		console.log(msg);
		clients['client-'+msg.connection_id].socket.emit('ignore',msg);
	});
	
	browserSocket.on('disconnect',function(){
		delete browsers['browser'+browserConnection.id];
		console.log("[BROWSER DISCONN]".error+"connection.id %s".input, browserConnection.id);

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
	  
	});
	
	socket.on('touch',function(msg){
		console.log('[INBOUND REQUEST]'.warn+' [TOUCH] '.info + JSON.stringify(msg).input);
		database.plantTouch(msg,connection);
		  
	});
	
	socket.on('threshold',function(msg){
		msg.connection_id = connection.id;
		browserio.sockets.emit('threshold',msg);
		
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
}

/* ******************************************************************************************* */
/* Update the plant's mood based on interaction & envrionmental data 						   */
/* ******************************************************************************************* */

/*
function checkPlantMood(message,connection) {

	// fetch data from db for given device id
	plantsDb.findOne({ 'device_id': new BSON.ObjectID(message.device_id), 'index': message.plant_index } ,function(err,result) {
		if (err)  console.error(err); //throw err;
		if (!result) { console.log('[Check Plant Mood] No plant found for given id: ' + message.device_id); return; }
		
		// environmental data:
		//
		// moisture		dried out	|	good	|	wet
		// humidity		dried out	|	good	|	wet
		// temp 		cold		|	good	|	hot
		// light 		too dark	|	good	|	too bright
		//
		//
		// interaction data:
		//
		// touch		lonely		|	happy	|	worked up

		// need to pull thresholds for each data type from somewhere

		// set state variables to -1, 0, or 1 depending on where they fall wrt their thresholds
		// magic
		moistureState = moistureVal < moistureLowThreshold ? -1 : val < moistureHighThreshold ? 0 : 1;
		humidityState = humidityVal < humidityLowThreshold ? -1 : val < humidityHighThreshold ? 0 : 1;
		tempState = tempVal < tempLowThreshold ? -1 : val < tempHighThreshold ? 0 : 1;
		lightState = lightVal < lightLowThreshold ? -1 : val < lightHighThreshold ? 0 : 1;

		touchState = touchVal < touchLowThreshold ? -1 : val < touchHighThreshold ? 0 : 1;
		// (nested ternaries ftw)

		// we're prioritizing touch & attention over environmental data
		if (touchState == -1) {
			// here, not enough attention
			mood = "lonely";
		} else if (touchState == 1) {
			// too much attention
			mood = "workedup";
		} else if (touchState == 0) {

			// good amount of attention, we're happy
			// now look to environmental data to see what it needs

			if (moistureState == -1) {
				mood = "dry";
			} else if (moistureState == 1) {
				mood = "wet";
			} else if (moistureState == 0) {
				mood = "happy";
			}

		}
	});
}
*/


/* ******************************************************************************************* */
/* Generate a response based on the plant's mood 								 			   */
/* ******************************************************************************************* */

function generateResponse(message,connection) {

	// fetch data from db for given device id
	plantsDb.findOne({ '_id': new BSON.ObjectID(message.device_id), 'index': message.plant_index  } ,function(err,result) {
		if (err) console.error(err); //throw err;
		if (!result) { console.log('[Generate Response] No plant found for given id: ' + message.device_id); return; }

		if (result.mood == "lonely") {
			// lonely response
		}
		if (result.mood == "workedup") {
			// worked up response
		}
		if (result.mood == "dry") {
			// dry response
		}
		if (result.mood == "wet") {
			// wet/soggy response
		}
		if (result.mood == "happy") {
			// basic needs of attention & water met
			// responses could...
			// thank user for fulfilling a need (attention, water)
			// comment on weather (hot, dry, humid)
			// provide a factoid
		}
	});
}


/* ******************************************************************************************* */
/* Callback for dealing with processed stream data 								 			   */
/* ******************************************************************************************* */

function twitterCallback(data) {

	if (!data.water) {

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

	} else {

		// User is providing water

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
	}
}

