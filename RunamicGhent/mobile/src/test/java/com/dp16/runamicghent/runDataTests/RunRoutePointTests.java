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

import com.dp16.runamicghent.RunData.RunDirection;
import com.dp16.runamicghent.RunData.RunRoutePoint;
import com.google.android.gms.maps.model.LatLng;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * Created by hendrikdepauw on 02/04/2017.
 */

public class RunRoutePointTests {
    LatLng testLocation;
    String testStringDirection;
    RunDirection testRunDirection;
    RunDirection.Direction testDirection;

    @Before
    public void init(){
        testLocation = new LatLng(51.056472, 3.721790);
        testStringDirection = "none";
        testRunDirection = new RunDirection("none");
        testDirection = RunDirection.Direction.NONE;
    }

    @Test
    public void runRoutePoint_testConstructor1(){
        RunRoutePoint runRoutePoint = new RunRoutePoint(testLocation, testRunDirection);

        Assert.assertEquals("RunRoutePoint does not set location properly", testLocation, runRoutePoint.getLocation());
        Assert.assertEquals("RunRoutePoint does not set direction properly", testDirection, runRoutePoint.getDirection().getDirection());
    }

    @Test
    public void runRoutePoint_testConstructor2(){
        RunRoutePoint runRoutePoint = new RunRoutePoint(testLocation.latitude, testLocation.longitude, testRunDirection);

        Assert.assertEquals("RunRoutePoint does not set location properly", testLocation, runRoutePoint.getLocation());
        Assert.assertEquals("RunRoutePoint does not set direction properly", testDirection, runRoutePoint.getDirection().getDirection());
    }

    @Test
    public void runRoutePoint_testConstructor3(){
        RunRoutePoint runRoutePoint = new RunRoutePoint(testLocation, testStringDirection);

        Assert.assertEquals("RunRoutePoint does not set location properly", testLocation, runRoutePoint.getLocation());
        Assert.assertEquals("RunRoutePoint does not set direction properly", testDirection, runRoutePoint.getDirection().getDirection());
    }

    @Test
    public void runRoutePoint_testConstructor4(){
        RunRoutePoint runRoutePoint = new RunRoutePoint(testLocation.latitude, testLocation.longitude, testStringDirection);

        Assert.assertEquals("RunRoutePoint does not set location properly", testLocation, runRoutePoint.getLocation());
        Assert.assertEquals("RunRoutePoint does not set direction properly", testDirection, runRoutePoint.getDirection().getDirection());
    }
}
