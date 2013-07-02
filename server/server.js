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


app.use(express.favicon(__dirname+'/public/favicon.ico'));
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
	this.plant_type;
	this.plant_slug;
}

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

