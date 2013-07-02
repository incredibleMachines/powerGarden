var ps = require('./pumpServer');
var pgt = require('./pgtwitter/index');


pgt.start(twitterCallback);

function twitterCallback(data) {
	if (data.text.indexOf('on') > -1) {
		ps.turnOnSprinklers();
	}
	if (data.text.indexOf('off') > -1) {
		ps.turnOffSprinklers();
	}
}