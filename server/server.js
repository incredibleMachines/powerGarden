/*
//Depreciated
var WebSocketServer = require('ws').Server
  , wss = new WebSocketServer({port: 9000});
*/

/* ******************************************************************************************* */
/* Pull in required modules						 								 			   */
/* ******************************************************************************************* */

var io = require('socket.io').listen(9001);
  
/*
var MongoClient = require('mongodb').MongoClient,
	Server = require('mongodb').Server,
	mongo = new MongoClient(new Server('localhost', 27017)),
	BSON = require('mongodb').BSONPure;
*/

var ntwitter = require('ntwitter');


/* ******************************************************************************************* */
/* Define globals								 								 			   */
/* ******************************************************************************************* */

//var db, dataDb, personalitiesDb, devicesDb, plantsDb, touchesDb;
var clients = {};
var twitter;


/* ******************************************************************************************* */
/* Connect to mongo server, store collection references							 			   */
/* ******************************************************************************************* */

/*
mongo.open(function(err,mongo){
	db = mongo.db('powergarden');
	devicesDb = db.collection('devices');
	plantsDb = db.collection('plants');
	personalitiesDb = db.collection('personalities');
	dataDb = db.collection('data');
	touchesDb = db.collection('touches');
});
*/

var DB = require('./db/dbInterface');
var database = new DB();

/* ******************************************************************************************* */
/* Configure twitter stream & connect											 			   */
/* ******************************************************************************************* */

twitter = new ntwitter({
	consumer_key: 'bTc9jPplp8SegUtH9EGhTA',
	consumer_secret: 'Tin9GFVUfqZKVzLCrKRrMAl9Y3TX7IlxiIVRSW0OWU',
	access_token_key: '1534210819-fSgoQxNsrkY8ORr2t4w6f6jjuQecnY0V8wN5cnm',
	access_token_secret: 'ZAsYsPhimfghWJZ3xefpGPhEhs5dcUt7G7ylX6k'
});

// Set up our handle & hashtags to follow
var twitterHandle = 'IncMachinesDev';
var twitterHashtags = ['IncMachinesDev', 'PowerGarden', 'ThePowerGarden'];
var twitterTrackString = twitterHandle + ',' + twitterHashtags.join(',');

var twitterHandleRegex = new RegExp('@'+twitterHandle, 'i');
var twitterHashtagRegexes = [];
twitterHashtagRegexes.push( new RegExp('#'+twitterHandle, 'i') );
for (var i = 0; i < twitterHashtags.length; i++) {
	twitterHashtagRegexes.push( new RegExp('#'+twitterHashtags[i], 'i') );
}

// Set up keywords to respond to
var twitterWaterKeywords = ['rain', 'rained', 'raining', 'water', 'thirsty', 'drink', 'sprinkler', 'sprinkers', 'mist', 'misty'];
var twitterPlantKeywords = [
	{
		"type": "tomatoes",
		"keywords": ["tomato", "tomatoe", "tomatoes"]
	},
	{
		"type": "peppers",
		"keywords": ["pepper", "peppers", "peper", "pepers"]
	},
	{
		"type": "celery",
		"keywords": ["celery", "celry"],
	},
	{
		"type": "carrot",
		"keywords": ["carrot", "carrots", "carot", "carots"],
	},
	{
		"type": "beets",
		"keywords": ["beet", "beets"],
	},
];

// Connect!
twitter.stream('statuses/filter', { track: twitterTrackString, stall_warnings: true }, function(stream) {
	console.log('[Twitter Stream] Connected');

	// Process with our callback
	stream.on('data', processesTwitterStreamData);

	// Deal with errors
	stream.on('error', function(error, code){
		console.error('[Twitter Stream] Error: ' + code);
		console.error(error);
		//throw error;
	});
	stream.on('end', function (response) {
		// Handle a disconnection
		console.log('[Twitter Stream] End:');
		console.log(response);
	});
	stream.on('destroy', function (response) {
		// Handle a 'silent' disconnection from Twitter, no end/error event fired
		console.log('[Twitter Stream] Destroy:');
		console.log(response);
	});

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
	    database.routeRegister(msg,connection);
	  
	});
	
	socket.on('update', function (msg) {
	    console.log('[Device Update Request]: ', msg);
	    database.routeUpdate(msg,connection);
	  
	});
	
	socket.on('touch',function(msg){
		console.log('[Plant Touch Signaled]', msg);
		database.plantTouch(msg,connection);
		  
	});
	
	socket.on('disconnect', function () {
	  /* io.sockets.emit('user disconnected'); */
	  delete clients["client-"+connection.id];
	  console.log("[DISCONN] connection.id %s",connection.id);
	  console.log("[DISCONN] connection.device_id %s",connection.device_id);
	  //set device to inactive

	});
});

/* ******************************************************************************************* */
/*																							   */
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
function checkPlantMood(message,connection) {

	// fetch data from db for given device id
	plantsDb.findOne({ 'device_id': new BSON.ObjectID(message.device_id), 'index': message.plant_index } ,function(err,result) {
		if (err)  console.error(err); //throw err;
		if (!result) { console.log('[Check Plant Mood] No plant found for given id: ' + message.device_id); return; }
		
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
function generateResponse(message,connection) {

	// fetch data from db for given device id
	plantsDb.findOne({ '_id': new BSON.ObjectID(message.device_id), 'index': message.plant_index  } ,function(err,result) {
		if (err) console.error(err); //throw err;
		if (!result) { console.log('[Generate Response] No plant found for given id: ' + message.device_id); return; }

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


/* ******************************************************************************************* */
/* Various routines around twitter communication 								 			   */
/* ******************************************************************************************* */

//
// Main stream processing handler
//
function processesTwitterStreamData(data) {
	// console.log(data);

	// Make sure necessary data exists before continuing
	if (!data.id_str) return;
	if (!data.text) return;
	if (!data.user) return;
	if (!data.user.screen_name) return;

	// Pull out the data we care about
	var id = data.id_str;
	var text = data.text;
	var user = data.user.screen_name;

	// Make sure there's a user mention or matched hashtag before continuing
	var hashtagMatched = false;
	for (var i = 0; i < twitterHashtagRegexes.length; i++) {
		if (twitterHashtagRegexes[i].test(text)) {
			//console.log("[Twitter Stream] Match on regex: " + twitterHashtagRegexes[i]);
			hashtagMatched = true;
			break;
		}
	}
	if (!twitterHandleRegex.test(text) && !hashtagMatched) {
		console.log("[Twitter Stream] No match on handles or hashtags, returning.")
		return;
	}

	//console.log('@'+user + ' ' + text);

	// Check for trigger words
	var foundWaterKeyword = false;
	var foundPlantKeyword = false;
	var mentionedPlant = '';

	for (var i = 0; i < twitterWaterKeywords.length; i++) {
		// Use whole word regex-matching
		if (new RegExp("\\b" + twitterWaterKeywords[i] + "\\b", "i").test(text)) {
			foundWaterKeyword = true;
			console.log('[Twitter Stream] Water match! ' + twitterWaterKeywords[i]);
			break;
		}
	}

	plantKeywordLoop:
	for (var i = 0; i < twitterPlantKeywords.length; i++) {
		for (var j = 0; j < twitterPlantKeywords[i].keywords.length; j++) {

			// Use whole word regex-matching
			if (new RegExp("\\b" + twitterPlantKeywords[i].keywords[j] + "\\b", "i").test(text)) {
				foundPlantKeyword = true;
				mentionedPlant = twitterPlantKeywords[i].type;
				console.log('[Twitter Stream] Plant match! ' + twitterPlantKeywords[i].type + ': ' + twitterPlantKeywords[i].keywords[j]);

				// magic
				break plantKeywordLoop;
			}
		}
	}

	// Let's figure out how we should reply
	if (!foundWaterKeyword) {

		// Not providing water

		if (!foundPlantKeyword) {
			// No mention of a specific plant
			// Send thanks for attention from garden
			console.log('[Twitter Stream] Thanks for attention from garden');

		} else {
			// User mentioned a specific plant
			// Send thanks for attention from the plant
			console.log('[Twitter Stream] Thanks for attention from ' + mentionedPlant);
		}
	} else {

		// User is providing water

		if (!foundPlantKeyword) {
			// No mention of a specific plant
			// Send thanks for water from garden
			console.log('[Twitter Stream] Thanks for water from garden');

		} else {
			// User mentioned a specific plant
			// Send thanks for water from the plant
			console.log('[Twitter Stream] Thanks for water from ' + mentionedPlant);
		}

	}

	// // If a trigger word wasn't found, pull from generic responses
	// if (!foundTriggerWord) {
	// 	var responseIndex = parseInt(Math.random()*genericResponses.length);
	// 	response = genericResponses[responseIndex];
	// 	response = '@'+user + ' ' + response;
	// }

	// Send out reply
	// console.log('[Twitter Stream] Replying: ' + response);
	//postStatus(response, { in_reply_to_status_id: id });

}


//
// Convenience method for posting to twitter
// Can take a second parameter for options
//
function postTwitterUpdate(text, options) {

	var options = options || {};

	// Set location to our plaza in Chicago
	options.lat = 41.88251;
	options.long = -87.638771;
	options.place_id = '1d9a5370a355ab0c';
	options.display_coordinates = true;

	twitter.updateStatus(text, options, function (err, data) {
		if (err) {
			console.log('[Post Twitter Update] Error Posting Status:');
			console.log(err);
			console.log(data);
		}

		//console.log(JSON.stringify(data));
	});

}
