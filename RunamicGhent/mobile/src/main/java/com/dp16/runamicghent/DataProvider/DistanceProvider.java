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

import android.location.Location;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.RunData.RunDistance;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This should be started when a run is started.
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#DISTANCE}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#RAW_LOCATION}
 * </p>
 * Created by hendrikdepauw on 07/03/2017.
 */

public class DistanceProvider implements EventListener, EventPublisher, DataProvider {
    private int interval = 1000; // minimum amount of ms between readings
    private double minimumDistance = 5.0; // minimum distance to be traveled before taking the new measurement into consideration
    private double minimumAccuracy = 5.0; // minimum accuracy before taking the new measurement into account
    private double distanceCovered = 0.0;
    private double relativeAccuracy = 0.2;
    private Location previousLocation;
    private ExecutorService worker;

    public DistanceProvider() {
        // Make worker thread
        worker = Executors.newSingleThreadExecutor();
    }

    /**
     * For testing purposes
     *
     * @return The minimum distance (in meters) between two raw locations before a measurement is taken into consideration
     */
    public double getMinimumDistance() {
        return minimumDistance;
    }

    /**
     * For testing purposes
     *
     * @return The minimum accuracy needed before taking new measurements into account
     */
    public double getMinimumAccuracy() {
        return minimumAccuracy;
    }

    /**
     * For testing purposes
     *
     * @return The minimum relative accuracy needed before taking new measurements into account
     */
    public double getRelativeAccuracy() {
        return relativeAccuracy;
    }

    /**
     * For testing purposes, otherwise testing a track would take ages.
     *
     * @param newInterval interval to be used in the subscription to the EventBroker
     */
    @Deprecated
    public void setInterval(int newInterval) {
        interval = newInterval;
    }

    @Override
    public void start() {
        EventBroker.getInstance().addEventListener(Constants.EventTypes.RAW_LOCATION, this, interval);
    }

    @Override
    public void stop() {
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.RAW_LOCATION, this);
    }

    @Override
    public void resume() {
        start();
    }

    @Override
    public void pause() {
        previousLocation = null;
        stop();
    }

    @Override
    public void handleEvent(String eventType, Object message) {
        Runnable task = new Worker(message);
        worker.submit(task);
    }

    private class Worker implements Runnable, EventPublisher {
        private Location currentLocation;

        public Worker(Object message) {
            currentLocation = (Location) message;
        }

        /*
        @Override
        public void run() {
            if (previousLocation == null) {
                previousLocation = currentLocation;
            }
            //Only add to the distance if there is minimal movement
            else if (previousLocation.distanceTo(currentLocation) > minimumDistance && currentLocation.getAccuracy() < minimumAccuracy) {
                distanceCovered += previousLocation.distanceTo(currentLocation);
                previousLocation = currentLocation;

                EventBroker.getInstance().addEvent(Constants.EventTypes.DISTANCE, new RunDistance((int) distanceCovered), this);
            }
        }
        */

        /*Instead of using an absolute error bound on the distance, this method uses
        * a relative one. This means that if the distance covered between two locations is bigger,
        * the error on the locations itself can be bigger as well.*/
        @Override
        public void run() {
            if (previousLocation == null) {
                previousLocation = currentLocation;
            } else {
                double distance = previousLocation.distanceTo(currentLocation);

                if (distance > minimumDistance && (distance * relativeAccuracy) > currentLocation.getAccuracy()) {
                    distanceCovered += distance;
                    previousLocation = currentLocation;

                    EventBroker.getInstance().addEvent(Constants.EventTypes.DISTANCE, new RunDistance((int) distanceCovered), this);
                }
            }
        }
    }
}
