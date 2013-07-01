var MongoClient = require('mongodb').MongoClient,
	Server = require('mongodb').Server,
	mongo = new MongoClient(new Server('localhost', 27017)),
	BSON = require('mongodb').BSONPure;


//var utils = require('./dbUtils');	

	
function DB(){
	
	this.db; 
	this.dataDb; 
	this.personalitiesDb; 
	this.devicesDb; 
	this.plantsDb;
	this.touchesDb;
	this.settingsDb;
	this.twitter;
	this.colors;
	
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
	});
		
}

module.exports = DB;


/* ******************************************************************************************* */
/* ******************************************************************************************* */

DB.prototype.routeUpdate = function(message,connection){
	
	if(message.device_id != connection.device_id) { 
		console.error('[UPDATE ERROR] '.error+"\t\t message.device_id=%s connection.device_id=%s".data, message.device_id, connection.device_id );/*error error;*/ 
	}else{
		 
		//console.log("GOT MESSAGE.DATA :: "+JSON.stringify(message.data));
			
		var obj = {	'device_id': new BSON.ObjectID( String(message.device_id)), 
					'moisture':message.data.moisture, 
					'temp': message.data.temp, 
					'humidity':message.data.humidity, 
					'light': message.data.light,
					'timestamp': new Date()
				};
				
		var minutes = 15; //may need to be reset
		var time = new Date();
		time.setMinutes( time.getMinutes()-minutes );		
		
		var _db = this;
		
		dataDb.insert(obj, {safe:true}, function(err,doc){
			if(err) console.error(err);//throw err;
			
			//check the last values and determine mood of plant 
			//update plants/device if necessary
			var recent = {
							device_id : obj.device_id,
							timestamp: { $gte: time }
						  };
						  
			dataDb.find(recent).toArray( function(err,res){
				//console.log(res);
				if(err) console.error(err);
				//console.log(res.length);
				_db.calcDeviceMood(message,connection,res,_db);
			
			});
			
		
		});	
	}
}
/* ******************************************************************************************* */
/* ******************************************************************************************* */

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

/* ******************************************************************************************* */
/* ******************************************************************************************* */

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
			//console.log(Object.keys(touchesMood).length)
			console.log("[TOUCHES MOOD]".debug+" "+JSON.stringify(touchesMood).data);
			var resp = {};
			resp.device_id = message.device_id;
			if(message.plant_type) resp.plant_type = message.plant_type;
			resp.mood = mood;
			//console.log(JSON.stringify(storedMood['tomato']['moisture']['high'][0]));
			resp.message={ 	moisture: storedMood['tomato']['moisture'][mood.moisture][Math.round(Math.random(storedMood['tomato']['moisture'][mood.moisture].length))], 
							touches: storedMood['tomato']['touches'][mood.touches][Math.round(Math.random(storedMood['tomato']['touches'][mood.touches].length))] 
							};
			console.log(JSON.stringify(resp));
			connection.socket.emit('update',resp);
			//signal device with mood and updated text
		});
		
	});
	

	

	
	
	
	
}

/* ******************************************************************************************* */
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
					
					var res = { "device_id": message.device_id, "connection_id": connection.id };
					console.log("[Device Already Registered]");
					connection.device_id = message.device_id;
					//assignPlantData(result,connection);
					_db.setActive(connection, true);
					
					connection.socket.emit('register', res);
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

/* ******************************************************************************************* */
// Depricated
/* ******************************************************************************************* */

DB.prototype.assignPlantData = function(result,connection){
	//result is a db document of device
	//console.log(result.plants);
	for(var i = 0; i< result.plants.length; i++){
		console.log(result.plants[i]._id);		
/*
		var oID = new BSON.ObjectID(result.plants[i]._id);*/
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


/* ******************************************************************************************* */
/* ******************************************************************************************* */

DB.prototype.plantTouch = function(message,connection){
	
	//May need to create dual index of device_id & plant index
	var obj = {device_id: new BSON.ObjectID(String(message.device_id)), index: message.plant_index, cap_val: message.cap_val };
	//console.log("[PLANT TOUCH] ".info +JSON.stringify(obj));

	var json = {$inc : {touch: 1}};
	plantsDb.update(obj, json,function(err){
		if(err) console.error(err);//throw err; 
		//check & respond w mood update.

	});	
	
	obj.timestamp = new Date();
	
	// console.log(obj.timestamp);
	
	var _db = this;

	// console.log(time);
	touchesDb.insert(obj, function(err){
				if(err) console.error(err) //throw err;
				
				_db.checkPlantTouches(obj,connection,true,_db);
		
	});

	//this.twitter.twitterRef.gotTouched();
}
/* ******************************************************************************************* */
/* ******************************************************************************************* */

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
/* ******************************************************************************************* */
/* ******************************************************************************************* */

DB.prototype.createPlant = function(message,connection,plant_index,_db){
	//blocking or non-blocking function?
	
	//console.log("plant: "+ JSON.stringify(plant));
	console.log('[CREATING PLANT]');
	var obj = {created: new Date(), device_id:connection.device_id, index: plant_index, type: message.plant_type, mood: "born", touch:0 };
	plantsDb.insert(obj,{safe:true},function(err,doc){
		  if(err) console.error(err); //throw err;

		   var json={ $push: { plants: { index:doc[0].index, _id:doc[0]._id } } };
		   _db.updateDocument(devicesDb,connection.device_id, json);
		   
	});
}

/* ******************************************************************************************* */
/* ******************************************************************************************* */

DB.prototype.logDevice = function(message,connection,_db){
	var obj = {date: new Date(), plants: [], type: message.plant_type, active: true, mood: 'born'};
	devicesDb.insert(obj, {safe:true}, function(err,doc){
		if(err) console.error(err); //throw err;
		
		connection.device_id = doc[0]._id;
		console.log('Created Record: '+connection.device_id);
		console.log("plants.length: "+message.num_plants);
		for(var i = 0; i<message.num_plants; i++) _db.createPlant(message,connection,i,_db);
		
		_db.createSettings(message,connection,_db);
		
		var res = { "device_id": connection.device_id, "connection_id": connection.id };
		//connection.socket.send(JSON.stringify(res));
		connection.socket.emit('register',res);
	});	
}
/* ******************************************************************************************* */
/* ******************************************************************************************* */

DB.prototype.createSettings = function(message,connection,_db){
	
	var obj = {};
	obj.device_id = connection.device_id;
	obj.humidity = { active: false, low: 10, high:60};
	obj.temp = {active: false, low: 15, high:35};
	obj.moisture = {active: true, low:0, high:100};
	obj.light = {active: false, low:400, high: 1000};	
	
	settingsDb.insert(obj,{safe:true},function(err){
		if(err) console.error(err);
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

var storedMood = {
			
			tomato:{	moisture:{
								high:[
									"Being TOO generous with the water can drown the tomatoes. Let's give 'em a break, shall we?",
									"Swimming is a fun summer activity for people, not tomatoes! Can you ease up on the water please?",
									"These tomatoes are getting soaked! The water is very refreshing but let's not over-do it, eh?",
									"Tomatoes love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
									"The tomatoes need time to drink up this water before they get more. Check in on them soon!",
									"Holy H20! That's a lot of water. The tomatoes probably need a break now…",
									"Water is awesome but we don't want the tomatoes to float away... how about petting them instead?",
									"Oh dear, these tomatoes are drenched! Any more water would be too much of a good thing.",
									"If the tomatoes keep getting this much water, they're gonna need an umbrella!"
								],
								content:[
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
								low:[
									"Hey you! Yeah, you. These tomatoes need some water, can you help?",
									"Can you water these tomatoes please? They'd do it themselves, but they don't have arms...",
									"If it gets any drier over here, these tomatoes are going to become tumbleweeds!",
									"These tomato plants sure look dry! Can you send some water their way?",
									"Boy, are these tomatoes parched... come quench their thirst!",
									"These tomatoes could sure use a shower; trust me, I'm standing right next to them!",
									"Show the tomatoes you care – give them some good ol' H20. They'll thank you for it.",
									"Tomatoes need water to grow. Can you help them out on this hot day?",
									"Sun-dried tomatoes are great in a salad but not in a garden... quick, send these guys some water!"
								]
						},
						touches:{
								high:[	
																
									"Careful, if the tomatoes get too worked-up they're going to fall off their vines! Let's spread the love around a bit.",
									"It's time for the tomatoes to get their beauty rest. Come back soon!",
									"The tomatoes need to focus on growing right now. Can you play with the other veggies for a bit?",
									"Tomatoes love attention, but so do the rest of the veggies!", 
									"Pay the carrots a visit before they get too jealous…",
									"Looks like you guys are getting along great. I bet the other veggies would love to meet you too!",
									"All of the hustle and bustle can be overwhelming for tomatoes. They need to relax for a bit… be sure to come back later!",
									"Sounds like these tomatoes need to calm down a little. Can you give the other veggies some attention?",
									"The tomatoes are getting tuckered out. Want to play with some other veggies?",
									"Looks like the tomatoes need to mellow out a little. Can you go say 'hi' to other veggies in the garden?",
									"Any more action and these tomatoes are going to turn into sauce!"
								],
								content:[
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
								low:[
									"Greetings from the garden! Come on over, it looks like the tomatoes would love to meet you.",
									"Get in touch with nature. Literally! See what happens when you pet these tomatoes...",
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
			pepper:{
				moisture:{
							high:[
									"Uh oh, the peppers have had too much water. Better let their soil dry out a bit.",
									"Being TOO generous with the water can drown the peppers. Let's give 'em a break, shall we?",
									"Swimming is a fun summer activity for people, not peppers! Can you ease up on the water please?",
									"These peppers are getting soaked! The water is very refreshing but let's not over-do it, eh?",
									"Peppers love water, but too much of a good thing can be dangerous. Better cut 'em off before they start swimming!",
									"The peppers need time to drink up this water before they get more. Check in on them soon!",
									"Holy H20! That's a lot of water. The peppers probably need a break now…",
									"Water is awesome but we don't want the peppers to float away... how about petting them instead?",
									"Oh dear, these peppers are drenched! Any more water would be too much of a good thing.",
									"If the peppers keep getting this much water, they're gonna need an umbrella!",
									"Water droplets look neat on a pepper's waxy peel, but too much and these guy will be in trouble..."
									],
							content:[
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
							low:[
									"It's like a desert over here; come water these peppers before they shrivel up!",
									"Hey you! Yeah, you. These peppers need some water, can you help?",
									"Can you water these peppers please? They'd do it themselves, but they don't have arms...",
									"If it gets any drier over here, these peppers are going to become parched!",
									"These pepper plants sure look dry! Can you send some water their way?",
									"Boy, are these peppers parched... come quench their thirst!",
									"These peppers could sure use a shower; trust me, I'm standing right next to them!",
									"Show the peppers you care – give them some good ol' H20. They'll thank you for it.",
									"Peppers need water to grow. Can you help them out on this hot day?",
									"Pepper flakes are great on pizza but not in a garden. Send 'em some water before they dry out too much!"
							]
				},
				touches:{
					high:[
						"Peppers like to be pet, but it looks like they need some 'me time' right now. Why not give the celery a little attention?",
						"Careful, if the peppers get too worked-up they're going to fall off the stalk! Let's spread the love around a bit.",
						"It's time for the peppers to get their beauty rest. Come back soon!",
						"The peppers need to focus on growing right now. Can you play with the other veggies for a bit?",
						"Peppers love attention, but so do the rest of the veggies! Pay the celery a visit before it gets too jealous...",
						"Looks like you guys are getting along great. I bet the other veggies would love to meet you too!",
						"All of the hustle and bustle can be overwhelming for peppers. They need to relax for a bit… be sure to come back later!",
						"Sounds like these peppers need to calm down a little. Can you give the other veggies some attention?",
						"The peppers are getting tuckered out. Want to play with some other veggies?",
						"Looks like the peppers need to mellow out a little. Can you go say 'hi' to other veggies in the garden?",
						"This pepper thinks it recognizes you... does it ring a bell?"
					],content:[
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
					low:[
						"The peppers are working hard to grow big and tangy. Give them a high five for their efforts!",
						"Greetings from the garden! Come on over, it looks like the tomatoes would love to meet you.",
						"Get in touch with nature. Literally! See what happens when you pet these peppers...",
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
			celery:{
				moisture:{
					high:[],content:[],low:[]
				},
				touches:{
					high:[],content:[],low:[]
				}
			},
			carrots:{
				moisture:{
					high:[],content:[],low:[]
				},
				touches:{
					high:[],content:[],low:[]
				}
			},
			beets:{
				moisture:{
					high:[],content:[],low:[]
				},
				touches:{
					high:[],content:[],low:[]
				}
			}
};