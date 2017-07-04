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

package com.dp16.runamicghent.statTrackerTests;

import android.app.Activity;

import com.dp16.runamicghent.StatTracker.StatTracker;
import com.dp16.eventbroker.EventBroker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for StatTracker.StatTracker
 * Created by Nick on 27-3-2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class StatTrackerTests {
    private static final double deltaDoulble = 0.001;
    private EventBroker broker;
    private StatTracker statTracker;
    private Activity activity;

    @Before
    public void init() {
        broker = EventBroker.getInstance();
        broker.start();
        broker.stop();
        activity = Robolectric.buildActivity(Activity.class).create().get();
        statTracker = new StatTracker(activity);
    }

    @Test
    public void StatTracker_startPauseStartStop_addsAndRemovesEventListeners() {
        statTracker.startStatTracker();
        int amountListenersAfterStart = broker.getAmountOfListeners();
        assertTrue("StatTracker.start does not attach eventListeners", amountListenersAfterStart > 0);
        statTracker.pauseStatTracker();
        assertTrue("StatTracker.pause does not detach eventListeners", broker.getAmountOfListeners() < amountListenersAfterStart);
        statTracker.resumeStatTracker();
        assertEquals("StatTracker.resume (after a pause) does not attach all eventListeners again", amountListenersAfterStart, broker.getAmountOfListeners());
        statTracker.stopStatTracker();
        assertEquals("StatTracker.stop does not detach all eventListeners", 0, broker.getAmountOfListeners());
    }

    @Test
    public void StatTracker_addRating_ratingFoundInStatistics() {
        double rating = 5;
        statTracker.startStatTracker();
        statTracker.addRating(rating);
        statTracker.stopStatTracker();
        assertEquals("StatTracker.addRating does not save it to its statistics", rating, statTracker.getRunningStatistics().getRating(), deltaDoulble);
    }
}
