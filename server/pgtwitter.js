/* ******************************************************************************************* */
/* Pull in required modules						 								 			   */
/* ******************************************************************************************* */

var ntwitter = require('ntwitter');


/* ******************************************************************************************* */
/* Define globals								 								 			   */
/* ******************************************************************************************* */

var twitter;
var callback;

// Set up our handle & hashtags to follow
var handle = 'IncMachinesDev';
var hashtags = ['IncMachinesDev', 'PowerGarden', 'ThePowerGarden'];
var trackString = handle + ',' + hashtags.join(',');

var handleRegex = new RegExp('@'+handle, 'i');
var hashtagRegexes = [];
hashtagRegexes.push( new RegExp('#'+handle, 'i') );
for (var i = 0; i < hashtags.length; i++) {
	hashtagRegexes.push( new RegExp('#'+hashtags[i], 'i') );
}

// Set up keywords to respond to
var waterKeywords = ['rain', 'rained', 'raining', 'water', 'thirsty', 'drink', 'sprinkler', 'sprinkers', 'mist', 'misty'];
var plantKeywords = [
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
		"type": "carrots",
		"keywords": ["carrot", "carrots", "carot", "carots"],
	},
	{
		"type": "beets",
		"keywords": ["beet", "beets"],
	},
];

/* ******************************************************************************************* */
/* In the constructor, configure twitter stream & connect 									   */
/* ******************************************************************************************* */

var start = function(_callback) {

	callback = _callback;

	twitter = new ntwitter({
		consumer_key: 'bTc9jPplp8SegUtH9EGhTA',
		consumer_secret: 'Tin9GFVUfqZKVzLCrKRrMAl9Y3TX7IlxiIVRSW0OWU',
		access_token_key: '1534210819-fSgoQxNsrkY8ORr2t4w6f6jjuQecnY0V8wN5cnm',
		access_token_secret: 'ZAsYsPhimfghWJZ3xefpGPhEhs5dcUt7G7ylX6k'
	});

	// Connect!
	twitter.stream('statuses/filter', { track: trackString, stall_warnings: true }, function(stream) {
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

}
exports.start = start;


/* ******************************************************************************************* */
/* Main stream processing handler 												 			   */
/* ******************************************************************************************* */

var processesTwitterStreamData = function(data) {
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
	for (var i = 0; i < hashtagRegexes.length; i++) {
		if (hashtagRegexes[i].test(text)) {
			//console.log("[Twitter Stream] Match on regex: " + hashtagRegexes[i]);
			hashtagMatched = true;
			break;
		}
	}
	if (!handleRegex.test(text) && !hashtagMatched) {
		console.log("[Twitter Stream] No match on handles or hashtags, returning.")
		return;
	}

	//console.log('@'+user + ' ' + text);

	// Check for trigger words
	var foundWaterKeyword = false;
	var foundPlantKeyword = false;
	var mentionedPlants = [];

	for (var i = 0; i < waterKeywords.length; i++) {
		// Use whole word regex-matching
		if (new RegExp("\\b" + waterKeywords[i] + "\\b", "i").test(text)) {
			foundWaterKeyword = true;
			console.log('[Twitter Stream] Water match! ' + waterKeywords[i]);
			break;
		}
	}

	for (var i = 0; i < plantKeywords.length; i++) {
		for (var j = 0; j < plantKeywords[i].keywords.length; j++) {

			// Use whole word regex-matching
			if (new RegExp("\\b" + plantKeywords[i].keywords[j] + "\\b", "i").test(text)) {
				foundPlantKeyword = true;
				mentionedPlants.push(plantKeywords[i].type);
				console.log('[Twitter Stream] Plant match! ' + plantKeywords[i].type + ': ' + plantKeywords[i].keywords[j]);

				// break out of inner loop and continue searching for plants
				break;
			}
		}
	}

	// Build the data we pass to the callback
	var result = {

		water: foundWaterKeyword,
		plants: mentionedPlants.length ? mentionedPlants : false,
		id: id,
		user: user,
		text: text,

	};

	// Finally call the callback and pass our data
	callback(result);

}


/* ******************************************************************************************* */
/* Convenience method for posting to twitter									 			   */
/* ******************************************************************************************* */

var updateStatus = function(text, options) {

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
exports.updateStatus = updateStatus;