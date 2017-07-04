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

package com.dp16.runamicghent.Activities;

import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.RunningMapLocationSource;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

/**
 * This class holds a Google Map but offers some more functionality.
 * Created by lorenzvanherwaarden on 10/03/2017.
 */
public class RunningMap implements GoogleMap.OnCameraMoveStartedListener {
    public static final String TAG = "RunningMap";

    // Google RunningMap
    protected GoogleMap map;

    // LatLng object of the current location
    private LatLng currentLatLng;

    // Fragment that uses this map
    private Fragment fragment;

    // Must camera keep updating to location?
    private Boolean cameraTracking = true;

    // Fab
    final private FloatingActionButton fab;

    private boolean firstMovement = true;

    public RunningMap(GoogleMap map, final Fragment fragment) {
        this.map = map;
        this.fragment = fragment;

        // Disable standard location button
        map.getUiSettings().setMyLocationButtonEnabled(false);

        // Running Map will handle when the camera moves
        map.setOnCameraMoveStartedListener(this);

        // Set onClickListener for FAB
        fab = (FloatingActionButton) fragment.getView().findViewById(R.id.myLocationButton);
        fab.setOnClickListener(new View.OnClickListener() {

            // If Fab is clicked, camera must track location updates, and color of fab is set to colorAccentDarker
            @Override
            public void onClick(View v) {

                if (currentLatLng != null) {
                    cameraTracking = true;
                    animateCamera(currentLatLng);
                    fab.getDrawable().mutate().setTint(ContextCompat.getColor(fragment.getContext(), R.color.colorAccent));
                }
            }
        });


        map.setLocationSource(new RunningMapLocationSource());

        // Set parameters for Google RunningMap
        map.setMinZoomPreference(Constants.MapSettings.MIN_ZOOM);
        map.setMaxZoomPreference(Constants.MapSettings.MAX_ZOOM);
        map.getUiSettings().setCompassEnabled(Constants.MapSettings.COMPASS);
        map.getUiSettings().setTiltGesturesEnabled(Constants.MapSettings.TILT);
    }

    /**
     * Animate camera of RunningMap to currentLocation with Desired Zoom Level
     */
    public void animateCamera(LatLng currentLatLng) {
        this.currentLatLng = currentLatLng;

        if (currentLatLng != null && cameraTracking) {
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(currentLatLng, Constants.MapSettings.DESIRED_ZOOM);
            if (firstMovement) {
                map.moveCamera(update);
                firstMovement = false;
            } else {
                /*
                 We need to temporarily disable cameratracking because location update might
                 otherwise interrupt these camera animation.
                  */
                cameraTracking = false;
                map.animateCamera(update, new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        cameraTracking = true;
                    }

                    @Override
                    public void onCancel() {
                        // Don't need onCancel
                    }
                });
            }
        }
    }

    /**
     * Enables location tracking on map.
     */
    public void setMyLocationEnabled(Boolean b) {
        try {
            if (map.isMyLocationEnabled() != b) {
                map.setMyLocationEnabled(b);
            }
        } catch (SecurityException e) {
            Log.e(this.getClass().getName(), e.getMessage(), e);
        }
    }

    /**
     * When user moves camera by performing gesture on screen -> snaps out of location tracking.
     */
    @Override
    public void onCameraMoveStarted(int reason) {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            cameraTracking = false;
            fab.getDrawable().mutate().setTint(ContextCompat.getColor(fragment.getContext(), R.color.colorPrimary));
        }
    }
}
