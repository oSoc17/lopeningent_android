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

import org.json.JSONObject;

/**
 * This class is used to associate a track response with a response number.
 * Created by lorenzvanherwaarden on 10/04/2017.
 */
public class TrackResponse {
    private JSONObject track;
    private boolean dynamic;
    private int responseNumber;

    public TrackResponse(JSONObject track, boolean dynamic, int responseNumber) {
        this.track = track;
        this.dynamic = dynamic;
        this.responseNumber = responseNumber;
    }

    public JSONObject getTrack() {
        return track;
    }

    public int getResponseNumber() {
        return responseNumber;
    }

    public boolean getDynamic(){
        return dynamic;
    }
}
