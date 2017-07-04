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

import android.app.Activity;
import android.location.Location;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.DataProvider.LocationProvider;
import com.dp16.runamicghent.util.ThreadingUtils;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.google.android.gms.location.LocationServices;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.fail;

/**
 * Tests for DataProvider.LocationProvider
 * Created by Nick on 4-4-2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class LocationProviderTests {
    private EventBroker broker;
    private LocationProvider provider;
    private Activity activity;

    @Before
    public void init(){
        broker = EventBroker.getInstance();
        activity = Robolectric.buildActivity(Activity.class).create().get();
        provider = new LocationProvider(activity);
    }


    @Test
    public void LocationProvider_startPauseResumeStop_doesNotCauseProblems(){
        provider.start();
        provider.pause();
        provider.resume();
        provider.stop();
    }

    @Ignore("LocationProvider test has to be in an instrumentation test. Is not possible on a JVM, because this class uses the GoogleApiClient of the device")
    @Test
    public void LocationProvider_publishesToEventBroker(){
        final AtomicInteger messagesReceivedRaw = new AtomicInteger(0);
        final AtomicInteger messagesReceived = new AtomicInteger(0);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messagesReceived.incrementAndGet();
            }
        };

        EventListener listenerRaw = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messagesReceivedRaw.incrementAndGet();
            }
        };

        broker.addEventListener(Constants.EventTypes.RAW_LOCATION, listenerRaw);
        broker.addEventListener(Constants.EventTypes.LOCATION, listener);

        broker.start();
        provider.start();

        Location mockLocation = new Location("blabla");
        mockLocation.setLatitude(51);
        mockLocation.setLongitude(3);
        mockLocation.setTime(System.currentTimeMillis());

        // wait until the GoogleApiClient is connected
        int i = 0;
        while(!provider.getGoogleApiClient().isConnected()){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e){
                //
            }
            i++;
            if(i > 100){
                fail("GoogleApiClient not connected within 10 seconds");
            }
        }

        try {
            LocationServices.FusedLocationApi.setMockLocation(provider.getGoogleApiClient(), mockLocation);
        } catch (SecurityException e){
            e.printStackTrace();
        }

        ThreadingUtils.waitOneSecUntilAtomicVariableReachesValue("LocationProvider did not publish a location message within ", messagesReceived, 1);
        ThreadingUtils.waitOneSecUntilAtomicVariableReachesValue("LocationProvider did not publish a raw location message within ", messagesReceivedRaw, 1);

        provider.stop();
        broker.stop();
    }
}
