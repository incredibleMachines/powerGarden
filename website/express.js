var MongoClient = require('mongodb').MongoClient,
	Server = require('mongodb').Server,
	mongo = new MongoClient(new Server('localhost', 27017)),
	BSON = require('mongodb').BSONPure;
	
	
mongo.open(function(err,mongo){
	if (err) console.error(err);
	
	db = mongo.db('powergarden');
	devicesDb = db.collection('devices');
	plantsDb = db.collection('plants');
	personalitiesDb = db.collection('personalities');
	dataDb = db.collection('data');
	touchesDb = db.collection('touches');
});



var express = require('express');
var app = express();

// serve files from filesystem
app.use(express.static(__dirname));

// return all devices
app.get('/devices', function(req, res) {

	devicesDb.find().toArray(function(err, result) {
		res.send(result);
	});

});

// return devices for a given plant
app.get('/plants', function(req, res) {

	var device_id = BSON.ObjectID(req.query.device_id);

	plantsDb.find({ device_id: device_id }).toArray(function(err, result) {
		res.send(result);
	});

});

app.listen(8080);