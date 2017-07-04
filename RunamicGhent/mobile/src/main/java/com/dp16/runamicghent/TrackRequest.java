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

import java.util.Random;

/**
 * This class is used to associate a track request with a request number.
 * Created by lorenzvanherwaarden on 10/04/2017.
 */
public class TrackRequest {
    private LatLng location;
    private RunDistance distance;
    private boolean dynamic;
    private String tag;
    private int requestNumber;

    public TrackRequest(LatLng location, RunDistance distance, boolean dynamic, int requestNumber) {
        this.location = location;
        this.distance = distance;
        this.dynamic = dynamic;
        this.tag = null;
        this.requestNumber = requestNumber;
    }

    public TrackRequest(LatLng location, RunDistance distance, boolean dynamic, String tag, int requestNumber) {
        this(location, distance, dynamic, requestNumber);
        this.tag = tag;
    }

    // Constructor without request number will generate random request number in TrackRequest
    public TrackRequest(LatLng location, RunDistance distance, boolean dynamic) {
        this.location = location;
        this.distance = distance;
        this.dynamic = dynamic;

        Random random = new Random();
        this.requestNumber = random.nextInt();
    }

    public TrackRequest(LatLng location, RunDistance distance, boolean dynamic, String tag) {
        this(location, distance, dynamic);
        this.tag = tag;
    }

    public LatLng getLocation() {
        return location;
    }

    public int getRequestNumber() {
        return requestNumber;
    }

    public boolean getDynamic(){
        return dynamic;
    }

    public RunDistance getDistance(){
        return distance;
    }

    public String getTag(){
        return tag;
    }
}
