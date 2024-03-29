/* ******************************************************************************************* */
/* Pull in required modules						 								 			   */
/* ******************************************************************************************* */

var io = require('socket.io').listen(9000).set('heartbeat timeout', 45).set('heartbeat invterval', 20).set('log level', 2);
var DB = require('./db/index');
var pgtwitter = require('./pgtwitter/index');
var pump = require('./pump/index');
var colors = require('colors');
var dialogue = require('./dialogue');

//console.log(dialogue);

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
app.get('/dialogue',function(req,res,next){
	res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' }); 
	var type=req.param('type');
	var json = '{"'+type+'" : '+JSON.stringify(dialogue[type])+'}';
	
	res.end(json);
});

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
		if (clients[key]['device_id'] == 'set_id') continue;

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
		database.updateThreshold(msg, function() {
			clients['client-'+msg.connection_id].socket.emit('threshold',msg);
			browserSocket.broadcast.emit('threshold', msg);
		});
	});
	browserSocket.on('firehose',function(msg){
		console.log(msg);
		clients['client-'+msg.connection_id].socket.emit('firehose',msg);
		browserSocket.broadcast.emit('firehose', msg);
		
	});

	browserSocket.on('ignore',function(msg){
		console.log(msg);
		database.updateIgnore(msg, function() {
			clients['client-'+msg.connection_id].socket.emit('ignore',msg);
			browserSocket.broadcast.emit('ignore', msg);
		});
	});

	browserSocket.on('tablet',function(msg){
		console.log(msg);
		database.updateTablet(msg, function() {
			clients['client-'+msg.connection_id].socket.emit('tablet',msg);
			browserSocket.broadcast.emit('tablet', msg);
		});
	});

	browserSocket.on('chorus',function(msg){
		console.log(msg);
		for (var key in clients) {
			var obj = msg;
			obj.device_id = clients[key].device_id; 
			clients[key].socket.emit('chorus', obj);
		}
	});

	browserSocket.on('settings',function(msg){
		console.log(msg);
		database.updateSettings(msg, function() {
			clients['client-'+msg.connection_id].socket.emit('settings',msg);
			browserSocket.broadcast.emit('settings', msg);
		});
	});

	browserSocket.on('sprinklers',function(msg){
		//console.log(msg);
		if (msg.state) {
			// run for 10 minutes by default
			pump.turnOnSprinklers(60 * 5);
		} else {
			pump.turnOffSprinklers();
		}
	});
	browserSocket.on('pump', function(msg){
		//console.log(msg);
		database.WateringEnabled = msg.state;
		
		console.log("[PUMP] Watering State Set: "+ database.WateringEnabled);
	});
	// browserSocket.on('restart-twitter-stream',function(msg){
	// 	pgtwitter.restart();
	// });
	
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

// testing auto kickoff if no register within 5 sec. doesn't really help issue of tablets connecting multiple times
// function terminateUnregisteredClients() {
// 	for(var key in clients) {
// 		if (clients[key]['device_id'] == 'set_id') {
// 			console.log('Killing %s, %s:', clients[key].id, clients[key].device_id);
// 			clients[key].socket.disconnect();
// 		}
// 	}
// }

io.sockets.on('connection', function (socket) {
  
  	var connection = new Connection( ++clientID, 'set_id', socket);
	var connectKey = 'client-'+clientID;
	clients[connectKey]=connection;

	console.log("[NEW CONN]".help+" connection.device_id %s".data,connection.device_id);
	
	// setTimeout(terminateUnregisteredClients, 5000);

	//browserio.sockets.emit('device_conn',connectKey);
	

	/* Messaging events */

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
		database.routeTouch(msg,connection, function(chorus) {
			// callback receives a boolean for whether or not we should run the chorus
			// based on if we get touches on 3+ plants in last 30 sec and it hasn't
			// been run in 30 min
			if (chorus) {
				var obj = { start_time: new Date(), file_name: 'thissong_1.mp3' };
				for (var key in clients) {
					obj.connection_id = clients[key].id; 
					obj.device_id = clients[key].device_id; 
					clients[key].socket.emit('chorus', obj);
				}
			}
		});
		browserio.sockets.emit('touch',msg);
	});

	socket.on('display',function(msg){
		console.log('[DISPLAY] '.warn + JSON.stringify(msg).input);
		// probably should be logging all things displayed by tablets. necessary?
		// database.routeDisplay(msg,connection);
		browserio.sockets.emit('display',msg);
	});


	/* Control protocol events */
	
	socket.on('threshold',function(msg){
		msg.connection_id = connection.id;
		console.log('[CONTROL]'.warn+' [THRESHOLD] '.warn + JSON.stringify(msg).input);
		database.updateThreshold(msg, function() {
			browserio.sockets.emit('threshold',msg);
		});
	});

	socket.on('ignore',function(msg){
		msg.connection_id = connection.id;
		console.log('[CONTROL]'.warn+' [IGNORE] '.warn + JSON.stringify(msg).input);
		database.updateIgnore(msg, function() {
			browserio.sockets.emit('ignore',msg);
		});
	});

	socket.on('tablet',function(msg){
		msg.connection_id = connection.id;
		console.log('[CONTROL]'.warn+' [TABLET] '.warn + JSON.stringify(msg).input);
		database.updateTablet(msg, function() {
			browserio.sockets.emit('tablet',msg);
		});
	});

	socket.on('chorus',function(msg){
		msg.connection_id = connection.id;
		console.log('[CONTROL]'.warn+' [CHORUS] '.warn + JSON.stringify(msg).input);
		browserio.sockets.emit('chorus',msg);
	});

	socket.on('stream',function(msg){
		msg.connection_id = connection.id;
		console.log('[CONTROL]'.warn+' [STREAM] '.warn + JSON.stringify(msg).input);
		browserio.sockets.emit('stream',msg);
	});


	/* Disconnect */

	socket.on('disconnect', function () {
	  /* io.sockets.emit('user disconnected'); */
  	  
	  console.log("[CLIENT DISCONN]".error+"connection.id %s, connection.device_id %s".input, connection.id, connection.device_id);
	  
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

function twitterCallback(data, raw) {

	// here just for debugging. this will exist further down in the logic
	// pump.turnOnSprinklers(15);

	// before we process, just log the incoming tweet
	var inboundObj = { timestamp: new Date(), type: 'inbound', water: data.water, plants: data.plants, data: raw };
	database.logTweet(inboundObj);

	if (data.water) {
		// console.log('User providing water');

		// User is trying to provide water

		// look up water needs of all devices
		var pumpDuration = 30;
		database.calculateGardenWaterNeeds(function(pumpDuration) {
			console.log('Result from calculateGardenWaterNeeds() is: ' + pumpDuration);

			if (pumpDuration > 0) {

				// PLANTS NEED WATER

				// add time for priming
				database.neededTimeForPriming(function(primingDuration) {
					// console.log('Adding '+primingDuration+'to pump');

					pumpDuration += primingDuration;

					// log needs to happen after neededTimeForPriming()!
					// otherwise we insert a timestamp and then calculate off it, meaning
					// we will never need to prime since we just ran
					database.logPump(pumpDuration);
					pump.turnOnSprinklers(pumpDuration);


					// TWITTER RESPONSE
					// Send appreciation for watering

					if (!data.plants) {
						// console.log('Water tweeted at garden');
						var plant = 'garden';
					} else {
						// Just pull the first mentioned one for now
						var plant = data.plants[0];
						// console.log('Water tweeted at '+plant);
					}

					var responses = dialogue[plant].waterResponseGood.stage_copy;
					processResponses(plant, responses, true);
				});

			} else {

				// PLANTS DO NOT NEED WATER
				// Don't run pump, only send twitter response
				// Tell them to hold off on watering

				if (!data.plants) {
					// console.log('Water tweeted at garden BUT WE DON\'T NEED IT');
					var plant = 'garden';
				} else {
					// just pull the first mentioned one for now
					var plant = data.plants[0];
					// console.log('Water tweeted at '+plant+' BUT WE DON\'T NEED IT');
				}

				var responses = dialogue['garden'].waterResponseBad.stage_copy;
				processResponses(plant, responses);
			}
		});

	} else {

		// Not providing water
		// Send thanks for attention

		/* NO LONGER SENDING PLANT-SPECIFIC RESPONSES OR GENERIC APPRECIATION OF ATTENTION */
		/* INSTEAD IT'S A THANKS + MENTION OF NEXT TASTING EVENT!! */

		// if (!data.plants) {
		// 	// console.log('Attention tweeted at garden');
		// 	var plant = 'garden';
		// } else {
		// 	// just pull the first mentioned one for now
		// 	var plant = data.plants[0];
		// 	// console.log('Attention tweeted at '+plant);
		// }

		// set up date for next tasting event
		var responses = dialogue['garden'].events;
		processResponses(plant, responses);
	}

	function processResponses(plant, responses, _watering) {
		// console.log('Plant targeted: ' + plant);
		// if (data.emit) console.log('Emits also to: ' + data.emit);
		// console.log('Potential responses are...');
		// console.log(responses);

		var index = Math.floor(Math.random()*responses.length);
		var text = '@'+data.user_name + ' ' + responses[index];
		var watering = _watering || false;

		// check if response has [DATE] placeholder and update it if so
		if (text.indexOf('[DATE]') > -1) {
			var today = new Date( (new Date()).toDateString() );
			var tomorrow = new Date(today.getTime() + (24 * 60 * 60 * 1000));

			var tastingDates = ['07/23/2013', '08/06/2013', '08/14/2013', '08/20/2013', '08/27/2013'];
			var weekDays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
			var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];


			for (var i = 0; i < tastingDates.length; i++) {
				var date = new Date( new Date(tastingDates[i]).toDateString() );
				var dateFormatted = weekDays[date.getDay()] +' '+ months[date.getMonth()] +' '+ date.getDate();

				// check if date has passed. if so, move on to next date
				if (today > date) continue;

				if (today.toDateString() == date.toDateString()) {
					text = text.replace('[DATE]', 'TODAY');
					break;
				}
				if (tomorrow.toDateString() == date.toDateString()) {
					text = text.replace('[DATE]', 'TOMORROW');
					break;
				}

				text = text.replace('[DATE]', 'on ' + dateFormatted);
				break;

			}

			// we got through the loop but no text was substituted
			// in other words, all of the tasting events have already happened but we're still running
			// instead, pull from a different set of responses that mention finale
			if (text.indexOf('[DATE]') > -1) {
				responses = dialogue['garden'].eventsPast;
				index = Math.floor(Math.random()*responses.length);
				text = '@'+data.user_name + ' ' + responses[index];
			}
		}

		// send a tweet event to appropriate tablets
		for (var key in clients) {
			// console.log('key: '+key+', slug: '+clients[key].plant_slug+', emit: '+data.emit);
			if (!data.plants || clients[key].plant_slug == plant || clients[key].plant_slug == data.emit) {
				var obj = {
					device_id: clients[key].device_id,
					user_name: data.user_name,
					text: responses[index],
					plant_type: plant,
					water: watering
				}
				clients[key].socket.emit('tweet', obj);
				console.log('[TWEET] '.warn + JSON.stringify(obj).input);
				// console.log('Sending tweet event to: plant_type: '+clients[key].plant_type+', plant_slug: '+clients[key].plant_slug);
				// console.log(obj);
			}
		}

		// tweet that b
		pgtwitter.updateStatus(text, { in_reply_to_status_id: data.id }, function(tweet) {

			// log the outgoing tweet
			var outboundObj = { timestamp: new Date(), type: 'outbound', water: data.water, plants: data.plants, data: tweet };
			database.logTweet(outboundObj);

		});

	}

}

