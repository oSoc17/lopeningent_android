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
import com.dp16.runamicghent.DataProvider.RatingTransmitter;
import com.dp16.runamicghent.RunData.RunRating;
import com.dp16.runamicghent.util.ThreadingUtils;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisherClass;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Tests for DataProvider.RatingTransmitter
 * Created by Lorenz 29-4-2017
 */

//@RunWith(RobolectricTestRunner.class)
//@Config(manifest = Config.NONE)
public class RatingTransmitterTests {
    private EventBroker broker;
    private RatingTransmitter ratingTransmitter;
    private static final int maxWaitTime = 5;

    @Before
    public void init(){
        broker = EventBroker.getInstance();
        ratingTransmitter = new RatingTransmitter(true);
    }

    @Test
    public void ratingTransmitter_startPauseResumeStop_doesNotThrowExceptions(){
        ratingTransmitter.start();
        ratingTransmitter.pause();
        ratingTransmitter.resume();
        ratingTransmitter.stop();
    }

    @Ignore("RatingTransmitter can't make connection with our server due to SSL handshake error that only occurs when testing")
    @Test
    public void ratingTransmitter_setRating(){
        final AtomicInteger messagesReceived = new AtomicInteger(0);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                int statusCode = (int) message;
                assertEquals("Status code is equal to 200", 200, statusCode);
                if (statusCode == 200) {
                    messagesReceived.incrementAndGet();
                }
            }
        };

        // Tag of a very small route in the middle of nowhere, adjusting these rating doesn't matter
        String tag = "BmwfOyj0EZj-O*fRfyBK8EcgSOyD";

        // Create RunRating object
        RunRating runRating = new RunRating(tag, 3.5f);

        // Create EventPublisherClass
        EventPublisherClass publisher = new EventPublisherClass();

        // Start EventBroker and RatingTransmitter
        broker.addEventListener(Constants.EventTypes.STATUS_CODE, listener);
        broker.start();
        ratingTransmitter.start();

        publisher.publishEvent(Constants.EventTypes.RATING, runRating);

        // wait 'maxWaitTime' seconds at max
        ThreadingUtils.waitUntilAtomicVariableReachesValue(25, maxWaitTime * 40, "Did not receive 200 status from ratingTransmitter within ", messagesReceived, 1);

        ratingTransmitter.stop();
        broker.stop();
    }
}
