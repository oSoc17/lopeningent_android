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

import com.dp16.runamicghent.RunData.RunDistance;
import com.google.android.gms.maps.model.LatLng;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Created by hendrikdepauw on 12/04/2017.
 */

public class TrackRequestTests {

    @Test
    public void trackRequest_constructor_noTagWithNumber(){
        LatLng location = new LatLng(10, 10);
        RunDistance distance = new RunDistance(1010);
        boolean dynamic = false;
        int REQUEST_NUMBER = 10;

        TrackRequest trackRequest = new TrackRequest(location, distance, dynamic, REQUEST_NUMBER);

        Assert.assertEquals("TrackRequest did not save Location correctly", trackRequest.getLocation(), location);
        Assert.assertEquals("TrackRequest did not save Distance correctly", trackRequest.getDistance(), distance);
        Assert.assertEquals("TrackRequest did not save Dynamic correctly", trackRequest.getDynamic(), dynamic);
        Assert.assertEquals("TrackRequest did not save Request number correctly", trackRequest.getRequestNumber(), REQUEST_NUMBER);
        Assert.assertNull("TrackRequest did not set tag to null", trackRequest.getTag());
    }

    @Test
    public void trackRequest_constructor_noTagNoNumber(){
        LatLng location = new LatLng(10, 10);
        RunDistance distance = new RunDistance(1010);
        boolean dynamic = false;

        TrackRequest trackRequest = new TrackRequest(location, distance, dynamic);

        Assert.assertEquals("TrackRequest did not save Location correctly", trackRequest.getLocation(), location);
        Assert.assertEquals("TrackRequest did not save Distance correctly", trackRequest.getDistance(), distance);
        Assert.assertEquals("TrackRequest did not save Dynamic correctly", trackRequest.getDynamic(), dynamic);
        Assert.assertNotNull("TrackRequest did not save Request number correctly", trackRequest.getRequestNumber());
        Assert.assertNull("TrackRequest did not set tag to null", trackRequest.getTag());
    }

    @Test
    public void trackRequest_constructor_withTagWithNumber(){
        LatLng location = new LatLng(10, 10);
        RunDistance distance = new RunDistance(1010);
        boolean dynamic = false;
        String tag = "ueywfghj";
        int REQUEST_NUMBER = 10;

        TrackRequest trackRequest = new TrackRequest(location, distance, dynamic, tag, REQUEST_NUMBER);

        Assert.assertEquals("TrackRequest did not save Location correctly", trackRequest.getLocation(), location);
        Assert.assertEquals("TrackRequest did not save Distance correctly", trackRequest.getDistance(), distance);
        Assert.assertEquals("TrackRequest did not save Dynamic correctly", trackRequest.getDynamic(), dynamic);
        Assert.assertEquals("TrackRequest did not save Request number correctly", trackRequest.getRequestNumber(), REQUEST_NUMBER);
        Assert.assertEquals("TrackRequest did not save Tag correctly", trackRequest.getTag(), tag);
    }

    @Test
    public void trackRequest_constructor_withTagNoNumber(){
        LatLng location = new LatLng(10, 10);
        RunDistance distance = new RunDistance(1010);
        boolean dynamic = false;
        String tag = "ueywfghj";

        TrackRequest trackRequest = new TrackRequest(location, distance, dynamic, tag);

        Assert.assertEquals("TrackRequest did not save Location correctly", trackRequest.getLocation(), location);
        Assert.assertEquals("TrackRequest did not save Distance correctly", trackRequest.getDistance(), distance);
        Assert.assertEquals("TrackRequest did not save Dynamic correctly", trackRequest.getDynamic(), dynamic);
        Assert.assertNotNull("TrackRequest did not save Request number correctly", trackRequest.getRequestNumber());
        Assert.assertEquals("TrackRequest did not save Tag correctly", trackRequest.getTag(), tag);
    }
}
