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

package com.dp16.runamicghent.StatTracker;

import com.dp16.runamicghent.RunData.RunDistance;
import com.dp16.runamicghent.RunData.RunDuration;
import com.dp16.runamicghent.RunData.RunHeartRate;
import com.dp16.runamicghent.RunData.RunSpeed;
import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Object that holds all statistics from a run.
 * Most statistics are wrapped in a Datapoint.
 * Created by hendrikdepauw on 07/03/2017.
 */

public class RunningStatistics implements Serializable {
    private List<Datapoint<LatLngSerializable>> location;
    private List<Datapoint<RunSpeed>> speed;
    private List<Datapoint<RunHeartRate>> heartrate;
    private List<Datapoint<RunDistance>> distance;
    private long startTime;
    private RunDuration runDuration;
    private Double rating;

    public RunningStatistics() {
        //Store the time the run is started
        startTime = System.currentTimeMillis();

        //Initialize the lists to store data
        location = new ArrayList<>();
        speed = new ArrayList<>();
        heartrate = new ArrayList<>();
        distance = new ArrayList<>();
    }

    /**
     * Add a location
     *
     * @param location location to be added
     */
    public void addLocation(LatLng location) {
        this.location.add(new Datapoint(new LatLngSerializable(location), startTime));
    }

    /**
     * Add a speed value
     *
     * @param speed speed to be added
     */
    public void addSpeed(RunSpeed speed) {
        this.speed.add(new Datapoint(speed, startTime));
    }

    /**
     * Add a heart rate value
     *
     * @param heartrate heart rate to be added
     */
    public void addHeartrate(RunHeartRate heartrate) {
        this.heartrate.add(new Datapoint(heartrate, startTime));
    }

    /**
     * Add a distance value
     *
     * @param distance distance to be added
     */
    public void addDistance(RunDistance distance) {
        this.distance.add(new Datapoint(distance, startTime));
    }

    /**
     * Store the duration of the run
     *
     * @param runDuration duration to be added
     */
    public void addRunDuration(RunDuration runDuration) {
        this.runDuration = runDuration;
    }

    /**
     * Store the rating of the run
     *
     * @param rating rating to be added;
     */
    public void addRating(double rating) {
        this.rating = rating;
    }

    /**
     * Get the rating associated with this run
     *
     * @return rating of the run, -1 if no rating available
     */
    public double getRating() {
        if (rating != null) {
            return rating;
        }

        return -1;
    }

    /**
     * This method retrieves the location at the latest time before @param time.
     * In case there are no locations to return, null is returned.
     *
     * @param time in milliseconds since start of run
     * @return LatLng object, null if no location found
     */
    public LatLng getLocation(long time) {
        int index = location.size() - 1;
        if (index >= 0) {
            while (index > 0 && location.get(index).getTime() > time) {
                index--;
            }
            return location.get(index).getValue().getLatLng();
        }
        return null;
    }

    /**
     * This method retrieves the complete route from the run.
     *
     * @return ArrayList of LatLng objects that represents the route.
     * The list is empty if no locations were present.
     */
    public ArrayList<LatLng> getRoute() {
        ArrayList<LatLng> route = new ArrayList<>();

        for (int i = 0; i < location.size(); i++) {
            route.add(location.get(i).getValue().getLatLng());
        }

        return route;
    }

    /**
     * This method retrieves the speed at the latest time before @param time.
     *
     * @param time in milliseconds since start of run
     * @return RunSpeed object, contains 0 if no speed found.
     */
    public RunSpeed getSpeed(long time) {
        RunSpeed toReturn;
        int index = speed.size() - 1;

        while (index > 0 && speed.get(index).getTime() > time) {
            index--;
        }

        if (index >= 0) {
            toReturn = speed.get(index).getValue();
        } else {
            toReturn = new RunSpeed(0);
        }

        return toReturn;
    }

    /**
     * This method calculates the average speed of the user.
     * Zero order hold is used.
     *
     * @return RunSpeed object containing average speed
     */
    public RunSpeed getAverageSpeed() {
        long totalMillis = speed.isEmpty() ? 0 : (speed.get(speed.size() - 1)).getTime();
        long previousTime = 0;
        int totalWeightedSpeed = 0;
        RunSpeed toReturn;

        for (int i = 0; i < speed.size(); i++) {
            totalWeightedSpeed += (speed.get(i).getTime() - previousTime) * speed.get(i).getValue().getSpeed();
            previousTime = speed.get(i).getTime();
        }

        if (totalMillis > 0) {
            toReturn = new RunSpeed(((double) totalWeightedSpeed) / totalMillis);
        } else {
            toReturn = new RunSpeed(0);
        }

        return toReturn;
    }

    /**
     * This method retrieves the heart rate at the latest time before @param time.
     *
     * @param time in milliseconds since start of run
     * @return RunHeartRate object, contains 0 if no heart rate found
     */
    public RunHeartRate getHeartrate(long time) {
        RunHeartRate toReturn;
        int index = heartrate.size() - 1;

        while (index > 0 && heartrate.get(index).getTime() > time) {
            index--;
        }

        if (index >= 0) {
            toReturn = heartrate.get(index).getValue();
        } else {
            toReturn = new RunHeartRate(0);
        }

        return toReturn;
    }

    /**
     * This method calculates the average heart rate of the user.
     * Zero order hold is used.
     *
     * @return RunSpeed object containing average speed
     */
    public RunHeartRate getAverageHeartRate() {
        long totalMillis = heartrate.isEmpty() ? 0 : (heartrate.get(heartrate.size() - 1)).getTime();
        long previousTime = 0;
        long totalWeightedHeartRate = 0;
        RunHeartRate toReturn;

        for (int i = 0; i < heartrate.size(); i++) {
            totalWeightedHeartRate += (heartrate.get(i).getTime() - previousTime) * heartrate.get(i).getValue().getHeartRate();
            previousTime = heartrate.get(i).getTime();
        }

        if (totalMillis > 0) {
            toReturn = new RunHeartRate((int) (totalWeightedHeartRate / totalMillis));
        } else {
            toReturn = new RunHeartRate(0);
        }

        return toReturn;
    }

    /**
     * This method retrieves the distance at the latest time before @param time.
     *
     * @param time in milliseconds since start of run
     * @return RunDistance object, contains 0 if no distance found
     */
    public RunDistance getDistance(long time) {
        RunDistance toReturn;
        int index = distance.size() - 1;

        while (index > 0 && distance.get(index).getTime() > time) {
            index--;
        }

        if (index >= 0) {
            toReturn = distance.get(index).getValue();
        } else {
            toReturn = new RunDistance(0);
        }

        return toReturn;
    }

    /**
     * This method retrieves the total distance of the run, being the
     * last RunDistance object of the run.
     *
     * @return RunDistance object, 0 if no distance found
     */
    public RunDistance getTotalDistance() {
        RunDistance toReturn;

        if (!distance.isEmpty()) {
            toReturn = distance.get(distance.size() - 1).getValue();
        } else {
            toReturn = new RunDistance(0);
        }

        return toReturn;
    }

    /**
     * @return RunDuration object with total duration of the run
     */
    public RunDuration getRunDuration() {
        return runDuration;
    }

    /**
     * @return long with the startTime of the run in milliseconds since 1970
     */
    public long getStartTimeMillis() {
        return startTime;
    }

    /**
     * Reset the starttime of the run. Should be called when resuming the run.
     * This makes sure the run seems like one smooth run without breaks
     */
    public void includePause(long pauseLengthMillis) {
        startTime += pauseLengthMillis;
    }

    /**
     * This method formats the startime of the run to a readable string.
     * Format of string: DayOfTheWeek, DayOfTheMonth MonthName Year
     *
     * @return String containing starttime
     */
    public String getStartTimeDate() {
        DateFormat formatter = new SimpleDateFormat("EEEE, d MMMM yyyy");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);

        return formatter.format(calendar.getTime());
    }

    /**
     * Wrapper class for datapoints in the arraylist.
     * Contains a RunData object T with its time relative to start of the run.
     */
    private class Datapoint<T extends Serializable> implements Serializable {
        private long time;
        private T value;

        public Datapoint(T value, long startTime) {
            this.time = System.currentTimeMillis() - startTime;
            this.value = value;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }

    /**
     * LatLng is not serializable. This class fixes that.
     */
    public class LatLngSerializable implements Serializable {
        private double latitude;
        private double longtitude;

        public LatLngSerializable(LatLng latlng) {
            latitude = latlng.latitude;
            longtitude = latlng.longitude;
        }

        public LatLng getLatLng() {
            return new LatLng(latitude, longtitude);
        }

    }
}
