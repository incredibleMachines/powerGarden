var http = require('http');
var nodemailer = require('nodemailer');

// create reusable transport method (opens pool of SMTP connections)
var transport = nodemailer.createTransport("SMTP", {
	service: "Gmail",
	auth: {
		user: "powergarden_tablets@incrediblemachines.net",
		pass: "$0^^Uc4P0w34"
	}
});

// set up e-mail
var mailOptions = {
	from: "The Power Garden <powergarden@incrediblemachines.net>",
	to: "The Power Garden <powergarden@incrediblemachines.net>",
}

// set up ustream request
var ustreamOptions = {
	host: 'api.ustream.tv',
	path: '/json/channel/the-power-garden/getValueOf/status?key=5D5BD550FFC27185E8E039EF1127C604'
};

var lastStatus = '';

callback = function(response) {
	var str = '';

	response.on('data', function (chunk) {
		str += chunk;
	});

	response.on('end', function () {
	    var obj = JSON.parse(str);

	    if (!obj.results) {
	    	// No results key in object. Just return.
	    	return;
	    }

	    var d = new Date();
	    // console.log('[USTREAM] ['+d.toLocaleTimeString()+'] ' + obj.results);

		if (obj.results != lastStatus) {

			console.log('[USTREAM] ['+d.toLocaleTimeString()+'] Updating status to: ' + obj.results);

			if (lastStatus == '') {
				// Check if this is the first run and set email properties
				mailOptions.subject = "Ustream now being monitored";
				mailOptions.text = "The Ustream channel is now being monitored and is currently "+obj.results+".\n\nCheck here to view: http://ustre.am/10WaX";
			} else {
				// Set email properties
				mailOptions.subject = "Ustream is " + obj.results;
				mailOptions.text = "The Ustream channel is now "+obj.results+".\n\nCheck here to view: http://ustre.am/10WaX";
			}

			// send mail
			transport.sendMail(mailOptions, function(error, response){
				if (error) {
					console.log(error);
				} else {
					console.log('[USTREAM] ['+d.toLocaleTimeString()+'] Notification email sent: ' + response.message);
				}
			});

			// Update last status
			lastStatus = obj.results;
		}
	});
}

pingDaStream = function() {
	http.request(ustreamOptions, callback).end();
}

setInterval(pingDaStream, 60 * 1000);
pingDaStream();