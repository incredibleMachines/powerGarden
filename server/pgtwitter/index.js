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
var handle = 'ThePowerGarden';
var hashtags = ['ThePowerGarden', 'PowerGarden'];
// var handle = 'IncMachinesDev';
// var hashtags = ['IncMachinesDev', 'imgardentest'];
var trackString = handle + ',' + hashtags.join(',');

var ustreamLink = 'http://ustre.am/10WaX';

var handleRegex = new RegExp('@'+handle, 'i');
var hashtagRegexes = [];
hashtagRegexes.push( new RegExp('#'+handle, 'i') );
for (var i = 0; i < hashtags.length; i++) {
	hashtagRegexes.push( new RegExp('#'+hashtags[i], 'i') );
}

// Profanity list, including GMO lawsuit/settlement stuff
var profanity = [
	'legal', 'lawsuit', 'law suit', 'settlement', 'class action', 'file suit', '$9', '$9m', '$9million', 'classaction', 'gmo', 'gmos', 'genetically modified', 'genetic modification', 'modified organism', 'modified foods', 'genetic engineering', '2g1c','2 girls 1 cup','acrotomophilia','anal','anilingus','anus','arsehole','ass','asshole','assmunch','auto erotic','autoerotic','babeland','baby batter','ball gag','ball gravy','ball kicking','ball licking','ball sack','ball sucking','bangbros','bareback','barely legal','barenaked','bastardo','bastinado','bbw','bdsm','beaver cleaver','beaver lips','bestiality','bi curious','big black','big breasts','big knockers','big tits','bimbos','birdlock','bitch','black cock','blonde action','blonde on blonde action','blow j','blow your l','blue waffle','blumpkin','bollocks','bondage','boner','boob','boobs','booty call','brown showers','brunette action','bukkake','bulldyke','bullet vibe','bung hole','bunghole','busty','butt','buttcheeks','butthole','camel toe','camgirl','camslut','camwhore','carpet muncher','carpetmuncher','chocolate rosebuds','circlejerk','cleveland steamer','clit','clitoris','clover clamps','clusterfuck','cock','cocks','coprolagnia','coprophilia','cornhole','cum','cumming','cunnilingus','cunt','darkie','date rape','daterape','deep throat','deepthroat','dick','dildo','dirty pillows','dirty sanchez','dog style','doggie style','doggiestyle','doggy style','doggystyle','dolcett','domination','dominatrix','dommes','donkey punch','double dong','double penetration','dp action','eat my ass','ecchi','ejaculation','erotic','erotism','escort','ethical slut','eunuch','faggot','fecal','felch','fellatio','feltch','female squirting','femdom','figging','fingering','fisting','foot fetish','footjob','frotting','fuck','fuck buttons','fudge packer','fudgepacker','futanari','g-spot','gang bang','gay sex','genitals','giant cock','girl on','girl on top','girls gone wild','goatcx','goatse','gokkun','golden shower','goo girl','goodpoop','goregasm','grope','group sex','guro','hand job','handjob','hard core','hardcore','hentai','homoerotic','honkey','hooker','hot chick','how to kill','how to murder','huge fat','humping','incest','intercourse','jack off','jail bait','jailbait','jerk off','jigaboo','jiggaboo','jiggerboo','jizz','juggs','kike','kinbaku','kinkster','kinky','knobbing','leather restraint','leather straight jacket','lemon party','lolita','lovemaking','make me come','male squirting','masturbate','menage a trois','milf','missionary position','motherfucker','mound of venus','mr hands','muff diver','muffdiving','nambla','nawashi','negro','neonazi','nig nog','nigga','nigger','nimphomania','nipple','nipples','nsfw images','nude','nudity','nympho','nymphomania','octopussy','omorashi','one cup two girls','one guy one jar','orgasm','orgy','paedophile','panties','panty','pedobear','pedophile','pegging','penis','phone sex','piece of shit','piss pig','pissing','pisspig','playboy','pleasure chest','pole smoker','ponyplay','poof','poop chute','poopchute','porn','porno','pornography','prince albert piercing','pthc','pubes','pussy','queaf','raghead','raging boner','rape','raping','rapist','rectum','reverse cowgirl','rimjob','rimming','rosy palm','rosy palm and her 5 sisters','rusty trombone','s&m','sadism','scat','schlong','scissoring','semen','sex','sexo','sexy','shaved beaver','shaved pussy','shemale','shibari','shit','shota','shrimping','slanteye','slut','smut','snatch','snowballing','sodomize','sodomy','spic','spooge','spread legs','strap on','strapon','strappado','strip club','style doggy','suck','sucks','suicide girls','sultry women','swastika','swinger','tainted love','taste my','tea bagging','threesome','throating','tied up','tight white','tit','tits','titties','titty','tongue in a','topless','tosser','towelhead','tranny','tribadism','tub girl','tubgirl','tushy','twat','twink','twinkie','two girls one cup','undressing','upskirt','urethra play','urophilia','vagina','venus mound','vibrator','violet blue','violet wand','vorarephilia','voyeur','vulva','wank','wet dream','wetback','white power','women rapping','wrapping men','wrinkled starfish','xx','xxx','yaoi','yellow showers','yiffy','zoophilia'
];

// Set up keywords to respond to
var waterKeywords = ['rain', 'rains', 'rained', 'raining', 'water', 'waters', 'watered', 'watering', 'thirsty', 'drink', 'drinks', 'drank', 'drinking', 'sprinkle', 'sprinkles', 'sprinkled', 'sprinkling', 'sprinkler', 'sprinklers', 'mist', 'misty', 'mists', 'misted', 'misting', 'shower', 'showers', 'showered', 'showering'];
// precipitation?

// PLACE MORE SPECIFIC KEYWORDS FIRST SO THEY GET CAUGHT FIRST IN THE WORD SEARCH LOOP
// e.g., try to match "purple carrots" before "carrots"
// emit is a plant slug for tablets which should receive plant-specific tweets even
// if they wren't matched
// e.g., send matches for tomatoes to cherry tomatoes as well
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

	var credentials = require('./credentials').credentials;
	// twitter = new ntwitter(credentials);
	twitter = ImmortalNTwitter.create(credentials);

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

	// Check if the stream sent us one of our own tweets. If so return so that we
	// don't wind up an in infinite loop responding to our own tweets
	if (handle == user) {
		console.log('[TWITTER] Matched a tweet from self, exiting: @' + user + ': ' + text);
		return;
	}

	// Ignore retweets
	if (data.hasOwnProperty('retweeted_status')) {
		console.log('[TWITTER] Ignoring retweet: @' + user + ': ' + text);
		return
	}

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

	// Check profanity list
	for (i = 0; i < profanity.length; i++) {
		// Use whole word match regex for things like 'analysis' not testing positive for 'anal'
		if (new RegExp("\\b" + profanity[i] + "\\b", "i").test(text)) {
			return;
		}
	}

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
	callback(result, data);

}

var restart = function() {
	twitter.resurrect();
}
exports.restart = restart;


/* ******************************************************************************************* */
/* Convenience method for posting to twitter									 			   */
/* ******************************************************************************************* */

var updateStatus = function(text, options, callback) {

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


	console.log('[TWITTER] [OUTBOUND] ' + text + ', options: ' + JSON.stringify(options));
	twitter.updateStatus(text, options, function (err, data) {
		if (err) {
			console.log('[TWITTER] Error Posting Status:');
			console.log(err);
		}
		
		callback(data);
		//console.log(JSON.stringify(data));
	});

}
exports.updateStatus = updateStatus;