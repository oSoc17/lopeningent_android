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

import android.util.Log;

import com.dp16.runamicghent.RunData.RunRoute;
import com.dp16.runamicghent.RunData.RunRoutePoint;
import com.google.android.gms.maps.model.LatLng;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

/**
 * Created by hendrikdepauw on 02/04/2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RunRouteTests {
    JSONObject JSONObject;
    ArrayList<RunRoutePoint> testRoute;
    ArrayList<LatLng> testCoordinates;
    ArrayList<RunRoutePoint> testInstructions;
    String tag;

    @Before
    public void init(){
        try {
            String json = new String("{\"tag\": vfibdjk, \"coordinates\": [ { \"lat\": 51.0386722, \"c\": \"none\", \"lon\": 3.730139 }, { \"lat\": 51.0386317, \"c\": \"left\", \"lon\": 3.7301503 }, { \"lat\": 51.038596, \"c\": \"right\", \"lon\": 3.7301377 } ] }");
            JSONObject = new JSONObject(json);
        } catch (JSONException e) {
            Log.e("RunRouteTests", e.getMessage());
        }

        tag = "vfibdjk";

        testRoute = new ArrayList<>();
        testRoute.add(new RunRoutePoint(51.0386722, 3.730139, "none"));
        testRoute.add(new RunRoutePoint(51.0386317, 3.7301503, "left"));
        testRoute.add(new RunRoutePoint(51.038596, 3.7301377, "right"));
        testRoute.add(new RunRoutePoint(51.0386722, 3.730139, "none")); // Add first point again to close circle

        testCoordinates = new ArrayList<>();
        testCoordinates.add(new LatLng(51.0386722, 3.730139));
        testCoordinates.add(new LatLng(51.0386317, 3.7301503));
        testCoordinates.add(new LatLng(51.038596, 3.7301377));
        testCoordinates.add(new LatLng(51.0386722, 3.730139)); // Add first point again to close circle

        testInstructions = new ArrayList<>();
        testInstructions.add(new RunRoutePoint(51.0386317, 3.7301503, "left"));
        testInstructions.add(new RunRoutePoint(51.038596, 3.7301377, "right"));
    }

    @Test
    public void runRoute_objectConstructor_validJSONObject(){
        RunRoute runRoute = new RunRoute(JSONObject);

        Assert.assertEquals("RunRoute handles JSON Object incorrectly", testRoute, runRoute.getRoute());
        Assert.assertEquals("RunRoute handles JSON Object incorrectly", testCoordinates, runRoute.getRouteCoordinates());
    }

    @Test
    public void runRoute_objectConstructor_invalidJSONObject(){
        RunRoute runRoute = new RunRoute(3);

        Assert.assertTrue("RunRoute handles invalid JSON Object incorrectly", runRoute.getRoute().isEmpty());
        Assert.assertTrue("RunRoute handles invalid JSON Object incorrectly", runRoute.getRouteCoordinates().isEmpty());
    }

    @Test
    public void runRoute_ArrayListConstructor_validArrayList(){
        RunRoute runRoute = new RunRoute(testRoute, tag);

        Assert.assertEquals("RunRoute handles RunRoutePoint ArrayList incorrectly", testRoute, runRoute.getRoute());
        Assert.assertEquals("RunRoute handles RunRoutePoint ArrayList incorrectly", testCoordinates, runRoute.getRouteCoordinates());
    }

    @Test
    public void runRoute_getRunRouteInstructions_givesCorrectRunRoutePoints(){
        RunRoute runRoute = new RunRoute(testRoute, tag);

        Assert.assertEquals("RunRoute selects meaningful directions incorrectly", testInstructions, runRoute.getRunRouteInstructions());
    }

    @Test
    public void runRoute_routeLength(){
        // distance (in km) calculated with http://boulter.com/gps/distance/
        float distanceCalculated = 0.016f;

        Assert.assertEquals("RunRoute selects meaningful directions incorrectly", distanceCalculated, ((double) RunRoute.calculateRouteLength(testRoute).getDistance())/1000, 0.002f);
    }

}
