/*
//Depreciated
var WebSocketServer = require('ws').Server
  , wss = new WebSocketServer({port: 9000});
*/
  
var io = require('socket.io').listen(9001);
  
var MongoClient = require('mongodb').MongoClient
	,Server = require('mongodb').Server
	,mongo = new MongoClient(new Server('localhost', 27017))
	,BSON = require('mongodb').BSONPure;

var db, dataDb, personalitiesDb, devicesDb, plantsDb, touchesDb;
var clients={};


//twitter

/* ******************************************************************************************* */
/* ******************************************************************************************* */

mongo.open(function(err,mongo){
	
	db = mongo.db('powergarden');
	devicesDb = db.collection('devices');
	plantsDb = db.collection('plants');
	personalitiesDb = db.collection('personalities');
	dataDb = db.collection('data');
	touchesDb = db.collection('touches');
		
});


/* ******************************************************************************************* */
/* ******************************************************************************************* */
var clientID = 0;

io.sockets.on('connection', function (socket) {
  //io.sockets.emit('this', { will: 'be received by everyone'});
  	var connection = new Connection( ++clientID, 'set_id', socket);
	var connectKey = 'client-'+clientID;
	clients[connectKey]=connection;

	console.log("[NEW CONN] connection.id %s",connection.id);
	console.log("[NEW CONN] connection.device_id %s",connection.device_id);


	socket.on('register', function (msg) {
	    console.log('[Device Register Request]: ', msg);
	    routeRegister(msg,connection);
	  
	});
	socket.on('update', function (msg) {
	    console.log('[Device Update Request]: ', msg);
	    routeUpdate(msg,connection);
	  
	});
	  
	socket.on('touch',function(msg){
		  console.log('[Plant Touch Signaled]', msg);
		  plantTouch(msg,connection);
		  
	});
	
	socket.on('disconnect', function () {
	    /* io.sockets.emit('user disconnected'); */
	  console.log("[DISCONN] connection.id %s",connection.id);
	  console.log("[DISCONN] connection.device_id %s",connection.device_id);
	});
});

/* ******************************************************************************************* */
/* ******************************************************************************************* */

function routeUpdate(message,connection){
	
	if(message.device_id != connection.device_id) { 
		console.log('[Update Error] message.device_id=%s connection.device_id=%s', message.device_id, connection.device_id );/*error error;*/ 
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
			if(err) throw err;
			
			//check the last values and determine mood of plant 
			//update plants/device if necessary
		
		});
			
			
	
		
	}
	
	
}
/* ******************************************************************************************* */
/* ******************************************************************************************* */

function routeRegister(message,connection){
	//check for ID in DB
	//if ID exists
	//return ID, status - connected, rejoined
	if(message.device_id == "set_id"){
	
		console.log("New Device - Logging Now");		
		logDevice(message,connection);
	
	}else{
		console.log("Size of device_id: "+message.device_id.length);
		
		if(message.device_id.length == 24){
			var oID = new BSON.ObjectID(message.device_id);
			var obj = {'_id': oID};
			//find device by id
			devicesDb.findOne(obj,function(err,result){
				
				if(!result){
					console.log("[Device Registration Failed]");
					logDevice(message,connection);
					
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
			
			logDevice(message,connection);

		}
		//check plants and register them
		
	}

}

/* ******************************************************************************************* */
/* ******************************************************************************************* */

function assignPlantData(result,connection){
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

function logDevice(message,connection){
		var obj = {date: new Date(), plants: []};
		devicesDb.insert(obj, {safe:true}, function(err,doc){
			if(err)throw err;
			
			connection.device_id = doc[0]._id;
			console.log('Created Record: '+connection.device_id);
			console.log("plants.length: "+message.num_plants);
			for(var i = 0; i<message.num_plants; i++) createPlant(message,connection,i);
			
			
			var res = { "device_id": connection.device_id, "connection_id": connection.id };
			//connection.socket.send(JSON.stringify(res));
			connection.socket.emit('connected',res);
		});	
	
}
/* ******************************************************************************************* */
/* ******************************************************************************************* */
//update mongo db document
function updateDocument(collection,id,json){
	console.log("[Updating Document] Collection: "+collection+" id: "+id);
	var obj = { _id : new BSON.ObjectID( String(id) ) };
	collection.update(obj,json,function(err){
		if(err)throw err;
	});

//no upsert 
}
/* ******************************************************************************************* */
/* ******************************************************************************************* */

function createPlant(message,connection,plant_index){
	//blocking or non-blocking function?
	
	//console.log("plant: "+ JSON.stringify(plant));
	console.log('[CREATING PLANT]');
	var obj = {created: new Date(), device_id:connection.device_id, index: plant_index, type: message.plant_type, mood: "born", touch:0 };
	plantsDb.insert(obj,{safe:true},function(err,doc){
			if(err) throw err;

		   var json={ $push: { plants: { index:doc[0].index, _id:doc[0]._id } } };
		   updateDocument(devicesDb,connection.device_id, json);
		   
	});
		
	

	
}

/* ******************************************************************************************* */
/* ******************************************************************************************* */

function plantTouch(message,connection){
	
	//May need to create dual index of device_id & plant index

	var obj = {'device_id': new BSON.ObjectID(String(message.device_id)), 'index': message.plant_index };
	var json = {$inc : {touch: 1}};
	plantsDb.update(obj, json,function(err){
		if(err) throw err;
		//check & respond w mood update.

	});	
	
	obj.timestamp = new Date();
		
	touchesDb.insert(obj, function(err){
		if(err) throw err;
	});
				
	 
				
}

/* ******************************************************************************************* */
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
function updatePlantMood(message,connection) {

	// fetch data from db for given device id
	plantsDb.findOne({ '_id': new BSON.ObjectID(message.device_id) } ,function(err,result) {
		
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

/* ******************************************************************************************* */
/* Generate a response based on the plant's mood 								 			   */
/* ******************************************************************************************* */
function generateResponse(id) {

	// fetch data from db for given device id
	plantsDb.findOne({ '_id': new BSON.ObjectID(id) } ,function(err,result) {

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

