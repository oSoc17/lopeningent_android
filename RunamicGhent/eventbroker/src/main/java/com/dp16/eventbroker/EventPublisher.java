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
 * Interface that must be implemented by a class that wishes to publish events.
 * When using this interface, you must publish an event to the EventBroker explicitly by calling {@link EventBroker#addEvent(String, Object, EventPublisher)}.
 *
 * The EventPublisherClass provides a wrapper in the form of publishEvent(String type, Object message) and thus is recommended over the interface.
 *
 * @see EventPublisherClass
 * @see EventBroker
 * Created by Nick on 25-2-2017.
 */

public interface EventPublisher {
}
