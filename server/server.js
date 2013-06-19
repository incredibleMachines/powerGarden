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

var db, dataDb, personalitiesDb, devicesDb, plantsDb;
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

/* Depreciated
  socket.on('message', function (msg) {
    console.log('I received a message: ', msg);
    //var json = JSON.parse(msg);
    //console.log(JSON.stringify(json));
    //checkType(msg, connection);
	console.log(msg);
  
  });
*/
  socket.on('register', function (msg) {
    console.log('[Device Register Request]: ', msg);
    
    routeRegister(msg,connection);
    //console.log(typeof msg);
    //var json = JSON.parse(msg);
    //console.log(JSON.stringify(json));
    //checkType(msg, connection);
  
  });
  socket.on('update', function (msg) {
    console.log('[Device Update Request]: ', msg);
    
    routeUpdate(msg,connection);
    //var json = JSON.parse(msg);
    //console.log(JSON.stringify(json));
    //checkType(msg, connection);
  
  });

  socket.on('disconnect', function () {
    /* io.sockets.emit('user disconnected'); */
    console.log("[DISCONN] connection.id %s",connection.id);
	console.log("[DISCONN] connection.device_id %s",connection.device_id);
  });
});

/* ******************************************************************************************* */
//Depreciated
/* ******************************************************************************************* */


/*
wss.on('connection', function(socket) {
	//Create unique connection OBJECT
	var connection = new Connection( ++clientID, 'set_id', socket);
	var connectKey = 'client-'+clientID;
	clients[connectKey]=connection;

	console.log("[NEW CONN] connection.id %s",connection.id);
	console.log("[NEW CONN] connection.device_id %s",connection.device_id);
	//When socket recieves message
    socket.on('message', function(message) {
        
        var json = JSON.parse(message);
        console.log(JSON.stringify(json));

    	checkType(json, connection);
    	
        //socket.send(JSON.stringify(res));
    });

    socket.on('close',function(){
    	console.log("[CLOSE CONN] connection.id=%s",connection.id);
    	
    	//need somethign to keep track of indexs within 
    	//the array splice and removal
    	delete clients["client-"+connection.id];
    	//clients.splice(index, 1);
    });
    
    socket.on('error',function(err){
	    
	    if(err) throw err;
	    
    });
    
    var obj = {"status" : "device_connected", "data":{ "connection_num": connection.id }};

    socket.send(JSON.stringify(obj));
});
*/

/* ******************************************************************************************* */
//Depreciated
/* ******************************************************************************************* */

/*
function checkType(message, connection){

	//hold
	switch(message.type){
		case 'connect':
			console.log('[Recieved Incoming %s] Connection Request connection.id=%s',message.type, connection.id);
			routeConnect(message,connection);
		break;
		case 'update':
			//console.log("got update device request");
			console.log('[Recieved Incoming %s] Device ID: message.device_id=%s ',message.type,message.device_id);
			routeUpdate(message,connection);		
		break;
		case 'touch':
			return 'recieved';
		break;
	}
	

}
*/

/* ******************************************************************************************* */
/* ******************************************************************************************* */

function routeUpdate(message,connection){
	
	if(message.device_id != connection.device_id) { 
		console.log('[Update Error] message.device_id=%s connection.device_id=%s', message.device_id, connection.device_id );/*error error;*/ 
	}else{
		
		if(message.data) console.log("GOT MESSAGE.DATA :: "+JSON.stringify(message.data));
		
		if(message.plants){
			
			//check plants id
			//register plant 
			//update plant vals
			for(var i=0; i<message.plants.length;i++){
				if(message.plants[i].id.length==24){
					
					var json = { $inc: {'touch.count':message.plants[i].touch.count , 'touch.length':message.plants[i].touch.length },
								 $set: {'mood':message.plants[i].mood, 'type':message.plants[i].type, 'index':message.plants[i].index}
								 
								};	
					updateDocument(plantsDb,message.plants[i].id,json);
					
				}//else we need an update to the plant
			}			 
			
		}//end plants
		
		if(message.data){
			
			var obj = {	'device_id':message.device_id, 
						'moisture':message.data.moisture, 
						'temp': message.data.temp, 
						'humidity':message.data.humidity, 
						'light': message.data.light,
						'timestamp': new Date()
					};
					
			if(message.plants){
				var arr=new Array();
				for(var i=0; i<message.plants.length;i++){
					arr[i] = {'_id':message.plants[i].id, 'touch':{ 'count': message.plants[i].touch.count, 'length': message.plants[i].touch.length }};
				}
				obj['plants']=arr;
				
			}
			dataDb.insert(obj, {safe:true}, function(err,doc){
				if(err) throw err;
				
				//check the last values and determine mood of plant 
				//update plants/device if necessary
			
			});
			
			
		}
		
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
	var obj = {created: new Date(), device_id:connection.device_id, index: plant_index, type: message.plant_type, mood: "born", touch:{ count:0, length:0} };
	plantsDb.insert(obj,{safe:true},function(err,doc){
			if(err) throw err;
/*
			var res = {	//"status": "planted", 
						"device_id": connection.device_id, 
						"connection_id": connection.id, 
						"plant":{"id": doc[0]._id, "type":doc[0].type, "index":doc[0].index , "mood":doc[0].mood } 
		   };
		   connection.socket.emit('planted', res);
*/	
		   //connection.socket.send(JSON.stringify(res));
		   
		   //var something= "text";
		   var json={ $push: { plants: { index:doc[0].index, _id:doc[0]._id } } };
		   updateDocument(devicesDb,connection.device_id, json);
		   
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

