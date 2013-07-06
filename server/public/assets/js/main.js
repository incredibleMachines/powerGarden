$(document).ready(function() {

var deviceSettings = {};

// Connect to socket.io server & set up event handlers
var socket = io.connect('http://localhost:8080');

// init gets sent for each tablet device when the browser connects
socket.on('init', function(data){
	populate(socket, data);
});

// when a tablet connects to the server. same format as above.
socket.on('device_conn', function(data){
	populate(socket, data);
});
socket.on('settings', function(data) {
	buildSettings(data);
})

// when a tablet disconnects
socket.on('device_disconn',function(data){
	// remove table rows for device & the sub-data (plants & settings)
	$('tr#'+data.device_id).remove();
	$('tr#'+data.device_id+'-plants').remove();
	for(var i = 0; i < connections.length;i++){
		if(connections[i]== data.device_id){
			connections.splice(i,1);
			break;
		}
	}
});

socket.on('firehose',function(data){
	var $elem = $('button.firehose.client-'+data.connection_id);
	if(data.stream) {
		$elem.addClass('btn-warning');
		$elem.children('i').addClass('icon-fire').removeClass('icon-off');
	} else {
		// $(this).html('firehose Off');
		$elem.removeClass('btn-warning');
		$elem.children('i').addClass('icon-off').removeClass('icon-fire');
	}
});

socket.on('ignore',function(data){
	var $elem = $('button.plant-'+data.plant_index+'.disablePlant.client-'+data.connection_id);
	if(data.ignore) {
		// $(this).html('Disable');
		$elem.addClass('btn-danger').removeClass('btn-success');
		$elem.children('i').addClass('icon-remove').removeClass('icon-ok');
	} else {
		// $(this).html('Enable');
		$elem.addClass('btn-success').removeClass('btn-danger');
		$elem.children('i').addClass('icon-ok').removeClass('icon-remove');
	}
	$elem.data('ignore',data.ignore);
	
});

socket.on('threshold',function(data){
	if(data.type=='cap'){
		var input = $('input.plant-'+data.plant_index+'.cap.client-'+data.connection_id);
		input.val(data.value);
		// input.tooltip('destroy').tooltip({ animation: false, title: data.value }).tooltip('show');
		$('td.plant-'+data.plant_index+'.value.client-'+data.connection_id).html(data.value)
	}
});

socket.on('update',function(data){
	var device_id = data.device_id;
	var readings = data.data;

	for (var key in readings) {
		if (key == 'timestamp') continue;
		$('#'+device_id+'-plants .sensor-table .reading.'+key).html(readings[key]);
	}

});

socket.on('stream',function(data){
	$.each(data.cap_val, function(key, val){	
		$('.plant-'+key+'.incoming.client-'+data.connection_id).html(val);
	});
	
});

socket.on('disconnect',function(data){
	$('.device-table > tbody').html('');
});

var connections = [];

function populate(socket, data){
	
	if( $.inArray(data.device_id, connections) == -1 ) {
		connections.push(data.device_id);
		var deviceFirehose = false;

		// GENERATE TABLE ROW FOR DEVICE
		if (deviceFirehose) {
			var activeButton = '<button class="btn-mini btn-danger firehose client-'+data.connection_id+'"><i class="icon-fire"></i></button>';
		} else {
			var activeButton = '<button class="btn-mini firehose client-'+data.connection_id+'"><i class="icon-off"></i></button>';
		}

		var append = '<tr id="'+data.device_id+'" data-connection="'+data.connection_id+'" data-device="'+data.device_id+'"><td>'+activeButton+'</td><td>'+data.device_id+'</td><td>'+data.plants.length+'</td><td>'+data.type+'</td><td>'+data.mood.touches+'</td><td>'+data.mood.moisture+'</td><td>'+(new Date(data.date)).toLocaleString()+'</td></tr>';
		$('.device-table > tbody').append(append);



		// GENERATE TABLE ROW WHICH HOLDS DEVICE-RELATED DATA
		// embedded table for plants
		// embedded table for settings

		// GENERATE TABLE FOR PLANTS

		// the actual table row that appended to the main devices table, in which our two tables are placed
		var commonPre = '<tr id="'+data.device_id+'-plants" class="client-'+data.connection_id+'" data-connection="'+data.connection_id+'" data-device="'+data.device_id+'" style="display: none;"><td></td><td colspan="6">';
		var commonPost = '</td></tr>';

		// table header and closing tags
		var plantPre = '<table class="table plant-table"><thead><tr><th width="5%">Active</th><th width="20%">Plant ID</th><th width="7%">Index</th><th width="11%">Mood</th><th width="8%">Touch</th><th width="22%">Cap Slider</th><th width="7%">Threshold</th><th width="20%">Incoming</th></tr></thead><tbody>';
		var plantPost = '</tbody></table>';

		// loop through results and generate a new table row for each
		var plantString = '';
		console.log(data.plants);
		for (var i = 0; i < data.plants.length; i++) {
			var j = data.plants[i].index;
			plantString += '<tr data-connection="'+data.connection_id+'" data-device="'+data.device_id+'"><td><button class="btn-mini btn-success plant-'+j+' disablePlant client-'+data.connection_id+'" data-ignore="false" data-plant_index="'+j+'"><i class="icon-ok"></i></button></td><td>'+data.plants[i]._id+'</td><td>'+j+'</td><td>TODO</td><td>TODO</td><td><input type="range" min="0" max="15000" step="1" class="plant-'+j+' cap client-'+data.connection_id+'" data-plant_index="'+j+'"></td><td class="plant-'+j+' value client-'+data.connection_id+'"></td><td class="plant-'+j+' incoming client-'+data.connection_id+'"></td></tr>';
		}


		// build it and append it
		var string = commonPre + plantPre + plantString + plantPost + /* settingsPre + settingsString + settingsPost + */ commonPost;
		$('tr#'+data.device_id).after(string);


		$('input.cap.client-'+data.connection_id).change(function() {
			var connection = $(this).parents('tr').data('connection');
			var device = $(this).parents('tr').data('device');
			var plant = $(this).data('plant_index');
			var val = $(this).val();
			var json = { value: val, type:'cap', connection_id: connection, device_id:device, plant_index: plant};

			socket.emit('threshold', json);
			$('.plant-'+plant+'.value.client-'+connection).html(val);
			// $(this).tooltip('destroy').tooltip({ animation: false, title: val }).tooltip('show');

		});

		$('button.firehose.client-'+data.connection_id).click(function(){ 
			deviceFirehose = !deviceFirehose;

			if(deviceFirehose) {
				// $(this).html('firehose On');
				$(this).addClass('btn-warning');
				$(this).children('i').addClass('icon-fire').removeClass('icon-off');
			} else {
				// $(this).html('firehose Off');
				$(this).removeClass('btn-warning');
				$(this).children('i').addClass('icon-off').removeClass('icon-fire');
			}

			var connection = $(this).parents('tr').data('connection');
			var device = $(this).parents('tr').data('device');
			socket.emit('firehose', {device_id:device, connection_id:connection, stream:deviceFirehose });
			return false;
		});

		$('button.disablePlant.client-'+data.connection_id).on("click",function(){
			var index = $(this).data('plant_index');
			var bool = $(this).data('ignore');

			bool = !bool;
			$(this).data('ignore',bool);

			var connection = $(this).parents('tr').data('connection');
			var device = $(this).parents('tr').data('device');

			if(bool) {
				// $(this).html('Disable');
				$(this).addClass('btn-danger').removeClass('btn-success');
				$(this).children('i').addClass('icon-remove').removeClass('icon-ok');
			} else {
				// $(this).html('Enable');
				$(this).addClass('btn-success').removeClass('btn-danger');
				$(this).children('i').addClass('icon-ok').removeClass('icon-remove');
			}
			
			socket.emit('ignore', {	device_id:device, connection_id:connection, plant_index:index, ignore:bool});

			
		});	
	}
}

function buildSettings(data) {


	// GENERATE TABLE FOR SENSORS

	// table header and closing tags
	var settingsPre = '<table class="table table-condensed sensor-table"><thead><tr><th>Active</th><th>Property</th><th>Reading</th><th>Low Threshold</th><th>High Threshold</th><th>Window</th></tr></thead><tbody>';
	var settingsPost = '</tbody></table>';

	// expected result is a list with only one element, so grab just the first one
	// loop through the object keys and generate table rows
	var settingsString = '';
	var d = data;
	for (var key in d) {

		// ignore keys that aren't objects, e.g. _id and device_id which are strings
		if (typeof(d[key]) != 'object') continue;

		// generate code for whether or not we're active
		if (d[key].active) {
			var activeButton = '<button class="btn-mini btn-success toggle-sensor-active" data-sensor="'+key+'""><i class="icon-ok"></i></button>';
		} else {
			var activeButton = '<button class="btn-mini btn-danger toggle-sensor-active" data-sensor="'+key+'""><i class="icon-remove"></i></button>';
		}

		if (d[key].hasOwnProperty('high')) {
			var highString = '<input class="input-small" type="text" name="high" placeholder="high" value="'+d[key].high+'" data-sensor="'+key+'">';
		} else {
			var highString = '';
		}

		if (d[key].hasOwnProperty('window')) {
			var windowString = '<input class="input-small" type="text" name="window" placeholder="window" value="'+d[key].window+'" data-sensor="'+key+'">';
		} else {
			var windowString = '';
		}


		settingsString += '<tr><td>'+activeButton+'</td><td>'+key+'</td><td class="'+key+' reading"></td><td><input class="input-small" type="text" name="low" placeholder="low" value="'+d[key].low+'" data-sensor="'+key+'"></td><td>'+highString+'</td><td>'+windowString+'</td></tr>'
	}

	var append = settingsPre + settingsString + settingsPost;

	$('#'+d.device_id+'-plants .sensor-table').remove();
	$('#'+d.device_id+'-plants .plant-table').after(append);

	// store data in global var. get rid of extraneous stuff
	delete data['_id'];
	deviceSettings[data.device_id] = data;

}


$('.device-table > tbody > tr').live('click', function() {

	var device_id = this.id;
	var sel = this;

	// we only want rows for devices, not the rows that contain plant data for an expanded device
	// perfect this check based on id...we only care about rows with an id that don't contain "-plants"
	// device row id: 51cca0844188e27904000001
	// plant data row id : 51cca0844188e27904000001-plants
	if (device_id.indexOf('plants') > -1)
		return;

	$('#'+device_id+'-plants').toggle();
	return;

});

$('input[type=text]').live('change', function() {
	var device_id = $(this).parents('[id$=plants]').attr('id').substring(0, 24);
	var sensor = $(this).attr('data-sensor');
	var property = $(this).attr('name');

	var val = $(this).val();
	if (val.indexOf('.') > -1)
		val = parseFloat(val);
	else
		val = parseInt(val);

	deviceSettings[device_id][sensor][property] = val;
	socket.emit('settings', deviceSettings[device_id]);

});

$('.toggle-sensor-active').live('click', function() {
	var device_id = $(this).parents('[id$=plants]').attr('id').substring(0, 24);
	var sensor = $(this).attr('data-sensor');
	var active = !$(this).hasClass('btn-success');

	deviceSettings[device_id][sensor]['active'] = active;
	socket.emit('settings', deviceSettings[device_id]);

	if (active) {
		$(this).addClass('btn-success').removeClass('btn-danger');
		$(this).children('i').addClass('icon-ok').removeClass('icon-remove');
	} else {
		$(this).addClass('btn-danger').removeClass('btn-success');
		$(this).children('i').addClass('icon-remove').removeClass('icon-ok');
	}

});

// $('.toggle-device-control').live('click', function() {
// 	var button = this;
// 	var device_id = $(button).parents('tr').attr('id');
// 	var active = !$(button).hasClass('btn-success');

// 	var data = {
// 		device_id: device_id,
// 		active: !active
// 	};

// 	// console.log(data);
	
// 	if (active) {
// 		$.get('/update', data, function(data) {
// 			$(button).addClass('btn-success').removeClass('btn-danger');
// 			$(button).children('i').addClass('icon-off').removeClass('icon-fire');
// 		});
// 	} else {
// 		$.get('/update', data, function(data) {
// 			$(button).addClass('btn-danger').removeClass('btn-success');
// 			$(button).children('i').addClass('icon-fire').removeClass('icon-off');
// 		});
// 	}

// 	// return false here to prevent the even bubbling up to parent elements
// 	// i.e., don't trigger the parent <tr>'s click, which will toggle the row being shown
// 	return false;
// });



});