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

import android.location.Location;

public interface LocalService {

	/**
	 * Creates an Artifact for the provided location
	 * 
	 * @param artifactName Artifact name
	 * @param artifactData Artifact data
	 * @param locationName Artifact's location name
	 * @param latitude Artifact latitude
	 * @param longitude Artifact longitude
	 * @return byte mask indicating different status combinations
	 */
	public byte createArtifact(String artifactName, String artifactData, String locationName, String latitude, String longitude);
	
	/**
	 * Update an Artifact
	 * 
	 * @param artifactId
	 * @param artifactName
	 * @param artifactData
	 * @param locationId
	 * @param locationName
	 * @return 1 on success, -1 if we have an artifact name collision, -4 if we have a location name collision, -2 on error
	 */
	public int updateArtifact(String artifactId, String artifactName, String artifactData, String locationId, String locationName);
	
	/**
	 * 
	 * @param artifactId
	 * @param artifactData
	 * @return 1 on success, -1 on error
	 */
	public int updateArtifactData(String artifactId, String artifactData);
	
	/**
	 * Update a Location
	 * 
	 * @param locationId
	 * @param locationName
	 * @param locationLat
	 * @param locationLng
	 * @return 1 on success, -1 if we have a location name collision, -2 on error
	 */
	public int updateLocation(String locationId, String locationName, String locationLat, String locationLng);
	
	/**
	 * Update a location's coordinates
	 * 
	 * @param locationId
	 * @param locationName
	 * @param locationLat
	 * @param locationLng
	 * @return 1 on success, -1 if we have a location coordinate collision, -2 on error
	 */
	public int updateLocationCoodinates(String locationId, String locationName, String locationLat, String locationLng);

	/**
	 * Get the current location, latitude, longitude, accuracy
	 * 
	 * @return location
	 */
	public Location getLocation();
	
	/**
	 * Get all artifacts
	 * 
	 * @return JSON all artifacts
	 */
	public String getArtifacts();
	
	/**
	 * Get artifacts for current location
	 * 
	 * @return JSON current location artifacts
	 */
	public String getArtifactsForCurrentLocation();
	
	/**
	 * Delete an artifact
	 * 
	 * @param artifactId
	 * @param locationId
	 * @return -1 on error, 0 on cannot delete because artifact has associated locations, 1 on success
	 */
	public int deleteArtifact(String artifactId, String locationId);
	
	/**
	 * 
	 * @param locationId
	 * @return -1 on error, 0 on cannot delete because location has associated artifacts, 1 on success
	 */
	public int deleteLocation(String locationId);
	
	/**
	 * Get an artifact that matches the provided id
	 * 
	 * @param artId Artifact DB row id
	 * @param locId Location DB row id
	 * @return JSON artifact that matches the provided IDs
	 */
	public String getAtrifact(String artId, String locId);
	
	/**
	 * Get all locations
	 * 
	 * @return JSON containing all locations
	 */
	public String getLocations();
	
	/**
	 * Has location any associated artifacts
	 * 
	 * @param locId Location DB row id
	 * 
	 * @return true if location has associated artifacts, false otherwise 
	 */
	public boolean hasArtifactsAtLocation(String locId);

}
