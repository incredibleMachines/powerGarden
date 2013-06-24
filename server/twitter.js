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

var triggers = [
	{
		type: 'sprinklers',
		keywords: ['rain', 'raining', 'water', 'thirsty', 'drink', 'sprinkler', 'sprinkers'],
		responses: ["Thanks so much, I was drying out!", "Thanks, I'm dying over here"]
	}
]

var genericResponses = [
	"Careful, if the tomatoes get too worked-up they're going to fall off their vines! Let's spread the love around a bit.",
	"It's time for the tomatoes to get their beauty rest. Come back soon!",
	"The tomatoes need to focus on growing right now. Can you play with the other veggies for a bit?",
	"Tomatoes love attention, but so do the rest of the veggies! Pay the carrots a visit before they get too jealous...",
	"Looks like you guys are getting along great. I bet the other veggies would love to meet you too!",
	"All of the hustle and bustle can be overwhelming for tomatoes. They need to relax for a bit… be sure to come back later!",
	"Sounds like these tomatoes need to calm down a little. Can you give the other veggies some attention?",
	"The tomatoes are getting tuckered out. Want to play with some other veggies?",
	"Looks like the tomatoes need to mellow out a little. Can you go say \"hi\" to other veggies in the garden?",
	"Any more action and these tomatoes are going to turn into sauce!"
]

var processesStreamData = function(data) {
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
			console.log("Match on regex: " + hashtagRegexes[i]);
			hashtagMatched = true;
			break;
		}
	}
	if (!handleRegex.test(text) && !hashtagMatched) {
		console.log("No match on handles or hashtags, returning.")
		return;
	}

	console.log('@'+user + ' ' + text);

	//
	// Check for trigger words
	//
	var foundTriggerWord = false;
	var response = '';

	triggerLoop:
	for (var i = 0; i < triggers.length; i++) {
		for (var j = 0; j < triggers[i].keywords.length; j++) {

			// Use whole word regex-matching
			if (new RegExp("\\b" + triggers[i].keywords[j] + "\\b", "i").test(text)) {
				foundTriggerWord = true;
				console.log('Match! ' + triggers[i].type + ': ' + triggers[i].keywords[j]);

				// Pull a random response and set up the response string
				var responseIndex = parseInt(Math.random()*triggers[i].responses.length);
				response = triggers[i].responses[responseIndex];
				response = '@'+user + ', ' + response;

				// magic
				break triggerLoop;

			}
		}
	}

	// If a trigger word wasn't found, pull from generic responses
	if (!foundTriggerWord) {
		var responseIndex = parseInt(Math.random()*genericResponses.length);
		response = genericResponses[responseIndex];
		response = '@'+user + ' ' + response;
	}

	// Send out reply
	console.log('Replying: ' + response);
	//postStatus(response, { in_reply_to_status_id: id });

}

var postStatus = function(text, options) {

	options.lat = 41.88251;
	options.long = -87.638771;
	options.place_id = '1d9a5370a355ab0c';
	options.display_coordinates = true;

	twit.updateStatus(text, options, function (err, data) {
		if (err) {
			console.log('ERROR POSTING STATUS');
			console.log(err);
			console.log(data);
		}

		//console.log(JSON.stringify(data));
	});

}

// Pull in ntwitter module
var twitter = require('ntwitter');

// Set up our ntwitter object
var twit = new twitter({

	consumer_key: 'bTc9jPplp8SegUtH9EGhTA',
	consumer_secret: 'Tin9GFVUfqZKVzLCrKRrMAl9Y3TX7IlxiIVRSW0OWU',
	access_token_key: '1534210819-fSgoQxNsrkY8ORr2t4w6f6jjuQecnY0V8wN5cnm',
	access_token_secret: 'ZAsYsPhimfghWJZ3xefpGPhEhs5dcUt7G7ylX6k'

});

// Start stream
twit.stream('statuses/filter', { track: trackString, stall_warnings: true }, function(stream) {
	console.log('Stream connected');

	stream.on('data', processesStreamData);

	stream.on('error', function(error, code){
		console.log('STREAM ERROR: ' + code);
		console.log(error);
		throw error;
	});
	stream.on('end', function (response) {
		// Handle a disconnection
		console.log('STREAM END');
		console.log(response);
	});
	stream.on('destroy', function (response) {
		// Handle a 'silent' disconnection from Twitter, no end/error event fired
		console.log('STREAM DESTROY');
		console.log(response);
	});

});

// Post a test status
// twit.updateStatus('Test tweet from ntwitter/' + twitter.VERSION,
// 	function (err, data) {
// 		console.log(data);
// 	}
// );

// Geo search
// twit.geoSearch({ lat: 41.88251, long: -87.638771, max_results: 10 }, function(err, data) {
// 	if (err) throw err;
// 	console.log(JSON.stringify(data));
// })

// twit.geoReverseGeocode(41.88251, -87.638771, { granularity: 'neighborhood', max_results: 10 }, function(err, data) {
// 	if (err) throw err;
// 	console.log(JSON.stringify(data));
// })