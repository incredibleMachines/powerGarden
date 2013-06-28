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
	settingsDb = db.collection('settings');
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

// return plants for a given device
app.get('/plants', function(req, res) {

	var device_id = BSON.ObjectID(req.query.device_id);

	plantsDb.find({ device_id: device_id }).toArray(function(err, result) {
		res.send(result);
	});

});

// return settings for a for a given device
app.get('/settings', function(req, res) {

	var device_id = BSON.ObjectID(req.query.device_id);

	settingsDb.find({ device_id: device_id }).toArray(function(err, result) {
		res.send(result);
	});

});

// make changes to db
app.get('/update', function(req, res) {

	// console.log('[Update Request] ' + JSON.stringify(req.query));

	var device_id = BSON.ObjectID(req.query.device_id);
	var set = {};

	if (!req.query.sensor) {

		// if there's no sensor, then we're just toggling whether a device is active
		// { device_id: "51ccbb988bce5acc05000001", active: false }
		set['active'] = req.query.active == 'true' ? true : false;

	} else {

		// otherwise we're setting a sensor property. check if it's whether we're toggling
		// the active state vs. setting a property value
		if (req.query.active) {
			// { device_id: "51ccbb988bce5acc05000001", sensor: "distance", active: false }
			// assemble the key want to set, e.g. humidity.active
			var setKey = req.query.sensor+'.active';
			set[setKey] = req.query.active == 'true' ? true : false;
		} else {
			// { device_id: "51ccbb988bce5acc05000001", sensor: "distance", property: "low", val: "35" }
			// assemble the key want to set, e.g. distance.low or distance.high
			var setKey = req.query.sensor+'.'+req.query.property;
			set[setKey] = req.query.val;
		}

	}
	
	var obj = { $set: set }
	// console.log(obj);

	if (!req.query.sensor) {
		// console.log('updating devices');
		devicesDb.update({ _id: device_id }, obj, function(err, result) {
			if (err) console.error(err);
			res.send(obj);
		});
	} else {
		// console.log('updating sensors');
		settingsDb.update({ device_id: device_id }, obj, function(err, result) {
			if (err) console.error(err);
			res.send(obj);
		});
	}


})

app.listen(8080);