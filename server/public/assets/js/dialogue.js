$(document).ready(function(){
	
	//check from URL get parameter of type	
	var $request = $.urlParam('type');
	
	//pull json file
	$.getJSON('assets/dialogue.json',function(json){
		
		//check if json file has the requested type
		if(json.hasOwnProperty($request)){
			//append the text to the page
			$('body').html(JSON.stringify(json[$request]));
			//console.log(json[$request]);
		}else{
			//report an error
			$('body').html('{"error":"unknown_type"}');
		}
		
	})
	
	
});

//via http://nack.co/get-url-parameters-using-jquery/
$.urlParam = function(name){
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    return results[1] || 0;
}
 