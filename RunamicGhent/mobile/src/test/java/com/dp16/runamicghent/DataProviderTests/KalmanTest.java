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

import android.location.Location;

import com.dp16.runamicghent.DataProvider.Kalman;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

/**
 * Created by Simon on 2/04/17.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class KalmanTest {

    @Test
    public void kalman_simpleSetOfLocations_EstimateIsInsideLocations(){

        Kalman kalman = new Kalman();
        long accuracy = (long)3.0; //meters
        long speed = (long) 5.0;
        long time = (long) 0.0;

        //define three locations (triangle)
        Location loc1 = new Location("");
        Location loc2 = new Location("");
        Location loc3 = new Location("");
        Location loc4 = new Location("");


        //set coordinates
        loc1.setLatitude(51.046276d);
        loc1.setLongitude(3.723964d);
        loc1.setSpeed(speed);
        loc1.setTime(time);
        loc1.setAccuracy(accuracy);

        loc2.setLatitude(51.046197);
        loc2.setLongitude(3.724233);
        loc2.setSpeed(speed);
        loc2.setTime(5000);
        loc2.setAccuracy(accuracy);

        loc3.setLatitude(51.046305);
        loc3.setLongitude(3.724464);
        loc3.setSpeed(speed);
        loc3.setTime(10000);
        loc3.setAccuracy(accuracy);

        loc4.setLatitude(51.046186);
        loc4.setLongitude(3.724843);
        loc4.setSpeed(speed);
        loc4.setTime(15000);
        loc4.setAccuracy(accuracy);

        // test if first estimate initializes well and returns the same coordinate
        LatLng res = kalman.estimatePosition(loc1);
        assertTrue(compare_loc_latlng(res, loc1));

        // test the second estimate. Estimate should be in between both locations
        res = kalman.estimatePosition(loc2);

        assertTrue("Second estimate",
                res.latitude > Math.min(loc1.getLatitude(), loc2.getLatitude()) &&
                        res.latitude < Math.max(loc1.getLatitude(), loc2.getLatitude()) &&
                        res.longitude > Math.min(loc1.getLongitude(), loc2.getLongitude()) &&
                        res.longitude < Math.max(loc1.getLatitude(), loc2.getLongitude()));

        res = kalman.estimatePosition(loc3);
        res = kalman.estimatePosition(loc4);

        double min_lat = Math.min(loc1.getLatitude(), Math.min(loc2.getLatitude(), Math.min(loc3.getLatitude(), loc4.getLatitude())));
        double max_lat = Math.max(loc1.getLatitude(), Math.max(loc2.getLatitude(), Math.max(loc3.getLatitude(), loc4.getLatitude())));
        double min_lng = Math.min(loc1.getLongitude(), Math.min(loc2.getLongitude(), Math.min(loc3.getLongitude(), loc4.getLongitude())));
        double max_lng = Math.max(loc1.getLatitude(), Math.max(loc2.getLatitude(), Math.max(loc3.getLatitude(), loc4.getLatitude())));

        assertTrue("Fourth estimate",
                res.latitude > min_lat &&
                        res.latitude < max_lat &&
                        res.longitude > min_lng &&
                        res.longitude < max_lng);

    }

    @Ignore
    private boolean compare_loc_latlng(LatLng latlng, Location location){
        return (Math.abs(latlng.longitude - location.getLongitude()) < 0.00000001) && (Math.abs(latlng.latitude - location.getLatitude()) < 0.00000001);
    }

}
