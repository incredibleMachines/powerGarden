var MongoClient = require('mongodb').MongoClient,
	Server = require('mongodb').Server,
	mongo = new MongoClient(new Server('localhost', 27017)),
	BSON = require('mongodb').BSONPure;

mongo.open(function(err,mongo){
	db = mongo.db('powergarden');
	devicesDb = db.collection('devices');
	plantsDb = db.collection('plants');
	personalitiesDb = db.collection('personalities');
	dataDb = db.collection('data');
	touchesDb = db.collection('touches');
});

console.log('running');
setInterval(insertData, 60000);


var c=0;
var base={};
function insertData(){
		
	if(!c++){
		console.log("Initializing Vars");
		base.humidity = Math.round(map(Math.random(), 0,1, 40,100));
		base.temp = Math.round(map(Math.random(), 0, 1, 70,100));
		base.light = Math.round(map(Math.random(), 0,1, 0,1000));
		base.moisture = Math.round(map(Math.random(), 0,1, 0,1024));
	}else{
		
		base.humidity += map(Math.random(), 0,1, -1,1);
		base.temp += map(Math.random(), 0,1, -1,1);
		base.light += map(Math.random(), 0,1, -5,5);
		base.moisture+= map(Math.random(), 0,1, -10,10);
		
	}
	
	base.humidity = Math.round(base.humidity*10)/10;
	base.temp = Math.round(base.temp*10)/10;
	base.light = Math.round(base.light*10)/10;
	base.moisture = Math.round(base.moisture*10)/10;
	
	base.device_id = new BSON.ObjectID("51c88d7e8bfc736ca9000001");
	base.timestamp = new Date();
	console.log(base);
	if(base._id)delete base._id;	
	dataDb.insert(base, {safe:true}, function(err,doc){ 
		//console.log(base); if(err)throw err;	
	
	});
}

function map(x, in_min, in_max, out_min, out_max)
{
  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}