/*
 * Copyright 2011 Thomas Amsler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * jQM Default Settings
 */
$(document).bind("mobileinit", function() {
	
	$.mobile.defaultPageTransition = 'none';
});

$(document).ready(function() {
	
	/*
	 * Variable used for Google local search API to keep track of search center point
	 */
	$('body').data({ searchCenterPoinLatLng : null });
	
	/*
	 * Boolean flag that is used to determine if we need to rebuild the location selection options.
	 * When the new artifact's page is shown, we get all the currently stored locations. We don't want to 
	 * do that if we just added a new location option via the 'new location' page.
	 */
	$('body').data({ refreshLocations : true });

	/*
	 * Clicking on the welcome page's new artifact image
	 */
	$('#welcome').delegate('#welcome-new-artifact-img', 'click', function(event) {
		
		$.mobile.changePage($('#new-artifact'), "none");
	});
	
	/*
	 * Clicking on the welcome page's artifacts image
	 */
	$('#welcome').delegate('#welcome-artifacts-img', 'click' , function(event) {
	
		$.mobile.changePage($('#manage-artifacts'), "none");
		window.android.getArtifactsForCurrentLocation();
	});
	
	/*
	 * Clicking on the welcome page's locations image
	 */
	$('#welcome').delegate('#welcome-locations-img', 'click', function(event) {
		
		window.android.getLocations("list");
	});
	
	/*
	 * Clicking on the welcome page's options image
	 */
	$('#welcome').delegate('#welcome-options-img', 'click', function(event) {
		
		$.mobile.changePage($('#options'), "none");
	});
	
	/*
	 * Clicking on an artifact list item
	 */
	$('#artifactly-list').delegate('.artifactly-list-item', 'click', function(event) {  

		var artId = $(this).data("artId");
		var locId = $(this).data("locId");
		window.android.getArtifact(artId, locId);
	});
	
	/*
	 * Click on a location list item
	 */
	$('#artifactly-list').delegate('.artifactly-list-divider', 'click', function(event) {  

		var location = $(this).data();
		
		viewLocationPage(location, false, false);
	});
	
	/*
	 * Clicking on a location list item
	 */
	$('#manage-locations-list').delegate('li', 'click', function(event) {  

		var location = $(this).data();
		
		/*
		 * When we construct the manage locations list, we already lookup 
		 * the address and attache it to the location. Thus we pass in 'true'
		 */
		viewLocationPage(location, true, true);
	});
	
	/*
	 * Swiping right on an artifact list item, starts the delete dialog
	 */
	$('#artifactly-list').delegate('.artifactly-list-item', 'swiperight', function(event) {
		
		var artifact = $(this).data();
		$('#delete-artifact-name').html(artifact.artName);
		$('#delete-artifact-name').data(artifact);
		$.mobile.changePage($('#artifact-dialog'), "none");
	});
	
	/*
	 * Swiping right on a location list item, starts the delete dialog
	 */
	$('#manage-locations-list').delegate('li', 'swiperight', function(event) {

		$('#delete-location-name').html($(this).data("locName"));
		$('#delete-location-name').data({ locId : $(this).data("locId") });
		$('#delete-location-name').data({ navigateTo : '#manage-locations' });
		$.mobile.changePage($('#location-dialog'), "none");
	});

	/*
	 * Clicking on a search result list item, populates the location options menu
	 */
	$('#search-result-list').delegate('li', 'click', function(event) {
		
		$('<option/>', { text : $(this).data("locName")})
		.data({
			locId : '',
			locName : $(this).data("locName"),
			locLat : $(this).data("locLat"),
			locLng : $(this).data("locLng")
		})
		.attr('selected', 'selected')
		.appendTo($('#artifact-location-selection'));
		
		$('#artifact-location-selection').selectmenu("refresh");
		$('body').data({ refreshLocations : false });
		$.mobile.changePage($('#new-artifact'), "none");
	});

	/*
	 * Clicking on the artifact deletion dialog yes button
	 */
	$('#delete-artifact-yes').click(function(event) {

		var data = $('#delete-artifact-name').data();
		window.android.deleteArtifact(data.artId, data.locId);
		$('.ui-dialog').dialog('close');
		$.mobile.changePage($('#manage-artifacts'), "none");
	});
	
	/*
	 * Clicking on the location deletion dialog yes button
	 */
	$('#delete-location-yes').click(function(event) {

		var locId = $('#delete-location-name').data("locId");
		var navigateTo = $('#delete-location-name').data("navigateTo");
		window.android.deleteLocation(locId);
		$('.ui-dialog').dialog('close');
		$.mobile.changePage($(navigateTo), "none");
	});
	
	/*
	 * Clicking on the artifact deletion dialog cancel button
	 */
	$('#delete-artifact-cancel').click(function(event) {

		$('#artifact-dialog').dialog('close');
	});
	
	/*
	 * Clicking on the location deletion dialog cancel button
	 */
	$('#delete-location-cancel').click(function(event) {

		var navigateTo = $('#delete-location-name').data("navigateTo");
		$('#location-dialog').dialog('close');
		$.mobile.changePage($(navigateTo), "none");
	});
	
	/*
	 * Initialize the new artifact page
	 */
	$('#new-artifact').bind('pageshow', function() {
		
		$('#artifact-location-name-div').hide();
		
		if($('body').data("refreshLocations")) {
		
			window.android.getLocations("options");
		}
		
		$('body').data({ refreshLocations : true });
	});
	
	/*
	 * Initialize the option's page
	 */
	$('#options').bind('pageshow', function() {

		var preferences = JSON.parse(window.android.getPreferences());
		
		// Setting radius preference
		$('#options-radius-input').val(preferences.radius);
		
		// Keep track of the current radius so that we only call save if the value changed
		$('#options').data({optionsRadius:preferences.radius});
		
		// Resetting options
		$("input:radio[name='sound-notification']:checked").attr("checked", false).checkboxradio("refresh");
		$("input:radio[name='load-maps']:checked").attr("checked", false).checkboxradio("refresh");
		$("input:radio[name='options-radius-unit']:checked").attr("checked", false).checkboxradio("refresh");
		
		// Setting sound notification preference
		if(preferences.soundNotification) {
		
			$("#sound-notification-on").attr("checked", true).checkboxradio("refresh");
		}
		else {
			
			$("#sound-notification-off").attr("checked", true).checkboxradio("refresh");
		}
		
		// Setting load static map preference
		if(preferences.loadStaticMap) {
			
			$("#load-maps-on").attr("checked", true).checkboxradio("refresh");
		}
		else {
			
			$("#load-maps-off").attr("checked", true).checkboxradio("refresh");
		}
		
		/*
		 * Constructing the jQuery selector using : options-radius-unit- + [m, km, ft, mi]
		 */
		$("#options-radius-unit-" + preferences.radiusUnit).attr("checked", true).checkboxradio("refresh");
	});

	/*
	 * Loading the map and marker on the map page
	 */
	$('#map').bind('pageshow', function() {
		
		// Showing the loading animation
		$.mobile.showPageLoadingMsg();

		loadMapApi('loadMap');
	});
	
	/*
	 * Initialize Google's JSAPI when entering the create artifact page
	 */
	$('#new-location').bind('pageshow', function() {
		
		$('#search-entry').val('');
		$('#search-result-message').html('');
		$('#google-search-branding').html('');
		
		/*
		 * Disabling the nearby button until the center point address has been loaded.
		 * If the center point is not ready, the nearby search won't work because of null Lat/Lng
		 */
		$('#nearby-places-button').button('disable');
		
		$('#search-result-list li').remove();
		
		try {
			
			$('#search-result-list ul').listview('refresh');
		}
		catch(exception) {
			
			$('#search-result-list ul').listview();
		}
		
		var canAccessInternet = window.android.canAccessInternet();
		
		if(!canAccessInternet) {
			
			$('#search-result-message').html('No Internet connection available');
			return;
		}
		
		$('#new-location-center-point').html("Loading ...");
		loadMapApi('getSearchCenterPoint');
		
		var apiKeys = JSON.parse(window.android.getApiKeys());
		$('#new-location').data(apiKeys);
		
		if(typeof(google) == "undefined") {
			
			// Can access the Internet, thus we can load the Google JSAPI
			var script = document.createElement("script");
			script.src = "https://www.google.com/jsapi?key=" + apiKeys.localSearch + "&callback=loadSearchApi";
			script.type = "text/javascript";
			document.getElementsByTagName("head")[0].appendChild(script);
		}
		else if(typeof(google.search) == "undefined") {
			
			// Can access the Internet, thus we can load the Google JSAPI
			var script = document.createElement("script");
			script.src = "https://www.google.com/jsapi?key=" + apiKeys.localSearch + "&callback=loadSearchApi";
			script.type = "text/javascript";
			document.getElementsByTagName("head")[0].appendChild(script);
		}
	});
	
	/*
	 * Update artifact
	 */
	$('#update-artifact').click(function() {
		
		var data = $('#view-artifact-art-name').data();
		var artName = $('#view-artifact-art-name').val();
		var artData = $('#view-artifact-art-data').val();
		var locName = $('#view-artifact-loc-name').val();
		
		/*
		 * First, we cherck if the use onluy update the artifact data
		 * Second, we update if the artifact name, or the artifact data, or the location name changed
		 */
		if(data.artName == artName && data.artData != artData && data.locName == locName) {
			
			window.android.updateArtifactData(data.artId, artData);
			$('#view-artifact-art-name').data({artName:artName, artData:artData, locName:locName});
		}
		else if(data.artName != artName || data.artData != artData || data.locName != locName) {
		
			window.android.updateArtifact(data.artId, artName, artData, data.locId, locName);
			$('#view-artifact-art-name').data({artName:artName, artData:artData, locName:locName});
		}
	});
	
	/*
	 * Update location
	 */
	$('#update-location').click(function() {
	
		var location = $('#view-location-loc-name').data();
		var updatedLocName = $('#view-location-loc-name').val();
		
		/*
		 * Only update if the location name changed
		 * TODO: Allow the user to change lat/lng via moving the marker on a map
		 */
		if(location.locName != updatedLocName) {
			
			/*
			 * Setting a flag on the view-location so that we know not to navigate to
			 * a different page when the getLocationsListCallback() is executed
			 */
			$('#view-location').data({navigate:'no'});
			window.android.updateLocation(location.locId, updatedLocName, (+location.locLat).toFixed(6), (+location.locLng).toFixed(6));
			$('#view-location-loc-name').data({locName:updatedLocName});
		}
	});

	/*
	 * Delete artifact
	 */
	$('#delete-artifact').click(function() {
		
		var data = $('#view-artifact-art-name').data();
		$('#delete-artifact-name').html(data.artName);
		$('#delete-artifact-name').data(data);
		$.mobile.changePage($('#artifact-dialog'), "none");
	});
	
	/*
	 * Close/home button in update artifact view
	 */
	$('#close-artifact-icon').click(function() {
		
		$.mobile.changePage($('#welcome'), "none");
	});
	
	/*
	 * Delete location
	 */
	$('#delete-location').click(function() {
	
		var location = $('#view-location-loc-name').data();
		$('#delete-location-name').html(location.locName);
		$('#delete-location-name').data(location);
		$.mobile.changePage($('#location-dialog'), "none");
	});
	
	/*
	 * Click on view location map image
	 */
	$('#view-location-map').click(function() {
		
		$.mobile.changePage($('#location-chooser-map'), "none");
		
		// Showing the loading animation
		$.mobile.showPageLoadingMsg();

		loadMapApi('loadLocationChooserMap');
	});
	
	/*
	 * Close/home button in update location view
	 */
	$('#close-location-icon').click(function() {
		
		$.mobile.changePage($('#welcome'), "none");
	});

	/*
	 * Set radius button click handler
	 */
	$('#set-radius').click(function() {

		var radius = $('#options-radius-input').val();
		
		var currentRadius = $('#options').data('optionsRadius');
		
		/*
		 * Remove whitespace and check the entered value if it's a number.
		 * If it's not a number, we set the radius to -1 and let the 
		 * Activity handle the error case. -1 is less than the minimum radius.
		 */
		if(isNaN($.trim(radius))) {

			radius = -1;
		}
		
		/*
		 * Making sure that the radius value has changed. There is not need 
		 * to call setRadius if the value hasn't changed.
		 */
		if(radius != currentRadius) {
			
			$('#options').data({optionsRadius:radius});
			window.android.setRadius(+radius);
		}
	});
	
	/*
	 * Select radius unit click handler
	 */
	$("input:radio[name='options-radius-unit']").change(function() {
		
		var value = $("input:radio[name='options-radius-unit']:checked").val();
		
		window.android.setRadiusUnit(value);
		
	});

	/*
	 * Get radius button click handler
	 */
	$('#get-radius').click(function() {

		window.android.showRadius();
	});
	
	/*
	 * Create artifact close button
	 */
	$('#close-artifact-button').click(function() {

		$('#artifact-name').val('');
		$('#artifact-data').val('');
		
		/*
		 * When user clicks on close button, we show the updated
		 * artifacts page
		 */
		$.mobile.changePage($('#manage-artifacts'), "none");
		window.android.getArtifactsForCurrentLocation();
	});

	/*
	 * Create artifact add button
	 */
	$('#add-artifact-button').click(function() {

		var artName = $('#artifact-name').val();
		var artData = $('#artifact-data').val();
	
		var selectedLocation = $('#artifact-location-selection option:selected').data();
		
		if(selectedLocation.locName == "Current Location") {
		
			var locationName = $('#artifact-location-name').val();
			window.android.createArtifact(artName, artData, locationName, (+selectedLocation.locLat).toFixed(6), (+selectedLocation.locLng).toFixed(6));
		}
		else {
		
			window.android.createArtifact(artName, artData, selectedLocation.locName, (+selectedLocation.locLat).toFixed(6), (+selectedLocation.locLng).toFixed(6));
		}
	});
	
	/*
	 * Nearby Google places button
	 */
	$('#nearby-places-button').click(function() {
		
		$(document).ready(function() {
			
			var canAccessInternet = window.android.canAccessInternet();
			if(!canAccessInternet) {
				
				$('#search-result-message').html('No Internet connection available');
				return;
			}
			
			// Reset DIVs and the list
			$('#search-result-list li').remove();
			
			try {
			
				$('#search-result-list ul').listview('refresh');
			}
			catch(exception) {
				
				$('#search-result-list ul').listview();
			}
			
			$('#google-search-branding').html('');
			$('#search-result-message').html('Loading ...');
			
			var latLng = $('body').data("searchCenterPoinLatLng");
			var apiKeys = $('#new-location').data();
			
			var searchName = "";
			
			var search = $('#search-entry').val();
			
			if(search && "" !== search) {
				searchName = "&name=" + search;
			}
			
			$.ajax({
				type:'Get',
				url:'https://maps.googleapis.com/maps/api/place/search/json?location=' + latLng.lat() + ',' + latLng.lng() + '&types=establishment' + searchName + '&radius=1000&sensor=false&key=' + apiKeys.placesSearch,
				success:function(data) {
					
					$('#search-result-message').html('');
					
					if (data.results && data.results.length > 0) {
						
						// First we check if the user wants to load map images
						var canLoadStaticMap = window.android.canLoadStaticMap();
						
						for (var i = 0; i < data.results.length; i++) {
							
							var element = $('<li/>', { html : '<h3>' + data.results[i].name + '</h3>' +
												              '<p>Loading address ...</p>' })        
										  .data({ locName : htmlDecode(stripHtml(data.results[i].name)),
											  	  locLat : data.results[i].geometry.location.lat,
											  	  locLng : data.results[i].geometry.location.lng    
										  })        
										  .appendTo($('#search-result-list ul'));
						
							if(canAccessInternet) {

								/*
								 * The following method will append the address to the <li/> element
								 */
								appendLocationAddress(data.results[i].geometry.location.lat, data.results[i].geometry.location.lng, element, data.results[i].name, data.results[i].id);
							}
						}
						
						// Refresh the list so that all the data is shown
						try {
						
							$('#search-result-list ul').listview('refresh');
						}
						catch(exception) {
							
							$('#search-result-list ul').listview();
						}
					}
					else {
						
						if(data.status == "ZERO_RESULTS") {
							
							$('#search-result-message').html("<p>Search: Zero Results</p>");
						}
						else if(data.status == "OVER_QUERY_LIMIT") {
						
							$('#search-result-message').html("<p>Search: Over Query Limit</p>");
						}
						else if(data.status == "REQUEST_DENIED") {
							
							$('#search-result-message').html("<p>Search: Request Denied</p>");
							
						}
						else if(data.status == "INVALID_REQUEST") {
							
							$('#search-result-message').html("<p>Search: Invalid Request</p>");
						}
						else {
							
							$('#search-result-message').html("<p>Search: Zero Results</p>");
						}
					}
				}
			});
		});
	});
	
	/*
	 * Search location button
	 */
	$('#search-location-button').click(function() {
		
		var search = $('#search-entry').val();
		
		if(!search || "" === search) {
			return;
		}
		
		var canAccessInternet = window.android.canAccessInternet();
		if(!canAccessInternet) {
			
			$('#search-result-message').html('No Internet connection available');
			return;
		}
		
		// Reset the list
		$('#search-result-list li').remove();
		
		try {
			
			$('#search-result-list ul').listview('refresh');
		}
		catch(exception) {
			
			$('#search-result-list ul').listview();
		}
		
		if(google.search) {
			
			// Create a LocalSearch instance.
			var localSearch = new google.search.LocalSearch();

			// Set the Local Search center point
			if($('body').data("searchCenterPoinLatLng")) {

				localSearch.setCenterPoint($('body').data("searchCenterPoinLatLng"));

				// Set searchComplete as the callback function when a search is complete. The
				// localSearch object will have results in it.
				var result = new Array();
				result[0] = localSearch;
				localSearch.setSearchCompleteCallback(this, searchComplete, result);

				// Specify search query
				localSearch.execute(search);

				// Include the required Google branding.
				// Note that getBranding is called on google.search.Search
				google.search.Search.getBranding('google-search-branding');
				
				$('#search-result-message').html("<p>Searching ...</p>");
			}
			else {
				
				console.log("ERROR: Search center point is not defined");
			}
		}
	});
	
	/*
	 * Options' page sound notification on/off
	 */
	$("input:radio[name='sound-notification']").change(function() {
		
		var value = $("input:radio[name='sound-notification']:checked").val();
		
	    if (value == 'on') {
	    	
	    	window.android.setSoundNotificationPreference(true);
	    }
	    else if (value == 'off') {
	    	
	    	window.android.setSoundNotificationPreference(false);
	    }
	    else {
	    	
	    	console.log("ERROR: No matching sound notification preference");
	    }
	});
	
	/*
	 * Options' page static maps on/off
	 */
	$("input:radio[name='load-maps']").change(function() {
		
		var value = $("input:radio[name='load-maps']:checked").val();
		
	    if (value == 'on') {
	    	
	    	window.android.setLoadStaticMapPreference(true);
	    }
	    else if (value == 'off') {
	    	
	    	window.android.setLoadStaticMapPreference(false);
	    }
	    else {
	    	
	    	console.log("ERROR: No matching sound notification preference");
	    }
	});
	
	/*
	 * Locations' select menu
	 */
	$('#artifact-location-selection').change(function() {
		
		var selectedLocation = $('#artifact-location-selection option:selected').data();
		
		// If the selection is Current Location, then we need to allow the user to enter a location name
		if(selectedLocation.locName == "Current Location") {
			
			$('#artifact-location-name').val('');
			$('#artifact-location-name-div').show();
			
			var location = JSON.parse(window.android.getLocation());
			
			/*
			 * Since we have the current location, we attach it to the Current Locaiton menu option
			 */
			$('#artifact-location-selection option:selected').data({ locLat : location.locLat, locLng : location.locLng });
			
			/*
			 * For now, we load the static map in this specific case even if the user sets the option to load maps to false
			 * TODO: Check how we can provide current location information if the user doesn't want to load map images
			 */
			if(window.android.canAccessInternet()) {
			
				$('#artifact-current-location-map img').attr('src', getMapImage(location.locLat, location.locLng, "15", "250", "200"));
			}
		}
		else if(selectedLocation.locName == "New Location") {

			$.mobile.changePage($('#new-location'), "none");
		}
		else {

			$('#artifact-location-name-div').hide();
		}
	});
	
	/*
	 * Location chooser update button click handler
	 */
	$('#location-chooser-update').click(function() {
		var location = $('#location-chooser-map').data();
		$('#view-location').data({navigate:'no'});
		window.android.updateLocationCoodinates(location.locId, location.locName, (+location.locLat).toFixed(6), (+location.locLng).toFixed(6));
		viewLocationPage(location, false, false);
	});
	
}); // END jQuery main block

/*
 * Load Google search API
 */
function loadSearchApi() {

	var canAccessInternet = window.android.canAccessInternet();
	if(canAccessInternet) {

		google.load("search", "1", {"callback" : onLoadSearchApi});
	}
}

/*
 * Google search API callback
 */
function onLoadSearchApi() {
	
	if(typeof(google.search) == "undefined") {
		
		console.log("ERROR: Google Search API did not load");
	}
}

/*
 * Clicking on the search button in the new location page
 */
function searchComplete(localSearch) {
	
	$(document).ready(function() {
			
		// Do we have any search results
		if (localSearch.results && localSearch.results.length > 0) {
			
			$('#search-result-message').html('');
			
			// First we check if the user wants to load map images
			var canLoadStaticMap = window.android.canLoadStaticMap();
			
			// Iterate over the search result
			for (var i = 0; i < localSearch.results.length; i++) {
								
				$('<li/>', { html : '<span class="location-address-name">' + localSearch.results[i].title + '</span><br />' +
									'<span class="location-formatted-address">' + localSearch.results[i].addressLines[0] + '</span><br />' +
									'<span class="location-formatted-address">' + localSearch.results[i].city + '</span>' })       
			      .data({
			    	locName : htmlDecode(stripHtml(localSearch.results[i].title)),
			    	locLat : localSearch.results[i].lat,
			    	locLng : localSearch.results[i].lng    
			      })        
			      .appendTo($('#search-result-list ul'));
			}
	
			// Refresh the list so that all the data is shown
			try {
			
				$('#search-result-list ul').listview('refresh');
			}
			catch(exception) {
			
				$('#search-result-list ul').listview();
			}
		}
		else {
			
			$('#search-result-message').html("<p>No Search results</p>");
		}
	});
}

/*
 * Map page callback
 */
function loadMap() {

	$(document).ready(function() {
		
		var location = JSON.parse(window.android.getLocation());
		var latLng = new google.maps.LatLng(location.locLat, location.locLng);

		var myOptions = {
				zoom: 15,
				center: latLng,
				mapTypeId: google.maps.MapTypeId.ROADMAP,
				navigationControl: true,
			    navigationControlOptions: {
			        style: google.maps.NavigationControlStyle.SMALL,
			        position: google.maps.ControlPosition.TOP_RIGHT
			    },
		};

		var map = new google.maps.Map(document.getElementById("map-canvas"), myOptions);
		map.panTo(latLng);

		var marker = new google.maps.Marker({
			position: latLng,
			map: map,
			animation : google.maps.Animation.DROP
		});
		
		var content = "Latitude = " + (+location.locLat).toFixed(6) + "<br />Longitude = " + (+location.locLng).toFixed(6) +"<br />Accuracy = " + (+location.locAccuracy).toFixed(2) + " m";
		var infowindow = new google.maps.InfoWindow({
			content: content
		});

		google.maps.event.addListener(marker, 'click', function() {
			infowindow.open(map, marker);
		});
		
		/*
		 * Storing the map reference so that we can update it in the broadcastCurrentLocation() function
		 */
		$('#map-content').data({ mapReference : map });
		
		// Hide the maps loading animation
		$.mobile.hidePageLoadingMsg();
	});
}

/*
 * Map page callback
 */
function loadLocationChooserMap() {

	$(document).ready(function() {
		
		$('#location-chooser-update').button('disable');
		
		var location = $('#view-location-loc-name').data();
		var latLng = new google.maps.LatLng(location.locLat, location.locLng);

		var myOptions = {
				zoom: 15,
				center: latLng,
				mapTypeId: google.maps.MapTypeId.ROADMAP,
				navigationControl: true,
			    navigationControlOptions: {
			        style: google.maps.NavigationControlStyle.SMALL,
			        position: google.maps.ControlPosition.TOP_RIGHT
			    },
		};

		var map = new google.maps.Map(document.getElementById("location-chooser-canvas"), myOptions);
		map.panTo(latLng);

		var marker = new google.maps.Marker({
			position: latLng,
			map: map,
			animation: google.maps.Animation.DROP,
			draggable: true
		});
		
		google.maps.event.addListener(marker, 'dragend', function() {
			
			/*
			 * Store the location data so that we can persist it when the user clicks 
			 * on the update button
			 */
			$('#location-chooser-update').button('enable');
			$('#location-chooser-map').data({locId:location.locId, locName:location.locName, locLat:marker.position.lat(), locLng:marker.position.lng()});
		});
		
		// Hide the maps loading animation
		$.mobile.hidePageLoadingMsg();
		
		window.android.showMessage("update_location_help");
	});
}


/*
 * Create artifact callback
 */
function createArtifactCallback(data) {
	
	// If the createArtifact call succeeded, we clear the form fields
	if(data.isSuccess) {

		$('#artifact-name').val('');
		$('#artifact-data').val('');
		$('#artifact-location-name').val('');
	}
}

/*
 * Get all artifacts callback
 */
function getArtifactsCallback(artifacts) {

	// Not needed yet
	console.log("ERROR: getArtifactsCallback() is not implemented.");
}

/*
 * Get artifact callback
 */
function getArtifactCallback(artifact) {

	$(document).ready(function() {

		/*
		 * Attach the artifact id to the artifact name field so that
		 * we can use it to update the artifact values
		 */
		$('#view-artifact-art-name').data(artifact);
		$('#view-artifact-art-name').val(artifact.artName);
		$('#view-artifact-art-data').val(artifact.artData);
		$('#view-artifact-loc-name').val(artifact.locName);
		$('#view-artifact-lat').val((+artifact.lat).toFixed(6));
		$('#view-artifact-lng').val((+artifact.lng).toFixed(6));

		// Remove stale map image
		$('#view-artifact-map img').remove();

		// Add new map
		if(window.android.canLoadStaticMap()) {

			$('<img/>')
			.attr('src', getMapImage(artifact.lat, artifact.lng, "15", "250", "200"))
			.appendTo($('#view-artifact-map'));
		}

		$.mobile.changePage($('#view-artifact'), "none");
	});
}

function getArtifactsForCurrentLocationCallback(locations) {

	$(document).ready(function() {

		var numLi = $('#artifactly-list li').size();
		if(numLi > 0) {
		
			$('#artifactly-list li').remove();
			
			try {

				$('#artifactly-list ul').listview('refresh');
			}
			catch(exception) {

				$('#artifactly-list ul').listview();
			}
		}
		
		if(!locations || locations.length < 1) {

			$('#artifactly-message').text("There are no Artifacts close by");
		}
		else {

			$('#artifactly-message').text("");
			$.each(locations, function(i, location) {

				$('<li/>', { html : '<img src="images/map-marker.png"/>' +
									'<h3>' + location.locName + '</h3>' +
									'<span class="ui-li-count">' + location.artifacts.length + '</span>'})
				.attr('data-role', 'list-divider')
				.data(location)
				.addClass('artifactly-list-divider')
				.appendTo($('#artifactly-list ul'));

				$.each(location.artifacts, function(j, artifact) {
					$('<li/>', { html : '<h3>' + artifact.artName + '</h3>' })
					.data({ artId : artifact.artId, artName : artifact.artName, locId : location.locId })
					.addClass('artifactly-list-item')
					.appendTo($('#artifactly-list ul'))
				});
			});

			try {

				$('#artifactly-list ul').listview('refresh');
			}
			catch(exception) {
				// Left empty on purpose
			}
		}
	});
}

function getLocationsOptionsCallback(locations) {
	
	$(document).ready(function() {
	
		// Remove all existing location options
		$('#artifact-location-selection option').remove();
		
		// Adding Choose one
		$('<option/>', { text :  'Choose one ...' })
		.attr('disabled', 'disabled')
		.data({ locId : '', locName : '', locLat : '', locLng : '' })
		.appendTo($('#artifact-location-selection'));
		
		// Adding new location option
		$('<option/>', { text :  'New Location' })
		.data({ locId : '', locName : 'New Location', locLat : '', locLng : '' })
		.appendTo($('#artifact-location-selection'));
		
		// Adding current locaiton
		$('<option/>', { text :  'Current Location' })
		.data({ locId : '', locName : 'Current Location', locLat : '0', locLng : '0' })
		.appendTo($('#artifact-location-selection'));
		
		// Adding all stored locations
		if(locations.length > 0) {
			
			$.each(locations, function(i, location) {
				
				$('<option/>', { text : location.locName})
				.data(location)
				.appendTo($('#artifact-location-selection'));
			});
		}
		
		$('#artifact-location-selection').selectmenu("refresh");
	});
}

function getLocationsListCallback(locations) {
	
	$(document).ready(function() {
	
		/*
		 * In case this callback was triggered by a locaiton update, we don't navigate
		 * to another page
		 */
		if("no" != $('#view-location').data("navigate")) {
			
			// This call has to occur before the manage-locations-list is manipulated via remove, refresh, etc.
			$.mobile.changePage($('#manage-locations'), "none");
		}
		else {
			
			$('#view-location').data({navigate:'yes'});
		}

		// Reset the list
		$('#manage-locations-list li').remove();
		
		try {
		
			$('#manage-locations-list ul').listview('refresh');
		}
		catch(exception) {
			
			$('#manage-locations-list ul').listview();
		}

		if(!locations || locations.length < 1) {

			$('#manage-locations-list-message').text("There are no locations");
		}
		else {

			var canAccessInternet = window.android.canAccessInternet();

			$('#manage-locations-list-message').text("");
			$.each(locations, function(i, location) {

				var element = $('<li/>', { html : '<h3>' + location.locName + '</h3>' +
												  '<p>Loading address ...</p>' })
				.data(location)
				.appendTo($('#manage-locations-list ul'));

				if(canAccessInternet) {

					/*
					 * The following method will append the address to the <li/> element
					 */
					appendLocationAddress(location.locLat, location.locLng, element, location.locName, location.locId);
				}
			});
		}

		try {
		
			$('#manage-locations-list ul').listview('refresh');
		}
		catch(exception) {
			
			$('#manage-locations-list ul').listview();
		}
	});
}

/*
 * This will hide the delete button in the view-location page
 * if the location has associated artifacts
 */
function hasArtifactsAtLocationCallback() {
	
	$('#delete-location-button').hide();
}

/*
 * Handle activity dispatching location change broadcast
 * On each alocation change, we add a small marker on the map
 */
function broadcastCurrentLocation(location) {
	
	$(document).ready(function() {
		
		// Get the current page and check if it's the map page
		var pageId = $.mobile.activePage.attr("id");
		var mapReference = $('#map-content').data("mapReference");
		
		if(pageId == "map" && undefined != mapReference && null != mapReference) {
			
			var latLng = new google.maps.LatLng(location.locLat, location.locLng);
			
			var marker = new google.maps.Marker({
				position: latLng,
				map: mapReference,
				icon: 'images/marker-light-blue.png',
				animation : google.maps.Animation.DROP
			});
		}
	});
}

/*
 * Helper method that appends the formatted address to the provided DOM element
 */
function appendLocationAddress(lat, lng , element, locationName, cacheKey) {
	
	$(document).ready(function() {
		
		/*
		 * NOTE:
		 * We use the "manage-locations" div to store the location address lookup cache
		 */

		// First we check if we have the location cached
		var key = '' + cacheKey;
		var cachedLocation = $('#manage-locations').data(key);

		if(typeof(cachedLocation) != "undefined") {
			
			element.html("");
			element.append('<span class="location-address-name">' + locationName + '</span><br />');
			element.append('<span class="location-formatted-address">' + cachedLocation.streetNumber + ' ' + cachedLocation.street + '</span><br />');
			element.append('<span class="location-formatted-address">' + cachedLocation.city + '</span>');
			element.data({ locAddress : cachedLocation.address });
		}
		else {

			$.ajax({
				type:'Get',
				url:'https://maps.googleapis.com/maps/api/geocode/json?address=' + lat + ',' + lng + '&sensor=true',
				success:function(data) {
					
					element.html("");
					element.append('<span class="location-address-name">' + locationName + '</span><br />');
					
					/*
					 * In some case there is no address_component, we just show the locationName
					 */
					if(typeof(data.results[0]) != 'undefined') {
						
						element.append('<span class="location-formatted-address">' + data.results[0].address_components[0].long_name + ' ' + data.results[0].address_components[1].long_name + '</span><br />');
						element.append('<span class="location-formatted-address">' + data.results[0].address_components[2].long_name + '</span>');
						element.data({ locAddress : data.results[0].formatted_address });
						
						// Caching address data
						$('#manage-locations').data(key,
								{streetNumber: data.results[0].address_components[0].long_name,
								 street: data.results[0].address_components[1].long_name,
								 city: data.results[0].address_components[2].long_name,
								 address: data.results[0].formatted_address});
					}
					else {
						
						element.append('<span class="location-formatted-address">Address Not Available</span><br />');
						element.data({ locAddress : "Address Not Available" });
					}
				}
			});
		}
	});
}

/*
 * Helper method that set the formatted address to the "view-location-address" 
 * div tag in the "view-location" page
 */
function addLocationAddressToViewLocationPage(lat, lng) {
	
	$(document).ready(function() {
	
		$.ajax({
			type:'Get',
			url:'https://maps.googleapis.com/maps/api/geocode/json?address=' + lat + ',' + lng + '&sensor=true',
			success:function(data) {
				
				if(typeof(data.results[0]) != 'undefined') {
			
					$('#view-location-address').html(data.results[0].formatted_address);
				}
				else {
					
					$('#view-location-address').html('Address Not Available');
				}
			}
		});
	});
}

/*
 * Method that resets the the view to the main page
 */
function resetWebView() {

	$(document).ready(function() {
		
		$.mobile.changePage($('#welcome'), "none");
	});
}

function loadMapApi(callback) {
	
	$(document).ready(function() {

		/*
		 * First we check if we have Internet access. The Activity will show a 
		 * message if we don't have Internet access
		 */
		var canAccessInternet = window.android.canAccessInternet();
		if(canAccessInternet && typeof(google) == "undefined") {

			// Can access the Internet, thus we can load the Google maps API and map
			$.getScript('https://maps.google.com/maps/api/js?sensor=true&callback=' + callback);
		}
		else if(canAccessInternet && typeof(google.maps) == "undefined") {

			// Can access the Internet, thus we can load the Google maps API and map
			$.getScript('https://maps.google.com/maps/api/js?sensor=true&callback=' + callback);
		}
		else {

			/*
			 * Since the maps api is loaded via "show map" or "new location" we have
			 * to make sure that if the api is loaded, that we still execute the callback
			 */
			eval(callback + "()");
		}
	});
}

function getSearchCenterPoint() {
	
	$(document).ready(function() {

		var location = JSON.parse(window.android.getLocation());
		var latlng = new google.maps.LatLng(location.locLat, location.locLng);

		$('body').data({ searchCenterPoinLatLng : latlng });
		geocoder = new google.maps.Geocoder(); 

		geocoder.geocode({'latLng':latlng}, function(results, status) {

			if (status == google.maps.GeocoderStatus.OK) {
				
				$('#new-location-center-point').html(results[0].formatted_address);
				$('#nearby-places-button').button('enable');
				
			} else {
				
				console.log("ERROR: Geocode was not successful for the following reason: " + status);
			}
		});
	});
}


/*
 * When user clicks on the Artifactly notification event, the activity
 * dispatches to show the artifacts
 */
function showArtifactsPage() {
	
	$(document).ready(function() {

		$.mobile.changePage($('#manage-artifacts'), "none");
	});
}

/*
 * Android menu option: Options
 */
function showOptionsPage() {

	$(document).ready(function() {

		$.mobile.changePage($('#options'), "none");
	});
}

/*
 * Android menu option: Map
 */
function showMapPage() {
	
	$(document).ready(function() {

		$.mobile.changePage($('#map'), "none");
	});
}

/*
 * Android menu option: Info/About
 */
function showAppInfoPage() {

	$(document).ready(function() {

		var version = window.android.getVersion();
		$('#app-info-version').html("v" + version);
		$.mobile.changePage($('#app-info'), "none");
	});
}

/*
 * Android menu option: Welcome
 */
function showWelcomePage() {
	
	$(document).ready(function() {

		$.mobile.changePage($('#welcome'), "none");
	});
}

/*
 * Android orientation event
 */
function showOrientationPortraitMode() {
	
	// Keep the next three paddings synchronzied with what's in CSS
	$('.welcome-item-c').css('padding-top', '35px !important');
	$('.welcome-item-d').css('padding-top', '35px !important');

	$('.welcome-grid').removeClass('ui-grid-c').addClass('ui-grid-a');
	$('.welcome-item-a').removeClass('ui-block-a').addClass('ui-block-a');
	$('.welcome-item-b').removeClass('ui-block-b').addClass('ui-block-b');
	$('.welcome-item-c').removeClass('ui-block-c').addClass('ui-block-a');
	$('.welcome-item-d').removeClass('ui-block-d').addClass('ui-block-b');
}

/*
 * Android orientation event
 */
function showOrientationLandscapeMode() {
	
	/*
	 * The next three lines reset CSS top padding for two items, which is set
	 * in showOrientationPortraitMode() funciton. welcome-item-a or 
	 * welcome-item-b have the correct top padding for this orientation
	 */
	var padding = $('.welcome-item-a').css('padding-top');
	$('.welcome-item-c').css('padding-top', padding + ' !important');
	$('.welcome-item-d').css('padding-top', padding + ' !important');
	
	$('.welcome-grid').removeClass('ui-grid-a').addClass('ui-grid-c');
	$('.welcome-item-a').removeClass('ui-block-a').addClass('ui-block-a');
	$('.welcome-item-b').removeClass('ui-block-b').addClass('ui-block-b');
	$('.welcome-item-c').removeClass('ui-block-a').addClass('ui-block-c');
	$('.welcome-item-d').removeClass('ui-block-b').addClass('ui-block-d');
}

/*
 * Helper method that strips HTML from a string
 */
function stripHtml(data) {
	
	return data.replace(/(<([^>]+)>)/ig,"");
}

/*
 * Helper method to HTML decode
 */
function htmlDecode(value) {
	
	return $('<div/>').html(value).text();
}

/*
 * Helper method to HTML encode
 */
function htmlEncode(value) {
	
	return $('<div/>').text(value).html();
}

/*
 * Helper method that creates an URL that returns a Google Maps image
 */
function getMapImage(lat, lng, zoom, width, height) {

	return "https://maps.google.com/maps/api/staticmap?center=" + lat + "," + lng + "&zoom=" + zoom + "&size=" + width + "x" + height + "&markers=color:red%7Csize:small%7C" + lat + "," + lng + "&sensor=false";
}

/*
 * Helper method that populates the view-location page
 * 
 * @param location : location object
 * @param hasAddress : boolean indicating if the location parameter has the address
 */
function viewLocationPage(location, hasAddress, hasDeleteButton) {
	
	$(document).ready(function() {
		
		$('#view-location-loc-name').data(location);
		$('#view-location-loc-name').val(location.locName);

		if(hasAddress) {

			$('#view-location-address').html(location.locAddress);
		}

		// Remove stale map image
		$('#view-location-map img').remove();

		// Add new map
		if(window.android.canLoadStaticMap()) {

			$('#view-location-map-help').show();
			$('<img/>')
			.attr('src', getMapImage(location.locLat, location.locLng, "15", "250", "200"))
			.appendTo($('#view-location-map'));
		}
		else {
			
			/*
			 * We only show the help content if map loading is enabled
			 */
			$('#view-location-map-help').hide();
		}

		if(!hasAddress) {

			addLocationAddressToViewLocationPage(location.locLat, location.locLng);
		}
		
		/*
		 * We only show the delete button in the view-location page if the location
		 * has not associated artifacts and when it's accessed via the manage-locations page
		 */
		if(hasDeleteButton) {
			
			$('#delete-location-button').show();
			window.android.hasArtifactsAtLocation(location.locId);
		}
		else {
			
			$('#delete-location-button').hide();
		}

		$('#delete-location-name').data({ navigateTo : '#view-location' });
		$.mobile.changePage($('#view-location'), "none");
	});
}

