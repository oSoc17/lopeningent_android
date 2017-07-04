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

package com.dp16.runamicghent.runDataTests;

import com.dp16.runamicghent.RunData.RunDistance;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the RunData.RunDistance class.
 * Created by Nick on 25-3-2017.
 */

public class RunDistanceTests {

    @Test
    public void runDistance_doesNotChangeData(){
        int dist = 1235;
        RunDistance distance = new RunDistance(dist);
        assertEquals("RunDistance does not keep data in original form", dist, distance.getDistance());
    }

    @Test
    public void runDistance_toString_correctFormatting(){
        int smallDist = 178;
        RunDistance smallDistance = new RunDistance(smallDist);
        assertTrue("RunDistance toString formatting does not confirm interface description", smallDistance.toString().contains(" m"));
        assertTrue("RunDistance toString formatting does not confirm interface description", smallDistance.toString().contains(Integer.toString(smallDist)));

        int longDist = 4597;
        RunDistance longDistance = new RunDistance(longDist);
        assertTrue("RunDistance toString formatting does not confirm interface description", longDistance.toString().contains(" km"));
        assertTrue("RunDistance toString formatting does not confirm interface description", longDistance.toString().contains(Integer.toString(longDist/1000)));
    }

    @Test
    public void runDistance_add_addsCorrectly(){
        int distance1 = 3676;
        int distance2 = 976;

        RunDistance runDistance1 = new RunDistance(distance1);
        RunDistance runDistance2 = new RunDistance(distance2);
        runDistance1.add(runDistance2);

        assertEquals("RunDistance add does not add two runDistances correctly", distance1+distance2, runDistance1.getDistance());
    }

    @Test //this test may fail due to localization errors (dutch numbers are with comma, english with a point)
    public void runDistance_toAudioString_formatsCorrectly(){
        int distance1 = 3676;
        int distance2 = 976;

        RunDistance runDistance1 = new RunDistance(distance1);
        RunDistance runDistance2 = new RunDistance(distance2);

        assertEquals("3.68 kilometers", runDistance1.toAudioString());
        assertEquals("976 meters", runDistance2.toAudioString());
    }
}
