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

package com.dp16.runamicghent.RunData;

import com.google.android.gms.maps.model.LatLng;

/**
 * This class holds one point in a route.
 * This point contains a location and a RunDirection.
 * Created by hendrikdepauw on 31/03/2017.
 */

public class RunRoutePoint {
    private LatLng location;
    private RunDirection direction;

    public RunRoutePoint(LatLng location, RunDirection direction) {
        setLocation(location);
        setDirection(direction);
    }

    public RunRoutePoint(double lat, double lon, RunDirection direction) {
        setLocation(lat, lon);
        setDirection(direction);
    }

    public RunRoutePoint(LatLng location, String direction) {
        setLocation(location);
        setDirection(direction);
    }

    public RunRoutePoint(double lat, double lon, String direction) {
        setLocation(lat, lon);
        setDirection(direction);
    }

    /*
    All possible getter and setter methods.
     */

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public void setLocation(double lat, double lon) {
        this.location = new LatLng(lat, lon);
    }

    public RunDirection getDirection() {
        return direction;
    }

    public void setDirection(RunDirection direction) {
        this.direction = direction;
    }

    public void setDirection(String direction) {
        this.direction = new RunDirection(direction);
    }

    /**
     * Checks if two RunRoutePoint objects are the same. Mostly used in testing.
     *
     * @param other Object to compare this object to.
     * @return True if other object is the same (value wise), false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof RunRoutePoint)) {
            return false;
        }

        // Other object is a RunRoutePoint and is not null.
        RunRoutePoint otherRunRoutePoint = (RunRoutePoint) other;
        return this.getLocation().equals(otherRunRoutePoint.getLocation()) && this.getDirection().equals(otherRunRoutePoint.getDirection());
    }

    /**
     * Fix for SonarQube
     * According to the Java Language Specification, there is a contract between equals(Object) and hashCode():
     * If two objects are equal according to the equals(Object) method, then calling the hashCode method on each of the two objects must produce the same integer result.
     * It is not required that if two objects are unequal according to the equals(java.lang.Object) method, then calling the hashCode method on each of the two objects must produce distinct integer results.
     */
    @Override
    public int hashCode() {
        return 0;
    }
}
