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

import org.artifactly.client.ApplicationConstants;
import org.artifactly.client.Artifactly;
import org.artifactly.client.R;
import org.artifactly.client.content.DbAdapter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class ArtifactlyService extends Service implements OnSharedPreferenceChangeListener, ApplicationConstants {

	// Logging
	//private static final String DEBUG_LOG_TAG = "** DEBUG A.S. **";
	private static final String PROD_LOG_TAG = "** A.S. **";

	// Preferences
	private static final String PREFS_NAME = "ArtifactlyPrefsFile";
	private SharedPreferences settings;

	// Location constants
	private static final long GPS_LOCATION_MIN_TIME = 300000;     // 5 min
	private static final float GPS_LOCATION_MIN_DISTANCE = 50.0f; // 50 m
	private static final long NET_LOCATION_MIN_TIME = 240000;     // 3 min
	private static final float NET_LOCATION_MIN_DISTANCE = 50.0f; // 50 m
	private static final long PAS_LOCATION_MIN_TIME = 30000;      // 30 sec
	private static final float PAS_LOCATION_MIN_DISTANCE = 50.0f; // 50 m
	protected static final String DISTANCE = "dist";
	private static final String GPS_PROVIDER = "GPS";
	private static final String NET_PROVIDER = "NET";
	private static final String PAS_PROVIDER = "PAS";

	// Location radius
	private int radius = PREFERENCE_RADIUS_DEFAULT;
	private String radiusUnit = PREFERENCE_RADIUS_UNIT_DEFAULT;
	
	// Sound Notification Preference
	private boolean soundNotificationPreference = PREFERENCE_SOUND_NOTIFICATION_DEFAULT;
	
	// Location expiration delta is used to determine if the current location
	// is current enough. If it's not, we enable the GPS listener if available 
	private static final long LOCATION_TIME_EXPIRATION_DELTA = 300000; // 5 min
	
	// The new location can only be older than the current location plus this delta
	// in order for it to be considered slightly inaccurate 
	private static final long LOCATION_TIME_ALLOWED_DELTA = 300000; // 5 min

	// Max allowed location accuracy delta
	private static final int LOCATION_MAX_ACCURACY_DELTA = 2000; // 2 km
	
	// Notification constants
	private static final int NOTIFICATION_ID = 95691;
	
	// Context Resources
	private String NOTIFICATION_TICKER_TEXT;
	private String NOTIFICATION_CONTENT_TITLE;
	private String NOTIFICATION_MESSAGE;

	// Initialization flag
	private boolean runInit = true;
	
	// Managers
	private LocationManager locationManager;
	private NotificationManager notificationManager;

	// Location provider
	private String mainLocationProviderName;
	
	// Location listeners
	private LocationListener gpsLocationListener;
	private LocationListener networkLocationListener;
	private LocationListener passiveLocationListener;
	
	// Location state
	private boolean isGpsListenerEnabled = false;
	
	// Last location update
	private long lastLocationUpdateTime = 0;
	
	// Last send notification time
	private long lastSendNotificationTime = 0;
	private long lastSendNotificationSoundTime = 0;
	private static final long LAST_SEND_NOTIFICATION_TIME_DELTA = 60000; // 1 min
	private static final long LAST_SOUND_NOTIFICATION_TIME_DELTA = 300000; // 5 min
	
	// DB adapter
	private DbAdapter dbAdapter;

	// Keeping track of current location
	private Location currentLocation;

	// Binder access to service API
	private IBinder localServiceBinder;

	// Intent for sending location update broadcast
	private Intent locationUpdateIntent = new Intent(LOCATION_UPDATE_INTENT);
	
	// Intent for sending has artifacts at current location broadcast
	private Intent hasArtifactsAtCurrentLocationIntent = new Intent(HAS_ARTIFACTS_AT_CURRENT_LOCATION_INTENT);
	
	public ArtifactlyService() {
		
		super();
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// Initialize the service
		try {

			init();
		}
		catch(Exception e) {

			Log.w(PROD_LOG_TAG, "Exception occured", e);
			runInit = true;
		}

		return START_STICKY;
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		
		localServiceBinder = new LocalServiceImpl(this);
		return localServiceBinder;
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onUnbind(android.content.Intent)
	 */
	@Override
	public boolean onUnbind(Intent intent) {

		return super.onUnbind(intent);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onRebind(android.content.Intent)
	 */
	@Override
	public void onRebind(Intent intent) {

		super.onRebind(intent);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {

		super.onCreate();

		// Initialize the service
		try {

			init();
		}
		catch(Exception e) {

			Log.w(PROD_LOG_TAG, "Exception occured", e);
			runInit = true;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		
		super.onDestroy();
		
		settings.unregisterOnSharedPreferenceChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences, java.lang.String)
	 */
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		if(PREFERENCE_RADIUS.equals(key)) {
			
			int newRadius = sharedPreferences.getInt(key, PREFERENCE_RADIUS_DEFAULT);

			if(0 < newRadius) {
				
				radius = newRadius;
				sendBroadcast(locationUpdateIntent);
			}
		}
		else if(PREFERENCE_SOUND_NOTIFICATION.equals(key)) {
			
			soundNotificationPreference = sharedPreferences.getBoolean(key, PREFERENCE_SOUND_NOTIFICATION_DEFAULT);
		}
		else if(PREFERENCE_RADIUS_UNIT.equals(key)) {
			
			radiusUnit = sharedPreferences.getString(key, PREFERENCE_RADIUS_UNIT_DEFAULT);
		}
	}

	/*
	 * Initialization 
	 */
	private void init() {
		
		// Determine if we actually need to initialize
		synchronized(this) {
			
			if(!runInit) {

				return;
			}
			
			runInit = false;
		}
		
		// Getting the constants from resources
		NOTIFICATION_TICKER_TEXT = getResources().getString(R.string.notification_ticker_text);
		NOTIFICATION_CONTENT_TITLE = getResources().getString(R.string.notification_content_title);
		NOTIFICATION_MESSAGE = getResources().getString(R.string.notification_message);

		// Getting shared preferences such as search radius, etc.
		settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
		settings.registerOnSharedPreferenceChangeListener(this);
		radius = settings.getInt(PREFERENCE_RADIUS, PREFERENCE_RADIUS_DEFAULT);
		radiusUnit = settings.getString(PREFERENCE_RADIUS_UNIT, PREFERENCE_RADIUS_UNIT_DEFAULT);
		soundNotificationPreference = settings.getBoolean(PREFERENCE_SOUND_NOTIFICATION, PREFERENCE_SOUND_NOTIFICATION_DEFAULT);

		// Setting up the database
		dbAdapter = new DbAdapter(this);

		// Setting up the notification manager
		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// Setting up the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Register location listener and getting last known location
		registerLocationListener();
	}
	
	/*
	 * DbAdater getter method
	 */
	protected DbAdapter getDbAdapter() {
		
		return dbAdapter;
	}
	
	/*
	 * Helper method
	 */
	protected boolean isNearbyCurrentLocation(String lat, String lng) {
		
		float[] distanceResult = new float[1];

		if(null == currentLocation) {
			
			return false;
		}
		
		try {
		
			Location.distanceBetween(currentLocation.getLatitude(),
					currentLocation.getLongitude(),
					Double.parseDouble(lat),
					Double.parseDouble(lng), distanceResult);
		}
		catch(NumberFormatException	exception) {

			Log.e(PROD_LOG_TAG, "ERROR: Was not able to parse povided lat/lng to a Double", exception);
		}

		if(((int)distanceResult[0]) <= getRadiusInMeters()) {
			
			return true;
		}
		
		return false;
	}

	/*
	 * Helper method that returns the defined radius in meters
	 */
	protected int getRadiusInMeters() {
		
		if(UNIT_M.equals(radiusUnit)) {
			
			return radius;
		}
		else if(UNIT_KM.equals(radiusUnit)) {
			
			return radius * 1000;
			
		}
		else if(UNIT_FT.equals(radiusUnit)) {
			
			return (int)(radius * 0.3048f);
			
		}
		else if(UNIT_MI.equals(radiusUnit)) {
			
			return (int)(radius * 1609.344f);
		}
		
		Log.e(PROD_LOG_TAG, "ERROR: Radius unit not recoginzied");
		return radius;
	}
	
	/*
	 * Dispatch method for local service
	 */
	protected void startLocationTracking() {

		registerLocationListener();
	}

	/*
	 * Dispatch method for local service
	 */
	protected void stopLocationTracking() {

		unregisterLocationListeners();
	}
	
	/*
	 * Dispatch method for local service
	 */
	protected Location getLocation() {

		return currentLocation;
	}
	
	/*
	 * Dispatch method for local service
	 */
	protected long getLastLocationUpdateTime() {
		
		return lastLocationUpdateTime;
	}

	/*
	 * Register location listener
	 * 1. network
	 * 2. gps
	 * ... and passive provider
	 * 
	 * Also setting currentLocation via last known location
	 */
	private void registerLocationListener() {
		
		// If location manager is null, we just return
		if(null == locationManager) {
			
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			
			if(null == locationManager) {
				
				Log.e(PROD_LOG_TAG, "Was not able to get LocationManager instance via getSystemService()");
				return;
			}
		}

		try {

			// First, use network provided location if available
			if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

				mainLocationProviderName = LocationManager.NETWORK_PROVIDER;
				networkLocationListener = getNewLocationListener(NET_PROVIDER);
				locationManager.requestLocationUpdates(mainLocationProviderName, NET_LOCATION_MIN_TIME, NET_LOCATION_MIN_DISTANCE, networkLocationListener);
				currentLocation = locationManager.getLastKnownLocation(mainLocationProviderName);
			}
			else if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

				mainLocationProviderName = LocationManager.GPS_PROVIDER;
				gpsLocationListener = getNewLocationListener(GPS_PROVIDER);
				locationManager.requestLocationUpdates(mainLocationProviderName, GPS_LOCATION_MIN_TIME, GPS_LOCATION_MIN_DISTANCE, gpsLocationListener);
				currentLocation = locationManager.getLastKnownLocation(mainLocationProviderName);
			}
			
			// Also enable Passive provider
			if(locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
				
				passiveLocationListener = getNewLocationListener(PAS_PROVIDER);
				locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, PAS_LOCATION_MIN_TIME, PAS_LOCATION_MIN_DISTANCE, passiveLocationListener);
			}
		}
		catch(IllegalArgumentException iae) {
			
			Log.w(PROD_LOG_TAG, "registerLocationListener() IllegalArgumentException", iae);
		}
		catch(SecurityException se) {
			
			Log.w(PROD_LOG_TAG, "registerLocationListener() SecurityException", se);

		}
		catch(RuntimeException re) {
			
			Log.w(PROD_LOG_TAG, "registerLocationListener() RuntimeException", re);
		}
	}
	
	/*
	 * Unregister location listeners
	 * 
	 */
	private void unregisterLocationListeners() {
		
		if(null == locationManager) {
			
			return;
		}

		try {
		
			locationManager.removeUpdates(networkLocationListener);
			networkLocationListener = null;
			locationManager.removeUpdates(passiveLocationListener);
			passiveLocationListener = null;
		}
		catch(IllegalArgumentException iae) {
			
			Log.w(PROD_LOG_TAG, "IllegalArgumentException thrown while removing location listener updates", iae);
		}
		catch(RuntimeException re) {
			
			Log.w(PROD_LOG_TAG, "unregisterLocationListeners() RuntimeException", re);
		}
	}

	/*
	 * This method sends a message using Android's notification manager.
	 * It sets up an intent so that the UI can be launched from within the notification message.
	 */
	private void sendNotification() {
		
		Intent notificationIntent = new Intent(this, Artifactly.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notification = new Notification(R.drawable.artifactly_launcher, NOTIFICATION_TICKER_TEXT, System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		// We only send notifications with default sound no more frequent than every LAST_SOUND_NOTIFICATION_TIME_DELTA minutes
		if(soundNotificationPreference && ((lastSendNotificationSoundTime + LAST_SOUND_NOTIFICATION_TIME_DELTA) <= System.currentTimeMillis())) {
			
			lastSendNotificationSoundTime = System.currentTimeMillis();
			notification.defaults |= Notification.DEFAULT_SOUND;
		}
		
		notification.setLatestEventInfo(getApplicationContext(), NOTIFICATION_CONTENT_TITLE, NOTIFICATION_MESSAGE, contentIntent);
		notificationManager.notify(NOTIFICATION_ID, notification);
	}
	
	/*
	 * Method that removes all notifications
	 */
	private void cancelNotificaiton() {
	
		notificationManager.cancel(NOTIFICATION_ID);
	}

	/*
	 * Method that checks if there are any artifacts that are close to the current 
	 * location. It uses the defined radius to determine the closeness
	 */
	protected boolean hasArtifactsForCurrentLocation() {
		
		if(null == dbAdapter) {
			
			Log.w(PROD_LOG_TAG, "DB Adapter is null");
			return false;
		}
		
		// Getting all the locations
		Cursor cursor = dbAdapter.select();
		if(null == cursor) {
			
			Log.w(PROD_LOG_TAG, "Cursor is null");
			return false;
		}
		
		// Checking if the cursor set has any items
		boolean hasItems = cursor.moveToFirst();
		
		if(!hasItems) {
			
			cursor.close();
			return false;
		}

		// Determine the table column indexes 
		int longitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LONGITUDE]);
		int latitudeColumnIndex = cursor.getColumnIndex(DbAdapter.LOC_FIELDS[DbAdapter.LOC_LATITUDE]);

		/*
		 *  Iterating over all result and calculate the distance between
		 *  radius, we notify the user that there is an artifact.
		 */
		for(;cursor.isAfterLast() == false; cursor.moveToNext()) {

			// Getting latitude and longitude
			String lat = cursor.getString(latitudeColumnIndex).trim();
			String lng = cursor.getString(longitudeColumnIndex).trim();

			if(isNearbyCurrentLocation(lat, lng)) {

				cursor.close();
				return true;
			}
		}

		cursor.close();

		return false;
	}
		
	/*
	 * Method that determines if a new location is more accurate the the currently saved location
	 */
	private boolean isMoreAccurate(Location newLocation) {

		if(null == currentLocation && null != newLocation) {
			
			return true;
		}

		if(null != currentLocation && null == newLocation) {
		
			return false;
		}
		
		// Check if the new location's accuracy lies within the defined search radius
		if(newLocation.hasAccuracy() &&
		   ((int)newLocation.getAccuracy()) > getRadiusInMeters() &&
		   ((int)newLocation.getAccuracy()) > LOCATION_MAX_ACCURACY_DELTA) {
			
			return false;
		}
		
		boolean isMoreAccurate = false;
		
		// Check if the new location is more accurate 
		if(currentLocation.hasAccuracy() && newLocation.hasAccuracy()) {

			float accuracyDelta = currentLocation.getAccuracy() - newLocation.getAccuracy();
			isMoreAccurate = accuracyDelta >= 0;
		}
		
		// Check if the new location is more current in terms of location fix time
		long locationTimeDelta = currentLocation.getTime() - newLocation.getTime();
		boolean isMoreCurrent = locationTimeDelta <= 0;
		
		if(isMoreAccurate && isMoreCurrent) {
			
			return true;
		}
		
		boolean isSlightlyLessCurrent = (locationTimeDelta > 0 && locationTimeDelta < LOCATION_TIME_ALLOWED_DELTA);
		
		if(isMoreAccurate && isSlightlyLessCurrent) {
			
			return true;
		}
		
		boolean isSlightlyLessAccurate = (newLocation.hasAccuracy() && newLocation.getAccuracy() <= LOCATION_MAX_ACCURACY_DELTA);
		if(isSlightlyLessAccurate && isMoreCurrent) {
			
			return true;
		}
		
		return false;
	}
	
	
	/*
	 * Create a location listener
	 */
	private LocationListener getNewLocationListener(final String listenerName) {
		
		return new LocationListener() {

			public void onLocationChanged(final Location location) {
				
				locationChanged(location);
			}

			public void onProviderDisabled(String provider) {
				
				locationProviderDisabled(provider);
			}

			public void onProviderEnabled(String provider) {
				
				locationProviderEnabled(provider);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
				
				locationStatusChanged(provider, status, extras);
			}
		};
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	private void locationChanged(Location location) {

		// Don't handle consecutive location changes. e.g. PASSIVE provider
		if(null != currentLocation && Float.compare(currentLocation.distanceTo(location), 0.0f) == 0) {
			
			return;
		}
		
		lastLocationUpdateTime = System.currentTimeMillis();

		// First we check if the new location is more accurate
		if(isMoreAccurate(location)) {
			
			// Update the current location with the new one
			currentLocation = location;

			// Sending a location update broadcast to client 
			sendBroadcast(locationUpdateIntent);

			// Since we are getting a more accurate location, we should check if the 
			// GPS listener is still enabled. If it is enabled we can turn it off
			if(isGpsListenerEnabled) {
				
				try {
					
					locationManager.removeUpdates(gpsLocationListener);
					gpsLocationListener = null;
					isGpsListenerEnabled = false;
				}
				catch(IllegalArgumentException iae) {
					
					isGpsListenerEnabled = false;
					Log.w(PROD_LOG_TAG, "Was not able to remove GPS listener updates", iae);
				}
			}
		
			// Make sure that we don't send too many notifications
			boolean canSendNotificaiton = ((lastSendNotificationTime + LAST_SEND_NOTIFICATION_TIME_DELTA) <= System.currentTimeMillis()) ? true : false;
			
			/*
			 * Check if there are any artifacts close to the current location. If there are,
			 * we send a notification.
			 * 
			 */
			if(canSendNotificaiton && hasArtifactsForCurrentLocation()) {

				lastSendNotificationTime = System.currentTimeMillis();
				sendNotification();
				sendBroadcast(hasArtifactsAtCurrentLocationIntent);
			}
			else {
				
				// Clear any old notifications
				cancelNotificaiton();
			}
		}
		else {
			
			// Check if the currentLocation has been updated recently
			long expirationTime = System.currentTimeMillis() - currentLocation.getTime() - LOCATION_TIME_EXPIRATION_DELTA;

			if(expirationTime > 0) {
				
				// The current location's fix time is too old. Check if GPS provider is 
				// available and try to get a better fix from it
				if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !isGpsListenerEnabled) {

					try {
						
						isGpsListenerEnabled = true;
						gpsLocationListener = getNewLocationListener(GPS_PROVIDER);
						locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_LOCATION_MIN_TIME, GPS_LOCATION_MIN_DISTANCE, gpsLocationListener);
					}
					catch(Exception e) {
						
						isGpsListenerEnabled = false;
						Log.w(PROD_LOG_TAG, "Was not able to start GPS listener", e);
					}
				}
			}
		}	
	}

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	private void locationProviderDisabled(String provider) {

	}

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	private void locationProviderEnabled(String provider) {

	}

	/*
	 * (non-Javadoc)
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	private void locationStatusChanged(String provider, int status, Bundle extras) {

	}
}
