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
import com.dp16.runamicghent.DataProvider.RouteProvider;
import com.dp16.runamicghent.RunData.RunDistance;
import com.dp16.runamicghent.TrackRequest;
import com.dp16.runamicghent.TrackResponse;
import com.dp16.runamicghent.util.ThreadingUtils;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisherClass;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for DataProvider.RouteProvider
 * Created by Nick on 4-4-2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RouteProviderTests {
    private EventBroker broker;
    private RouteProvider provider;
    private static final int maxWaitTime = 5; // 5 seconds as specified in the acceptance tests

    @Before
    public void init(){
        broker = EventBroker.getInstance();
        provider = new RouteProvider(RuntimeEnvironment.application);
    }

    @Test
    public void routeProvider_startPauseResumeStop_doesNotThrowExceptions(){
        provider.start();
        provider.pause();
        provider.resume();
        provider.stop();
    }

    @Ignore("RouteProvider test seems to hang in robolectric on sharedPreferences.getInt()")
    @Test
    public void routeProvider_requestRoute_returnsRoute(){
        final AtomicInteger messagesReceived = new AtomicInteger(0);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                TrackResponse answer = (TrackResponse) message;
                assertNotNull("Answer from RouteProvider is null", answer);
                messagesReceived.incrementAndGet();
            }
        };

        EventPublisherClass publisher = new EventPublisherClass();

        broker.addEventListener(Constants.EventTypes.TRACK, listener);
        broker.start();
        provider.start();

        LatLng middleOfGhent = new LatLng(51.053401, 3.725119);
        RunDistance distance = new RunDistance(5000);
        TrackRequest trackRequest = new TrackRequest(middleOfGhent, distance, false);
        publisher.publishEvent(Constants.EventTypes.TRACK_REQUEST, trackRequest);

        // wait 'maxWaitTime' seconds at max
        ThreadingUtils.waitUntilAtomicVariableReachesValue(25, maxWaitTime * 40, "Did not receive a route for RouteProvider within ", messagesReceived, 1);

        provider.stop();
        broker.stop();
    }
}
