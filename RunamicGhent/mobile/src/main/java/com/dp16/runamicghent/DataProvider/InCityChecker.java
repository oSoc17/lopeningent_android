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

import android.util.Log;

import com.dp16.runamicghent.Constants;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

/**
 * Class that checks whether the runner in currently located in a given city (as of now only Ghent).
 * This class subscribes itself to {@link Constants.EventTypes#LOCATION} events and publishes {@link Constants.EventTypes#IN_CITY} events, depending on the location of the runner.
 * Checking if a runner is in a city is done by checking if the location of the runner is inside a box that is predefined for each city.
 *
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#IN_CITY}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#LOCATION}
 * </p>
 * Created by Simon on 9/03/17.
 */
public class InCityChecker implements DataProvider, EventPublisher, EventListener {

    protected final String tag = this.getClass().getName();

    private LatLngBounds box;

    /**
     * Minimum interval between listener updates.
     * See {@link EventBroker#addEventListener(String, EventListener, int)}.
     */
    private int listenerInterval = 0;

    /**
     * Constructor for InCityChecker.
     *
     * @param city     Case-insensitive String of the user's current city location.
     * @param interval Minimum interval on which we like to receive updates.
     */
    public InCityChecker(String city, int interval) {
        String city1 = validateInput(city);
        if (city1.isEmpty()) {
            Log.d(this.getClass().getSimpleName(), "city is not valid");
        }
        //use dict to get the bounding box of the city.
        this.box = Constants.CityBoundingBoxes.latLngBoundGhent;
        //set the listener interval
        this.listenerInterval = interval < 0 ? 0 : interval;
        //register to EventBroker AFTER getting constants, avoiding NPE
        this.start();
    }

    /**
     * Constructor for InCityChecker.
     *
     * @param city Case-insensitive String of the user's current city location.
     */
    public InCityChecker(String city) {
        this(city, 0);
    }


    @Override
    public void start() {
        EventBroker.getInstance().addEventListener(Constants.EventTypes.LOCATION, this, this.listenerInterval);
    }

    @Override
    public void stop() {
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.LOCATION, this);
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
     * Changes the minimum interval on which the listener will receive updates.
     * This is done by stopping (i.e. de-registering), changing the interval and starting (i.e. re-registering).
     *
     * @param milliseconds The new minimum interval on which we want to receive updates.
     */
    public void changeListenerInterval(int milliseconds) {
        this.stop();
        this.listenerInterval = milliseconds;
        Log.d(tag, "INTERVAL Changed to " + this.listenerInterval + "ms");
        this.start();
    }

    public int getListenerInterval() {
        return this.listenerInterval;
    }

    /**
     * Handles the reception of LOCATION events. On reception, we verify that the current location in inside the city.
     * If the user is in the city, a IN_CITY event containing the message true is published.
     * Otherwise the message is false.
     *
     * @param eventType The type of received event. In this case LOCATION.
     * @param message   In this case the LatLng location of the user.
     */
    @Override
    public void handleEvent(String eventType, Object message) {
        LatLng location = (LatLng) message;
        Log.d(this.tag, location.toString());
        Log.d(this.tag, this.box.toString());

        EventBroker.getInstance().addEvent(Constants.EventTypes.IN_CITY, this.box.contains(location), this);
    }

    /**
     * Checks if this city is covered by our application.
     *
     * @param city name of the city for which to check if we have a coordinate box.
     * @return returns the name of the city if it exists and throws an exception if not.
     */
    private String validateInput(String city) {
        if (Constants.Cities.cityIsKnown(city.toUpperCase())) {
            return city.toUpperCase();
        } else
            throw new IllegalArgumentException("The city " + city + " is either misspelled or is not covered by this app.");
    }

}
