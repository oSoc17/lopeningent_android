/*
 * Copyright (c) 2017 Hendrik Depauw
 * Copyright (c) 2017 Lorenz van Herwaarden
 * Copyright (c) 2017 Nick Aelterman
 * Copyright (c) 2017 Olivier Cammaert
 * Copyright (c) 2017 Maxim Deweirdt
 * Copyright (c) 2017 Gerwin Dox
 * Copyright (c) 2017 Simon Neuville
 * Copyright (c) 2017 Stiaan Uyttersprot
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */

package com.dp16.runamicghent.DataProvider;

import android.app.Activity;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dp16.runamicghent.Constants;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventPublisher;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;

/**
 * LocationProvider class provides location requests.
 * An activity should be given to the constructor. This is necessary to make a connection with the Google API.
 * Google API is used to process location updates from the smartphone for more accurate location updates.
 * LocationRequest is built with some configuration possibilities like fastest update interval and priority.
 * This class also creates a dialog to enable location setting if it is disabled on the user's smartphone.
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#LOCATION}, {@link com.dp16.runamicghent.Constants.EventTypes#RAW_LOCATION}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> None.
 * </p>
 * Created by lorenzvanherwaarden on 02/03/2017.
 */
public class LocationProvider implements DataProvider, EventPublisher, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<LocationSettingsResult>, LocationListener {

    protected static final String TAG = "LocationProvider";

    // API for Google Play Services
    private GoogleApiClient googleApiClient;

    // Request for FusedLocationApi
    private LocationRequest locationRequest;

    // The activity that requests the location updates, is needed for building the Google Api Client
    protected Activity activity;

    // Have the location updates been requested or stopped?
    private boolean locationRequested = false;

    /*
     * How many times has the LocationProvider received an accurate location?
     * If -1, no accuracy notification is needed for the requesting entity.
     */
    private int accuracyNotificationCounter = -1;

    private Boolean fakeLatLng = false;

    // Filter used to get smoother paths
    private Kalman kalmanFilter;

    /**
     * Constructor for LocationProvider
     *
     * @param activity
     */
    public LocationProvider(Activity activity) {
        this.activity = activity;

        // Build Google Api for location services
        googleApiClient = new GoogleApiClient.Builder(activity)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        this.kalmanFilter = new Kalman();

        if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(Constants.SettingTypes.PREF_KEY_DEBUG_FAKE_LATLNG_LOCATIONPROVIDER, false)) {
            fakeLatLng = true;
        }
    }

    /**
     * Constructor for LocationProvider where the receiving entity will be notified if the GPS is accurate
     *
     * @param activity
     */
    public LocationProvider(Activity activity, Boolean accuracyNotification) {
        // Call main constructor
        this(activity);

        if (accuracyNotification) {
            this.accuracyNotificationCounter = 0;
        }
    }

    /**
     * Start LocationProvider => Connect to the Google Api client
     */
    @Override
    public void start() {
        // Connect to Google API client
        googleApiClient.connect();
    }

    /**
     * Resume LocationProvider => if location updates haven't started, then start them
     */
    @Override
    public void resume() {
        // Start Location Updates
        startLocationUpdates();
    }

    /**
     * Pause LocationProvider => Stop Location Updates if Google Api Client is connected
     */
    @Override
    public void pause() {
        // Stop location updates
        if (googleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    /**
     * Stop LocationProvider => Disconnect from the Google Api Client
     */
    @Override
    public void stop() {
        // Disconnect Google API client
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    /**
     * Callback when connection is made with GoogleApiClient
     *
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = LocationRequest.create();

        // Set parameters for location requests
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(Constants.MapSettings.UPDATE_INTERVAL);
        locationRequest.setFastestInterval(Constants.MapSettings.FASTEST_UPDATE_INTERVAL);

        // Build Location Settings Request to check if location setting has been enabled by user on device
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        googleApiClient,
                        locationSettingsRequest
                );
        result.setResultCallback(this);
    }

    /**
     * Connection to GoogleApiClient suspended
     *
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection to GoogleApiClient suspended");
    }

    /**
     * Connection to GoogleApiClient failed
     *
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection to GoogleApiClient failed");
    }

    /**
     * Callback of the LocationSettingsRequest with object that can be examined to check if the adequate settings are enabled on device
     *
     * @param locationSettingsResult
     */
    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();

        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.i(TAG, "All location settings are satisfied.");

                // Start Location Updates because all settings are satisfied
                startLocationUpdates();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                startResolutionForResult(status);
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                break;
            default:
                break;
        }
    }

    /**
     * Location Settings have not been enabled by the user, show a dialog. Result of this dialog is handled in the fragment/activity.
     */
    private void startResolutionForResult(Status status) {
        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");
        try {
            // Show the dialog by calling startResolutionForResult(), and check the result
            // in onActivityResult().
            status.startResolutionForResult(activity, Constants.MapSettings.REQUEST_CHECK_SETTINGS);
        } catch (IntentSender.SendIntentException e) {
            Log.i(TAG, "PendingIntent unable to execute request.", e);
        }
    }

    // Start location updates from the FusedLocationApi
    public void startLocationUpdates() {
        try {
            if (googleApiClient.isConnected() && !locationRequested) {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

                // Try to quickly get last known location for a faster update
                Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                if (lastLocation != null) {
                    publishEvent(lastLocation);
                }
                locationRequested = true;
            }
        } catch (SecurityException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Stop location updates from FusedLocationApi. With an app such as this one with frequent location updates,
     * it is best practice to stop location updates if not necessary to save battery.
     */
    private void stopLocationUpdates() {
        if (locationRequested) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            locationRequested = false;
        }
    }

    /**
     * Callback when a new location update is received by FusedLocationApi.
     * Publish this new raw location to the eventbroker
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            publishEvent(location);
        }
    }

    /**
     * Publish location to EventBroker
     *
     * @param location
     */
    private void publishEvent(Location location) {
        //For testers not in Ghent, location spoofing
        if (fakeLatLng) {
            location.setLatitude(51.047648);
            location.setLongitude(3.727159);
        }

        // Publish Raw Location as location
        // Publish Location as LatLng

        EventBroker.getInstance().addEvent(Constants.EventTypes.RAW_LOCATION, location, this);

        LatLng latLng = kalmanFilter.estimatePosition(location);

        EventBroker.getInstance().addEvent(Constants.EventTypes.LOCATION, latLng, this);

        // if accuracyNotificationCounter is -1, then no accurate message is needed
        if (accuracyNotificationCounter != -1) {
            processAccuracy(location.getAccuracy());
        }
    }

    // Process the accuracy of the location and send LOCATION_ACCURATE events
    private void processAccuracy(float accuracy) {
        /*
        The higher accuracyNotificationCounter, the more certain we are that a good accuracy is maintained.
        The counter has a minimum (0) and a maximum (COUNTER_MAX)
        Taking into account the min and max of the counter,
            Accuracy below MAX_GOOD_ACCURACY threshold => counter is increased
            Accuracy above MIN_BAD_ACCURACY threshold => counter is decreased
        Counter has the maximum value => send LOCATION_ACCURATE (true) event
        Counter has the min value => send LOCATION_ACCURATE (false) event
         */
        if (accuracy < Constants.Location.MAX_GOOD_ACCURACY && accuracyNotificationCounter < Constants.Location.COUNTER_MAX) {
            accuracyNotificationCounter++;
        } else if (accuracy > Constants.Location.MIN_BAD_ACCURACY && accuracyNotificationCounter > 0) {
            accuracyNotificationCounter--;
        }

        if (accuracyNotificationCounter == 0) {
            EventBroker.getInstance().addEvent(Constants.EventTypes.LOCATION_ACCURATE, false, this);
        } else if (accuracyNotificationCounter == Constants.Location.COUNTER_MAX) {
            EventBroker.getInstance().addEvent(Constants.EventTypes.LOCATION_ACCURATE, true, this);
        }
    }

    /**
     * retrieve googleApiClient (for testing)
     */
    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }
}
