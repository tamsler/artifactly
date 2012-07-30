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

package org.artifactly.client.content;

import org.artifactly.client.ApplicationConstants;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class DbAdapter implements ApplicationConstants {

	private static final String PROD_LOG_TAG = "** A.S.DB **";

	private DatabaseHelper mDatabaseHelper;
	private SQLiteDatabase mSQLiteDatabase;

	private static final String DATABASE_NAME = "ArtifactlyData";
	private static final String DB_TABLE_LOCATION = "Location";
	private static final String DB_TABLE_ARTIFACT = "Artifact";
	private static final String DB_TABLE_LOC_TO_ART = "LocToArt";
	private static final int DATABASE_VERSION = 3;

	public static final String [] LOC_FIELDS = {"_id", "locName", "lat", "lng"};
	public static final String [] LOC_FIELDS_AS = {"locId", "locName", "locLat", "locLng"};
	public static final String [] ART_FIELDS = {"_id", "artName", "artData", "artCreationDate"};
	public static final String [] LOC_ART_FIELDS = {"artId", "locId" };

	public static final int LOC_ID = 0;
	public static final int LOC_NAME = 1;
	public static final int LOC_LATITUDE = 2;
	public static final int LOC_LONGITUDE = 3;

	public static final int ART_ID = 0;
	public static final int ART_NAME = 1;
	public static final int ART_DATA = 2;
	public static final int ART_CREATION_DATE = 3;

	public static final int FK_ART_ID = 0;
	public static final int FK_LOC_ID = 1;
	
	private static final String CREATE_LOCATION_TABLE =
		"create table " + DB_TABLE_LOCATION + " (" + LOC_FIELDS[LOC_ID] + " INTEGER primary key autoincrement, "
		+ LOC_FIELDS[LOC_NAME] + " TEXT not null, "
		+ LOC_FIELDS[LOC_LATITUDE] + " TEXT not null, "
		+ LOC_FIELDS[LOC_LONGITUDE] + " TEXT not null);";

	private static final String CREATE_ARTIFACT_TABLE =
		"create table " + DB_TABLE_ARTIFACT + "(" + ART_FIELDS[ART_ID] + " INTEGER primary key autoincrement, "
		+ ART_FIELDS[ART_NAME] + " TEXT not null, "
		+ ART_FIELDS[ART_DATA] + " TEXT, "
		+ ART_FIELDS[ART_CREATION_DATE] + " DATETIME default current_timestamp);";

	private static final String CREATE_LOC_TO_ART_TABLE =
		"create table " + DB_TABLE_LOC_TO_ART + "(" + LOC_ART_FIELDS[FK_ART_ID] + " INTEGER REFERENCES " + DB_TABLE_ARTIFACT + "(" + ART_FIELDS[ART_ID] + "), "
		+ LOC_ART_FIELDS[FK_LOC_ID] + " INTEGER REFERENCES " + DB_TABLE_LOCATION + "(" + LOC_FIELDS[LOC_ID] + "),"
		+ "PRIMARY KEY (" + LOC_ART_FIELDS[FK_ART_ID] + ", " + LOC_ART_FIELDS[FK_LOC_ID] + "))";


	// Constructor that initializes the database
	public DbAdapter(Context context) {

		mDatabaseHelper = new DatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
		mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
	}

	public void close() {

		mDatabaseHelper.close();
	}

	/*
	 * The insert checks first if the entity exists, if the entity exists, nothing is inserted.
	 * If the entity doesn't exist, a new DB record is created for it.
	 */
	public byte insert(String locationName, String latitude, String longitude, String artifactName, String artifactData) {

		long locationRowID = -1;
		long artifactRowId = -1;
		byte returnStatus = DEFAULT_MASK;
		ContentValues contentValues = null;

		try {

			/*
			 * If the user selected the current location, and the provided name matches a different
			 * location (lat/lng) then we inform the user that he/she needs to choose a different name.
			 */
			if(!isValidLocation(locationName, latitude, longitude)) {
				
				returnStatus |= CHOOSE_DIFFERENT_LOC_NAME;
				return returnStatus;
			}

			/*
			 * Check if the artifact @ location already exists
			 */
			boolean hasArtifactAtLocation = hasArtifactAtLocation(artifactName, locationName, latitude, longitude);
			
			if(hasArtifactAtLocation) {
				
				returnStatus |= USING_EXISTING_ARTIFACT;
				returnStatus |= USING_EXISTING_LOCATION;
				return returnStatus;
			}
			
			// Check if the location exists. If it does, we reuse it
			locationRowID = getLocation(latitude, longitude);
			
			//If the above location search didn't match, we create a new location record
			if(-1 == locationRowID) {

				contentValues = new ContentValues();
				contentValues.put(LOC_FIELDS[LOC_NAME], locationName);
				contentValues.put(LOC_FIELDS[LOC_LATITUDE], latitude);
				contentValues.put(LOC_FIELDS[LOC_LONGITUDE], longitude);
				locationRowID = mSQLiteDatabase.insert(DB_TABLE_LOCATION, null, contentValues);
			}

			// Check if artifact already exists. If it does, we reuse it
			artifactRowId = getArtifact(artifactName);

			/*
			 * If the above search didn't result in a match, we create a new artifact record
			 */
			if(-1 == artifactRowId) {

				// Artifact doesn't exist so we create a new db record for it 
				contentValues = new ContentValues();
				contentValues.put(ART_FIELDS[ART_NAME], artifactName);
				contentValues.put(ART_FIELDS[ART_DATA], artifactData);
				artifactRowId = mSQLiteDatabase.insert(DB_TABLE_ARTIFACT, null, contentValues);
			}
			
			/*
			 * Creating the artifact / location association. Since we check if the artifact / location 
			 * association exists above, we know that this is a new association and thus we create it
			 */
			contentValues = new ContentValues();
			contentValues.put(LOC_ART_FIELDS[FK_ART_ID], artifactRowId);
			contentValues.put(LOC_ART_FIELDS[FK_LOC_ID], locationRowID);
			mSQLiteDatabase.insert(DB_TABLE_LOC_TO_ART, null, contentValues);
		}
		catch(SQLiteException e) {
			
			Log.e(PROD_LOG_TAG, "SQLiteException insert()", e);
			returnStatus |= CREATE_ARTIFACT_LOCATION_ERROR;
		}
		
		return returnStatus;
	}

	/*
	 * Deleting artifact and its artifact to location mapping
	 */
	public int deleteArtifact(String artifactId, String locationId) {
		
		try {
			
			// Delete from location/artifact mapping
			int numRowsAffected = mSQLiteDatabase.delete(DB_TABLE_LOC_TO_ART, LOC_ART_FIELDS[FK_ART_ID] + "=? AND " + LOC_ART_FIELDS[FK_LOC_ID] + "=?", new String[] {artifactId, locationId});

			if(numRowsAffected != 1) {
				
				return -1;
			}

			// Only delete artifact if it's not referenced by a location
			if(!isArtifactReferenced(artifactId)) {

				numRowsAffected = mSQLiteDatabase.delete(DB_TABLE_ARTIFACT, ART_FIELDS[ART_ID] + "=?", new String[] {artifactId});
				
				if(numRowsAffected != 1) {
					
					return -1;
				}
				else {
					
					return 1;
				}
			}
		}
		catch(SQLiteException e) {
			
			Log.e(PROD_LOG_TAG, "SQLiteException: deleteArtifact()", e);
			return -1;
		}
		
		return 1;
	}
	
	/*
	 * Select one artifact 
	 */
	public Cursor select(String artifactId, String locationId) {
		
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables("LocToArt JOIN Artifact ON (LocToArt.artId=Artifact._id) JOIN Location ON (LocToArt.locId=Location._id)");
		queryBuilder.setDistinct(true);
		return queryBuilder.query(mSQLiteDatabase,
				new String[] {"Artifact._id AS artId",
							  "Location._id AS locId",
							  "Artifact.artName AS artName",
							  "Artifact.artData AS artData",
							  "Location.locName AS locName",
							  "Location.lat AS lat",
							  "Location.lng AS lng"},
							  LOC_ART_FIELDS[FK_ART_ID] + "=? AND " + LOC_ART_FIELDS[FK_LOC_ID] + "=? ",
							  new String[] {artifactId, locationId}, null, null, null);
	}
	
	/*
	 * Select all locations
	 * NOTE: Caller must call cursor.close()
	 */
	public Cursor getLocations() {

		return mSQLiteDatabase.query(true,
				DB_TABLE_LOCATION,
				LOC_FIELDS,
				null, null, null, null, "Location.locName ASC", null);
	}
	
	/*
	 * Update an Artifact
	 */
	public int updateArtifact(String artifactId, String artifactName, String artifactData, String locationId, String locationName) {
		
		/*
		 * First, we need to check if the update changes the artifact name to 
		 * an existing artifact name at that location. We don't allow duplicate
		 * artifacts at the same location
		 */
		boolean hasArtifactAtLocation = hasArtifactAtLocation(artifactName, locationName, locationId);
		if(hasArtifactAtLocation) {
			
			return -1;
		}
		
		/*
		 * Then, we need to check if location name gets changed to an existing location name
		 */
		if(!isValidLocation(locationName, locationId)) {
			
			return -4;
		}
		
		ContentValues artContentValues = new ContentValues();
		artContentValues.put(ART_FIELDS[ART_NAME], artifactName);
		artContentValues.put(ART_FIELDS[ART_DATA], artifactData);
		int numberArtRowsAffected = mSQLiteDatabase.update(DB_TABLE_ARTIFACT, artContentValues, ART_FIELDS[ART_ID] + "=?", new String[] {artifactId});
		
		ContentValues locContentValues = new ContentValues();
		locContentValues.put(LOC_FIELDS[LOC_NAME], locationName);
		int numberLocRowsAffected = mSQLiteDatabase.update(DB_TABLE_LOCATION, locContentValues, LOC_FIELDS[LOC_ID] + "=?", new String[] {locationId});
		
		return ((numberArtRowsAffected == 1 && numberLocRowsAffected ==  1) ? 1 : -2);
	}
	
	/*
	 * Update Artifact Data
	 */
	public int updateArtifactData(String artifactId, String artifactData) {
		
		ContentValues artContentValues = new ContentValues();
		artContentValues.put(ART_FIELDS[ART_DATA], artifactData);
		int numberArtRowsAffected = mSQLiteDatabase.update(DB_TABLE_ARTIFACT, artContentValues, ART_FIELDS[ART_ID] + "=?", new String[] {artifactId});
		
		return ((numberArtRowsAffected == 1) ? 1 : -1);
	}
	
	/*
	 * Update a Location
	 */
	public int updateLocation(String locationId, String locationName, String locationLat, String locationLng) {
		
		/*
		 * First, we need to check if the update changes the location name to
		 * an existing location name. 
		 */
		boolean hasLocation = hasLocation(locationName);
		if(hasLocation) {
			
			return -1;
		}
		ContentValues locContentValues = new ContentValues();
		locContentValues.put(LOC_FIELDS[LOC_NAME], locationName);
		int numberLocRowsAffected = mSQLiteDatabase.update(DB_TABLE_LOCATION, locContentValues, LOC_FIELDS[LOC_ID] + "=?", new String[] {locationId});
		
		return ((numberLocRowsAffected ==  1) ? 1 : -2);
	}
	
	
	/*
	 * Update a location's coordinates
	 */
	public int updateLocationCoodinates(String locationId, String locationName, String locationLat, String locationLng) {
		
		
		/*
		 * Check if location for provided coordinates exist
		 */
		long locId = getLocation(locationLat, locationLng);
		
		if(-1 == locId) {
		
			ContentValues locContentValues = new ContentValues();
			locContentValues.put(LOC_FIELDS[LOC_NAME], locationName);
			locContentValues.put(LOC_FIELDS[LOC_LATITUDE], locationLat);
			locContentValues.put(LOC_FIELDS[LOC_LONGITUDE], locationLng);
			int numberLocRowsAffected = mSQLiteDatabase.update(DB_TABLE_LOCATION, locContentValues, LOC_FIELDS[LOC_ID] + "=?", new String[] {locationId});

			return ((numberLocRowsAffected ==  1) ? 1 : -2);
		}
		else {
			
			return -1;
		}
	}
	
	/*
	 * Select all the location and artifact relationships
	 * NOTE: Caller must call cursor.close()
	 */
	public Cursor select() {

		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables("LocToArt JOIN Artifact ON (LocToArt.artId=Artifact._id) JOIN Location ON (LocToArt.locId=Location._id)");
		queryBuilder.setDistinct(true);
		return queryBuilder.query(mSQLiteDatabase,
				new String[] {"Artifact._id AS artId",
							  "Location._id AS locId",
							  "Artifact.artName AS artName",
							  "Artifact.artData AS artData",
							  "Location.locName AS locName",
							  "Location.lat AS lat",
							  "Location.lng AS lng"},
				null, null, null, null, "Location.locName ASC, Artifact.artName ASC");
	}

	/*
	 * Delete location if it doesn't have any artifact mappings
	 */
	public int deleteLocation(String locationId) {
		
		try {
			
			// Only delete location if it's not referenced by an artifact
			if(!hasLocationInLocToArtTable(locationId)) {

				int numAffectedRows = mSQLiteDatabase.delete(DB_TABLE_LOCATION, LOC_FIELDS[LOC_ID] + "=?", new String[] {locationId});
				
				if(numAffectedRows != 1) {
					
					return -1;
				}
			}
			else {
				
				return 0;
			}

		}
		catch(SQLiteException e) {
			
			Log.e(PROD_LOG_TAG, "SQLiteException: deleteLocation()", e);
			return -1;
		}

		return 1;
	}
	
	/*
	 * Check if a location has any associated artifacts
	 */
	public boolean hasArtifactsAtLocation(String locId) {
		
		boolean hasArtifacts = false;
		
		try {
		
			hasArtifacts = hasLocationInLocToArtTable(locId);
		}
		catch(SQLiteException e) {

			Log.e(PROD_LOG_TAG, "SQLiteException: hasArtifactsAtLocation()", e);
		}
		
		return hasArtifacts;
	}
	
	/*
	 * Helper method that checks if the provided artifactRowId is part of an existing location and artifact relationship
	 */
	private boolean isArtifactReferenced(String artifactId) {

		Cursor cursor = mSQLiteDatabase.query(true, DB_TABLE_LOC_TO_ART, LOC_ART_FIELDS, LOC_ART_FIELDS[FK_ART_ID] + "=?", new String [] {artifactId}, null, null, null, null);

		if((null != cursor) && (0 < cursor.getCount())) {

			cursor.close();
			return true;
		}
		else {
			
			if(null != cursor) {
			
				cursor.close();
			}
			
			return false;
		}
	}

	/*
	 * Helper method that checks if a location with the provided name exists
	 */
	private boolean hasLocation(String locationName) {
		
		
		Cursor cursor = mSQLiteDatabase.query(true,
				DB_TABLE_LOCATION,
				LOC_FIELDS,
				LOC_FIELDS[LOC_NAME] + "=?",
				new String [] {locationName},
				null,
				null,
				null,
				null);

		if((null != cursor) && (1 == cursor.getCount())) {

			cursor.close();
			return true;
		}
		else {

			if(null != cursor) {

				cursor.close();
			}

			return false;
		}
	}
	
	/*
	 * Helper method that checks if the provided locationRowId is part of an existing location and artifact relationship
	 */
	private boolean hasLocationInLocToArtTable(String locationId) {

		Cursor cursor = mSQLiteDatabase.query(true, DB_TABLE_LOC_TO_ART, LOC_ART_FIELDS, LOC_ART_FIELDS[FK_LOC_ID] + "=?", new String [] {locationId}, null, null, null, null);

		if((null != cursor) && (0 < cursor.getCount())) {
			
			cursor.close();
			return true;
		}
		else {
			
			if(null != cursor) {

				cursor.close();
			}
			
			return false;
		}
	}

	/*
	 * Helper method that checks if an artifact/location exists
	 */
	private boolean hasArtifactAtLocation(String artifactName, String locationName, String locationLat, String locationLng) {
	
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables("LocToArt JOIN Artifact ON (LocToArt.artId=Artifact._id) JOIN Location ON (LocToArt.locId=Location._id)");
		queryBuilder.setDistinct(true);
		StringBuilder selection = new StringBuilder();
		selection.append(ART_FIELDS[ART_NAME])
		.append("=? AND ")
		.append(LOC_FIELDS[LOC_NAME])
		.append("=? AND ")
		.append(LOC_FIELDS[LOC_LATITUDE])
		.append("=? AND ")
		.append(LOC_FIELDS[LOC_LONGITUDE])
		.append("=? ");
		Cursor cursor = queryBuilder.query(mSQLiteDatabase,
										  new String[] {"Artifact._id"},
														selection.toString(), 
														new String [] {artifactName, locationName, locationLat, locationLng},
														null, null, null);

		if((null != cursor) && (1 == cursor.getCount())) {

			cursor.close();
			return true;
		}
		else {

			if(null != cursor) {

				cursor.close();
			}

			return false;
		}
	}

	
	/*
	 * Helper method that checks if an artifact/location exists
	 */
	private boolean hasArtifactAtLocation(String artifactName, String locationName, String locationId) {
	
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables("LocToArt JOIN Artifact ON (LocToArt.artId=Artifact._id) JOIN Location ON (LocToArt.locId=Location._id)");
		queryBuilder.setDistinct(true);
		StringBuilder selection = new StringBuilder();
		selection.append(ART_FIELDS[ART_NAME])
		.append("=? AND ")
		.append(LOC_FIELDS[LOC_NAME])
		.append("=? AND ")
		.append(LOC_ART_FIELDS[FK_LOC_ID])
		.append("=? ");
		Cursor cursor = queryBuilder.query(mSQLiteDatabase,
										  new String[] {"Artifact._id"},
														selection.toString(), 
														new String [] {artifactName, locationName, locationId},
														null, null, null);

		if((null != cursor) && (1 == cursor.getCount())) {

			cursor.close();
			return true;
		}
		else {

			if(null != cursor) {

				cursor.close();
			}

			return false;
		}
	}
	
	/*
	 * Helper method that searches by latitude, and longitude for an existing location
	 */
	private long getLocation(String latitude, String longitude) {

		Cursor cursor = mSQLiteDatabase.query(true,
				DB_TABLE_LOCATION,
				LOC_FIELDS,
				LOC_FIELDS[LOC_LATITUDE] + "=? AND " + LOC_FIELDS[LOC_LONGITUDE] + "=?",
				new String [] {latitude, longitude},
				null,
				null,
				null,
				null);

		if((null != cursor) && (1 == cursor.getCount())) {
			
			cursor.moveToFirst();
			int idColumnIndex = cursor.getColumnIndex(LOC_FIELDS[LOC_ID]);
			long rowId = cursor.getLong(idColumnIndex);
			cursor.close();
			return rowId;
		}
		else {
			
			if(null != cursor) {
			
				cursor.close();
			}
			
			return -1;
		}
	}
	
	/*
	 * Helper method that queries locations for the provided name. The result should contain zero or only
	 * one location. If the location doesn't match the provided location ID, then we signal a failure
	 * so that the user can choose another name.
	 */
	private boolean isValidLocation(String name, String locationId) {

		Cursor cursor = null;
		
		try {
			
			// Search for locations that match the given name
			cursor = mSQLiteDatabase.query(true,
					DB_TABLE_LOCATION,
					LOC_FIELDS,
					LOC_FIELDS[LOC_NAME] + "=?",
					new String [] {name},
					null,
					null,
					null,
					null);

			if((null != cursor) && (1 == cursor.getCount())) {

				cursor.moveToFirst();

				int locIdColumnIndex = cursor.getColumnIndex(LOC_FIELDS[LOC_ID]);
				String locId = String.valueOf(cursor.getLong(locIdColumnIndex));
				
				if(null != locationId && null != locId && locationId.equals(locId)) {

					cursor.close();
					return true;
				}
				else {

					cursor.close();
					return false;
				}
			}
			else if((null != cursor) && (0 == cursor.getCount())) {

				cursor.close();
				return true;
			}
			else {

				if(null != cursor) {

					cursor.close();
				}
				return false;
			}
		}
		catch(SQLiteException e) {

			if(null != cursor) {

				cursor.close();
				return false;
			}
		}

		return false;
	}
	
	/*
	 * Helper method that queries locations for the provided name. The result should contain zero or only
	 * one location. If the location doesn't match the provided Lat/Lng, then we signal a failure
	 * so that the user can choose another name.
	 */
	private boolean isValidLocation(String name, String latitude, String longitude) {

		Cursor cursor = null;
		
		try {
			
			// Search for locations that match the given name
			cursor = mSQLiteDatabase.query(true,
					DB_TABLE_LOCATION,
					LOC_FIELDS,
					LOC_FIELDS[LOC_NAME] + "=?",
					new String [] {name},
					null,
					null,
					null,
					null);

			if((null != cursor) && (1 == cursor.getCount())) {

				cursor.moveToFirst();

				int latColumnIndex = cursor.getColumnIndex(LOC_FIELDS[LOC_LATITUDE]);
				int lngColumnIndex = cursor.getColumnIndex(LOC_FIELDS[LOC_LONGITUDE]);
				String lat = cursor.getString(latColumnIndex);
				String lng = cursor .getString(lngColumnIndex);

				if(null != lat && null != lng && lat.equals(latitude) && lng.equals(longitude)) {
					
					cursor.close();
					return true;
				}
				else {
					
					cursor.close();
					return false;
				}
			}
			else if((null != cursor) && (0 == cursor.getCount())) {
				
				cursor.close();
				return true;
			}
			else {

				if(null != cursor) {
				
					cursor.close();
				}
				return false;
			}
		}
		catch(SQLiteException e) {

			if(null != cursor) {
				
				cursor.close();
				return false;
			}
		}
		
		return false;
	}

	/*
	 * Helper method that searches by name for an existing artifact
	 */
	private long getArtifact(String name) {

		Cursor cursor = mSQLiteDatabase.query(true,
				DB_TABLE_ARTIFACT, ART_FIELDS, ART_FIELDS[ART_NAME] + "=?",
				new String [] {name},
				null,
				null,
				null,
				null);

		if((null != cursor) && (1 == cursor.getCount())) {
			
			cursor.moveToFirst();
			int idColumnIndex = cursor.getColumnIndex(ART_FIELDS[ART_ID]);
			long rowId = cursor.getLong(idColumnIndex);
			cursor.close();
			return rowId;
		}
		else {
			
			if(null != cursor) {
				
				cursor.close();
			}
			return -1;
		}
	}


	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
			
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(CREATE_LOCATION_TABLE);
			db.execSQL(CREATE_ARTIFACT_TABLE);
			db.execSQL(CREATE_LOC_TO_ART_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase arg0, int oldVersion, int newVersion) {
			
		}
	}
}
