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

package com.dp16.runamicghent.Activities.RunningScreen;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dp16.runamicghent.Activities.Utils;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.RunningMapLocationSource;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.ui.IconGenerator;

import java.util.List;

/**
 * This Fragment is loaded as a background and displays a map.
 * Advantage of using this strategy is that the map does not have to be reloaded in the RunningActivity.
 * Included in this fragment is a button (fab) that enables and disables location tracking.
 * Location tracking is automatically disabled if the user starts moving the map around manually.
 * To display a route and plot it on the map as a polyline, call setRoute() followed
 * by displayRoute(). If a polyline was already plotted on the map it will be removed.
 * This Fragment replaces the RunningMap class.
 *
 * <p>
 *     <b>Messages Produced: </b> None.
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#LOCATION}
 * </p>
 *
 * Created by hendrikdepauw on 31/03/2017.
 */

public class MapRunningFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnCameraMoveStartedListener, EventListener, EventPublisher {
    private MapView mapView;
    private FloatingActionButton fab;
    private GoogleMap googleMap;
    private LatLng currentLocation;
    private boolean cameraTracking = true;
    private List<LatLng> route;
    private List<LatLng> secondaryRoute;
    private Polyline polyline;
    private Polyline secondaryPolyline;
    private boolean firstMovement = true;
    Utils.BearingCalculator bearingCalculator;
    private float bearing = -20.0f;
    private int bottomPadding = 0;
    private int topPadding = 0;
    private int sidePadding = 0;
    private int routePadding = 0;
    Marker startArrow = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_running_map, container, false);

        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) view.findViewById(R.id.runningMap);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        fab = (FloatingActionButton) view.findViewById(R.id.myLocationButton);

        EventBroker.getInstance().addEventListener(Constants.EventTypes.LOCATION, this);

        // Calculate (in px) the padding for the sides of the map
        sidePadding = Utils.dpToPx(getContext(), Constants.MapSettings.SIDE_MAP_PADDING);

        // Calculate (in px) the padding for the route
        routePadding = Utils.dpToPx(getContext(), Constants.MapSettings.ROUTE_PADDING);

        bearingCalculator = new Utils.BearingCalculator();

        return view;
    }

    /**
     * Callback for when Google Map is ready. Initially called by {@link #mapView}.getMapAsync(this)
     *
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // Disable standard location button
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Map Running Fragment will handle when the camera moves (snaptolocation disable)
        googleMap.setOnCameraMoveStartedListener(this);

        // Set our custom location source.
        googleMap.setLocationSource(new RunningMapLocationSource());

        // Set parameters for Google RunningMap
        googleMap.setMinZoomPreference(Constants.MapSettings.MIN_ZOOM);
        googleMap.setMaxZoomPreference(Constants.MapSettings.MAX_ZOOM);
        googleMap.getUiSettings().setCompassEnabled(Constants.MapSettings.COMPASS);
        googleMap.getUiSettings().setTiltGesturesEnabled(Constants.MapSettings.TILT);
        // Disable toolbar such that 'navigation' and 'gps pointer' buttons don't appear when marker is clicked
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        // Enable the blue location dot.
        setMyLocationEnabled(true);

        // Set onClickListener for FAB
        fab.setOnClickListener(new View.OnClickListener() {
            // If Fab is clicked, camera must track location updates, and color of fab is set to colorAccent
            @Override
            public void onClick(View v) {
                if (currentLocation != null) {
                    cameraTracking = true;
                    animateCamera();
                    fab.getDrawable().mutate().setTint(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorAccent));
                }
            }
        });

        // Set top padding of map to take into account that the map is showed under the status bar.
        setStatusBarPadding();
    }

    /**
     * Listens for manual movements of the map. This disables the snap to location.
     * Color of the fab changes.
     */
    @Override
    public void onCameraMoveStarted(int reason) {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE && cameraTracking) {
            cameraTracking = false;
            fab.getDrawable().mutate().setTint(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorPrimary));
        }
    }

    /**
     * Handle event messages from the EventBroker.
     *
     * @param eventType Type of the event to be handled, always LOCATION.
     * @param message   A LatLng object.
     */
    @Override
    public void handleEvent(String eventType, Object message) {
        // When a location is received, the camera is moved to center on that location
        currentLocation = (LatLng) message;
        Handler mainHandler = new Handler(getActivity().getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                animateCamera();
            }
        };
        mainHandler.post(runnable);
    }

    /**
     * Enable location tracking on map.
     */
    public void setMyLocationEnabled(Boolean b) {
        try {
            if (googleMap.isMyLocationEnabled() != b) {
                googleMap.setMyLocationEnabled(b);
            }
        } catch (SecurityException e) {
            Log.e(this.getClass().getName(), e.getMessage(), e);
        }
    }

    /**
     * Animate camera of RunningMap to currentLocation with desired Zoom Level.
     */
    public void animateCamera() {
        if (currentLocation != null && cameraTracking) {
            CameraUpdate update;

            // if route is not empty, rotate camera to take into account the direction of the runner
            if (route == null) {
                update = CameraUpdateFactory.newLatLngZoom(currentLocation, Constants.MapSettings.DESIRED_ZOOM);
            } else {
                /*
                 * Calculate bearing using Util method.
                 * Due to rotating the map, it is possible that the map isn't centered perfectly.
                 * This could be solved by using 2 different camerapositions that are animated
                 * after each other, but this takes too much time.
                 */
//                float[] distance = {0.0f};
//                Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, previousLocation.latitude, previousLocation.longitude, distance);
//                if (distance[0] > 8.0){
//                    float newBearing = (float) Utils.calculateBearing(previousLocation, currentLocation);
//                    if (Math.abs(bearing - newBearing) > 180.0) newBearing += 360.0f;
//                    bearing = Constants.MapSettings.DISCOUNT_FACTOR * bearing + (1 - Constants.MapSettings.DISCOUNT_FACTOR) * newBearing;
//
//                    previousLocation = currentLocation;
//                }

                CameraPosition position = new CameraPosition.Builder()
                        .zoom(Constants.MapSettings.DESIRED_ZOOM)
                        .bearing(bearingCalculator.calculateBearing(currentLocation))
                        .target(currentLocation)
                        .build();
                update = CameraUpdateFactory.newCameraPosition(position);
            }

            /*
            If this is the first camera movement, it should not be animated. Because then
            it starts in africa and it gives an ugly animation. After that the camera can be
            updated in a smooth manner.
             */
            if (firstMovement) {
                firstMovement = false;
                googleMap.moveCamera(update);
            } else {
                googleMap.animateCamera(update);
            }

        }
    }

    /**
     * Update the route from the map with a new route.
     * This method does not display the route.
     * To actually display the new route you have to call displayRoute().
     *
     * @param route new route
     */
    public void setRoute(List<LatLng> route) {
        this.route = route;
    }

    public void setSecondaryRoute(List<LatLng> secondaryRoute){
        this.secondaryRoute = secondaryRoute;
    }

    /**
     * Displays the route on the map as a polyline.
     * The route should be a not empty ArrayList<LatLng>.
     * If a polyline was already displayed on the map, it is replaced.
     */
    public void displayRoute() {
        Handler mainHandler = new Handler(getActivity().getMainLooper());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (polyline != null) {
                    polyline.remove();
                }

                if (secondaryPolyline != null) {
                    secondaryPolyline.remove();
                }

                if (!route.isEmpty()) {
                    //double length = calculateLength
                    polyline = googleMap.addPolyline(new PolylineOptions()
                            .addAll(route)
                            .color(ContextCompat.getColor(((RunningActivity) getActivity()), R.color.colorAccent)));
                }
                if (startArrow != null) {
                    startArrow.remove();
                }

            }
        };

        mainHandler.post(runnable);
    }

    public void displaySecondaryRoute(){
        Handler mainHandler = new Handler(getActivity().getMainLooper());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (secondaryPolyline != null) {
                    secondaryPolyline.remove();
                }
                if (!secondaryRoute.isEmpty()) {
                    //double length = calculateLength
                    secondaryPolyline = googleMap.addPolyline(new PolylineOptions()
                            .addAll(secondaryRoute)
                            .color(ContextCompat.getColor(((RunningActivity) getActivity()), R.color.colorPrimary)));
                }
            }
        };

        mainHandler.post(runnable);
    }

    /**
     * This method focuses the camera of the GoogleMap over the route.
     * The fab button is shown in the darker color and cameraTracking is disabled.
     */
    public void focusOnRoute() {
        cameraTracking = false;
        fab.getDrawable().mutate().setTint(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorPrimary));

        //Construct LatLngBounds, this is necessary to pan camera to correct position
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int i = 0; i < route.size(); i++) {
            builder.include(route.get(i));
        }

        // Update camera movements. Don't update if the route is empty
        if (!route.isEmpty()) {
            /*
             * Create cameraposition with north rotation and desired zoom on currentlocation.
             * The map first needs to be rotated back to north because it has been rotated previously.
             * Afterwards, the map can be bounded to the route.
             */
            CameraPosition position = new CameraPosition.Builder()
                    .bearing(0.0f)
                    .target(currentLocation)
                    .zoom(googleMap.getCameraPosition().zoom)
                    .build();
            CameraUpdate updateRotate = CameraUpdateFactory.newCameraPosition(position);

            // Only animate to bounds once the rotation has finished, otherwise the bounds won't be good.
            final CameraUpdate updateBounds = CameraUpdateFactory.newLatLngBounds(builder.build(), routePadding);
            // Shorter animation of 500ms with callback when finished
            googleMap.animateCamera(updateRotate, 500, new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    googleMap.animateCamera(updateBounds);
                }

                @Override
                public void onCancel() {
                    // Don't need onCancel
                }
            });
        }
    }

    /**
     * This method forces the GoogleMap to focus on the location of the user.
     * The fab button is turned green and cameraTracking is turned on.
     */
    public void focusOnLocation() {
        cameraTracking = true;
        fab.getDrawable().mutate().setTint(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorAccent));

        animateCamera();
    }

    /**
     * This method moves the center of the map upwards, according to bottomPadding.
     * Necessary to center the map when the CardView with statistics is shown.
     *
     * @param bottomPadding amount of dp it should be moved upwards
     */
    public void setBottomPadding(int bottomPadding) {
        this.bottomPadding = bottomPadding;
        googleMap.setPadding(sidePadding, topPadding, sidePadding, bottomPadding);
    }

    // Get statusbar size and set it as toppadding for the google map.
    private void setStatusBarPadding() {
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        // Add 8 for extra padding for compass button
        topPadding = Utils.dpToPx(getContext(), Utils.pxToDp(getContext(), (int) getResources().getDimension(resourceId)) + 8);
        googleMap.setPadding(sidePadding, topPadding, sidePadding, bottomPadding);
    }

    /**
     * Hide Location Fab button once in the post running fragment
     */
    public void disableLocationFab() {
        fab.setVisibility(View.GONE);
    }

    /**
     * Add Start marker to Google Map on start location.
     * Called when running starts so currentLocation at that moment is the starting location.
     */
    public Marker addStartMarker(IconGenerator iconFactory, CharSequence text) {
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(text))).
                position(currentLocation).
                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());
        return googleMap.addMarker(markerOptions);
    }

    /**
     * Add start arrow to map with direction dependent on beginning of the route
     */
    public void addStartArrow() {
        // if a previous startArrow exists, from previously generated route, remove it
        if (startArrow != null) {
            startArrow.remove();
        }
        // calculate rotation between 2 LatLng's
        double rotation = SphericalUtil.computeHeading(route.get(0), route.get(1));

        startArrow = googleMap.addMarker(new MarkerOptions()
                .anchor(0.5f, 1.0f)
                .alpha(1.0f)
                .position(route.get(0))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow))
                .flat(true)
                .rotation((float) rotation));
    }

    /**
     * Remove start arrow that is displayed on the map. This is only done after 20 seconds,
     * because perhaps runner still hasn't started running and forgot the direction
     */
    public void removeStartArrow() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startArrow.remove();
            }
        }, 30000);
    }

    // These lifecycle methods have to be overridden for the map to function correctly.
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

        EventBroker.getInstance().removeEventListener(Constants.EventTypes.LOCATION, this);
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.TRACK, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}