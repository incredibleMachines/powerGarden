var twitter = require('./twitter');
var server = require('./server');

server.setTwitterRef(twitter);
twitter.setServerRef(server);

twitter.startStream();
// twitter.postStatus((new Date()).toISOString());