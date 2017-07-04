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

package com.dp16.eventbroker;

/**
 * A simple class that can be used instead of the {@link EventPublisher} interface.
 * This class offers the {@link #publishEvent(String, Object)} convenience method
 * instead of having to use {@link EventBroker#addEvent(String, Object, EventPublisher)}.
 *
 * @see EventPublisher
 * @see EventBroker
 * Created by Nick on 26-2-2017.
 */

public class EventPublisherClass implements EventPublisher {
    /**
     * Publishes an event to the event broker.
     * @param eventType This event type can be any String.
     * @param message Message to send to all listeners.
     */
    public void publishEvent(String eventType, Object message){
        EventBroker.getInstance().addEvent(eventType, message, this);
    }
}
