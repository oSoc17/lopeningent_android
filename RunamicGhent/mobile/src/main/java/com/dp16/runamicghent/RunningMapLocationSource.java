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

package com.dp16.runamicghent;

import android.location.Location;

import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.google.android.gms.maps.LocationSource;

/**
 * Custom location source for the Google Maps
 * Normally they start their own location provider but this is not wanted for two reasons:
 * 1. We already implemented a location provider so we don't want google to start a second ne
 * 2. This way we can use our custom location provider mock while the normal 'blue dot' is still used
 * This class does not do much except for listening for RawLocations and passing them to
 * the OnLocationChangedListener
 * Created by hendrikdepauw on 22/03/2017.
 *
 * <p>
 *     <b>Messages Produced: </b> None.
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#RAW_LOCATION}
 * </p>
 *
 */

public class RunningMapLocationSource implements LocationSource, EventListener {
    private OnLocationChangedListener listener;

    //No constructor, default constructor without arguments is used.

    /**
     * Set the listener and start listening for location updates
     *
     * @param onLocationChangedListener where the location updates are passed to
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        this.listener = onLocationChangedListener;
        EventBroker.getInstance().addEventListener(Constants.EventTypes.RAW_LOCATION, this);
    }

    @Override
    public void deactivate() {
        EventBroker.getInstance().removeEventListener(this);
    }

    /**
     * Receives an event and passes it to the listener.
     * Might want to put this in a separate thread as we do not know if .onLocationChanged
     * can take a long time.
     *
     * @param eventType type of event, should be RAW_LOCATION
     * @param message   the Location object
     */
    @Override
    public void handleEvent(String eventType, Object message) {
        listener.onLocationChanged((Location) message);
    }
}
