$(document).ready(function() {

// request devices on page load & build up table
$.get('/devices', function(data) {

	var sel = $('.device-table > tbody');
	sel.html('');

	for (var i = 0; i < data.length; i++) {
		d = data[i];
		// console.log(d);

		// generate code for whether or not we're active
		if (d.active) {
			var activeButton = '<button class="btn-mini btn-success toggle-device-active"><i class="icon-off"></i></button>';
		} else {
			var activeButton = '<button class="btn-mini btn-danger toggle-device-active"><i class="icon-fire"></i></button>';
		}

		var append = '<tr id="'+d._id+'"><td>'+activeButton+'</td><td>'+d._id+'</td><td>'+d.plants.length+'</td><td>'+d.type+'</td><td>'+d.mood+'</td><td>'+d.date+'</td></tr>';
		sel.append(append);
	}
});

$('.device-table > tbody > tr').live('click', function() {

	var device_id = this.id;
	var sel = this;

	// we only want rows for devices, not the rows that contain plant data for an expanded device
	// perfect this check based on id...we only care about rows with an id that don't contain "-plants"
	// device row id: 51cca0844188e27904000001
	// plant data row id : 51cca0844188e27904000001-plants
	if (device_id.indexOf('plants') > -1)
		return;

	// fetch the plant data
	$.get('/plants', { device_id: device_id }, function(data) {
		// console.log(data);
		// console.log('#'+id+'-plants');

		// if the table row exists, we're clicking to hide to remove it
		if ($('#'+device_id+'-plants').length) {
			// console.log('hiding row');
			$('#'+device_id+'-plants').remove();
			return;
		} else {

			// otherwise let's build up all the data to display it

			// table header and closing tags
			var plantPre = '<table class="table plant-table"><thead><tr><th width="23%">Plant ID</th><th width="7%">Index</th><th width="20%">Created</th><th width="11%">Mood</th><th width="8%">Touch</th><th width="22%">Cap Slider</th><th width="7%">Val</th></tr></thead><tbody>';
			var plantPost = '</tbody></table>';

			// loop through results and generate a new table row for each
			var plantString = '';
			for (var i = 0; i < data.length; i++) {
				var d = data[i];
				plantString += '<tr><td>'+d._id+'</td><td>'+d.index+'</td><td>'+(new Date(d.created)).toLocaleString()+'</td><td>'+d.mood+'</td><td>'+d.touch+'</td><td><input type="range" min="0" max="15000" step="1"></td><td>16813</td></tr>';
			}

			// now grab all the settings data
			$.get('/settings', { device_id: device_id }, function (data) {

				// console.log(data);

				// table header and closing tags
				var settingsPre = '<table class="table table-condensed sensor-table"><thead><tr><th>Active</th><th>Property</th><th>Low Threshold</th><th>High Threshold</th><th>Window</th></tr></thead><tbody>';
				var settingsPost = '</tbody></table>';

				// expected result is a list with only one element, so grab just the first one
				// loop through the object keys and generate table rows
				var settingsString = '';
				var d = data[0];
				for (var key in d) {
					// ignore keys that aren't objects, e.g. _id and device_id which are strings
					if (typeof(d[key]) != 'object') continue;

					// generate code for whether or not we're active
					if (d[key].active) {
						var activeButton = '<button class="btn-mini btn-success toggle-sensor-active" data-sensor="'+key+'""><i class="icon-fire"></i></button>';
					} else {
						var activeButton = '<button class="btn-mini btn-danger toggle-sensor-active" data-sensor="'+key+'""><i class="icon-remove"></i></button>';
					}

					if (key == 'touch') {
						var windowString = '<input class="input-small" type="text" name="window" placeholder="window" value="'+d[key].window+'" data-sensor="'+key+'">';
					} else {
						var windowString = '';
					}


					settingsString += '<tr><td>'+activeButton+'</td><td>'+key+'</td><td><input class="input-small" type="text" name="low" placeholder="low" value="'+d[key].low+'" data-sensor="'+key+'"></td><td><input class="input-small" type="text" name="high" placeholder="high" value="'+d[key].high+'" data-sensor="'+key+'"></td><td>'+windowString+'</td></tr>'
				}

				// the actual table row that appended to the main devices table, in which our two tables are placed
				var commonPre = '<tr id="'+device_id+'-plants"><td></td><td colspan="5">';
				var commonPost = '</td></tr>';

				// build it and append it
				var string = commonPre + plantPre + plantString + plantPost + settingsPre + settingsString + settingsPost + commonPost;
				$(sel).after(string);

			});
		}
	});
});

$('input[type=text]').live('change', function() {
	var device_id = $(this).parents('[id$=plants]').attr('id').substring(0, 24);

	var data = {
		device_id: device_id,
		sensor: $(this).attr('data-sensor'),
		property: $(this).attr('name'),
		val: $(this).val()
	}

	// console.log(data);

	$.get('/update', data);
})

$('.toggle-device-active').live('click', function() {
	var button = this;
	var device_id = $(button).parents('tr').attr('id');
	var active = $(button).hasClass('btn-success');

	var data = {
		device_id: device_id,
		active: !active
	};

	// console.log(data);
	
	if (active) {
		$.get('/update', data, function(data) {
			$(button).addClass('btn-danger').removeClass('btn-success');
			$(button).children('i').addClass('icon-fire').removeClass('icon-off');
		});
	} else {
		$.get('/update', data, function(data) {
			$(button).addClass('btn-success').removeClass('btn-danger');
			$(button).children('i').addClass('icon-off').removeClass('icon-fire');
		});
	}

	// return false here to prevent the even bubbling up to parent elements
	// i.e., don't trigger the parent <tr>'s click, which will toggle the row being shown
	return false;
});

$('.toggle-sensor-active').live('click', function() {

	var device_id = $(this).parents('[id$=plants]').attr('id').substring(0, 24);
	var sensor = $(this).attr('data-sensor');

	var button = this;
	var active = $(button).hasClass('btn-success');

	var data = {
		device_id: device_id,
		sensor: $(this).attr('data-sensor'),
		active: !active
	};

	// console.log(data);
	
	if (active) {
		$.get('/update', data, function(data) {
			$(button).addClass('btn-danger').removeClass('btn-success');
			$(button).children('i').addClass('icon-remove').removeClass('icon-fire');
		});
	} else {
		$.get('/update', data, function(data) {
			$(button).addClass('btn-success').removeClass('btn-danger');
			$(button).children('i').addClass('icon-fire').removeClass('icon-remove');
		});
	}	
	
});

$('input[type=range]').live('change', function() {

	$(this).tooltip('destroy').tooltip({ animation: false, title: $(this).val() }).tooltip('show');

	// $(this).tooltip('destroy');
	// $(this).tooltip({ title: $(this).val() }).show();
});

});