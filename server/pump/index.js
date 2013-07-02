// Load the TCP Library
net = require('net');
 
// Keep track of the pumpClients
var pumpClients = [];

// Global timeout for turning off sprinklers
// Necessary so we can replace the timer if multiple turn it on
var turnOffTimer;
 
// Start a TCP Server
net.createServer(function (socket) {
 
  // Identify this client
  socket.name = socket.remoteAddress + ":" + socket.remotePort 
  console.log('Connection: ' + socket.name);
 
  // Put this new client in the list
  pumpClients.push(socket);
  console.log('Clients: ' + pumpClients.length)
 
  // Send a nice welcome message and announce
  // socket.write("Welcome " + socket.name + "\n");
 
  // Handle incoming messages from pumpClients.
  socket.on('data', function (data) {
    console.log(socket.name + ': ' + data);
  });

  socket.on('error', function(error) {
  	if (error.code == "ECONNRESET") {
	    console.log('Disconnection: ' + socket.name);
	    pumpClients.splice(pumpClients.indexOf(socket), 1);
	    console.log('Clients: ' + pumpClients.length)
  	}
  });
 
  // Remove the client from the list when it leaves
  socket.on('end', function () {
    console.log('Disconnection: ' + socket.name);
    pumpClients.splice(pumpClients.indexOf(socket), 1);
    console.log('Clients: ' + pumpClients.length)
  });

 
}).listen(9001);
 
console.log("Server running");

// setInterval(turnOnSprinklers, 10*1000);

function writeToClents(message) {
	if (!pumpClients.length) return;

	for (var i = 0; i < pumpClients.length; i++) {
		console.log("Checking "+ i +" of "+ pumpClients.length);
		var socket = pumpClients[i];

		socket.write(message);
	}
}

exports.turnOnSprinklers = function(_duration) {
	var duration = _duration || 15;
	var message = '1,' + duration * 1000;
	writeToClents(message);
	console.log('['+pumpClients.length+'] Turning on sprinklers...');

	clearTimeout(turnOffTimer);
	turnOffTimer = setTimeout(this.turnOffSprinklers, duration * 1000);
}

exports.turnOffSprinklers = function() {
	var message = '0,0';
	writeToClents(message);
	console.log('['+pumpClients.length+'] Turning off sprinklers!');
}