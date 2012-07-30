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
 * This is a mock implementation of the Artifactly.java JavaScriptInterface,
 * which allows for UI testing in a browser
 * 
 */


// Only define window.android and its methods if it's undefined
if(typeof window.android == "undefined") {
		
	window.android = {};
	
	window.android.getApiKeys = function() {
		
		return '';
	}

	window.android.getArtifactsForCurrentLocation = function () {

		var data = '[{"artifacts":[{"artData":"1 lb","artName":"sugar ","artId":4}],"lng":"-121.534371","lat":"38.551307","locName":"Nugget Markets"},{"artifacts":[{"artData":"regular","artName":"coffee ","artId":1},{"artData":"moca","artName":"coffee 2","artId":2}],"lng":"-121.535248","lat":"38.551115","locName":"Starbucks"}]';
	
		getArtifactsForCurrentLocationCallback(JSON.parse(data));
	};

	window.android.getRadius = function () {

		return 1000;
	}

	window.android.canAccessInternet = function () {

		return true;
	}

	window.android.getLocation = function() {

		return '{"locLat":38.5400438,"locLng":-121.5798584,"locAccuracy":84}';
	}

	window.android.setRadius = function(radius) {

		alert("Setting Radius = " + radius);
	}

	window.android.showRadius = function() {

		alert("Setting Radius = 1000");
	}

	window.android.createArtifact = function(name, data) {

		alert("Created Artifact: name = " + name + " : data = " + data);
	}


	window.android.getArtifacts = function() {

		var data = '[{"artifacts":[{"artData":"1 lb","artName":"sugar ","artId":4}],"lng":"-121.534371","lat":"38.551307","locName":"Nugget Markets"},{"artifacts":[{"artData":"regular","artName":"coffee ","artId":1},{"artData":"moca","artName":"coffee 2","artId":2}],"lng":"-121.535248","lat":"38.551115","locName":"Starbucks"}]';
		
		getArtifactsCallback(JSON.parse(data));
	}
	
	window.android.getArtifact = function(id) {
		
		var data =  new Array();
		data[0] = "[{}]";
		data[1] = '[{"artData":"regular","artId":1,"lng":"-121.535248","lat":"38.551115","artName":"coffee","locName":"Starbucks"}]';
		data[2] = '[{"artData":"moca","artId":2,"lng":"-121.535248","lat":"38.551115","artName":"coffee","locName":"Starbucks"}]';
		data[3] = '[{}]';
		data[4] = '[{"artData":"1 lb","artId":4,"lng":"-121.534371","lat":"38.551307","artName":"sugar","locName":"Nugget Markets"}]';
		getArtifactCallback(JSON.parse(data[id]));
	}
	
	window.android.getLocations = function(type) {
		
		var locations = '[{"locLat":"38.53991935","locId":1,"locLng":"-121.57961713333333","locName":"home"},{"locLat":"38.540062000000006","locId":2,"locLng":"-121.5798648","locName":"home"},{"locLat":"38.754536","locId":3,"locLng":"-121.252692","locName":"carmax"},{"locLat":"38.540018360000005","locId":4,"locLng":"-121.57981720000001","locName":"library"},{"locLat":"38.53995125","locId":5,"locLng":"-121.57965679999998","locName":"home "},{"locLat":"38.551167","locId":6,"locLng":"-121.70006","locName":"psl"},{"locLat":"38.539981600000004","locId":7,"locLng":"-121.5797042","locName":"cvvbbb"},{"locLat":"38.54002381428571","locId":8,"locLng":"-121.57970111428573","locName":"Target Specialty Products"},{"locLat":"38.563057","locId":9,"locLng":"-121.498367","locName":"Target"},{"locLat":"38.540013","locId":10,"locLng":"-121.57983","locName":"Amsler Consulting"}]';
	
		if(type == "list") {
			
			getLocationsListCallback(JSON.parse(locations));
		}
		else if(type == "options") {
			
			getLocationsOptionsCallback(JSON.parse(locations));
		}
	}

	window.android.setBackgroundColor = function(color) {
	
		console.log("MOCK: setBackgroundColor() color = " + color);
	}
	
	window.android.setSoundNotificationPreference = function(checked) {
		
	}
	
	window.android.getSoundNotificationPreference = function() {
		
		return true;
	}
	
	window.android.getPreferences = function() {
		
		return '{ "radius":1000,"soundNotification":true,"loadStaticMap":true }';
	}
	
	window.android.canLoadStaticMap = function() {
		
		return true;
	}
}