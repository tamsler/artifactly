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

package org.artifactly.client.service;

import org.artifactly.client.content.DbAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.location.Location;
import android.os.Binder;
import android.util.Log;

public class LocalServiceImpl extends Binder implements LocalService {

	// Logging
	private static final String PROD_LOG_TAG = "** A.L.S. **";
	
	// Artifact Filters
	private static final int ALL_ARTIFACTS_FILTER = 0;
	private static final int CURRENT_LOCATION_ARTIFACTS_FILTER = 1;
	
	private ArtifactlyService artifactlyService;
	
	private DbAdapter dbAdapter;
	
	// Constructor
	public LocalServiceImpl(ArtifactlyService artifactlyService) { 

		this.artifactlyService = artifactlyService;
		
		if(null != artifactlyService) {
		
			this.dbAdapter = artifactlyService.getDbAdapter();
		}
	}
	
	// API method
	public byte createArtifact(String artifactName, String artifactData, String locationName, String latitude, String longitude) {
		
		if(null == dbAdapter) {

			return -1;
		}
		
		return dbAdapter.insert(locationName, latitude, longitude, artifactName, artifactData);
	}

	// API method
	public boolean startLocationTracking() {
		
		if(null != artifactlyService) {
			
			artifactlyService.startLocationTracking();
		}
		else {
			
			return false;
		}
		
		return true;
	}
	
	// API method
	public boolean stopLocationTracking() {

		if(null != artifactlyService) {

			artifactlyService.stopLocationTracking();
		}
		else {

			return false;
		}
		
		return true;
	}

	// API method
	public Location getLocation() {
		
		return artifactlyService.getLocation();
	}

	// API method
	public String getArtifacts() {
		
		return getFilteredArtifacts(ALL_ARTIFACTS_FILTER);
	}

	// API method
	public String getArtifactsForCurrentLocation() {
		
		return getFilteredArtifacts(CURRENT_LOCATION_ARTIFACTS_FILTER);
	}

	// API method
	public int deleteArtifact(String artifactId, String locationId) {
		
		if(null == dbAdapter) {
			
			return -1;
		}
		
		return dbAdapter.deleteArtifact(artifactId, locationId);
	}
	
	public int deleteLocation(String locationId) {
		
		if(null == dbAdapter) {

			return -1;
		}

		return dbAdapter.deleteLocation(locationId);
	}
	
	// API method
	public String getAtrifact(String artId, String locId) {
	
		// JSON array that holds the result
		JSONObject artifact = new JSONObject();
		
		if(null == dbAdapter) {
			
			return artifact.toString();
		}
		
		// Getting all the locations
		Cursor cursor = dbAdapter.select(artId, locId);
		if(null == cursor) {
			
			return artifact.toString();
		}
		
		// Checking if the cursor set has any items
		boolean hasItems = cursor.moveToFirst();
		
		if(!hasItems) {
			
			cursor.close();
			return artifact.toString();
		}

		if(cursor.getCount() != 1) {
			
			Log.e(PROD_LOG_TAG, "Expetecd the cursor to have only one row");
		}
		
		// Determine the table column indexes 
		int artIdColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID]);
		int artNameColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME]);
		int artDataColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA]);
		int locIdColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_LOC_ID]);
		int locNameColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_NAME]);
		int longitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE]);
		int latitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE]);

		try {
			
			artifact.put(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID], cursor.getInt(artIdColumnIndex));
			artifact.put(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME], cursor.getString(artNameColumnIndex));
			artifact.put(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA], cursor.getString(artDataColumnIndex));
			artifact.put(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_LOC_ID], cursor.getInt(locIdColumnIndex));
			artifact.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_NAME], cursor.getString(locNameColumnIndex));
			artifact.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE], cursor.getString(latitudeColumnIndex));
			artifact.put(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE], cursor.getString(longitudeColumnIndex));
		}
		catch (JSONException e) {
			
			Log.e(PROD_LOG_TAG, "Error while populating JSONObject", e);
		}

		cursor.close();
		
		return artifact.toString();
	}

	// API method
	public String getLocations() {
		
		// JSON array that holds the result
		JSONArray items = new JSONArray();
		
		if(null == dbAdapter) {
			
			return items.toString();
		}
		
		// Getting all the locations
		Cursor cursor = dbAdapter.getLocations();
		if(null == cursor) {
			
			return items.toString();
		}
		
		// Checking if the cursor set has any items
		boolean hasItems = cursor.moveToFirst();
		
		if(!hasItems) {
			
			cursor.close();
			return items.toString();
		}

		// Determine the table column indexes
		int locIdColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_ID]);
		int locNameColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_NAME]);
		int locLatColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE]);
		int locLngColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE]);
		
		/*
		 *  Iterating over all result and calculate the distance between
		 *  radius, we notify the user that there is an artifact.
		 */
		for(;cursor.isAfterLast() == false; cursor.moveToNext()) {

			JSONObject item = new JSONObject();

			try {
				
				item.put(DbAdapter.LOC_FIELDS_AS[DbAdapter.LOC_ID], cursor.getInt(locIdColumnIndex));
				item.put(DbAdapter.LOC_FIELDS_AS[DbAdapter.LOC_NAME], cursor.getString(locNameColumnIndex));
				item.put(DbAdapter.LOC_FIELDS_AS[DbAdapter.LOC_LATITUDE], cursor.getString(locLatColumnIndex));
				item.put(DbAdapter.LOC_FIELDS_AS[DbAdapter.LOC_LONGITUDE], cursor.getString(locLngColumnIndex));
			}
			catch (JSONException e) {
				
				Log.e(PROD_LOG_TAG, "Error while populating JSONObject", e);
			}

			items.put(item);
		}

		cursor.close();

		if(items.length() == 0) {

			return null;
		}
		else {
	
			return items.toString();
		}
	}
	
	// Helper method
	private String getFilteredArtifacts(int filter) {
		
		// JSON array that holds the result
		JSONArray locations = new JSONArray();
		
		if(null == dbAdapter) {
			
			return locations.toString();
		}
		
		// Getting all the locations
		Cursor cursor = dbAdapter.select();
		if(null == cursor) {
			
			return locations.toString();
		}
		
		// Checking if the cursor set has any items
		boolean hasItems = cursor.moveToFirst();
		
		if(!hasItems) {
			
			cursor.close();
			return locations.toString();
		}

		// Determine the table column indexes 
		int artIdColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID]);
		int artNameColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME]);
		int artDataColumnIndex = cursor.getColumnIndex(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA]);
		int locIdColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_LOC_ID]);
		int locNameColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_NAME]);
		int longitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE]);
		int latitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE]);

		String currentLocationName = null;
		JSONObject location = null;
		
		for(;cursor.isAfterLast() == false; cursor.moveToNext()) {
			
			switch(filter) {
				case CURRENT_LOCATION_ARTIFACTS_FILTER:
					// If the current location is not nearby, we don't add it
					String lat = cursor.getString(latitudeColumnIndex).trim();
					String lng = cursor.getString(longitudeColumnIndex).trim();
					if(!artifactlyService.isNearbyCurrentLocation(lat, lng)) {
						continue;
					}
					break;
				case ALL_ARTIFACTS_FILTER:
					// Don't do anything
					break;
				default:
					// Don't do anything
			}
			
			// Get the location
			String locationName = cursor.getString(locNameColumnIndex);
			
			try {
				
				// Check if we need to setup an new location
				if(null == currentLocationName || !currentLocationName.equals(locationName)) {

					currentLocationName = locationName;
					location = new JSONObject();
					location.put(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_LOC_ID], cursor.getInt(locIdColumnIndex));
					location.put(DbAdapter.LOC_FIELDS_AS[DbAdapter.LOC_NAME], cursor.getString(locNameColumnIndex));
					location.put(DbAdapter.LOC_FIELDS_AS[DbAdapter.LOC_LATITUDE], cursor.getString(latitudeColumnIndex));
					location.put(DbAdapter.LOC_FIELDS_AS[DbAdapter.LOC_LONGITUDE], cursor.getString(longitudeColumnIndex));
					location.put("artifacts", new JSONArray());
					locations.put(location);
				}
				
				JSONObject artifact = new JSONObject();	
				artifact.put(DbAdapter.LOC_ART_FIELDS[DbAdapter.FK_ART_ID], cursor.getInt(artIdColumnIndex));
				artifact.put(DbAdapter.ART_FIELDS[DbAdapter.ART_NAME], cursor.getString(artNameColumnIndex));
				artifact.put(DbAdapter.ART_FIELDS[DbAdapter.ART_DATA], cursor.getString(artDataColumnIndex));
				
				location.getJSONArray("artifacts").put(artifact);
				
			}
			catch (JSONException e) {
				
				Log.e(PROD_LOG_TAG, "Error while populating JSONObject", e);
			}
		}

		cursor.close();

		if(locations.length() == 0) {

			return null;
		}
		else {
	
			return locations.toString();
		}
	}

	public int updateArtifact(String artifactId, String artifactName, String artifactData, String locationId, String locationName) {
		
		if(null == dbAdapter) {
			
			return -2;
		}
		
		return dbAdapter.updateArtifact(artifactId, artifactName, artifactData, locationId, locationName);
	}

	
	public int updateArtifactData(String artifactId, String artifactData) {
		
		if(null == dbAdapter) {
			
			return -1;
		}
		
		return dbAdapter.updateArtifactData(artifactId, artifactData);
	}
	
	public int updateLocation(String locationId, String locationName, String locationLat, String locationLng) {
		
		if(null == dbAdapter) {
			
			return -2;
		}
		
		return dbAdapter.updateLocation(locationId, locationName, locationLat, locationLng);
	}
	
	public int updateLocationCoodinates(String locationId, String locationName, String locationLat, String locationLng) {
		
		if(null == dbAdapter) {
			
			return -2;
		}
		
		return dbAdapter.updateLocationCoodinates(locationId, locationName, locationLat, locationLng);
	}

	public boolean hasArtifactsAtLocation(String locId) {
		
		if(null == dbAdapter) {
			
			return false;
		}
		
		return dbAdapter.hasArtifactsAtLocation(locId);
	}
}
