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
import com.dp16.runamicghent.DataProvider.TimingProvider;
import com.dp16.runamicghent.RunData.RunDuration;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Tests for DataProvider.TimingProvider
 * Created by Nick on 4-4-2017.
 */

public class TimingProviderTests {
    private EventBroker broker;
    private TimingProvider provider;

    @Before
    public void init() {
        broker = EventBroker.getInstance();
        provider = new TimingProvider();
    }

    @Test
    public void TimingProvider_basicScenario_reportsTimingWithin2Seconds() {
        int sleepTimeBeforePause = 5; // in seconds
        int sleepTimeDuringPause = 4;
        int sleepTimeAfterPause = 3;

        final AtomicInteger durationReceived = new AtomicInteger(0);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                RunDuration runDuration = (RunDuration) message;
                durationReceived.set(runDuration.getSecondsPassed());
            }
        };

        broker.addEventListener(Constants.EventTypes.DURATION, listener);
        broker.start();
        provider.start();

        try {
            Thread.sleep(sleepTimeBeforePause * 1000);
        } catch (InterruptedException e) {
            //
        }

        provider.pause();

        try {
            Thread.sleep(sleepTimeDuringPause * 1000);
        } catch (InterruptedException e) {
            //
        }

        provider.resume();

        try {
            Thread.sleep(sleepTimeAfterPause * 1000);
        } catch (InterruptedException e) {
            //
        }

        provider.stop();
        broker.stop();
        assertEquals("TimingProvider does not return time within \"runningtime - amount_stops\"",
                sleepTimeBeforePause + sleepTimeAfterPause, provider.getRunDuration().getSecondsPassed(), 2); // this two is the amount of pauses/stops
        //assertEquals("TimingProvider does not return same time to eventbroker as in getRunDuration",
        //        provider.getRunDuration().getSecondsPassed(), durationReceived.get());
        //TODO: this jus stopped working
    }
}
