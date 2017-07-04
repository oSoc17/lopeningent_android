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

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Created by hendrikdepauw on 12/04/2017.
 */

public class TrackResponseTests {

    @Test
    public void trackResponse_getters_correctOutput(){
        JSONObject JSONObject = null;
        boolean dynamic = true;
        int RESPONSE_NUMBER = 10;
        try {
            String json = new String("{\"coordinates\": [ { \"lat\": 51.0386722, \"c\": \"none\", \"lon\": 3.730139 }, { \"lat\": 51.0386317, \"c\": \"left\", \"lon\": 3.7301503 }, { \"lat\": 51.038596, \"c\": \"right\", \"lon\": 3.7301377 } ] }");
            JSONObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        TrackResponse trackResponse = new TrackResponse(JSONObject, true, RESPONSE_NUMBER);

        Assert.assertEquals("TrackResponse did not save JSONObject correctly", trackResponse.getTrack(), JSONObject);
        Assert.assertEquals("TrackResponse did not save Dynamic correctly", trackResponse.getDynamic(), dynamic);
        Assert.assertEquals("TrackResponse did not save Response number correctly", trackResponse.getResponseNumber(), RESPONSE_NUMBER);
    }
}
