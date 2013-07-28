$(document).ready(function(){
	
	//alert('true');
	var $request = $.urlParam('type');
	
	
	$.getJSON('assets/dialogue.json',function(json){
		
		if(json.hasOwnProperty($request)){
			
			
			
			$('body').html(JSON.stringify(json[$request]));
			console.log(json[$request]);
			//alert('yes');
		}else{
			$('body').html('{"error":"unknown_type"}');
		}
		//console.log(json[$request]);
		
	})
	
	
});
//via http://nack.co/get-url-parameters-using-jquery/
$.urlParam = function(name){
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    return results[1] || 0;
}
 