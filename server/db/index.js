var MongoClient = require('mongodb').MongoClient,
	Server = require('mongodb').Server,
	mongo = new MongoClient(new Server('localhost', 27017)),
	BSON = require('mongodb').BSONPure;


//var utils = require('./dbUtils');	

	
function DB(browserSockets){
	
	this.db; 
	this.dataDb; 
	this.personalitiesDb; 
	this.devicesDb; 
	this.plantsDb;
	this.touchesDb;
	this.settingsDb;
	// this.moodDb;
	this.pumpDb;
	this.twitter;
	this.browserIO = browserSockets;
	
	//console.log(storedMood);
	
	//this.statusMood = new moods();
	
	//console.log(Moods);
	
	/* ******************************************************************************************* */
	/* Connect to mongo server, store collection references							 			   */
	/* ******************************************************************************************* */
	
	mongo.open(function(err,mongo){
		console.log("Database Connected");
		this.db = mongo.db('powergarden');
		this.devicesDb = db.collection('devices');
		this.plantsDb = db.collection('plants');
		this.personalitiesDb = db.collection('personalities');
		this.dataDb = db.collection('data');
		this.touchesDb = db.collection('touches');
		this.settingsDb = db.collection('settings');
		// this.moodDb = db.collection('mood');
		this.pumpDb = db.collection('pump');
	});
		
}

module.exports = DB;



/* ******************************************************************************************* */
/* Routines related to pulling info about devices (tablets) when browsers connect			   */
/* ******************************************************************************************* */

DB.prototype.deviceInfo = function(connection_id, device_id, callback) {
	if (device_id == 'set_id') return;

	// console.log('Device info for: ' + device_id);
	var obj = {	_id: new BSON.ObjectID( String(device_id)) };
	devicesDb.findOne(obj, function(err, res) {
		res.connection_id = connection_id;
		res.device_id = device_id;
		callback(res);
	});
}

DB.prototype.settingsForDevice = function(connection_id, device_id, callback) {
	if (device_id == 'set_id') return;

	// console.log('Checking settings for: ' + device_id);
	var obj = {	device_id: new BSON.ObjectID( String(device_id)) };
	settingsDb.findOne(obj, function(err, res) {
		res.connection_id = connection_id;
		res.device_id = device_id;
		callback(res);
	});
}

DB.prototype.updateSettings = function(data, callback) {
	// console.log(data);
	// kind of hacky here...
	// data.device_id comes in as a string
	// convert it to an ObjectId() in order to find the settings document
	var obj = { device_id: new BSON.ObjectID(String(data.device_id)) };

	// then take ObjectId() and store it back into the data object so it gets inserted
	// into the db as the object and not the string
	data.device_id = obj.device_id;
	settingsDb.update(obj, data, function(err, count) {
		callback();
	});
}

DB.prototype.updateThreshold = function(data, callback) {

	var obj = { device_id: new BSON.ObjectID(String(data.device_id)), 'cap_thresh.plant_index': data.plant_index };
	var settingsObj = { $set: { 'cap_thresh.$.value': data.value } }

	settingsDb.update(obj, settingsObj, function(err, count) {
		callback();
	});
}



/* ******************************************************************************************* */
/* Routines related to a tablet registering													   */
/* ******************************************************************************************* */

DB.prototype.routeRegister = function(message,connection){
	//check for ID in DB
	//if ID exists
	//return ID, status - connected, rejoined
	if(message.device_id == "set_id"){
	
		console.log("New Device - Logging Now");
		this.logDevice(message,connection,this);
	
	}else{
		console.log("Size of device_id: "+message.device_id.length);
		
		if(message.device_id.length == 24){
			var obj = {'_id': new BSON.ObjectID(message.device_id)};
			var _db = this;
			//find device by id
			devicesDb.findOne(obj,function(err,result){
				
				if(!result){
					console.log("[Device Registration Failed]");
					_db.logDevice(message,connection,_db);
					
				}else{

					console.log("[Device Already Registered]");

					// Update plant type in db if what was sent in register is different than what we have
					var plant_slug = _db.determineSlug(message.plant_type);
					if (plant_slug != result.plant_type) {
						result.plant_type = plant_slug;
						_db.updateDocument(devicesDb, message.device_id, { $set: { plant_type: plant_slug }});
					}

					result.connection_id = connection.id;
					result.device_id = message.device_id;

					connection.plant_type = message.plant_type;		
					connection.device_id = message.device_id;
					connection.plant_slug = plant_slug;
					_db.setActive(connection, true);
					//assignPlantData(result,connection);

					// Emit connection/register events
					_db.browserIO.emit('device_conn', result);
					connection.socket.emit('register', result);

					// Find settings and emit them for connected browsers
					_db.settingsForDevice(connection.id, connection.device_id, function(result) {
						connection.socket.emit('settings', result);
						_db.browserIO.emit('settings', result);
					});

					//connection.socket.send(JSON.stringify(res));	
				}
				
			});
			
		}else{
			
			this.logDevice(message,connection,this);

		}
		//check plants and register them
		
	}
}

DB.prototype.setActive = function(connection,state){
	var json = { $set:{active:state} };
	this.updateDocument(devicesDb,connection.device_id, json);
}

DB.prototype.determineSlug = function(plant_type) {

	var plant_slug;

	switch (plant_type) {
		case 'Tomatoes':
			plant_slug = 'tomatoes'; break;
		case 'Cherry Tomatoes':
			plant_slug = 'cherry_tomatoes'; break;
		case 'Orange Carrots':
			plant_slug = 'orange_carrots'; break;
		case 'Purple Carrots':
			plant_slug = 'purple_carrots'; break;
		case 'Celery':
			plant_slug = 'celery'; break;
		case 'Beets':
			plant_slug = 'beets'; break;
		case 'Bell Peppers':
			plant_slug = 'peppers'; break;

		default:
			plant_slug = plant_type; break;
	}

	return plant_slug;

	// if(connection.plant_type == "Cherry Tomatoes" || connection.plant_type == "Tomatoes"){
	//	plant_slug = 'tomatoes';
	// }else if(plant_type == "Orange Carrots" || plant_type == "Purple Carrots"){
		// plant_slug = 'carrots';
}

// Can probably deprecate this now. Not called from anywhere
DB.prototype.setSlug=function(connection){
	connection.plant_slug = this.determineSlug(connection.plant_type);
}

DB.prototype.logDevice = function(message,connection,_db){

	var plant_slug = this.determineSlug(message.plant_type);
	var obj = { date: new Date(), plants: [], plant_type: plant_slug, active: true, state: {touch:'born', moisture: 'born'} };

	devicesDb.insert(obj, {safe:true}, function(err,doc){
		if(err) console.error(err); //throw err;
		
		connection.device_id = doc[0]._id;
		connection.plant_type = message.plant_type;
		connection.plant_slug = plant_slug;
		
		console.log('Created Record: '+connection.device_id);
		console.log("plants.length: "+message.num_plants);
		for(var i = 0; i<message.num_plants; i++) _db.createPlant(message,connection,i,_db);
		
		_db.createSettings(message,connection,_db,message.num_plants);
		
		// var res = { device_id: connection.device_id, connection_id: connection.id };

		obj.connection_id = connection.id;
		obj.device_id = connection.device_id;

		//connection.socket.send(JSON.stringify(res));
		connection.socket.emit('register', obj);
		_db.browserIO.emit('device_conn', obj);
	});	
}

DB.prototype.createPlant = function(message,connection,plant_index,_db){
	//blocking or non-blocking function?
	
	//console.log("plant: "+ JSON.stringify(plant));
	console.log('[CREATING PLANT]');
	var obj = {created: new Date(), device_id:connection.device_id, index: plant_index, type: message.plant_type, state: "born", touch: 0 };
	plantsDb.insert(obj,{safe:true},function(err,doc){
		  if(err) console.error(err); //throw err;

		   var json={ $push: { plants: { index:doc[0].index, _id:doc[0]._id } } };
		   _db.updateDocument(devicesDb,connection.device_id, json);
		   
	});
}

DB.prototype.createSettings = function(message,connection,_db,num_plants){

	var cap_thresh = [];
	for (var i = 0; i < num_plants; i++) {
		cap_thresh[i] = { plant_index: i, value: 1500 };
	}

	var obj = {
		device_id: connection.device_id,
		humidity: { active: false, low: 10, high:60},
		temp: {active: false, low: 15, high:35},
		moisture: {active: true, low:0, high:100},
		light: {active: false, low:400, high: 1000},
		touch: {active: true, low:10, high: 30, window: 1},
		range: {active: false, low: 50},
		cap_thresh: cap_thresh
	};
	
	settingsDb.insert(obj,{safe:true},function(err){
		if(err) console.error(err);

		connection.socket.emit('settings', obj);
		_db.browserIO.emit('settings', obj);
	});
	
}


/* ******************************************************************************************* */
/* ******************************************************************************************* */

DB.prototype.routeUpdate = function(message,connection){
	
	if(message.device_id != connection.device_id) { 
		console.error('[UPDATE ERROR] '.error+"\t\t message.device_id=%s connection.device_id=%s".data, message.device_id, connection.device_id );/*error error;*/ 
	}else{

		var obj = {
			device_id: new BSON.ObjectID( String(message.device_id) ),
			timestamp: new Date(),
			state: message.state,
			data: message.data,
			plant_type: message.plant_type
		}

		dataDb.insert(obj, {safe:true}, function(err,doc){
			if(err) console.error(err);//throw err;	
		});

		devicesDb.update({ _id: obj.device_id }, { $set: { state: obj.state }}, function(err) {
			if (err) console.error(err);
		});

		// console.log("GOT MESSAGE.DATA :: "+JSON.stringify(message.data));
			
		// var obj = {	'device_id': new BSON.ObjectID( String(message.device_id)), 
		// 			'moisture':message.data.moisture, 
		// 			'temp': message.data.temp, 
		// 			'humidity':message.data.humidity, 
		// 			'light': message.data.light,
		// 			'timestamp': new Date()
		// 		};
				
		// var minutes = 15; //may need to be reset
		// var time = new Date();
		// time.setMinutes( time.getMinutes()-minutes );		
		
		// var _db = this;
		
		// dataDb.insert(obj, {safe:true}, function(err,doc){
		// 	if(err) console.error(err);//throw err;
			
		// 	//check the last values and determine mood of plant 
		// 	//update plants/device if necessary
		// 	var recent = {
		// 					device_id : obj.device_id,
		// 					timestamp: { $gte: time }
		// 				  };
						  
		// 	dataDb.find(recent).toArray( function(err,res){
		// 		//console.log(res);
		// 		if(err) console.error(err);
		// 		//console.log(res.length);
		// 		_db.calcDeviceMood(message,connection,res,_db);
			
		// 	});
			
		
		// });	
	}
}


/* ******************************************************************************************* */
/* ******************************************************************************************* */

DB.prototype.routeTouch = function(message,connection){
	
	//May need to create dual index of device_id & plant index
	var obj = {device_id: new BSON.ObjectID(String(message.device_id)), index: message.plant_index };
	//console.log("[PLANT TOUCH] ".info +JSON.stringify(obj));

	// increment lifetime touch count for plant in db.plants
	var json = { $inc: {touch: 1}, $set: {state: message.state} };
	plantsDb.update(obj, json,function(err){
		if(err) console.error(err);//throw err; 
	});	
	
	// set up object to insert into db.touches
	var touchObj = {
		device_id: new BSON.ObjectID(String(message.device_id)),
		timestamp: new Date(),
		index: message.plant_index,
		cap_val: message.cap_val,
		count: message.count, 
		state: message.state
	};
	
	// console.log(obj.timestamp);	
	// var _db = this;

	// console.log(time);
	touchesDb.insert(touchObj, function(err){
		if(err) console.error(err) //throw err;
		
		// DEPRECATED. THIS GETS CALCULATED ON THE TABLETS
		// _db.checkPlantTouches(obj,connection,true,_db);
		
	});

}


/* ******************************************************************************************* */
/* ******************************************************************************************* */

DB.prototype.updateDocument = function(collection,id,json){

	console.log("[Updating Document] Collection: "+collection.collectionName+" id: "+id);
	var obj = { _id : new BSON.ObjectID( String(id) ) };
	collection.update(obj,json,function(err){
		if(err) console.error(err); //throw err;
	});

//no upsert 
}


/* ******************************************************************************************* */
/* Routines watering the garden, logging pump runs, etc. 									   */
/* ******************************************************************************************* */

DB.prototype.calculateGardenWaterNeeds = function(callback) {

	// Amount of time to increase the sprinkler duration length for different moisture moods
	var durations = {
		low: 5,
		medium: 2,
		high: 0
	}
	var duration = 0;

	// console.log('calculateGardenWaterNeeds: checking db');
	devicesDb.find({ active: true }).toArray( function(err,res) {
		if (err) console.error(err);

		// console.log('calculateGardenWaterNeeds inside active device query');
		
		for (var i = 0; i < res.length; i++) {
			// console.log('calculateGardenWaterNeeds: '+i+': '+JSON.stringify(res[i]));
			if (!res[i].state.moisture == 'born') continue;

			// Increase amount according to above map
			duration += durations[res[i].state.moisture];
		}

		// Pass result to the callback
		// console.log('calculateGardenWaterNeeds: returning '+duration);
		callback(duration);
	});
}

DB.prototype.logPump = function(duration) {
	var obj = { duration: duration, timestamp: new Date() };
	pumpDb.insert(obj, function(err){
		if(err) console.error(err) //throw err;
	});
}

DB.prototype.neededTimeForPriming = function(callback) {

	// check if the pump was run within the last x minutes
	// pass the callback either an amout of added time needed for priming, or 0

	var minutes = 10;
	var addedTime = 3;

	var time = new Date();
	time.setMinutes( time.getMinutes() - minutes );		

	var recent = {
		timestamp: { $gte: time }
	};
				  
	pumpDb.find(recent).toArray( function(err,res){
		if(err) console.error(err);

		if (res.length) {
			console.log('[PUMP] Pump ran within last '+minutes+' minutes, no priming needed.');
			callback(0);
		} else {
			console.log('[PUMP] Pump  NOT ran within last '+minutes+' minutes, adding '+addedTime+' seconds.');
			callback(addedTime);
		}

	});

}

var dialogue = {
    "garden": {
        "touchRequest": {
            "stage_copy": [
                "Get in touch with nature. Literally! Go ahead and touch the plants",
                "Greetings! Come on over, the veggies would love to meet you.",
                "Hi there! See what happens when you pet these plants…",
                "You seem like a veggie lover. Wanna hang out with these plants?",
                "Did you know these plants are extremely ticklish? Try it and see!",
                "Help these plants grow big and tall: give 'em a pinch to grow an inch!",

            ],
            "audio": [
                "large_tomatoes_proximity_1.mp3",
                "large_tomatoes_proximity_2.mp3",
                "large_tomatoes_proximity_3.mp3",
                "large_tomatoes_proximity_4.mp3",
                "large_tomatoes_proximity_5.mp3"
            ]
        },
        "touchResponseGood": {
            "stage_copy": [
                "Who knew that veggies could be such fun friends?",
                "Holy cow, you're like, the vegetable whisperer. Teach me your ways!",
                "Hey, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You're a great playmate. Hope you visit every day!",
                "Wow. Were you a vegetable in your past life?",
            ],
            "audio": [
                "large_tomatoes_happy_1.mp3",
                "large_tomatoes_happy_2.mp3",
                "large_tomatoes_happy_3.mp3",
                "large_tomatoes_happy_4.mp3",
                "large_tomatoes_happy_5.mp3",
                "large_tomatoes_happy_6.mp3",
                "large_tomatoes_happy_7.mp3",
                "large_tomatoes_happy_8.mp3",
                "large_tomatoes_happy_9.mp3"
            ]
        },
        "touchResponseBad": {
            "stage_copy": [
                "Why not spread the love around a bit?",
                "You guys are getting along great. The other veggies would love to meet you too!",
                "It's time for the plants to get their beauty rest. Come back soon!",
                "All of the hustle and bustle can be overwhelming for plants. They need to relax for a bit.",
            ],
            "audio": [
                "large_tomatoes_worked_up_1.mp3",
                "large_tomatoes_worked_up_2.mp3",
                "large_tomatoes_worked_up_3.mp3"
            ]
        },
        "waterRequest": {
            "stage_copy": [
                "Sounds like these plants need some water, can you help?",
                "Mind watering these plants? They'd do it themselves, but they don't have arms.",
                "If it gets any drier, these plants are going to become tumbleweeds!",
                "These plants sure look dry! Can you send some water their way?",
            ],
            "audio": [
                "large_tomatoes_dry_1.mp3",
                "large_tomatoes_dry_2.mp3",
                "large_tomatoes_dry_3.mp3",
                "large_tomatoes_dry_4.mp3"
            ]
        },
        "waterResponseGood": {
            "stage_copy": [
                "The veggies sure look refreshed! Way to beat the heat.",
                "The plants are super appreciative. If they could send you a drink in return, they would!",
            ],
            "audio": []
        },
        "waterResponseBad": {
            "stage_copy": [
                "Being TOO generous with the water can drown the tomatoes. Let's give 'em a break, shall we?",
                "Swimming is a fun summer activity for people, not tomatoes! Can you ease up on the water please?",
                "These tomatoes are getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Tomatoes love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
                "The tomatoes need time to drink up this water before they get more. Check in on them soon!",
                "Holy H2O! That's a lot of water. The plants probably need a break now…",
                "Water is awesome but we don't want the tomatoes to float away… how about petting them instead?",
                "Oh dear, these plants are drenched! Any more water would be too much of a good thing.",
                "If the plants keep getting this much water, they're gonna need an umbrella!"
            ],
            "audio": []
        }
    },
    "large_tomatoes": {
        "touchRequest": {
            "stage_copy": [
                "Get in touch with nature. Literally! Go ahead and touch the tomatoes",
                "Greetings! Come on over, the tomatoes would love to meet you.",
                "Hi there! See what happens when you pet these tomatoes…",
                "You seem like a veggie lover. Wanna hang out with these tomatoes?",
                "Did you know that tomatoes are extremely ticklish? Try it and see!",
                "Help these plants grow big and tall: give 'em a pinch to grow an inch!",
                "This tomato has an itch it can't scratch. Can you help out?",
                "Help wake these tomatoes up from their nap. Give 'em a poke!",
                "Don't be a stranger – greet these tomatoes with a tiny handshake!",
                "You’d get along with these tomatoes. Come on over and meet 'em!",
                "These tomatoes would love to spend some time with you!",
                "Plant your feet right here and get to know these amazing tomatoes.",
                "They grow in bunches, but tomatoes still get lonely. Come say hi!"
            ],
            "audio": [
                "large_tomatoes_proximity_1.mp3",
                "large_tomatoes_proximity_2.mp3",
                "large_tomatoes_proximity_3.mp3",
                "large_tomatoes_proximity_4.mp3",
                "large_tomatoes_proximity_5.mp3"
            ]
        },
        "touchResponseGood": {
            "stage_copy": [
                "Who knew that veggies could be such fun friends?",
                "Holy cow, you're like, the tomato whisperer. Teach me your ways!",
                "Wow. Were you a tomato in your past life?",
                "You sure know what tomatoes like! Thank goodness you're here.",
                "The tomatoes are really digging all this attention. Thanks!",
                "Fun, isn't it? Who knew tomatoes were so a-peel-ing?",
                "You're a great playmate. Hope you visit every day!",
                "Hey, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You're making the tomatoes so happy! Great job!",
                "You must have a green thumb; the veggies are loving you!",
                "You say tomato, I say tomato. You say delicious, I say delicious.",
                "Did you know that the tomato's official name is Solanum lycopersicum. That's Latin for… well, tomato?",
                "The first European explorers of the Americas actually mistook the tomato for a new breed of eggplant.",
                "Tomatoes are very friendly plants. They help carrots, asparagus, and a variety of herbs grow.",
                "Tomatoes are a great source of dietary fiber, vitamins, and antioxidants. Oh tomato, what can’t you do?"
            ],
            "audio": [
                "large_tomatoes_happy_1.mp3",
                "large_tomatoes_happy_2.mp3",
                "large_tomatoes_happy_3.mp3",
                "large_tomatoes_happy_4.mp3",
                "large_tomatoes_happy_5.mp3",
                "large_tomatoes_happy_6.mp3",
                "large_tomatoes_happy_7.mp3",
                "large_tomatoes_happy_8.mp3",
                "large_tomatoes_happy_9.mp3"
            ]
        },
        "touchResponseBad": {
            "stage_copy": [
                "Careful, if the tomatoes get too worked-up they'll fall off their vines!",
                "Why not spread the love around a bit?",
                "It's time for the tomatoes to get their beauty rest. Come back soon!",
                "The tomatoes need to focus on growing right now. Can you play with the other veggies?",
                "Tomatoes love attention, but so do the rest of the veggies! Don't ignore the carrots.",
                "You guys are getting along great. The other veggies would love to meet you too!",
                "All of the hustle and bustle can be overwhelming for tomatoes. They need to relax for a bit.",
                "Sounds like these tomatoes need to calm down a little. Can you give the other veggies some attention?",
                "The tomatoes are getting tuckered out. Want to play with some other veggies?",
                "Looks like the tomatoes need to mellow out a little. Can you go say hi to other veggies in the garden?",
                "Any more action and these tomatoes are going to turn into sauce!"
            ],
            "audio": [
                "large_tomatoes_worked_up_1.mp3",
                "large_tomatoes_worked_up_2.mp3",
                "large_tomatoes_worked_up_3.mp3"
            ]
        },
        "waterRequest": {
            "stage_copy": [
                "Sounds like these tomatoes need some water, can you help?",
                "Mind watering these tomatoes? They'd do it themselves, but they don't have arms.",
                "If it gets any drier, these tomatoes are going to become tumbleweeds!",
                "These tomato plants sure look dry! Can you send some water their way?",
                "Boy, are these tomatoes parched. Come quench their thirst!",
                "These tomatoes could use a shower. Trust me, I'm standing right next to them!",
                "Show the tomatoes you care – give them some good ol' H2O.",
                "Tomatoes need water to grow. Can you help them out on this hot day?",
                "Sun-dried tomatoes are great in a salad but not in a garden… Send them some water!"
            ],
            "audio": [
                "large_tomatoes_dry_1.mp3",
                "large_tomatoes_dry_2.mp3",
                "large_tomatoes_dry_3.mp3",
                "large_tomatoes_dry_4.mp3"
            ]
        },
        "waterResponseGood": {
            "stage_copy": [
                "The tomatoes sure look refreshed! Way to beat the heat.",
                "The tomatoes are super appreciative. If they could send you a drink in return, they would!",
                "Thanks for helping the tomatoes stay juicy!",
                "Look at those water works! Nice job!",
                "The tomatoes love you for giving them a shower. They were really thirsty until you came along!",
                "Thanks! Nothing beats a cool splash of water on a hot summer day.",
                "Excellent water-wielding skills. I knew you could do it!",
                "Great watering job, you must have done this before!",
                "Thanks for keeping the tomatoes hydrated! You are a big help!"
            ],
            "audio": []
        },
        "waterResponseBad": {
            "stage_copy": [
                "Being TOO generous with the water can drown the tomatoes. Let's give 'em a break, shall we?",
                "Swimming is a fun summer activity for people, not tomatoes! Can you ease up on the water please?",
                "These tomatoes are getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Tomatoes love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
                "The tomatoes need time to drink up this water before they get more. Check in on them soon!",
                "Holy H2O! That's a lot of water. The tomatoes probably need a break now…",
                "Water is awesome but we don't want the tomatoes to float away… how about petting them instead?",
                "Oh dear, these tomatoes are drenched! Any more water would be too much of a good thing.",
                "If the tomatoes keep getting this much water, they're gonna need an umbrella!"
            ],
            "audio": []
        }
    },
    "cherry_tomatoes": {
        "touchRequest": {
            "stage_copy": [
                "Get in touch with nature. Literally! Go ahead and touch the tomatoes",
                "Greetings! Come on over, the tomatoes would love to meet you.",
                "Hi there! See what happens when you pet these tomatoes…",
                "You seem like a veggie lover. Wanna hang out with these tomatoes?",
                "Did you know that tomatoes are extremely ticklish? Try it and see!",
                "Help these plants grow big and tall: give 'em a pinch to grow an inch!",
                "This tomato has an itch it can't scratch. Can you help out?",
                "Help wake these tomatoes up from their nap. Give 'em a poke!",
                "Don't be a stranger – greet these tomatoes with a tiny handshake!",
                "You’d get along with these tomatoes. Come on over and meet 'em!",
                "These tomatoes would love to spend some time with you!",
                "Plant your feet right here and get to know these amazing tomatoes.",
                "They grow in bunches, but tomatoes still get lonely. Come say hi!"
            ],
            "audio": [
                "cherry_tomatoes_proximity_1.mp3",
                "cherry_tomatoes_proximity_2.mp3",
                "cherry_tomatoes_proximity_3.mp3",
                "cherry_tomatoes_proximity_4.mp3",
                "cherry_tomatoes_proximity_5.mp3"
            ]
        },
        "touchResponseGood": {
            "stage_copy": [
                "Who knew that veggies could be such fun friends?",
                "Holy cow, you're like, the tomato whisperer. Teach me your ways!",
                "Wow. Were you a tomato in your past life?",
                "You sure know what tomatoes like! Thank goodness you're here.",
                "The tomatoes are really digging all this attention. Thanks!",
                "Fun, isn't it? Who knew tomatoes were so a-peel-ing?",
                "You're a great playmate. Hope you visit every day!",
                "Hey, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You're making the tomatoes so happy! Great job!",
                "You must have a green thumb; the veggies are loving you!",
                "You say tomato, I say tomato. You say delicious, I say delicious.",
                "Did you know that the tomato's official name is Solanum lycopersicum. That's Latin for… well, tomato?",
                "The first European explorers of the Americas actually mistook the tomato for a new breed of eggplant.",
                "Tomatoes are very friendly plants. They help carrots, asparagus, and a variety of herbs grow.",
                "Tomatoes are a great source of dietary fiber, vitamins, and antioxidants. Oh tomato, what can’t you do?"
            ],
            "audio": [
                "cherry_tomatoes_happy_1.mp3",
                "cherry_tomatoes_happy_2.mp3",
                "cherry_tomatoes_happy_3.mp3",
                "cherry_tomatoes_happy_4.mp3",
                "cherry_tomatoes_happy_5.mp3",
                "cherry_tomatoes_happy_6.mp3",
                "cherry_tomatoes_happy_7.mp3",
                "cherry_tomatoes_happy_8.mp3",
                "cherry_tomatoes_happy_9.mp3"
            ]
        },
        "touchResponseBad": {
            "stage_copy": [
                "Careful, if the tomatoes get too worked-up they'll fall off their vines!",
                "Why not spread the love around a bit?",
                "It's time for the tomatoes to get their beauty rest. Come back soon!",
                "The tomatoes need to focus on growing right now. Can you play with the other veggies?",
                "Tomatoes love attention, but so do the rest of the veggies! Don't ignore the carrots.",
                "You guys are getting along great. The other veggies would love to meet you too!",
                "All of the hustle and bustle can be overwhelming for tomatoes. They need to relax for a bit.",
                "Sounds like these tomatoes need to calm down a little. Can you give the other veggies some attention?",
                "The tomatoes are getting tuckered out. Want to play with some other veggies?",
                "Looks like the tomatoes need to mellow out a little. Can you go say hi to other veggies in the garden?",
                "Any more action and these tomatoes are going to turn into sauce!"
            ],
            "audio": [
                "cherry_tomatoes_worked_up_1.mp3",
                "cherry_tomatoes_worked_up_2.mp3",
                "cherry_tomatoes_worked_up_3.mp3",
                "cherry_tomatoes_worked_up_4.mp3"
            ]
        },
        "waterRequest": {
            "stage_copy": [
                "Sounds like these tomatoes need some water, can you help?",
                "Mind watering these tomatoes? They'd do it themselves, but they don't have arms.",
                "If it gets any drier, these tomatoes are going to become tumbleweeds!",
                "These tomato plants sure look dry! Can you send some water their way?",
                "Boy, are these tomatoes parched. Come quench their thirst!",
                "These tomatoes could use a shower. Trust me, I'm standing right next to them!",
                "Show the tomatoes you care – give them some good ol' H2O.",
                "Tomatoes need water to grow. Can you help them out on this hot day?",
                "Sun-dried tomatoes are great in a salad but not in a garden… Send them some water!"
            ],
            "audio": [
                "cherry_tomatoes_dry_1.mp3",
                "cherry_tomatoes_dry_2.mp3",
                "cherry_tomatoes_dry_3.mp3",
                "cherry_tomatoes_dry_4.mp3",
                "cherry_tomatoes_dry_5.mp3",
                "cherry_tomatoes_dry_6.mp3",
                "cherry_tomatoes_dry_7.mp3"
            ]
        },
        "waterResponseGood": {
            "stage_copy": [
                "The tomatoes sure look refreshed! Way to beat the heat.",
                "The tomatoes are super appreciative. If they could send you a drink in return, they would!",
                "Thanks for helping the tomatoes stay juicy!",
                "Look at those water works! Nice job!",
                "The tomatoes love you for giving them a shower. They were really thirsty until you came along!",
                "Thanks! Nothing beats a cool splash of water on a hot summer day.",
                "Excellent water-wielding skills. I knew you could do it!",
                "Great watering job, you must have done this before!",
                "Thanks for keeping the tomatoes hydrated! You are a big help!"
            ],
            "audio": []
        },
        "waterResponseBad": {
            "stage_copy": [
                "Being TOO generous with the water can drown the tomatoes. Let's give 'em a break, shall we?",
                "Swimming is a fun summer activity for people, not tomatoes! Can you ease up on the water please?",
                "These tomatoes are getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Tomatoes love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
                "The tomatoes need time to drink up this water before they get more. Check in on them soon!",
                "Holy H2O! That's a lot of water. The tomatoes probably need a break now…",
                "Water is awesome but we don't want the tomatoes to float away… how about petting them instead?",
                "Oh dear, these tomatoes are drenched! Any more water would be too much of a good thing.",
                "If the tomatoes keep getting this much water, they're gonna need an umbrella!"
            ],
            "audio": []
        }
    },
    "peppers": {
        "touchRequest": {
            "stage_copy": [
                "Get in touch with nature. Literally! Go ahead and touch a pepper.",
                "Greetings! Come on over, the peppers would love to meet you.",
                "Hi there! See what happens when you pet these peppers…",
                "You seem like a veggie lover. Wanna hang out with these peppers?",
                "Did you know that peppers are extremely ticklish? Try it and see!",
                "These peppers are working hard to get tangy. Give 'em a high five for their efforts!",
                "This pepper has an itch it can't scratch. Can you help out?",
                "Help wake these peppers up from their nap. Give 'em a poke!",
                "Don't be a stranger – greet these peppers with a tiny handshake!",
                "You’d get along with these peppers. Come on over and meet 'em!",
                "These peppers would love to spend some time with you!",
                "Plant your feet right here and get to know these delicious peppers.",
                "These peppers feel hollow on the inside. Would you cheer them up?",
                "This pepper thinks it recognizes you… does it ring a bell?",
                "Been living in the city so long you forgot what nature feels like? Come get your fix!"
            ],
            "audio": [
                "peppers_proximity_1.mp3",
                "peppers_proximity_2.mp3",
                "peppers_proximity_3.mp3"
            ]
        },
        "touchResponseGood": {
            "stage_copy": [
                "Who knew that veggies could be such fun friends?",
                "Wow, you're like, the pepper whisperer. Teach me your ways!",
                "Holy cow. Were you a pepper in your past life?",
                "You sure know what peppers like! Thank goodness you're here.",
                "The peppers are really digging all this attention. Thanks!",
                "The peppers are loving that! Thanks for entertaining them.",
                "You're a great playmate. Hope you visit every day!",
                "Hey, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You're making the peppers so happy! Great job!",
                "You must have a green thumb; the veggies are loving you!"
            ],
            "audio": [
                "peppers_happy_1.mp3",
                "peppers_happy_2.mp3",
                "peppers_happy_3.mp3",
                "peppers_happy_4.mp3",
                "peppers_happy_5.mp3",
                "peppers_happy_6.mp3",
                "peppers_happy_7.mp3",
                "peppers_happy_8.mp3"
            ]
        },
        "touchResponseBad": {
            "stage_copy": [
                "Careful, if the peppers get too worked-up they'll fall off their vines!",
                "Why not spread the love around a bit?",
                "It's time for the peppers to get their beauty rest. Come back soon!",
                "The peppers need to focus on growing right now. Can you play with the other veggies?",
                "Peppers love attention, but so do the rest of the veggies! Don't ignore the peppers.",
                "You guys are getting along great. The other veggies would love to meet you too!",
                "All of the hustle and bustle can be overwhelming for peppers. They need to relax for a bit.",
                "Sounds like these peppers need to calm down a little. Can you give the other veggies some attention?",
                "The peppers are getting tuckered out. Want to play with some other veggies?",
                "Looks like the peppers need to mellow out a bit. Can you go say hi to other veggies in the garden?",
                "Any more action and these peppers are going to turn into paste!"
            ],
            "audio": [
                "peppers_worked_up_1.mp3",
                "peppers_worked_up_2.mp3",
                "peppers_worked_up_3.mp3"
            ]
        },
        "waterRequest": {
            "stage_copy": [
                "Sounds like these peppers need some water, can you help?",
                "Mind watering these peppers? They'd do it themselves, but they don't have arms.",
                "If it gets any drier, these peppers are going to become parched!",
                "These peppers sure look dry! Can you send some water their way?",
                "Boy, are these peppers parched. Come quench their thirst!",
                "These peppers could use a shower. Trust me, I'm standing right next to them!",
                "Show the peppers you care – give them some good ol' H2O.",
                "Peppers need water to grow. Can you help them out on this hot day?",
                "Pepper flakes are great on pizza but not in a garden. Send 'em some water before they dry out!"
            ],
            "audio": [
                "peppers_dry_1.mp3",
                "peppers_dry_2.mp3"
            ]
        },
        "waterResponseGood": {
            "stage_copy": [
                "That drink really helped, the peppers feel better already!",
                "The peppers sure look refreshed! Way to beat the heat.",
                "The peppers are super appreciative. If they could send you a drink in return, they would!",
                "Thanks for helping the peppers stay crisp!",
                "Look at those water works! Nice job!",
                "The peppers love you for giving them a shower. They were really thirsty until you came along!",
                "Thanks! Nothing beats a cool splash of water on a hot summer day.",
                "Excellent water-wielding skills. I knew you could do it!",
                "Great watering job, you must have done this before!",
                "Thanks for keeping the peppers hydrated! You are a big help!"
            ],
            "audio": []
        },
        "waterResponseBad": {
            "stage_copy": [
                "Uh oh, the peppers have had too much water. Better let their soil dry out a bit.",
                "Being TOO generous with the water can drown the peppers. Let's give 'em a break, shall we?",
                "Swimming is a fun summer activity for people, not peppers! Can you ease up on the water please?",
                "These peppers are getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Peppers love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
                "The peppers need time to drink up this water before they get more. Check in on them soon!",
                "Holy H2O! That's a lot of water. The peppers probably need a break now…",
                "Water is awesome but we don't want the peppers to float away… how about petting them instead?",
                "Oh dear, these peppers are drenched! Any more water would be too much of a good thing.",
                "If the peppers keep getting this much water, they're gonna need an umbrella!",
                "Water droplets look neat on a pepper's waxy peel, but too much and these guy will be in trouble…"
            ],
            "audio": []
        }
    },
    "celery": {
        "touchRequest": {
            "stage_copy": [
                "Get in touch with nature. Literally! Go ahead and touch some celery.",
                "Greetings! Come on over, the celery would love to meet you.",
                "Hi there! See what happens when you pet this celery…",
                "You seem like a veggie lover. Wanna hang out with this celery?",
                "Did you know that celery stalks are extremely ticklish? Try it and see!",
                "This celery is working hard to get extra crunchy. Give 'em a high five for their efforts!",
                "This celery has an itch it can't scratch. Can you help out?",
                "Help wake this celery up from its nap. Give it a poke!",
                "Don't be a stranger – greet this celery with a tiny handshake!",
                "You’d get along with this celery. Come on over!",
                "This celery would love to spend some time with you!",
                "Plant your feet right here and get to know this nutritious celery.",
                "This celery wants attention too! It's getting green with envy."
            ],
            "audio": [
                "celery_proximity_1.mp3",
                "celery_proximity_2.mp3",
                "celery_proximity_3.mp3",
                "celery_proximity_4.mp3",
                "celery_proximity_5.mp3",
                "celery_proximity_6.mp3"
            ]
        },
        "touchResponseGood": {
            "stage_copy": [
                "Who knew that veggies could be such fun friends?",
                "Oh my, you're like, the celery whisperer. Teach me your ways!",
                "Whoa. Were you a celery in your past life?",
                "You sure know what celery likes! Thank goodness you're here.",
                "The celery is really digging all this attention. Thanks!",
                "The celery is loving that! Thanks for entertaining!",
                "You're a great playmate. Hope you visit every day!",
                "Hey, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You're making the celery so happy! Great job!",
                "You must have a green thumb; the veggies are loving you!"
            ],
            "audio": [
                "celery_happy_1.mp3",
                "celery_happy_2.mp3",
                "celery_happy_3.mp3",
                "celery_happy_4.mp3",
                "celery_happy_5.mp3",
                "celery_happy_6.mp3",
                "celery_happy_7.mp3",
                "celery_happy_8.mp3",
                "celery_happy_9.mp3",
                "celery_happy_10.mp3",
                "celery_happy_11.mp3",
                "celery_happy_12.mp3",
                "celery_happy_13.mp3",
                "celery_happy_14.mp3",
                "celery_happy_15.mp3",
                "celery_happy_16.mp3",
                "celery_happy_17.mp3",
                "celery_happy_18.mp3",
                "celery_happy_19.mp3",
                "celery_happy_20.mp3",
                "celery_happy_21.mp3",
                "celery_happy_22.mp3",
                "celery_happy_23.mp3",
                "celery_happy_24.mp3"
            ]
        },
        "touchResponseBad": {
            "stage_copy": [
                "Careful, if the celery gets too worked-up they'll snap a stalk!",
                "Why not spread the love around a bit?",
                "It's time for the celery to get their beauty rest. Come back soon!",
                "The celery needs to focus on growing right now. Can you play with the other veggies?",
                "Celery love attention, but so do the rest of the veggies! Don't ignore the peppers.",
                "The celery is getting tuckered out. Show some love to one of their neighbors",
                "You guys are getting along great. The other veggies would love to meet you too!",
                "All of the hustle and bustle can be overwhelming for peppers. They need to relax for a bit.",
                "Sounds like these peppers need to calm down a little. Can you give the other veggies some attention?",
                "The peppers are getting tuckered out. Want to play with some other veggies?",
                "Looks like the peppers need to mellow out a bit. Can you go say hi to other veggies in the garden?",
                "This celery is so excited it's gonna get its stalks all tangled up! Don't forget about the rest of the garden…",
                "Celery stick are good to eat, but these are still growing! Let them rest for a bit."
            ],
            "audio": [
                "celery_worked_up_1.mp3",
                "celery_worked_up_2.mp3",
                "celery_worked_up_3.mp3",
                "celery_worked_up_4.mp3",
                "celery_worked_up_5.mp3",
                "celery_worked_up_6.mp3",
                "celery_worked_up_7.mp3"
            ]
        },
        "waterRequest": {
            "stage_copy": [
                "Sounds like the celery needs some water, can you help?",
                "Mind watering this celery? They'd do it themselves, but they don't have arms.",
                "If it gets any drier, this celery is going to become parched!",
                "This celery sure looks dry! Can you send some water their way?",
                "Boy, are this celery is parched. Come quench their thirst @ThePowerGarden!",
                "These celery could use a shower. Trust me, I'm standing right next to them!",
                "Show the celery you care – give them some good ol' H2O.",
                "Celery needs water to grow. Can you help them out on this hot day?",
                "We're on the verge of some major wilt-age. Celery needs water pronto!"
            ],
            "audio": [
                "celery_dry_1.mp3",
                "celery_dry_2.mp3",
                "celery_dry_3.mp3",
                "celery_dry_4.mp3",
                "celery_dry_5.mp3",
                "celery_dry_6.mp3",
                "celery_dry_7.mp3",
                "celery_dry_8.mp3"
            ]
        },
        "waterResponseGood": {
            "stage_copy": [
                "That drink really helped, the celery feels better already!",
                "The celery sure looks refreshed! Way to beat the heat.",
                "The celery is super appreciative. If it could send you a drink in return, it would!",
                "Thanks for helping the celery stay crunchy!",
                "Look at those water works! Nice job!",
                "The celery loves you for giving it a shower. It was really thirsty until you came along!",
                "Thanks! Nothing beats a cool splash of water on a hot summer day.",
                "Excellent water-wielding skills. I knew you could do it!",
                "Great watering job, you must have done this before!",
                "Thanks for keeping the celery hydrated! You are a big help!",
                "Thanks for keeping the celery a lean, green crunchy machine!"
            ],
            "audio": []
        },
        "waterResponseBad": {
            "stage_copy": [
                "Uh oh, the celery has had too much water. Better let the soil dry out a bit.",
                "Being TOO generous with the water can drown the celery. Let's give it a break, shall we?",
                "Swimming is a fun summer activity for people, not celery! Can you ease up on the water please?",
                "This celery is getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Celery loves water, but too much of a good thing can be dangerous. Better cut it off before it starts swimming!",
                "The celery needs time to drink up this water before it gets more. Check in on it soon!",
                "Holy H2O! That's a lot of water. The celery probably needs a break now…",
                "Water is awesome but we don't want the celery to float away… how about petting it instead?",
                "Oh dear, this celery is drenched! Any more water would be too much of a good thing.",
                "If the celery keeps getting this much water, it's gonna need an umbrella!"
            ],
            "audio": []
        }
    },
    "orange_carrots": {
        "touchRequest": {
            "stage_copy": [
                "Get in touch with nature. Literally! Go ahead and touch those carrots.",
                "Greetings! Come on over, the carrots would love to meet you.",
                "Hi there! See what happens when you pet these carrots…",
                "You seem like a veggie lover. Wanna hang out with some carrots?",
                "Did you know that carrots are extremely ticklish? Try it and see!",
                "These carrots are working hard to get crunchy and sweet. Give 'em a high five for their efforts!",
                "These carrots have an itch they can't scratch. Can you help out?",
                "Help wake these carrots up from their nap. Give 'em a poke!",
                "Don't be a stranger – greet these carrots with a tiny handshake!",
                "You’d get along with these carrots. Come on over!",
                "These carrots would love to spend some time with you!",
                "Plant your feet right here and get to know these amazing carrots.",
                "Don't mistake a carrots underground hideout for shyness. They definitely want to play with you!",
                "Did you know that carrots contain iron and Vitamins A, B6, E, C and K (and more)? They're nature’s vitamin store."
            ],
            "audio": [
                "orange_carrots_proximity_1.mp3",
                "orange_carrots_proximity_2.mp3",
                "orange_carrots_proximity_3.mp3",
                "orange_carrots_proximity_4.mp3",
                "orange_carrots_proximity_5.mp3",
                "orange_carrots_proximity_6.mp3",
                "orange_carrots_proximity_7.mp3"
            ]
        },
        "touchResponseGood": {
            "stage_copy": [
                "Who knew that veggies could be such fun friends?",
                "Wow, you're like, the carrot whisperer. Teach me your ways!",
                "Holy Cow. Were you a carrot in your past life?",
                "You sure know what carrots likes! Thank goodness you're here.",
                "The carrots are really digging all this attention. Thanks!",
                "The carrots are loving that! Thanks for entertaining them!",
                "You're a great playmate. Hope you visit every day!",
                "Hey, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You're making the carrots so happy! Great job!",
                "You must have a green thumb, the veggies are loving you!",
                "Good thing you're not a rabbit. When they hang with carrots it never ends well."
            ],
            "audio": [
                "orange_carrots_happy_1.mp3",
                "orange_carrots_happy_2.mp3",
                "orange_carrots_happy_3.mp3",
                "orange_carrots_happy_4.mp3",
                "orange_carrots_happy_5.mp3",
                "orange_carrots_happy_6.mp3",
                "orange_carrots_happy_7.mp3",
                "orange_carrots_happy_8.mp3",
                "orange_carrots_happy_9.mp3",
                "orange_carrots_happy_10.mp3",
                "orange_carrots_happy_11.mp3",
                "orange_carrots_happy_12.mp3",
                "orange_carrots_happy_13.mp3",
                "orange_carrots_happy_14.mp3",
                "orange_carrots_happy_15.mp3",
                "orange_carrots_happy_16.mp3",
                "orange_carrots_happy_17.mp3",
                "orange_carrots_happy_18.mp3",
                "orange_carrots_happy_19.mp3",
                "orange_carrots_happy_20.mp3",
                "orange_carrots_happy_21.mp3",
                "orange_carrots_happy_22.mp3",
                "orange_carrots_happy_23.mp3",
                "orange_carrots_happy_24.mp3",
                "orange_carrots_happy_25.mp3",
                "orange_carrots_happy_26.mp3",
                "orange_carrots_happy_27.mp3",
                "orange_carrots_happy_28.mp3",
                "orange_carrots_happy_29.mp3",
                "orange_carrots_happy_30.mp3",
                "orange_carrots_happy_31.mp3",
                "orange_carrots_happy_32.mp3"
            ]
        },
        "touchResponseBad": {
            "stage_copy": [
                "Careful, if the carrots get too worked-up they'll dig themselves up!",
                "Why not spread the love around a bit?",
                "It's time for the carrots to get their beauty rest. Come back soon!",
                "The carrots need to focus on growing right now. Can you play with the other veggies?",
                "Carrots love attention, but so do the rest of the veggies! Don't ignore the beets.",
                "The carrots are getting tuckered out. Show some love to one of their neighbors",
                "You guys are getting along great. The other veggies would love to meet you too!",
                "All of the hustle and bustle can be overwhelming for carrots. They need to relax for a bit.",
                "Sounds like these carrots need to calm down a little. Can you give the other veggies some attention?",
                "The carrots are getting tuckered out. Want to play with some other veggies?",
                "Looks like the carrots need to mellow out a bit. Can you go say hi to other veggies in the garden?",
                "Too much attention and these carrots are gonna burrow on outta here.",
                "Celery stick are good to eat, but these are still growing! Let them rest for a bit."
            ],
            "audio": [
                "orange_carrots_worked_up_1.mp3",
                "orange_carrots_worked_up_2.mp3",
                "orange_carrots_worked_up_3.mp3",
                "orange_carrots_worked_up_4.mp3",
                "orange_carrots_worked_up_5.mp3",
                "orange_carrots_worked_up_6.mp3"
            ]
        },
        "waterRequest": {
            "stage_copy": [
                "Sounds like the carrots need some water, can you help?",
                "Mind watering these carrots? They'd do it themselves, but they don't have arms.",
                "If it gets any drier, these carrots are going to become parched!",
                "These carrots sure look dry! Can you send some water their way?",
                "Boy, are these carrots is parched. Come quench their thirst @ThePowerGarden!",
                "These carrots could use a shower. Trust me, I'm standing right next to them!",
                "Show the carrots you care – give them some good ol' H2O.",
                "Carrots need water to grow. Can you help them out on this hot day?",
                "If these dudes don't get some water, we're going to have a carrot riot on our hands! Help!"
            ],
            "audio": [
                "orange_carrots_dry_1.mp3",
                "orange_carrots_dry_2.mp3",
                "orange_carrots_dry_3.mp3",
                "orange_carrots_dry_4.mp3",
                "orange_carrots_dry_5.mp3",
                "orange_carrots_dry_6.mp3"
            ]
        },
        "waterResponseGood": {
            "stage_copy": [
                "That drink really helped, the carrots feel better already!",
                "The carrots sure look refreshed! Way to beat the heat.",
                "The carrots are super appreciative. If they could send you a drink in return, they would!",
                "Thanks for helping the carrots stay sweet!",
                "Look at those water works! Nice job!",
                "The carrots love you for giving them a shower. They were really thirsty until you came along!",
                "Thanks! Nothing beats a cool splash of water on a hot summer day.",
                "Excellent water-wielding skills. I knew you could do it!",
                "Great watering job, you must have done this before!",
                "Thanks for keeping the carrots hydrated! You are a big help!"
            ],
            "audio": []
        },
        "waterResponseBad": {
            "stage_copy": [
                "Uh oh, the carrots have had too much water. Better let their soil dry out a bit.",
                "Being TOO generous with the water can drown the carrots. Let's give 'em a break, shall we?",
                "Swimming is a fun summer activity for people, not carrots! Can you ease up on the water please?",
                "These carrots are getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Carrots love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
                "The carrots need time to drink up this water before they get more. Check in on them soon!",
                "Holy H2O! That's a lot of water. The carrots probably need a break now…",
                "Water is awesome but we don't want the carrots to float away… how about petting them instead?",
                "Oh dear, these carrots are drenched! Any more water would be too much of a good thing.",
                "If the carrots keep getting this much water, they're gonna need an umbrella!"
            ],
            "audio": []
        }
    },
    "purple_carrots": {
        "touchRequest": {
            "stage_copy": [
                "Get in touch with nature. Literally! Go ahead and touch those carrots.",
                "Greetings! Come on over, the carrots would love to meet you.",
                "Hi there! See what happens when you pet these carrots…",
                "You seem like a veggie lover. Wanna hang out with some carrots?",
                "Did you know that carrots are extremely ticklish? Try it and see!",
                "These carrots are working hard to get crunchy and sweet. Give 'em a high five for their efforts!",
                "These carrots have an itch they can't scratch. Can you help out?",
                "Help wake these carrots up from their nap. Give 'em a poke!",
                "Don't be a stranger – greet these carrots with a tiny handshake!",
                "You’d get along with these carrots. Come on over!",
                "These carrots would love to spend some time with you!",
                "Plant your feet right here and get to know these amazing carrots.",
                "Don't mistake a carrots underground hideout for shyness. They definitely want to play with you!",
                "Purple carrots are supercharged with antioxidants. Plus they’re purple, which is pretty cool all by itself."
            ],
            "audio": [
                "orange_carrots_proximity_1.mp3",
                "orange_carrots_proximity_2.mp3",
                "orange_carrots_proximity_3.mp3",
                "orange_carrots_proximity_4.mp3",
                "orange_carrots_proximity_5.mp3",
                "orange_carrots_proximity_6.mp3",
                "orange_carrots_proximity_7.mp3"
            ]
        },
        "touchResponseGood": {
            "stage_copy": [
                "Who knew that veggies could be such fun friends?",
                "Wow, you're like, the carrot whisperer. Teach me your ways!",
                "Holy Cow. Were you a carrot in your past life?",
                "You sure know what carrots likes! Thank goodness you're here.",
                "The carrots are really digging all this attention. Thanks!",
                "The carrots are loving that! Thanks for entertaining them!",
                "You're a great playmate. Hope you visit every day!",
                "Hey, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You're making the carrots so happy! Great job!",
                "You must have a green thumb, the veggies are loving you!",
                "Good thing you're not a rabbit. When they hang with carrots it never ends well."
            ],
            "audio": [
                "orange_carrots_happy_1.mp3",
                "orange_carrots_happy_2.mp3",
                "orange_carrots_happy_3.mp3",
                "orange_carrots_happy_4.mp3",
                "orange_carrots_happy_5.mp3",
                "orange_carrots_happy_6.mp3",
                "orange_carrots_happy_7.mp3",
                "orange_carrots_happy_8.mp3",
                "orange_carrots_happy_9.mp3",
                "orange_carrots_happy_10.mp3",
                "orange_carrots_happy_11.mp3",
                "orange_carrots_happy_12.mp3",
                "orange_carrots_happy_13.mp3",
                "orange_carrots_happy_14.mp3",
                "orange_carrots_happy_15.mp3",
                "orange_carrots_happy_16.mp3",
                "orange_carrots_happy_17.mp3",
                "orange_carrots_happy_18.mp3",
                "orange_carrots_happy_19.mp3",
                "orange_carrots_happy_20.mp3",
                "orange_carrots_happy_21.mp3",
                "orange_carrots_happy_22.mp3",
                "orange_carrots_happy_23.mp3",
                "orange_carrots_happy_24.mp3",
                "orange_carrots_happy_25.mp3",
                "orange_carrots_happy_26.mp3",
                "orange_carrots_happy_27.mp3",
                "orange_carrots_happy_28.mp3",
                "orange_carrots_happy_29.mp3",
                "orange_carrots_happy_30.mp3",
                "orange_carrots_happy_31.mp3",
                "orange_carrots_happy_32.mp3"
            ]
        },
        "touchResponseBad": {
            "stage_copy": [
                "Careful, if the carrots get too worked-up they'll dig themselves up!",
                "Why not spread the love around a bit?",
                "It's time for the carrots to get their beauty rest. Come back soon!",
                "The carrots need to focus on growing right now. Can you play with the other veggies?",
                "Carrots love attention, but so do the rest of the veggies! Don't ignore the beets.",
                "The carrots are getting tuckered out. Show some love to one of their neighbors",
                "You guys are getting along great. The other veggies would love to meet you too!",
                "All of the hustle and bustle can be overwhelming for carrots. They need to relax for a bit.",
                "Sounds like these carrots need to calm down a little. Can you give the other veggies some attention?",
                "The carrots are getting tuckered out. Want to play with some other veggies?",
                "Looks like the carrots need to mellow out a bit. Can you go say hi to other veggies in the garden?",
                "Too much attention and these carrots are gonna burrow on outta here.",
                "Celery stick are good to eat, but these are still growing! Let them rest for a bit."
            ],
            "audio": [
                "orange_carrots_worked_up_1.mp3",
                "orange_carrots_worked_up_2.mp3",
                "orange_carrots_worked_up_3.mp3",
                "orange_carrots_worked_up_4.mp3",
                "orange_carrots_worked_up_5.mp3",
                "orange_carrots_worked_up_6.mp3"
            ]
        },
        "waterRequest": {
            "stage_copy": [
                "Sounds like the carrots need some water, can you help?",
                "Mind watering these carrots? They'd do it themselves, but they don't have arms.",
                "If it gets any drier, these carrots are going to become parched!",
                "These carrots sure look dry! Can you send some water their way?",
                "Boy, are these carrots is parched. Come quench their thirst @ThePowerGarden!",
                "These carrots could use a shower. Trust me, I'm standing right next to them!",
                "Show the carrots you care – give them some good ol' H2O.",
                "Carrots need water to grow. Can you help them out on this hot day?",
                "If these dudes don't get some water, we're going to have a carrot riot on our hands! Help!"
            ],
            "audio": [
                "orange_carrots_dry_1.mp3",
                "orange_carrots_dry_2.mp3",
                "orange_carrots_dry_3.mp3",
                "orange_carrots_dry_4.mp3",
                "orange_carrots_dry_5.mp3",
                "orange_carrots_dry_6.mp3"
            ]
        },
        "waterResponseGood": {
            "stage_copy": [
                "That drink really helped, the carrots feel better already!",
                "The carrots sure look refreshed! Way to beat the heat.",
                "The carrots are super appreciative. If they could send you a drink in return, they would!",
                "Thanks for helping the carrots stay sweet!",
                "Look at those water works! Nice job!",
                "The carrots love you for giving them a shower. They were really thirsty until you came along!",
                "Thanks! Nothing beats a cool splash of water on a hot summer day.",
                "Excellent water-wielding skills. I knew you could do it!",
                "Great watering job, you must have done this before!",
                "Thanks for keeping the carrots hydrated! You are a big help!"
            ],
            "audio": []
        },
        "waterResponseBad": {
            "stage_copy": [
                "Uh oh, the carrots have had too much water. Better let their soil dry out a bit.",
                "Being TOO generous with the water can drown the carrots. Let's give 'em a break, shall we?",
                "Swimming is a fun summer activity for people, not carrots! Can you ease up on the water please?",
                "These carrots are getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Carrots love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
                "The carrots need time to drink up this water before they get more. Check in on them soon!",
                "Holy H2O! That's a lot of water. The carrots probably need a break now…",
                "Water is awesome but we don't want the carrots to float away… how about petting them instead?",
                "Oh dear, these carrots are drenched! Any more water would be too much of a good thing.",
                "If the carrots keep getting this much water, they're gonna need an umbrella!"
            ],
            "audio": []
        }
    },
    "beets": {
        "touchRequest": {
            "stage_copy": [
                "Get in touch with nature. Literally! Go ahead and touch those beet.",
                "Greetings! Come on over, the beets would love to meet you.",
                "Hi there! See what happens when you pet these beets…",
                "You seem like a veggie lover. Wanna hang out with some beets?",
                "Did you know that beets are extremely ticklish? Try it and see!",
                "These beets are working hard to get crunchy and sweet. Give 'em a high five for their efforts!",
                "These beets have an itch they can't scratch. Can you help out?",
                "Help wake these beets up from their nap. Give 'em a poke!",
                "Don't be a stranger – greet these beets with a tiny handshake!",
                "You’d get along with these beets. Come on over!",
                "These beets would love to spend some time with you!",
                "Plant your feet right here and get to know these amazing beets.",
                "Don't mistake a beets underground hideout for shyness. They definitely want to play with you!",
                "Purple beets are supercharged with antioxidants. Plus they’re purple, which is pretty cool all by itself."
            ],
            "audio": [
                "beets_proximity_1.mp3",
                "beets_proximity_2.mp3",
                "beets_proximity_3.mp3",
                "beets_proximity_4.mp3",
                "beets_proximity_5.mp3",
                "beets_proximity_6.mp3",
                "beets_proximity_7.mp3"
            ]
        },
        "touchResponseGood": {
            "stage_copy": [
                "Who knew that veggies could be such fun friends?",
                "Wow, you're like, the beet whisperer. Teach me your ways!",
                "Holy Cow. Were you a beet in your past life?",
                "You sure know what beets likes! Thank goodness you're here.",
                "The beets are really digging all this attention. Thanks!",
                "The beets are loving that! Thanks for entertaining them!",
                "You're a great playmate. Hope you visit every day!",
                "Hey, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You're making the beets so happy! Great job!",
                "You must have a green thumb, the veggies are loving you!",
                "Good thing you're not a rabbit. When they hang with beets it never ends well."
            ],
            "audio": [
                "beets_happy_1.mp3",
                "beets_happy_2.mp3",
                "beets_happy_3.mp3",
                "beets_happy_4.mp3",
                "beets_happy_5.mp3",
                "beets_happy_6.mp3",
                "beets_happy_7.mp3",
                "beets_happy_8.mp3",
                "beets_happy_9.mp3",
                "beets_happy_10.mp3",
                "beets_happy_11.mp3",
                "beets_happy_12.mp3",
                "beets_happy_13.mp3",
                "beets_happy_14.mp3",
                "beets_happy_15.mp3",
                "beets_happy_16.mp3",
                "beets_happy_17.mp3",
                "beets_happy_18.mp3",
                "beets_happy_19.mp3",
                "beets_happy_20.mp3",
                "beets_happy_21.mp3",
                "beets_happy_22.mp3",
                "beets_happy_23.mp3",
                "beets_happy_24.mp3",
                "beets_happy_25.mp3",
                "beets_happy_26.mp3",
                "beets_happy_27.mp3",
                "beets_happy_28.mp3",
                "beets_happy_29.mp3",
                "beets_happy_30.mp3",
                "beets_happy_31.mp3",
                "beets_happy_32.mp3"
            ]
        },
        "touchResponseBad": {
            "stage_copy": [
                "Careful, if the beets get too worked-up they'll dig themselves up!",
                "Why not spread the love around a bit?",
                "It's time for the beets to get their beauty rest. Come back soon!",
                "The beets need to focus on growing right now. Can you play with the other veggies?",
                "beets love attention, but so do the rest of the veggies! Don't ignore the beets.",
                "The beets are getting tuckered out. Show some love to one of their neighbors",
                "You guys are getting along great. The other veggies would love to meet you too!",
                "All of the hustle and bustle can be overwhelming for beets. They need to relax for a bit.",
                "Sounds like these beets need to calm down a little. Can you give the other veggies some attention?",
                "The beets are getting tuckered out. Want to play with some other veggies?",
                "Looks like the beets need to mellow out a bit. Can you go say hi to other veggies in the garden?",
                "Too much attention and these beets are gonna burrow on outta here.",
                "Celery stick are good to eat, but these are still growing! Let them rest for a bit."
            ],
            "audio": [
                "beets_worked_up_1.mp3",
                "beets_worked_up_2.mp3",
                "beets_worked_up_3.mp3",
                "beets_worked_up_4.mp3",
                "beets_worked_up_5.mp3",
                "beets_worked_up_6.mp3"
            ]
        },
        "waterRequest": {
            "stage_copy": [
                "Sounds like the beets need some water, can you help?",
                "Mind watering these beets? They'd do it themselves, but they don't have arms.",
                "If it gets any drier, these beets are going to become parched!",
                "These beets sure look dry! Can you send some water their way?",
                "Boy, are these beets is parched. Come quench their thirst @ThePowerGarden!",
                "These beets could use a shower. Trust me, I'm standing right next to them!",
                "Show the beets you care – give them some good ol' H2O.",
                "beets need water to grow. Can you help them out on this hot day?",
                "If these dudes don't get some water, we're going to have a beet riot on our hands! Help!"
            ],
            "audio": [
                "beets_dry_1.mp3",
                "beets_dry_2.mp3",
                "beets_dry_3.mp3",
                "beets_dry_4.mp3",
                "beets_dry_5.mp3",
                "beets_dry_6.mp3"
            ]
        },
        "waterResponseGood": {
            "stage_copy": [
                "That drink really helped, the beets feel better already!",
                "The beets sure look refreshed! Way to beat the heat.",
                "The beets are super appreciative. If they could send you a drink in return, they would!",
                "Thanks for helping the beets stay hearty!",
                "Look at those water works! Nice job!",
                "The beets love you for giving them a shower. They were really thirsty until you came along!",
                "Thanks! Nothing beats a cool splash of water on a hot summer day.",
                "Excellent water-wielding skills. I knew you could do it!",
                "Great watering job, you must have done this before!",
                "Thanks for keeping the beets hydrated! You are a big help!",
                "The beets sure look refreshed! Way to \"beet\" the heat. (Get it?)"
            ],
            "audio": []
        },
        "waterResponseBad": {
            "stage_copy": [
                "Uh oh, the beets have had too much water. Better let their soil dry out a bit.",
                "Being TOO generous with the water can drown the beets. Let's give 'em a break, shall we?",
                "Swimming is a fun summer activity for people, not beets! Can you ease up on the water please?",
                "These beets are getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Beets love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
                "The beets need time to drink up this water before they get more. Check in on them soon!",
                "Holy H2O! That's a lot of water. The beets probably need a break now…",
                "Water is awesome but we don't want the beets to float away… how about petting them instead?",
                "Oh dear, these beets are drenched! Any more water would be too much of a good thing.",
                "If the beets keep getting this much water, they're gonna need an umbrella!",
                "That's not how you make beet juice! Cut back on the water, will ya?"
            ],
            "audio": []
        }
    }
};



/* ******************************************************************************************* */
/* ******************************************************************************************* */

// Deprecated stuff for plant/device moods that is now all happening on tablets

/*
DB.prototype.calcDeviceMood = function(message,connection,results,_db){
	
		
				
		var obj = {	device_id: new BSON.ObjectID( String(message.device_id)) };
				   
		var avgResults = {};
		avgResults.temp =0;
		avgResults.moisture =0;
		avgResults.light = 0;
		avgResults.humidity=0;
		
		for(var i=0; i< results.length; i++){
			//console.log('Results :',results[i]);
			avgResults.temp += results[i].temp;
			avgResults.moisture += results[i].moisture;
			avgResults.light += results[i].light;
			avgResults.humidity += results[i].humidity;
			
		}
		//console.log(avgResults);
		avgResults.temp /= results.length;
		avgResults.moisture /= results.length;
		avgResults.light /= results.length;
		avgResults.humidity /= results.length;
		
		var mood ={};
		settingsDb.findOne(obj, function(err, res){
			if(err) console.error(err);
			//console.log('[FINDING SETTINGS]',res);		
			
			if(res.humidity.active==true){
				//does not affect mood
				if(avgResults.humidity>res.humidity.low && avgResults.humidity < res.humidity.high) mood['humidity']='content';
				else if(avgResults.humidity<res.humidity.low) mood.humidity='low';
				else if(avgResults.humidity>res.humidity.high) mood.humidity='high';
			}

			if(res.temp.active==true){
				//does not affect mood
				if(avgResults.temp>res.temp.low && avgResults.temp < res.temp.high) mood.temp='content';
				else if(avgResults.temp<res.temp.low) mood.temp='low';
				else if(avgResults.temp>res.temp.high) mood.temp='high';
				
			}
			if(res.light.active==true){
				//does not affect mood
				if(avgResults.light>res.light.low && avgResults.light < res.light.high) mood.light='content';
				else if(avgResults.light<res.light.low) mood.light='low';
				else if(avgResults.light>res.light.high) mood.light='high';
				
			}
			
			if(res.moisture.active==true){
				//affects mood
				if(avgResults.moisture>res.moisture.low && avgResults.moisture < res.moisture.high) mood.moisture='content';
				else if(avgResults.moisture<res.moisture.low) mood.moisture='low';
				else if(avgResults.moisture>res.moisture.high) mood.moisture='high';
				
			}				
			_db.processMood(message,connection,mood,_db);
			
			
		});
		
}
*/

/* ******************************************************************************************* */
/* ******************************************************************************************* */

// Deprecated
/*
DB.prototype.processMood = function(message,connection,mood,_db){
	
	//take in mood array
	//check touch values
		
	//head to look up table and discern
	//signal device about plants mood -
	var device = {	device_id: new BSON.ObjectID( String(message.device_id)) };
	//find device to check plants	
	plantsDb.find(device).toArray( function(err,result){
		//check each plants touch for most up to date results
		for(var i = 0; i< result.length; i++){
			var obj = {device_id: new BSON.ObjectID(String(message.device_id)), index: i };
			_db.checkPlantTouches(obj,connection,false,_db);
		}
		
		//count all plants values to discern device global touch mood
		plantsDb.find(device).toArray( function(err,result){
		
			var touchesMood={worked_up:0,lonely:0,content:0};
			
			for(var i = 0; i< result.length; i++){
				
				if(result[i].mood=='born'||result[i].mood=='lonely'){
					touchesMood.lonely +=1;
				}else if(result[i].mood=='worked_up'){
					touchesMood.worked_up +=1;
				}else if(result[i].mood=='content'){
					touchesMood.content+=1;
				}
			}
			
			if(touchesMood.worked_up > touchesMood.lonely && touchesMood.worked_up > touchesMood.content ){
				console.log("WORKED UP");
				mood.touches = 'high';

			}else if(touchesMood.lonely > touchesMood.worked_up && touchesMood.lonely > touchesMood.content){
				console.log("LONELY");
				mood.touches = 'low';

			}else if(touchesMood.content >touchesMood.worked_up && touchesMood.content > touchesMood.lonely ){
				console.log("CONTENT");
				mood.touches='content';
			}else if(touchesMood.worked_up>0 && (touchesMood.worked_up == touchesMood.content || touchesMood.worked_up == touchesMood.lonely)){
				console.log("WORKED UP");
				mood.touches = 'high';
				
			}else if(touchesMood.lonely >0 && touchesMood.lonely == touchesMood.content){
				console.log("LONELY");
				mood.touches = 'low';

			}

			var update = {$set: {mood: mood}} ;
			console.log(update);
			devicesDb.update({_id:device.device_id},update, function(err){ 
				console.log('updated'); 
				if(err)console.error(err);
			});

			var moodObj = { device_id: device.device_id, mood: mood, timestamp: new Date() };
			moodDb.insert(moodObj, function(err){
				if(err) console.error(err) //throw err;
			});

			//console.log(Object.keys(touchesMood).length)
			console.log("[TOUCHES MOOD]".debug+" "+JSON.stringify(touchesMood).data);
			var resp = {};
			resp.device_id = message.device_id;
			if(message.plant_type) resp.plant_type = message.plant_type;
			resp.mood = mood;
			//console.log(JSON.stringify(storedMood['tomato']['moisture']['high'][0]));
			//console.log(connection.plant_slug);

			resp.message={ 	moisture: storedMood[connection.plant_slug]['moisture'][mood.moisture][Math.round(Math.random(storedMood[connection.plant_slug]['moisture'][mood.moisture].length))], 
							touches: storedMood[connection.plant_slug]['touches'][mood.touches][Math.round(Math.random(storedMood[connection.plant_slug]['touches'][mood.touches].length))] 
							};

			console.log(JSON.stringify(resp));
			connection.socket.emit('update',resp);
			//signal device with mood and updated text
		});
		
	});
	
}
*/



/* ******************************************************************************************* */
// Deprecated
/* ******************************************************************************************* */

/*
DB.prototype.assignPlantData = function(result,connection){
	//result is a db document of device
	//console.log(result.plants);
	for(var i = 0; i< result.plants.length; i++){
		console.log(result.plants[i]._id);		

		// var oID = new BSON.ObjectID(result.plants[i]._id);
		var obj = {'_id': result.plants[i]._id };
		plantsDb.findOne(obj,function(err,result){
		
					var res = {	//"status": "planted", 
								"device_id": connection.device_id, 
								"connection_id": connection.id, 
								"plant":{"id": result._id, "index":result.index , "mood":result.mood } 
					};
					//connection.socket.send(JSON.stringify(res));
					connection.socket.emit('planted',res);
		});	
	}	
}
*/

// DEPRECATED. THIS GETS CALCULATED ON THE TABLETS
/*
DB.prototype.checkPlantTouches = function(obj,connection, signalToDevice, _db ){

	var TouchThreshold =10;
	var minutes =1;
	var time = new Date();
	time.setMinutes( time.getMinutes()-minutes );
	
	touchesDb.count({ device_id: obj.device_id, index: obj.index , timestamp:{ $gt : time}}, function(err,count){
	
		if(err) console.error(err);
		//console.log(count);
		
		var json = {};
		if(count == 0){
			console.log("[PLANT STATE]".info+" Lonely ".warn+count+"\t\t [DEVICE ID]".help+" %s".data,obj.device_id);
			json.mood = "lonely";
		}else if(count != 0 && count < TouchThreshold){
			console.log("[PLANT STATE]".info+" Content ".debug+count+"\t\t [DEVICE ID]".help+" %s".data,obj.device_id);
			json.mood = "content";
		}else if(count > TouchThreshold){
			console.log("[PLANT STATE]".info+" Worked Up ".error+count+"\t\t [DEVICE ID]".help+" %s".data,obj.device_id);
			json.mood = "worked_up";
		}		
				
		
		//json.mood=mood;
		json.plant_index=obj.index;
		plantsDb.update({ device_id: obj.device_id, index: obj.index }, {$set:json}, function(err, res) {
			if (err) console.error(err);
		});
		
		if(signalToDevice==true) {	//console.log(json);
			connection.socket.emit('touch', json);
		}
	
	});
	
}
*/



// Deprecated object for responses

/*
var storedMood = {
    tomatoes: {
        moisture: {
            high: [
                "Being TOO generous with the water can drown the tomatoes. Let's give 'em a break, shall we?",
                "Swimming is a fun summer activity for people, not tomatoes! Can you ease up on the water please?",
                "These tomatoes are getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Tomatoes love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
                "The tomatoes need time to drink up this water before they get more. Check in on them soon!",
                "Holy H20! That's a lot of water. The tomatoes probably need a break now…",
                "Water is awesome but we don't want the tomatoes to float away… how about petting them instead?",
                "Oh dear, these tomatoes are drenched! Any more water would be too much of a good thing.",
                "If the tomatoes keep getting this much water, they're gonna need an umbrella!"
            ],
            content: [
                "The tomatoes sure look refreshed! Way to beat the heat.",
                "The tomatoes are super appreciative. If they could send you a drink in return, they would!",
                "Thanks for helping the tomatoes stay juicy!",
                "Look at those water works! Nice job!",
                "The tomatoes love you for giving them a shower. They were really thirsty until you came along!",
                "Thanks! Nothing beats a cool splash of water on a hot summer day.",
                "Excellent water-wielding skills. I knew you could do it!",
                "Great watering job, you must have done this before!",
                "Thanks for keeping the tomatoes hydrated! You are a big help!"
            ],
            low: [
                "Hey you! Yeah, you. These tomatoes need some water, can you help?",
                "Can you water these tomatoes please? They'd do it themselves, but they don't have arms…",
                "If it gets any drier over here, these tomatoes are going to become tumbleweeds!",
                "These tomato plants sure look dry! Can you send some water their way?",
                "Boy, are these tomatoes parched… come quench their thirst!",
                "These tomatoes could sure use a shower; trust me, I'm standing right next to them!",
                "Show the tomatoes you care – give them some good ol' H20. They'll thank you for it.",
                "Tomatoes need water to grow. Can you help them out on this hot day?",
                "Sun-dried tomatoes are great in a salad but not in a garden… quick, send these guys some water!"
            ]
        },
        touches: {
            high: [
                "Careful, if the tomatoes get too worked-up they're going to fall off their vines! Let's spread the love around a bit.",
                "It's time for the tomatoes to get their beauty rest. Come back soon!",
                "The tomatoes need to focus on growing right now. Can you play with the other veggies for a bit?",
                "Tomatoes love attention, but so do the rest of the veggies!",
                "Pay the carrots a visit before they get too jealous…",
                "Looks like you guys are getting along great. I bet the other veggies would love to meet you too!",
                "All of the hustle and bustle can be overwhelming for tomatoes. They need to relax for a bit… be sure to come back later!",
                "Sounds like these tomatoes need to calm down a little. Can you give the other veggies some attention?",
                "The tomatoes are getting tuckered out. Want to play with some other veggies?",
                "Looks like the tomatoes need to mellow out a little. Can you go say \"hi\" to other veggies in the garden?",
                "Any more action and these tomatoes are going to turn into sauce!"
            ],
            content: [
                "You're a great playmate; hope you visit the garden every day!",
                "Wow, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You guys are getting along so well! Were you a tomato in your past life?",
                "The tomatoes are really digging all this attention. Thanks!",
                "Who knew that veggies could be such fun friends?",
                "You're making the tomatoes so happy! Great job!",
                "Holy cow, you're like, the tomato whisperer. Teach me your ways!",
                "You sure seem to know what tomatoes like! Thank goodness you're here.",
                "That's fun, isn't it? Who know tomatoes were so a-peel-ing?"
            ],
            low: [
                "Greetings from the garden! Come on over, it looks like the tomatoes would love to meet you.",
                "Get in touch with nature. Literally! See what happens when you pet these tomatoes…",
                "You seem like a veggie lover. Can you hang out with them for a sec?",
                "Did you know that tomatoes are extremely ticklish? Don't believe me? Try it and see!",
                "Help these tomato plants grow big and tall: give 'em a pinch to grow an inch!",
                "This tomato has an itch it can't scratch. Can you help out?",
                "I need your help waking these tomatoes up from their nap. Can you give 'em a poke for me please?",
                "Don't be a stranger – introduce yourself to these tomatoes with a tiny handshake!",
                "Seems like you would get along with these tomatoes. Come on over and meet 'em! They love being social.",
                "Plant your feet right here, these tomatoes want to spend some time with you!",
                "Even though tomatoes grow in bunches, they can still get lonely. Will you keep them company for a bit?"
            ]
        },
        
    },
    peppers: {
        moisture: {
            high: [
                "Uh oh, the peppers have had too much water. Better let their soil dry out a bit.",
                "Being TOO generous with the water can drown the peppers. Let's give 'em a break, shall we?",
                "Swimming is a fun summer activity for people, not peppers! Can you ease up on the water please?",
                "These peppers are getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Peppers love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
                "The peppers need time to drink up this water before they get more. Check in on them soon!",
                "Holy H20! That's a lot of water. The peppers probably need a break now…",
                "Water is awesome but we don't want the peppers to float away… how about petting them instead?",
                "Oh dear, these peppers are drenched! Any more water would be too much of a good thing.",
                "If the peppers keep getting this much water, they're gonna need an umbrella!",
                "Water droplets look neat on a pepper's waxy peel, but too much and these guy will be in trouble…"
            ],
            content: [
                "That drink really helped, the peppers feel better already!",
                "The peppers sure look refreshed! Way to beat the heat.",
                "The peppers are super appreciative. If they could send you a drink in return, they would!",
                "Thanks for helping the peppers stay crisp!",
                "Look at those water works! Nice job!",
                "The peppers love you for giving them a shower. They were really thirsty until you came along!",
                "Thanks! Nothing beats a cool splash of water on a hot summer day.",
                "Excellent water-wielding skills. I knew you could do it!",
                "Great watering job, you must have done this before!",
                "Thanks for keeping the peppers hydrated! You are a big help!"
            ],
            low: [
                "It's like a desert over here; come water these peppers before they shrivel up!",
                "Hey you! Yeah, you. These peppers need some water, can you help?",
                "Can you water these peppers please? They'd do it themselves, but they don't have arms…",
                "If it gets any drier over here, these peppers are going to become parched!",
                "These pepper plants sure look dry! Can you send some water their way?",
                "Boy, are these peppers parched… come quench their thirst!",
                "These peppers could sure use a shower; trust me, I'm standing right next to them!",
                "Show the peppers you care – give them some good ol' H20. They'll thank you for it.",
                "Peppers need water to grow. Can you help them out on this hot day?",
                "Pepper flakes are great on pizza but not in a garden. Send 'em some water before they dry out too much!"
            ]
        },
        touches: {
            high: [
                "Peppers like to be pet, but it looks like they need some \"me time\" right now. Why not give the celery a little attention?",
                "Careful, if the peppers get too worked-up they're going to fall off the stalk! Let's spread the love around a bit.",
                "It's time for the peppers to get their beauty rest. Come back soon!",
                "The peppers need to focus on growing right now. Can you play with the other veggies for a bit?",
                "Peppers love attention, but so do the rest of the veggies! Pay the celery a visit before it gets too jealous…",
                "Looks like you guys are getting along great. I bet the other veggies would love to meet you too!",
                "All of the hustle and bustle can be overwhelming for peppers. They need to relax for a bit… be sure to come back later!",
                "Sounds like these peppers need to calm down a little. Can you give the other veggies some attention?",
                "The peppers are getting tuckered out. Want to play with some other veggies?",
                "Looks like the peppers need to mellow out a little. Can you go say \"hi\" to other veggies in the garden?",
                "This pepper thinks it recognizes you… does it ring a bell?"
            ],
            content: [
                "Awww, the peppers are loving that! Thanks for entertaining them.",
                "You're a great playmate; hope you visit the garden every day!",
                "Wow, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You guys are getting along so well! Were you a pepper in your past life?",
                "The peppers are really digging all this attention. Thanks!",
                "Who knew that veggies could be such fun friends?",
                "You're making the peppers so happy! Great job!",
                "Holy cow, you're like, the pepper whisperer. Teach me your ways!",
                "You sure seem to know what peppers like! Thank goodness you're here.",
                "Thanks for joining the pep rally! These guys sure have spirit!"
            ],
            low: [
                "The peppers are working hard to grow big and tangy. Give them a high five for their efforts!",
                "Greetings from the garden! Come on over, it looks like the tomatoes would love to meet you.",
                "Get in touch with nature. Literally! See what happens when you pet these peppers…",
                "You seem like a veggie lover. Can you hang out with them for a sec?",
                "Did you know that peppers are extremely ticklish? Don't believe me? Try it and see!",
                "Help these pepper plants grow big and tall: give 'em a pinch to grow an inch!",
                "This pepper has an itch it can't scratch. Can you help out?",
                "It's time to wake these peppers up from their nap. Can you give 'em a poke?",
                "Don't be a stranger – introduce yourself to these peppers with a tiny handshake!",
                "Seems like you would get along with these peppers. Come on over and meet 'em! They love being social.",
                "Plant your feet right here, these peppers want to spend some time with you!",
                "These peppers feel hollow on the inside. Would you cheer them up?"
            ]
        }
    },
    celery: {
        moisture: {
            high: [
                "Uh oh, the celery has had too much water. Better let the soil dry out a bit.",
                "Being TOO generous with the water can drown the celery. Let's give it a break, shall we?",
                "Swimming is a fun summer activity for people, not celery! Can you ease up on the water please?",
                "This celery is getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Celery loves water, but too much of a good thing can be dangerous. Better cut it off before it starts swimming!",
                "The celery needs time to drink up this water before it gets more. Check in on it soon!",
                "Holy H20! That's a lot of water. The celery probably needs a break now…",
                "Water is awesome but we don't want the celery to float away… how about petting it instead?",
                "Oh dear, this celery is drenched! Any more water would be too much of a good thing.",
                "If the celery keeps getting this much water, it's gonna need an umbrella!"
            ],
            content: [
                "That drink really helped, the celery feels better already!",
                "The celery sure looks refreshed! Way to beat the heat.",
                "The celery is super appreciative. If it could send you a drink in return, it would!",
                "Thanks for helping the celery stay crunchy!",
                "Look at those water works! Nice job!",
                "The celery loves you for giving it a shower. It was really thirsty until you came along!",
                "Thanks! Nothing beats a cool splash of water on a hot summer day.",
                "Excellent water-wielding skills. I knew you could do it!",
                "Great watering job, you must have done this before!",
                "Thanks for keeping the celery hydrated! You are a big help!",
                "Thanks for keeping the celery a lean, green crunchy machine!"
            ],
            low: [
                "It's like a desert over here; come water this celery before it shrivels up!",
                "Hey you! Yeah, you. This celery needs some water, can you help?",
                "Can you water these celery stalks please? They'd do it themselves, but they don't have arms…",
                "If it gets any drier over here, this celery will turn to sand!",
                "These celery stalks sure look dry! Can you send some water their way?",
                "Boy, is this celery parched… come quench its thirst!",
                "This celery could sure use a shower; trust me, I'm standing right next to it!",
                "Show the celery you care – give it some good ol' H20. It'll thank you for it.",
                "Celery needs water to grow. Can you help it out on this hot day?",
                "We're on the verge of some major wilt-age… celery needs water pronto!"
            ]
        },
        touches: {
            high: [
                "This celery likes to be pet, but it needs some \"me time\" right now. Why not give the peppers some attention?",
                "Careful, if the celery gets too worked-up its leaves are going to fall off! Let's spread the love around.",
                "It's time for the celery to get its beauty rest. Come back soon!",
                "The celery needs to focus on growing right now. Can you play with the other veggies for a bit?",
                "Celery loves attention, but so do the rest of the veggies! Pay the tomatoes a visit before they get ripe with envy…",
                "Looks like you guys are getting along great. I bet the other veggies would love to meet you too!",
                "All of the hustle and bustle can be overwhelming for celery. They need to relax for a bit… be sure to come back later!",
                "Sounds like the celery needs to calm down a little. Can you give the other veggies some attention?",
                "The celery is getting tuckered out. Want to play with some other veggies?",
                "Looks like the celery needs to mellow out a little. Can you go say \"hi\" to other veggies in the garden?",
                "This celery is so excited it's gonna get its stalks all tangled up! Don't forget about the rest of the garden…"
            ],
            content: [
                "Awww, the celery is loving that! Thanks for entertaining it.",
                "You're a great playmate; hope you visit the garden every day!",
                "Wow, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You guys are getting along so well! Were you a celery stalk in your past life?",
                "The celery is really digging all this attention. Thanks!",
                "Who knew that veggies could be such fun friends?",
                "You're making the celery so happy! Great job!",
                "Holy cow, you're like, the celery whisperer. Teach me your ways!",
                "You sure seem to know what celery likes! Thank goodness you're here."
            ],
            low: [
                "The celery stalks are working hard to grow tall and crunchy. Give them a high five for their efforts!",
                "Greetings from the garden! Come on over, the celery would love to meet you.",
                "Get in touch with nature. Literally! See what happens when you pet this celery…",
                "You seem like a veggie lover… Can you hang out with it for a sec?",
                "Did you know that celery is extremely ticklish? Don't believe me? Try it and see!",
                "Help this celery grow big and tall: give it a pinch to grow an inch!",
                "This celery has an itch it can't scratch. Can you help out?",
                "I need your help waking the celery up from its nap. Can you give it a poke for me please?",
                "Don't be a stranger – introduce yourself to this celery with a tiny handshake!",
                "Seems like you would get along with the celery. Come on over and meet 'em! They love being social.",
                "Plant your feet right here, the celery wants to spend some time with you!",
                "Celery wants attention too! It's getting green with envy."
            ]
        }
    },
    carrots: {
        moisture: {
            high: [
                "Uh oh, the carrots have had too much water. Better let their soil dry out a bit.",
                "Being TOO generous with the water can drown the carrots. Let's give 'em a break, shall we?",
                "Swimming is a fun summer activity for people, not carrots! Can you ease up on the water please?",
                "These carrots are getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Carrots love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
                "The carrots need time to drink up this water before they get more. Check in on them soon!",
                "Holy H20! That's a lot of water. The carrots probably need a break now…",
                "Water is awesome but we don't want the carrots to float away… how about petting them instead?",
                "Oh dear, these carrots are drenched! Any more water would be too much of a good thing.",
                "If the carrots keep getting this much water, they're gonna need an umbrella!"
            ],
            content: [
                "That drink really helped, the carrots feel better already!",
                "The carrots sure look refreshed! Way to beat the heat.",
                "The carrots are super appreciative. If they could send you a drink in return, they would!",
                "Thanks for helping the carrots stay sweet!",
                "Look at those water works! Nice job!",
                "The carrots love you for giving them a shower. They were really thirsty until you came along!",
                "Thanks! Nothing beats a cool splash of water on a hot summer day.",
                "Excellent water-wielding skills. I knew you could do it!",
                "Great watering job, you must have done this before!",
                "Thanks for keeping the carrots hydrated! You are a big help!"
            ],
            low: [
                "It's like a desert over here; come water these carrots before they shrivel up!",
                "Hey you! Yeah, you. These carrots need some water, can you help?",
                "Can you water these carrots please? They'd do it themselves, but they don't have arms…",
                "If it gets any drier over here, these carrots are going to turn to dust!",
                "These carrots sure look dry! Can you send some water their way?",
                "Boy, are these carrots parched… come quench their thirst!",
                "These carrots could sure use a shower; trust me, I'm standing right next to them!",
                "Show the carrots you care – give them some good ol' H20. They'll thank you for it.",
                "Carrots need water to grow. Can you help them out on this hot day?",
                "If these dudes don't get some water, I'm going to have a carrot riot on my hands! Help!"
            ]
        },
        touches: {
            high: [
                "These carrots like to be pet, but they need some \"me time\" right now. Why not give the tomatoes some attention?",
                "Careful, if the carrots get too worked-up they're going to pop right out of the ground! Let's spread the love around.",
                "It's time for the carrots to get their beauty rest. Come back soon!",
                "The carrots need to focus on growing right now. Can you play with the other veggies for a bit?",
                "Carrots love attention, but so do the rest of the veggies! Pay the beets a visit before they get too jealous…",
                "Looks like you guys are getting along great. I bet the other veggies would love to meet you too!",
                "All of the hustle and bustle can be overwhelming for carrots. They need to relax for a bit… be sure to come back later!",
                "Sounds like these tomatoes need to calm down a little. Can you give the other veggies some attention?",
                "The carrots are getting tuckered out. Want to play with some other veggies?",
                "Looks like the carrots need to mellow out a little. Can you go say \"hi\" to other veggies in the garden?",
                "Too much attention and these carrots are gonna burrow on outta here…"
            ],
            content: [
                "Awww, the carrots are loving that! Thanks for entertaining them.",
                "You're a great playmate; hope you visit the garden every day!",
                "Wow, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You guys are getting along so well! Were you a carrot in your past life?",
                "The carrots are really digging all this attention. Thanks!",
                "Who knew that veggies could be such fun friends?",
                "You're making the carrots so happy! Great job!",
                "Holy cow, you're like, the carrot whisperer. Teach me your ways!",
                "You sure seem to know what carrots like! Thank goodness you're here.",
                "Good thing you aren't a rabbit; when those guys hang out with carrots it never ends well…"
            ],
            low: [
                "The carrots are working hard to become crunchy and sweet. Give them a high five for their efforts!",
                "Greetings from the garden! Come on over, the carrots would love to meet you.",
                "Get in touch with nature, literally! See what happens when you pet these carrots…",
                "You seem like a veggie lover… Can you hang out with them for a sec?",
                "Did you know that carrots are extremely ticklish? Don't believe me? Try it and see!",
                "Help these carrots grow big and tall: give 'em a pinch to grow an inch!",
                "This carrot has an itch it can't scratch. Can you help out?",
                "I need your help waking the carrots up from their nap. Can you give 'em a poke for me please?",
                "Don't be a stranger – introduce yourself to these carrots with a tiny handshake!",
                "Seems like you would get along with these carrots. Come on over and meet 'em! They love being social.",
                "Plant your feet right here, these carrots want to spend some time with you!",
                "Don't mistake the carrot's underground hideout as shyness; they definitely want to play with you!"
            ]
        }
    },
    beets: {
        moisture: {
            high: [
                "Uh oh, the beets have had too much water. Better let their soil dry out a bit.",
                "Being TOO generous with the water can drown the beets. Let's give 'em a break, shall we?",
                "Swimming is a fun summer activity for people, not beets! Can you ease up on the water please?",
                "These beets are getting soaked! The water is very refreshing but let's not over-do it, eh?",
                "Beets love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
                "The beets need time to drink up this water before they get more. Check in on them soon!",
                "Holy H20! That's a lot of water. The beets probably need a break now…",
                "Water is awesome but we don't want the beets to float away… how about petting them instead?",
                "Oh dear, these beets are drenched! Any more water would be too much of a good thing.",
                "If the beets keep getting this much water, they're gonna need an umbrella!",
                "That's not how you make beet juice! Cut back on the water, will ya?"
            ],
            content: [
                "That drink really helped, the beets feel better already!",
                "The beets sure look refreshed! Way to beat the heat.",
                "The beets are super appreciative. If they could send you a drink in return, they would!",
                "Thanks for helping the beets stay hearty!",
                "Look at those water works! Nice job!",
                "The beets love you for giving them a shower. They were really thirsty until you came along!",
                "Thanks! Nothing beats a cool splash of water on a hot summer day.",
                "Excellent water-wielding skills. I knew you could do it!",
                "Great watering job, you must have done this before!",
                "Thanks for keeping the beets hydrated! You are a big help!",
                "The beets sure look refreshed! Way to \"beet\" the heat. (Get it?)"
            ],
            low: [
                "It's like a desert over here; come water these beets before they dry out!",
                "Hey you! Yeah, you. These beets need some water, can you help?",
                "Can you water these beets please? They'd do it themselves, but they don't have arms…",
                "If it gets any drier over here, these beets are going to shrivel up!",
                "These beets sure look dry! Can you send some water their way?",
                "Boy, are these beets parched… come quench their thirst!",
                "These beets could sure use a shower; trust me, I'm standing right next to them!",
                "Show the beets you care – give them some good ol' H20. They'll thank you for it.",
                "Beets need water to grow. Can you help them out on this hot day?",
                "What's the raisin version of a beet? I don't know either, and I don't wanna find out… a little water please!"
            ]
        },
        touches: {
            high: [
                "These beets like to be pet, but they need some \"me time\" right now. Can you give the celery some attention?",
                "Careful, if the beets get too worked-up they're going to pop out of the ground! Let's spread the love around.",
                "It's time for the beets to get their beauty rest. Come back soon!",
                "The beets need to focus on growing right now. Can you play with the other veggies for a bit?",
                "Beets love attention, but so do the rest of the veggies! Pay the celery a visit before they get too jealous…",
                "Looks like you guys are getting along great. I bet the other veggies would love to meet you too!",
                "All of the hustle and bustle can be overwhelming for beets. They need to relax for a bit… be sure to come back later!",
                "Sounds like these beets need to calm down a little. Can you give the other veggies some attention?",
                "The beets are getting tuckered out. Want to play with some other veggies?",
                "Looks like the beets need to mellow out a little. Can you go say \"hi\" to other veggies in the garden?"
            ],
            content: [
                "Awww, the beets are loving that! Thanks for entertaining them.",
                "You're a great playmate; hope you visit the garden every day!",
                "Wow, you've got the magic touch! Keep it up!",
                "Perfect job, you're a natural!",
                "You guys are getting along so well! Were you a tomato in your past life?",
                "The beets are really digging all this attention. Thanks!",
                "Who knew that veggies could be such fun friends?",
                "You're making the beets so happy! Great job!",
                "Holy cow, you're like, the beet whisperer. Teach me your ways!",
                "You sure seem to know what beets like! Thank goodness you're here."
            ],
            low: [
                "The beets are working hard to grow big and juicy. Give them a high five for their efforts!",
                "Greetings from the garden! Come on over, the tomatoes would love to meet you.",
                "Get in touch with nature, literally! See what happens when you pet these beets…",
                "You seem like a veggie lover… Can you hang out with them for a sec?",
                "Did you know that beets are extremely ticklish? Don't believe me? Try it and see!",
                "Help these beets grow big and round: give 'em a pinch to grow an inch!",
                "This beet has an itch it can't scratch. Can you help out?",
                "I need your help waking the beets up from their nap. Can you give 'em a poke for me please?",
                "Don't be a stranger – introduce yourself to these beets with a tiny handshake!",
                "Seems like you would get along with these beets. Come on over and meet 'em! They love being social.",
                "Plant your feet right here, these beets want to spend some time with you!"
            ]
        }
    }
};
*/