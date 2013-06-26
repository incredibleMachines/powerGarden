/* ******************************************************************************************* */
/* ******************************************************************************************* */
//update mongo db document
exports.updateDocument = function(collection,id,json){
	console.log("[Updating Document] Collection: "+collection+" id: "+id);
	var obj = { _id : new BSON.ObjectID( String(id) ) };
	collection.update(obj,json,function(err){
		if(err) console.error(err); //throw err;
	});

//no upsert 
}


/* ******************************************************************************************* */
/* ******************************************************************************************* */

exports.logDevice = function(collection,message,connection){
	var obj = {date: new Date(), plants: []};
	collection.insert(obj, {safe:true}, function(err,doc){
		if(err) console.error(err); //throw err;
		
		connection.device_id = doc[0]._id;
		console.log('Created Record: '+connection.device_id);
		console.log("plants.length: "+message.num_plants);
		for(var i = 0; i<message.num_plants; i++) createPlant(message,connection,i);
		
		
		var res = { "device_id": connection.device_id, "connection_id": connection.id };
		//connection.socket.send(JSON.stringify(res));
		connection.socket.emit('connected',res);
	});	
}