var MongoClient = require('mongodb').MongoClient,
	Server = require('mongodb').Server,
	mongo = new MongoClient(new Server('localhost', 27017)),
	BSON = require('mongodb').BSONPure;
	
var utils = require('./dbutils');	
	
function DB(){
	
	this.db; 
	this.dataDb; 
	this.personalitiesDb; 
	this.devicesDb; 
	this.plantsDb;
	this.touchesDb;
	this.twitter;
	
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
	});
		
}

module.exports = DB;


/* ******************************************************************************************* */
/* ******************************************************************************************* */

DB.prototype.routeUpdate = function(message,connection){
	
	if(message.device_id != connection.device_id) { 
		console.error('[Update Error] message.device_id=%s connection.device_id=%s', message.device_id, connection.device_id );/*error error;*/ 
	}else{
		 
		console.log("GOT MESSAGE.DATA :: "+JSON.stringify(message.data));
			
		var obj = {	'device_id': new BSON.ObjectID( String(message.device_id)), 
					'moisture':message.data.moisture, 
					'temp': message.data.temp, 
					'humidity':message.data.humidity, 
					'light': message.data.light,
					'timestamp': new Date()
				};
				
		dataDb.insert(obj, {safe:true}, function(err,doc){
			if(err) console.error(err);//throw err;
			
			//check the last values and determine mood of plant 
			//update plants/device if necessary
		
		});	
	}
}

/* ******************************************************************************************* */
/* ******************************************************************************************* */

DB.prototype.routeRegister = function(message,connection){
	//check for ID in DB
	//if ID exists
	//return ID, status - connected, rejoined
	if(message.device_id == "set_id"){
	
		console.log("New Device - Logging Now");		
		utils.logDevice(message,connection);
	
	}else{
		console.log("Size of device_id: "+message.device_id.length);
		
		if(message.device_id.length == 24){
			var oID = new BSON.ObjectID(message.device_id);
			var obj = {'_id': oID};
			//find device by id
			devicesDb.findOne(obj,function(err,result){
				
				if(!result){
					console.log("[Device Registration Failed]");
					utils.logDevice(message,connection);
					
				}else{
					
					var res = { "device_id": message.device_id, "connection_id": connection.id };
					console.log("[Device Already Registered]");
					connection.device_id = message.device_id;
					//assignPlantData(result,connection);
					
					connection.socket.emit('connected', res);
					//connection.socket.send(JSON.stringify(res));	
				}
				
			});
			
		}else{
			
			utils.logDevice(message,connection);

		}
		//check plants and register them
		
	}
}

/* ******************************************************************************************* */
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
								"plant":{"id": result._id, "type":result.type, "index":result.index , "mood":result.mood } 
					};
					//connection.socket.send(JSON.stringify(res));
					connection.socket.emit('planted',res);
		});	
	}	
}




/* ******************************************************************************************* */
/* ******************************************************************************************* */

DB.prototype.createPlant = function(message,connection,plant_index){
	//blocking or non-blocking function?
	
	//console.log("plant: "+ JSON.stringify(plant));
	console.log('[CREATING PLANT]');
	var obj = {created: new Date(), device_id:connection.device_id, index: plant_index, type: message.plant_type, mood: "born", touch:0 };
	plantsDb.insert(obj,{safe:true},function(err,doc){
			if(err) console.error(err); //throw err;

		   var json={ $push: { plants: { index:doc[0].index, _id:doc[0]._id } } };
		   utils.updateDocument(devicesDb,connection.device_id, json);
		   
	});
}

/* ******************************************************************************************* */
/* ******************************************************************************************* */

DB.prototype.plantTouch = function(message,connection){
	
	//May need to create dual index of device_id & plant index

	var obj = {'device_id': new BSON.ObjectID(String(message.device_id)), 'index': message.plant_index };
	var json = {$inc : {touch: 1}};
	plantsDb.update(obj, json,function(err){
		if(err) console.error(err);//throw err; 
		//check & respond w mood update.

	});	
	
	obj.timestamp = new Date();
		
	touchesDb.insert(obj, function(err){
		if(err) console.error(err) //throw err;
	});

	//this.twitter.twitterRef.gotTouched();
}