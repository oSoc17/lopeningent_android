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

package com.dp16.runamicghent.DataProviderTests;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.DataProvider.InCityChecker;
import com.dp16.runamicghent.util.ThreadingUtils;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Simon on 31/03/17.
 */

public class InCityCheckerTest {

    InCityChecker inCityChecker;
    EventPublishTester testPublisher;
    EventListenerTester testListener;

    @Before
    public void init() {
        this.inCityChecker = new InCityChecker("Ghent");
        this.testPublisher = new EventPublishTester();
        this.testListener = new EventListenerTester();
    }

    @Test
    public void InCityChecker_changeListenerInterval_IntervalIsChanged() {
        assertFalse("Initialization put check interval at 0", this.inCityChecker.getListenerInterval() == 5);
        this.inCityChecker.changeListenerInterval(5);
        assertTrue("Interval is changed", this.inCityChecker.getListenerInterval() == 5);
    }

    @Test
    public void InCityChecker_basicInCityScenario_PublishesIsInCityEvent() throws InterruptedException {
        final AtomicInteger messagesReceived_IN_CITY = new AtomicInteger(0);
        final AtomicInteger messagesReceived_NOT_IN_CITY = new AtomicInteger(0);

        // listener for IN_CITY events
        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                if((boolean) message){
                    messagesReceived_IN_CITY.incrementAndGet();
                } else {
                    messagesReceived_NOT_IN_CITY.incrementAndGet();
                }
            }
        };
        EventBroker.getInstance().addEventListener(Constants.EventTypes.IN_CITY, listener, 100);

        //publish event with location inside Ghent
        EventBroker.getInstance().start();
        this.testPublisher.publishEvent(new LatLng(51.045734, 3.725));

        ThreadingUtils.waitUntilAtomicVariableReachesValue(25, 40, "InCityChecker did not publish an IN_CITY event for a point inside of Ghent ", messagesReceived_IN_CITY, 1);

        EventBroker.getInstance().stop();
        assertEquals("InCityChecker published no IN_CITY event for a point outside of Ghent", 1, messagesReceived_IN_CITY.get());
        assertEquals("InCityChecker also published a NOT_IN_CITY event, which should not happen for this location.", 0, messagesReceived_NOT_IN_CITY.get());
    }

    @Test
    public void InCityChecker_NotInCityScenario_PublishesNotInCityEvent() {
        final AtomicInteger messagesReceived_IN_CITY = new AtomicInteger(0);
        final AtomicInteger messagesReceived_NOT_IN_CITY = new AtomicInteger(0);

        // listener for IN_CITY events
        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                if((boolean) message){
                    messagesReceived_IN_CITY.incrementAndGet();
                } else {
                    messagesReceived_NOT_IN_CITY.incrementAndGet();
                }
            }
        };
        EventBroker.getInstance().addEventListener(Constants.EventTypes.IN_CITY, listener, 100);


        //publish event with location inside Ghent
        EventBroker.getInstance().start();
        this.testPublisher.publishEvent(new LatLng(12.3456, 7.8910));

        ThreadingUtils.waitUntilAtomicVariableReachesValue(25, 40, "InCityChecker did not publish an IN_CITY event for a point inside of Ghent ", messagesReceived_NOT_IN_CITY, 1);

        EventBroker.getInstance().stop();
        assertEquals("InCityChecker published also a IN_CITY event for a point outside of Ghent", 0, messagesReceived_IN_CITY.get());
        assertEquals("InCityChecker also published a NOT_IN_CITY event, which should not happen for this location.", 1, messagesReceived_NOT_IN_CITY.get());

    }


    private class EventPublishTester implements EventPublisher {
        void publishEvent(LatLng loc) {
            EventBroker.getInstance().addEvent(Constants.EventTypes.LOCATION, loc, this);
        }
    }

    private class EventListenerTester implements EventListener {

        boolean signalReceived_IN_CITY;
        boolean signalReceived_NOT_IN_CITY;

        EventListenerTester() {
            this.signalReceived_IN_CITY = false;
            this.signalReceived_NOT_IN_CITY = false;
            EventBroker.getInstance().addEventListener(Constants.EventTypes.IN_CITY, this, 100);
        }

        @Override
        public void handleEvent(String eventType, Object message) {

        }
    }
}
