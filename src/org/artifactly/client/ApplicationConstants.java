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

package org.artifactly.client;

public interface ApplicationConstants {

	/*
	 * Radius units
	 */
	public static final String UNIT_M = "m";
	public static final String UNIT_KM = "km";
	public static final String UNIT_FT = "ft";
	public static final String UNIT_MI = "mi";
	
	public static final String NOTIFICATION_INTENT_KEY = "message";
	public static final String PREFERENCE_RADIUS = "radius";
	public static final String PREFERENCE_BACKGROUND_COLOR = "background-color";
	public static final String PREFERENCE_SOUND_NOTIFICATION = "sound-notification";
	public static final int PREFERENCE_RADIUS_DEFAULT = 2000;
	public static final int PREFERENCE_RADIUS_MIN = 1;
	public static final int PREFERENCE_RADIUS_MAX = 20000;
	public static final String PREFERENCE_BACKGROUND_COLOR_DEFAULT = "#ADDFFF";
	public static final boolean PREFERENCE_SOUND_NOTIFICATION_DEFAULT = true;
	public static final String PREFERENCE_LOAD_STATIC_MAP = "load-static-map";
	public static final boolean PREFERENCE_LOAD_STATIC_MAP_DEFAULT = true;
	public static final String PREFERENCE_RADIUS_UNIT = "radius-unit";
	public static final String PREFERENCE_RADIUS_UNIT_DEFAULT = UNIT_M;
	public static final String LOCATION_UPDATE_INTENT = "org.artifactly.client.service.LocationUpdateIntent";
	public static final String HAS_ARTIFACTS_AT_CURRENT_LOCATION_INTENT = "org.artifactly.client.service.HasArtifactsAtCurrentLocationIntent";
	
	/*
	 *  Byte masks for creating artifact/location
	 *
	 *  1 : Provided name matches a location name with different latitude/longitude, please choose a different name
	 *  2 : Provided latitude and longitude match an existing location, we are using that location (location name) instead
	 *  4 : Using existing location that matched the provided location name, latitude, and longitude
	 *  8 : Creating new location
	 * 16 : Creating new artifact
	 * 32 : Using existing artifact
	 * 64 : ERROR
	 */
	public static final byte IS_MATCH = 0;						  // 00000000
	public static final byte DEFAULT_MASK = 0;					  // 00000000
	public static final byte CHOOSE_DIFFERENT_LOC_NAME = 1; 	  // 00000001
	public static final byte USING_EXISTING_LOCATION_NAME = 2;    // 00000010
	public static final byte USING_EXISTING_LOCATION = 4;		  // 00000100
	public static final byte CREATING_NEW_LOCATION = 8;		      // 00001000
	public static final byte CREATING_NEW_ARTIFACT = 16;		  // 00010000
	public static final byte USING_EXISTING_ARTIFACT = 32;		  // 00100000
	public static final byte CREATE_ARTIFACT_LOCATION_ERROR = 64; // 01000000
	
	/*
	 * Combined byte masks
	 */
	public static final byte ARTIFACT_AND_LOCATION_EXIST = 36;	  // 00100100
	
	/*
	 * Multibyte masks
	 */
	public static final byte ARTIFACT_NAME_ERROR = 7;			  // 00000111
	public static final byte LOCATION_NAME_ERROR = 56;			  // 00111000
	
}
