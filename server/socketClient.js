var io = require('socket.io-client');
var socket = io.connect('localhost', { port: 9000 });

var device_id = 'set_id';

socket.on('connect', function (data) {
	console.log('connected');
	socket.emit('register', { device_id: device_id, plant_type: 'cherry_tomatoes', num_plants: 8 });

	// setTimeout(sendChorusOn, 5000);
	setInterval(sendTablet, 3000);
});

socket.on('register', function (data) {
	console.log('register', data);
	device_id = data.device_id;
});

socket.on('chorus', function (data) {
	console.log('chorus', data);
	if (data.start_time != false) {
		sendChorusOn();
	} else {
		sendChorusOff();
	}
});

socket.on('message', function (data) {
	console.log(data);
});
socket.on('disconnect', function (data) {
	console.log(data);
});

function sendChorusOn() {
	socket.emit('chorus', { device_id: device_id, curr_time: new Date() });
	setTimeout(sendChorusOff, 5000);
}

function sendChorusOff() {
	socket.emit('chorus', { device_id: device_id, curr_time: false });
}

function sendTablet() {
	socket.emit('tablet', { device_id: device_id, volume: Math.random()*100, brightness: Math.random()*100, battery_status: Math.random()*100  });
}