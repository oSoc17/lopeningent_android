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

import com.dp16.runamicghent.DynamicCheckers.OnRouteChecker;
import com.dp16.runamicghent.util.ThreadingUtils;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisherClass;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;


/**
 * Unit tests for OnRouteChecker.
 * Created by Nick on 28-3-2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class OnRouteCheckerUnitTests {
    private static final int interval = 0;
    private static final double precision = Constants.RouteChecker.ACCURACY;
    private EventBroker broker;

    @Before
    public void init() {
        broker = EventBroker.getInstance();
    }

    @Test
    public void OnRouteChecker_basicOffPathScenario_doesPublishOffrouteEvents() {
        final AtomicInteger messagesReceived = new AtomicInteger(0);
        List<LatLng> path = new ArrayList<>();
        path.add(new LatLng(51, 3));
        path.add(new LatLng(51, 4));

        LatLng location = new LatLng(9, 8);

        // listener for OFFROUTE events
        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                boolean boolMessage = (Boolean) message;
                if (boolMessage) {
                    messagesReceived.incrementAndGet();
                }
            }
        };

        EventPublisherClass publisher = new EventPublisherClass();
        broker.addEventListener(Constants.EventTypes.OFFROUTE, listener);

        broker.start();
        OnRouteChecker checker = new OnRouteChecker(path, interval, precision);
        publisher.publishEvent(Constants.EventTypes.LOCATION, location);

        ThreadingUtils.waitUntilAtomicVariableReachesValue(25, 40, "OnRouteChecker did not publish an OFFROUTE event for a point off the route within ", messagesReceived, 1);

        broker.stop();
        assertEquals("OnRouteChecker published no OFFROUTE event for a point off the route", 1, messagesReceived.get());
    }

    @Test
    public void OnRouteChecker_basicOnPathScenario_doesNotPublishOffrouteEvents() {
        final AtomicInteger messagesReceived = new AtomicInteger(0);
        List<LatLng> path = new ArrayList<>();
        path.add(new LatLng(51, 2));
        path.add(new LatLng(51, 3));
        path.add(new LatLng(51, 4));

        LatLng location = new LatLng(51, 3);

        // listener for OFFROUTE events
        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messagesReceived.incrementAndGet();
            }
        };

        EventPublisherClass publisher = new EventPublisherClass();
        broker.addEventListener(Constants.EventTypes.OFFROUTE, listener);
        OnRouteChecker checker = new OnRouteChecker(path, interval, precision);

        broker.start();
        publisher.publishEvent(Constants.EventTypes.LOCATION, location);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            //
        }
        broker.stop();

        assertEquals("OnRouteChecker published an OFFROUTE event for a point on the route", 0, messagesReceived.get());
    }

    @Test
    public void OnRouteChecker_simpleChangePath_worksCorrectly() {
        final AtomicInteger messagesReceived = new AtomicInteger(0);
        List<LatLng> initialPath = new ArrayList<>();
        initialPath.add(new LatLng(1, 2));
        initialPath.add(new LatLng(1, 3));
        initialPath.add(new LatLng(1, 4));
        List<LatLng> secondPath = new ArrayList<>();
        secondPath.add(new LatLng(51, 3));
        secondPath.add(new LatLng(51, 3.5));
        secondPath.add(new LatLng(51, 4));

        LatLng location = new LatLng(51, 3.5);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messagesReceived.incrementAndGet();
            }
        };

        EventPublisherClass publisher = new EventPublisherClass();
        broker.addEventListener(Constants.EventTypes.OFFROUTE, listener);
        OnRouteChecker checker = new OnRouteChecker(initialPath, interval, precision);
        checker.changePath(secondPath);

        broker.start();
        publisher.publishEvent(Constants.EventTypes.LOCATION, location);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            //
        }
        broker.stop();
        assertEquals("OnRouteChecker published an OFFROUTE event for a point on the route", 0, messagesReceived.get());
    }

    @Test
    public void OnRouteChecker_changePath_checksOnNewPath() {
        final AtomicInteger offRouteMessagesReceived = new AtomicInteger(0);
        final AtomicInteger onRouteMessagesReceived = new AtomicInteger(0);

        List<LatLng> initialPath = new ArrayList<>();
        initialPath.add(new LatLng(1, 2));
        initialPath.add(new LatLng(1, 3));
        initialPath.add(new LatLng(1, 4));
        List<LatLng> secondPath = new ArrayList<>();
        secondPath.add(new LatLng(51, 3));
        secondPath.add(new LatLng(51, 3.5));
        secondPath.add(new LatLng(51, 4));

        LatLng locationOnInitialPath = new LatLng(1, 2.5);
        LatLng locationOffInitialPath = new LatLng(57, 55);
        LatLng locationOnSecondPath = new LatLng(51, 3.5);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                boolean boolMessage = (Boolean) message;
                // if the boolean is true: we went off path
                // if the boolean is false: we went back on path
                if (boolMessage) {
                    offRouteMessagesReceived.incrementAndGet();
                } else {
                    onRouteMessagesReceived.incrementAndGet();
                }
            }
        };
        EventPublisherClass eventPublisher = new EventPublisherClass();
        broker.addEventListener(Constants.EventTypes.OFFROUTE, listener);

        broker.start();
        OnRouteChecker checker = new OnRouteChecker(initialPath, interval, precision);

        // send the first two locations: the second should generate and OFFROUTE message
        eventPublisher.publishEvent(Constants.EventTypes.LOCATION, locationOnInitialPath);
        eventPublisher.publishEvent(Constants.EventTypes.LOCATION, locationOffInitialPath);
        ThreadingUtils.waitUntilAtomicVariableReachesValue(25, 40, "OnRouteChecker did not publish an OFFROUTE (true) event within ", offRouteMessagesReceived, 1);

        // swap path to check
        checker.changePath(secondPath);

        // send last location: should be once again on path -> should receive an OFFROUTE message with data 'false'
        eventPublisher.publishEvent(Constants.EventTypes.LOCATION, locationOnSecondPath);
        ThreadingUtils.waitUntilAtomicVariableReachesValue(25, 40, "OnRouteChecker did not publish an OFFROUTE (false) event within ", onRouteMessagesReceived, 1);

        broker.stop();
        assertEquals("OnRouteChecker did not publish an OFFROUTE (true) event for an off-route scenario", 1, offRouteMessagesReceived.get());
        assertEquals("OnRouteChecker did not publish an OFFROUTE (false) event for a back-on-route scenario", 1, onRouteMessagesReceived.get());
    }
}