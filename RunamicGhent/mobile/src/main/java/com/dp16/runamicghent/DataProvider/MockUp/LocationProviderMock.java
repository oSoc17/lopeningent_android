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

package com.dp16.runamicghent.DataProvider.MockUp;

import android.location.Location;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.DataProvider.DataProvider;
import com.dp16.runamicghent.DataProvider.Kalman;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventPublisher;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class mocks a location provider. It accepts a list of latlng that represent a route.
 * This route will be mocked as if a runner runs along the route.
 * The desiredSpeed of the runner can be adjusted, as well as the accuracy of the GPS.
 * Created by hendrikdepauw on 09/03/2017.
 */

public class LocationProviderMock implements DataProvider {
    private ArrayList<LatLng> route; //Route along which the location should move
    private double accuracyMeters; //Location with noise will always be within this boundry of the actual location
    private double accuracyDegrees; //Used internally
    private double metersInDegree = 111300.0; //Used to convert between accuracyMeters and accuracyDegrees
    private final double desiredSpeed; //Speed at which the location should move
    private double maximumDistance = 10.0; //Maximum distance between two consecutive locations
    private int routePosition; //Current position in the arraylist
    private long timeBetweenLocations; //Time between current and previous location publish
    private Location previousLocationNoise; //Previous published location with noise
    private Timer timer; //Timer used to publish locations
    private Kalman kalman; // Filter to get a smoother path during run.

    /**
     * Constructor of the DummyLocationProvider. Keep in mind that to start publishing locations,
     * the start() method should be invoked.
     *
     * @param route          Route along which location events should be published
     * @param accuracyMeters The desired accuracy of the GPS locations. Locations will be uniformly
     *                       distributed in a circle around the actual point with radius this parameter.
     * @param desiredSpeed   Speed of the runner. Speed provided in the published Location is the desiredSpeed
     *                       between two noisy locations, not this absolute desiredSpeed.
     */
    public LocationProviderMock(List<LatLng> route, double accuracyMeters, double desiredSpeed) {
        this.accuracyMeters = accuracyMeters;
        this.route = interpolatePolyline((ArrayList<LatLng>) route, maximumDistance);
        this.desiredSpeed = desiredSpeed;

        timer = new Timer();
        routePosition = 0;
        accuracyDegrees = accuracyMeters / metersInDegree;
        kalman = new Kalman();
    }

    @Override
    public void start() {
        timer.schedule(new LocationPublisher(kalman), 0);
    }

    @Override
    public void stop() {
        timer.cancel();
    }

    @Override
    public void resume() {
        start();
    }

    @Override
    public void pause() {
        stop();
    }


    /**
     * This method will interpolate a given route. This means that if two consecutive points
     * are more than maximumDistance spaced away from each other, an intermediate point will be inserted.
     * The method SphericalUtil.interpolate() from the Google Maps API is used to calculate the
     * intermediate points. (http://googlemaps.github.io/android-maps-utils/javadoc/)
     *
     * @param route           Route to be interpolated
     * @param maximumDistance Desired maximum distance between two consecutive points
     * @return Route with no points spaced further apart than maximumDistance
     */
    private ArrayList<LatLng> interpolatePolyline(ArrayList<LatLng> route, double maximumDistance) {
        for (int i = 0; i < route.size() - 1; i++) {
            LatLng location1 = route.get(i);
            LatLng location2 = route.get(i + 1);

            //Calculate the distance between two consecutive points
            float[] distance = new float[1];
            Location.distanceBetween(location1.latitude, location1.longitude, location2.latitude, location2.longitude, distance);

            //If the distance is too big, add an extra point
            if (distance[0] > maximumDistance) {
                double fraction = 1 / Math.ceil(distance[0] / maximumDistance);
                route.add(i + 1, SphericalUtil.interpolate(location1, location2, fraction));
            }
        }
        return route;
    }

    class LocationPublisher extends TimerTask implements EventPublisher {

        private Kalman filter;

        private LocationPublisher(Kalman kalman) {
            this.filter = kalman;
        }

        /**
         * This method will be called everytime a location should be published.
         * A timer is used to do this.
         */
        @Override
        public void run() {
            LatLng currentLocation = route.get(routePosition);

            //Generate new location with noise. Also make LatLng version
            Location rawLocationNoise = generateLocationWithNoise(currentLocation);

            // Removed due to kalman filter
            //LatLng locationNoise = new LatLng(rawLocationNoise.getLatitude(), rawLocationNoise.getLongitude());

            //Add desiredSpeed attribute to new location
            rawLocationNoise = calculateSpeed(rawLocationNoise);

            LatLng locationNoise = kalman.estimatePosition(rawLocationNoise);

            //Publish the events
            EventBroker.getInstance().addEvent(Constants.EventTypes.RAW_LOCATION, rawLocationNoise, this);
            EventBroker.getInstance().addEvent(Constants.EventTypes.LOCATION, locationNoise, this);

            if (routePosition != route.size() - 1) {
                LatLng nextLocation = route.get(routePosition + 1);
                float[] distance = new float[1];
                Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, nextLocation.latitude, nextLocation.longitude, distance);

                //Schedule next publish event
                timeBetweenLocations = Math.round((distance[0] / (desiredSpeed / 3.6)) * 1000);
                timer.schedule(new LocationPublisher(kalman), timeBetweenLocations);

                routePosition++;
                previousLocationNoise = rawLocationNoise;
            }
        }

        /**
         * This method returns a new location with noise.
         * The location is uniformly distributed in a circle around the location with radius accuracy.
         * This algorithm is not exact, but is more than adequate for our small radius.
         * This algorithm is from:
         * http://gis.stackexchange.com/questions/25877/generating-random-locations-nearby/68275#68275
         *
         * @param location Location without noise.
         * @return new location around the location above.
         */
        private Location generateLocationWithNoise(LatLng location) {
            double usedAccuracyMeters = accuracyMeters * Math.random();
            double usedAccuracyDegrees = usedAccuracyMeters / metersInDegree;

            double temp1 = usedAccuracyDegrees * Math.sqrt(Math.random());
            double temp2 = 2 * Math.PI * Math.random();

            double latNoise = (temp1 * Math.cos(temp2)) / Math.cos(location.longitude);
            double lonNoise = temp1 * Math.sin(temp2);

            Location locationWithNoise = new Location("DummyLocationProvider");
            locationWithNoise.setLatitude(location.latitude + latNoise);
            locationWithNoise.setLongitude(location.longitude + lonNoise);
            locationWithNoise.setAccuracy((float) usedAccuracyMeters);

            return locationWithNoise;
        }

        /**
         * Calculates the desiredSpeed between the points with noise.
         * This desiredSpeed is then added to the currentLocation.
         * If this is the first location, the desiredSpeed is set to zero.
         *
         * @param currentLocation Location to which the desiredSpeed should be added.
         * @return currentLocation with desiredSpeed added.
         */
        private Location calculateSpeed(Location currentLocation) {
            float speed = 0f;

            if (routePosition > 0) {
                float[] distance = new float[1];
                Location.distanceBetween(previousLocationNoise.getLatitude(), previousLocationNoise.getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude(), distance);

                speed = (distance[0] / timeBetweenLocations) * 1000;
            }

            currentLocation.setSpeed(speed);
            return currentLocation;
        }
    }
}
