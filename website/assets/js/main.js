$(document).ready(function() {

// request devices on page load & build up table
$.get('/devices', function(data) {

	var sel = $('.device-table > tbody');
	sel.html('');

	for (var i = 0; i < data.length; i++) {
		d = data[i];
		// console.log(d);

		if (d.active) {
			var activeButton = '<button class="btn-mini btn-success"><i class="icon-fire"></i></button>';
		} else {
			var activeButton = '<button class="btn-mini btn-danger"><i class="icon-remove"></i></button>';
		}

		var append = '<tr id="'+d._id+'"><td>'+activeButton+'</td><td>'+d._id+'</td><td>'+d.plants.length+'</td><td>'+d.type+'</td><td>'+d.mood+'</td><td>'+d.date+'</td></tr>';
		sel.append(append);
	}
});

// when we 
$('.device-table > tbody > tr').live('click', function() {

	var id = this.id;
	var sel = this;

	// we only want rows for devices, not the rows that contain plant data for an expanded device
	// check based on id
	// device row id: 51cca0844188e27904000001
	// plant data row id : 51cca0844188e27904000001-plants
	if (id.indexOf('plants') > -1)
		return;

	$.get('/plants', { device_id: id }, function(data) {
		// console.log(data);
		// console.log('#'+id+'-plants');

		if ($('#'+id+'-plants').length) {
			// console.log('hiding row');
			$('#'+id+'-plants').remove();
		} else {

			// console.log('building row');

			var pre = '<tr id="'+id+'-plants"><td></td><td colspan="5"><table class="table plant-table"><thead><tr><th width="25%">Plant ID</th><th width="7%">Index</th><th width="28%">Created</th><th width="14%">Mood</th><th width="26%">Touch</th></tr></thead><tbody>';
			var post = '</tbody></table>';

			var string = '';
			for (var i = 0; i < data.length; i++) {
				d = data[i];
				string += '<tr><td>'+d._id+'</td><td>'+d.index+'</td><td>'+d.created+'</td><td>'+d.mood+'</td><td>'+d.touch+'</td></tr>';
			}

			string = pre + string + post;
			$(sel).after(string);

		}
	});

});



});