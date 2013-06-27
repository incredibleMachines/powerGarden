var MongoClient = require('mongodb').MongoClient,
	Server = require('mongodb').Server,
	mongo = new MongoClient(new Server('localhost', 27017)),
	BSON = require('mongodb').BSONPure;
	
	
mongo.open(function(err,mongo){
	
	if(err) console.error(err);
	
	db = mongo.db('powergarden');
	devicesDb = db.collection('devices');
	plantsDb = db.collection('plants');
	personalitiesDb = db.collection('personalities');
	dataDb = db.collection('data');
	touchesDb = db.collection('touches');
});


setInterval(simulateTouch, 1000 );

var devices = [	"51ca511efaea9671b1000037", 				
				"51ca5129faea9671b1000040",
				"51ca513dfaea9671b1000049",
				"51ca514afaea9671b1000052",
				"51ca5153faea9671b100005b",
				"51ca515bfaea9671b1000064",
				"51ca5166faea9671b100006d",
				"51ca517afaea9671b1000076"];
				
function simulateTouch(){
	
	
	var obj ={};
	var randDevice = devices[Math.round(Math.random()*devices.length)];
	//obj.device_id = new BSON.ObjectID(randDevice);
	obj.device_id = new BSON.ObjectID("51cb6279f8f5b226b7000013");
	//obj.index = Math.round(Math.random()*8);
	obj.index =2;

	obj.timestamp = new Date();
	touchesDb.insert(obj, {safe:true}, function(err,res){ if(err)console.error(err); });
	console.log(obj);
	
};