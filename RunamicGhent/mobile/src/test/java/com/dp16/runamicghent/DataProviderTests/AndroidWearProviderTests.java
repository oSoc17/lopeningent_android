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

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.DataProvider.AndroidWearProvider;
import com.dp16.runamicghent.util.ThreadingUtils;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.google.android.gms.wearable.MessageEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Stiaan on 2/05/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AndroidWearProviderTests {

    private EventBroker broker;
    private AndroidWearProvider provider;
    private Activity activity;

    @Before
    public void init(){
        broker =  EventBroker.getInstance();
        activity = Robolectric.buildActivity(Activity.class).create().get();
        provider = new AndroidWearProvider(activity);
    }

    @Test
    public void AndroidWearProvider_startPauseResumeStop_doesNotCauseProblems(){
        provider.start();
        provider.pause();
        provider.resume();
        provider.stop();
    }

    @Test
    public void AndroidWearProvider_onMessageReceived_messageHandlingWorks(){

        final AtomicInteger heartRateMessage = new AtomicInteger(0);
        final AtomicInteger stateMessage = new AtomicInteger(0);

        MessageEvent heartRateEvent = new MessageEvent() {
            @Override
            public int getRequestId() {
                return 0;
            }

            @Override
            public String getPath() {
                return Constants.WearMessageTypes.HEART_RATE_MESSAGE_WEAR;
            }

            @Override
            public byte[] getData() {
                return new String("15").getBytes();
            }

            @Override
            public String getSourceNodeId() {
                return null;
            }
        };

        MessageEvent requestStateEvent = new MessageEvent() {
            @Override
            public int getRequestId() {
                return 0;
            }

            @Override
            public String getPath() {
                return Constants.WearMessageTypes.REQUEST_STATE_MESSAGE_WEAR;
            }

            @Override
            public byte[] getData() {
                return new byte[0];
            }

            @Override
            public String getSourceNodeId() {
                return null;
            }
        };

        EventListener heartrateListener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                heartRateMessage.incrementAndGet();
            }
        };

        broker.addEventListener(Constants.EventTypes.HEART_RESPONSE, heartrateListener);

        broker.start();
        provider.start();


        provider.onMessageReceived(requestStateEvent);

        provider.onMessageReceived(heartRateEvent);

        ThreadingUtils.waitOneSecUntilAtomicVariableReachesValue("LocationProvider did not publish a location message within ", heartRateMessage, 1);



    }

}
