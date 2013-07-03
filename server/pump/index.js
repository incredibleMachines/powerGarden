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
  console.log('[PUMP] Connection: ' + socket.name);
 
  // Put this new client in the list
  pumpClients.push(socket);
  console.log('[PUMP] Number of clients: ' + pumpClients.length)
 
  // Send a nice welcome message and announce
  // socket.write("Welcome " + socket.name + "\n");
 
  // Handle incoming messages from pumpClients.
  socket.on('data', function (data) {
    console.log('[PUMP] data: ' + socket.name + ': ' + data);
  });

  socket.on('error', function(error) {
  	if (error.code == "ECONNRESET") {
	    console.log('[PUMP] Error: Disconnection: ' + socket.name);
	    pumpClients.splice(pumpClients.indexOf(socket), 1);
	    console.log('[PUMP] Number of clients: ' + pumpClients.length)
  	}
  });
 
  // Remove the client from the list when it leaves
  socket.on('end', function () {
    console.log('[PUMP] End: Disconnection: ' + socket.name);
    pumpClients.splice(pumpClients.indexOf(socket), 1);
    console.log('[PUMP] Number of clients: ' + pumpClients.length)
  });

 
}).listen(9001);
 
console.log("[PUMP] Server running");

// setInterval(turnOnSprinklers, 10*1000);

function writeToClents(message) {
	if (!pumpClients.length) return;

	for (var i = 0; i < pumpClients.length; i++) {
		// console.log("[PUMP] Writing, checking index "+ i +" of length "+ pumpClients.length);
		var socket = pumpClients[i];

		socket.write(message);
	}
}

exports.turnOnSprinklers = function(_duration) {
	var duration = _duration || 15;
	var message = '1,' + duration * 1000;
	writeToClents(message);
	console.log('[PUMP] ['+pumpClients.length+'] Turning on sprinklers...');

	clearTimeout(turnOffTimer);
	turnOffTimer = setTimeout(this.turnOffSprinklers, duration * 1000);
}

exports.turnOffSprinklers = function() {
	var message = '0,0';
	writeToClents(message);
	console.log('[PUMP] ['+pumpClients.length+'] Turning off sprinklers!');
}