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
				else if(avgResults.moisture<res.moisture.low) mood.moisture='dry';
				else if(avgResults.moisture>res.moisture.high) mood.moisture='wet';
				
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
			
			//check which value is most prevelant 
			if(touchesMood.worked_up > Object.keys(touchesMood).length){
				//a large number of plants are worked up
				console.log("WORKED UP");
			}
			if(touchesMood.lonely > Object.keys(touchesMood).length){
				//Large Number of plants are lonely
				console.log("LONELY");
			}if(touchesMood.content > Object.keys(touchesMood).length){
				//Large Number of plants are lonely
				console.log("CONTENT");
			}
			
			console.log(Object.keys(touchesMood).length)
			console.log(touchesMood);
		});
		
	});
	

	
	var resp = {};
	resp.device_id = message.device_id;
	if(message.plant_type) resp.plant_type = message.plant_type;
	resp.mood = mood;
	resp.message="Show this message - ";
	var time = new Date();
	resp.message+=time;
	console.log(JSON.stringify(resp));
	connection.socket.emit('update',resp);
	//signal device with mood and updated text
	
	
	
	
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
	var obj = {device_id: new BSON.ObjectID(String(message.device_id)), index: message.plant_index };
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

DB.prototype.checkPlantTouches=function(obj,connection, signalToDevice, _db ){
	
	var TouchThreshold =10;
	var minutes =1;
	var time = new Date();
	time.setMinutes( time.getMinutes()-minutes );
	
	touchesDb.count({ device_id: obj.device_id, index: obj.index , timestamp:{ $gt : time}}, function(err,count){
	
		if(err) console.error(err);
		//console.log(count);
		
		var json = {};
		if(count == 0){
			console.log("[PLANT STATE]".info+" Lonely ".warn+count);
			json.mood = "lonely";
		}else if(count != 0 && count < TouchThreshold){
			console.log("[PLANT STATE]".info+" Content ".debug+count);
			json.mood = "content";
		}else if(count > TouchThreshold){
			console.log("[PLANT STATE]".info+" Worked Up ".error+count);
			json.mood = "worked_up";
		}				
		
		// json.mood=mood;
		json.plant_index=obj.index;
		plantsDb.update({ device_id: obj.device_id, index: obj.index }, {$set:json}, function(err, res) {
			if (err) console.error(err);
		});
		
		if(signalToDevice==true) connection.socket.emit('touch', json);
	
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
	var obj = {date: new Date(), plants: [], type: message.plant_type, active: true};
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