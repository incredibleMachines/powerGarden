/* ******************************************************************************************* */
/* Pull in required modules						 								 			   */
/* ******************************************************************************************* */

var ntwitter = require('ntwitter');


/* ******************************************************************************************* */
/* Define globals								 								 			   */
/* ******************************************************************************************* */

var twitter;
var callback;

/* ******************************************************************************************* */
/* In the constructor, configure twitter stream & connect 									   */
/* ******************************************************************************************* */
// function PGTwitter() {

	//this.processesTwitterStreamData;
	//this.postTwitterUpdate;



var start = function(_callback) {

	callback = _callback;

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

}
exports.start = start;
// }
// module.exports = PGTwitter;

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
	var mentionedPlant = false;

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

	var obj = {

		water: foundWaterKeyword,
		plants: mentionedPlant || false,
		id: id,
		user: user,
		text: text,

	};
	callback(obj);

	// Let's figure out how we should reply
	// if (!foundWaterKeyword) {

	// 	// Not providing water

	// 	if (!foundPlantKeyword) {
	// 		// No mention of a specific plant
	// 		// Send thanks for attention from garden
	// 		console.log('[Twitter Stream] Thanks for attention from garden');

	// 	} else {
	// 		// User mentioned a specific plant
	// 		// Send thanks for attention from the plant
	// 		console.log('[Twitter Stream] Thanks for attention from ' + mentionedPlant);
	// 	}
	// } else {

	// 	// User is providing water

	// 	if (!foundPlantKeyword) {
	// 		// No mention of a specific plant
	// 		// Send thanks for water from garden
	// 		console.log('[Twitter Stream] Thanks for water from garden');

	// 	} else {
	// 		// User mentioned a specific plant
	// 		// Send thanks for water from the plant
	// 		console.log('[Twitter Stream] Thanks for water from ' + mentionedPlant);
	// 	}

	// }

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


/* ******************************************************************************************* */
/* Convenience method for posting to twitter									 			   */
/* ******************************************************************************************* */

var postTwitterUpdate = function(text, options) {

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
