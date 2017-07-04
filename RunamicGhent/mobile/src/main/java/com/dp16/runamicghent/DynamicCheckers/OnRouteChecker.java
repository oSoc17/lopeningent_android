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

package com.dp16.runamicghent.DynamicCheckers;

import com.dp16.runamicghent.Constants;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class listens to the current location and checks this location with the path the user should run.
 * It will publish a notification on the OFFROUTE channel if users does not follow route.
 * The class only checks proximity to a polyline. In case a user reverses direction and follows the same path,
 * no error will be raised.
 *
 * <p>
 *     <b>Messages Produced: </b> None.
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#LOCATION}
 * </p>
 *
 * Created by hendrikdepauw on 28/02/2017.
 */

public class OnRouteChecker implements EventListener, EventPublisher {
    private List<LatLng> path;
    private double precision;
    private boolean geodesic = true;
    private boolean currentlyOffroute;
    private ExecutorService worker;

    /**
     * Constructor. Registers at the eventbroker to recieve location updates.
     * Also starts a new thread so the eventbroker does not have to wait for
     * isLocationOnPath to return.
     *
     * @param path      Initial path that should be followed
     * @param interval  How often the OnRouteChecker should check the path
     * @param precision How much does a user have to stray from the path before an event is published
     */
    public OnRouteChecker(List<LatLng> path, int interval, double precision) {
        this.path = path;
        this.precision = precision;
        currentlyOffroute = false;

        EventBroker broker = EventBroker.getInstance();
        broker.addEventListener(Constants.EventTypes.LOCATION, this, interval);

        //Make worker thread
        worker = Executors.newSingleThreadExecutor();
    }

    /**
     * Update the path to which the current location has to be compared with.
     *
     * @param newPath
     */
    public void changePath(List<LatLng> newPath) {
        this.path = newPath;
    }

    /**
     * Handles a received event. Starts a new thread to check the location with the path.
     * Should not be called too frequently because it will create a lot of threads.
     *
     * @param eventType Should always be LOCATION
     * @param message   A LatLng containing the current location of the user
     */
    @Override
    public void handleEvent(String eventType, Object message) {
        //Submit a task to the worker thread
        Runnable task = new Worker(message);
        worker.submit(task);
    }

    /**
     * This is the worker thread.
     * It will check if a location is on the path with the desired precision.
     * Will send an OFFROUTE event if location is not on path.
     */
    private class Worker implements Runnable, EventPublisher {
        private final Object objectLocation;

        public Worker(Object objectLocation) {
            this.objectLocation = objectLocation;
        }

        @Override
        public void run() {
            //Parse the location to a LatLng
            LatLng location = (LatLng) objectLocation;
            //Check the location
            Boolean offPath = !(PolyUtil.isLocationOnPath(location, path, geodesic, precision));
            //Send an OFFROUTE event if necessary.
            if (offPath && !currentlyOffroute) {
                currentlyOffroute = true;
                EventBroker.getInstance().addEvent(Constants.EventTypes.OFFROUTE, offPath, this);
            } else if (!offPath && currentlyOffroute) {
                currentlyOffroute = false;
                EventBroker.getInstance().addEvent(Constants.EventTypes.OFFROUTE, offPath, this);
            }
        }
    }
}
