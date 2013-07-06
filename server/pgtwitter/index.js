/* ******************************************************************************************* */
/* Pull in required modules						 								 			   */
/* ******************************************************************************************* */

// var ntwitter = require('ntwitter');
var ImmortalNTwitter = require('immortal-ntwitter');


/* ******************************************************************************************* */
/* Define globals								 								 			   */
/* ******************************************************************************************* */

var twitter;
var callback;

// Set up our handle & hashtags to follow
var handle = 'IncMachinesDev';
var hashtags = ['IncMachinesDev', 'PowerGarden', 'ThePowerGarden'];
var trackString = handle + ',' + hashtags.join(',');

var ustreamLink = 'http://ustre.am/10WaX';

var handleRegex = new RegExp('@'+handle, 'i');
var hashtagRegexes = [];
hashtagRegexes.push( new RegExp('#'+handle, 'i') );
for (var i = 0; i < hashtags.length; i++) {
	hashtagRegexes.push( new RegExp('#'+hashtags[i], 'i') );
}

// Set up keywords to respond to
var waterKeywords = ['rain', 'rained', 'raining', 'water', 'thirsty', 'drink', 'sprinkle', 'sprinkler', 'sprinkers', 'mist', 'misty', 'shower', 'showers'];
// precipitation?

// PLACE MORE SPECIFIC KEYWORDS FIRST SO THEY GET CAUGHT FIRST IN THE WORD SEARCH LOOP
// e.g., try to match "purple carrots" before "carrots"
var plantKeywords = [
	{
		"type": "cherry_tomatoes",
		"emit": "tomatoes",
		"keywords": ["cherry tomato", "cherry tomatoe", "cherry tomatoes"]
	},
	{
		"type": "tomatoes",
		"emit": "cherry_tomatoes",
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
		"type": "purple_carrots",
		"emit": "orange_carrots",
		"keywords": ["purple carrot", "purple carrots", "purple carot", "purple carots"],
	},
	{
		"type": "orange_carrots",
		"emit": "purple_carrots",
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

	// twitter = new ntwitter({
	twitter = ImmortalNTwitter.create({
		// consumer_key: 'bTc9jPplp8SegUtH9EGhTA',
		// consumer_secret: 'Tin9GFVUfqZKVzLCrKRrMAl9Y3TX7IlxiIVRSW0OWU',
		// access_token_key: '1534210819-fSgoQxNsrkY8ORr2t4w6f6jjuQecnY0V8wN5cnm',
		// access_token_secret: 'ZAsYsPhimfghWJZ3xefpGPhEhs5dcUt7G7ylX6k'

		// @mattfelsen "Matt Felsen Development App"
		consumer_key: '7b1GuJx7rk8ZoEoEA296oA',
		consumer_secret: 'Xc5DCVufF4M1dWi19pBPl8XTKCVoHtsmuPrTuc7glA',
		access_token_key: '519944852-xffsFHrqNBXVPPI5aAozIzaqG34PPeHpYu7CBSMv',
		access_token_secret: 'WkT3k0ClPvh8BZ6jeBZAQy2E9iFHmGVxUEKROyV8'
	});

	// Connect!
	// twitter.stream('statuses/filter', { track: trackString, stall_warnings: true }, function(stream) {
	twitter.immortalStream('statuses/filter', { track: trackString, stall_warnings: true }, function(stream) {
		console.log('[TWITTER] Connected');

		// Process with our callback
		stream.on('data', processesTwitterStreamData);

		// Deal with errors
		stream.on('error', function(error, code){
			console.error('[TWITTER] Error! Code: ' + code);
			console.error(error);
			//throw error;
		});
		stream.on('end', function (response) {
			// Handle a disconnection
			console.log('[TWITTER] End:');
			console.log(response);
		});
		stream.on('destroy', function (response) {
			// Handle a 'silent' disconnection from Twitter, no end/error event fired
			console.log('[TWITTER] Destroy:');
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

	// First check if we got a disconnect or a falling behind message
	if (data.disconnect) {
		console.error('[TWITTER] Received disconnect!');
		console.error(data);
		return;
	}
	if (data.warning) {
		console.error('[TWITTER] Received warning!');
		console.error(data);
		return;
	}

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
			//console.log("[TWITTER] Match on regex: " + hashtagRegexes[i]);
			hashtagMatched = true;
			break;
		}
	}
	if (!handleRegex.test(text) && !hashtagMatched) {
		console.log("[TWITTER] No match on handles or hashtags, returning.")
		return;
	}

	//console.log('@'+user + ' ' + text);

	// Check for trigger words
	var foundWaterKeyword = false;
	var foundPlantKeyword = false;
	var mentionedPlants = [];
	var emitPlant = false;

	for (var i = 0; i < waterKeywords.length; i++) {
		// Use whole word regex-matching
		if (new RegExp("\\b" + waterKeywords[i] + "\\b", "i").test(text)) {
			foundWaterKeyword = true;
			console.log('[TWITTER] Water match! ' + waterKeywords[i]);
			break;
		}
	}

	for (var i = 0; i < plantKeywords.length; i++) {
		for (var j = 0; j < plantKeywords[i].keywords.length; j++) {

			// Use whole word regex-matching
			if (new RegExp("\\b" + plantKeywords[i].keywords[j] + "\\b", "i").test(text)) {
				foundPlantKeyword = true;
				mentionedPlants.push(plantKeywords[i].type);
				if (plantKeywords[i].emit) emitPlant = plantKeywords[i].emit;
				console.log('[TWITTER] Plant match! ' + plantKeywords[i].type + ': ' + plantKeywords[i].keywords[j]);

				// break out of inner loop and continue searching for plants
				break;
			}
		}
	}

	// Check if more than one plant was mentioned. If so, set logic back to same as
	// not mentioning any plants, i.e. you're effectively talking to the entire garden
	if (mentionedPlants.length > 1) {
		console.log('Multiple plants detected: ' + mentionedPlants.join(', '));
		mentionedPlants = [];
		emitPlant = false;
	}

	// Build the data we pass to the callback
	var result = {

		water: foundWaterKeyword,
		plants: mentionedPlants.length ? mentionedPlants : false,
		emit: emitPlant,
		id: id,
		user_name: user,
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

	// Check if there's room to attach the ustream link
	// 140 (twitter max) - 1 (space between message and url) - 22 (length of t.co wrapped urls)
	text = text.trim();
	if (text.length <= (140 - 1 - 22)) {
		text += ' ' + ustreamLink;
	}

	twitter.updateStatus(text, options, function (err, data) {
		if (err) {
			console.log('[Post Twitter Update] Error Posting Status:');
			console.log(err);
		}

		//console.log(JSON.stringify(data));
	});

}
exports.updateStatus = updateStatus;