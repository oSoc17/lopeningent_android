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

package com.dp16.runamicghent.statTrackerTests;

import com.dp16.runamicghent.RunData.RunDistance;
import com.dp16.runamicghent.RunData.RunDuration;
import com.dp16.runamicghent.RunData.RunHeartRate;
import com.dp16.runamicghent.RunData.RunSpeed;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for StatTracker.RunningStatistics
 * Created by Nick on 26-3-2017.
 */
public class RunningStatisticsTests {
    private static final double doubleDelta = 0.001;
    private RunningStatistics statistics;
    private long endOfTimes;

    @Before
    public void init() {
        statistics = new RunningStatistics();
        endOfTimes = Long.MAX_VALUE;
    }

    @Test
    public void runningStatistics_addAndReadLocation_returnsInputData() {
        LatLng coords = new LatLng(45, 45.4);
        statistics.addLocation(coords);

        LatLng returnedCoords = statistics.getLocation(endOfTimes);
        assertEquals("RunningStatistics changes the value of a location", coords, returnedCoords);
    }

    @Test
    public void runningStatistics_addAndReadSpeed_returnsInputData() {
        RunSpeed speed = new RunSpeed(4587.4);
        statistics.addSpeed(speed);

        RunSpeed returnedSpeed = statistics.getSpeed(endOfTimes);
        assertEquals("RunningStatistics changes the value of RunSpeed", speed, returnedSpeed);
    }

    @Test
    public void runningStatistics_addAndReadHeartRate_returnsInputData() {
        RunHeartRate heartRate = new RunHeartRate(75);
        statistics.addHeartrate(heartRate);

        RunHeartRate returnedHeartRate = statistics.getHeartrate(endOfTimes);
        assertEquals("RunningStatistics changes the value of RunHeartRate", heartRate, returnedHeartRate);
    }

    @Test
    public void runningStatistics_addAndReadDistance_returnsInputData() {
        RunDistance distance = new RunDistance(95);
        statistics.addDistance(distance);

        RunDistance returnedDistance = statistics.getDistance(endOfTimes);
        assertEquals("RunningStatistics changes the value of RunDistance", distance, returnedDistance);
    }

    @Test
    public void runningStatistics_addAndReadDuration_returnsInputData() {
        int realDuration = 4587;
        RunDuration duration = new RunDuration();
        for (int i = 0; i < realDuration; i++) {
            duration.addSecond();
        }
        statistics.addRunDuration(duration);

        RunDuration returnedDuration = statistics.getRunDuration();
        assertEquals("RunningStatistics changes the value of RunDuration", duration, returnedDuration);
    }

    @Test
    public void runningStatistics_addAndReadRating_returnsInputData() {
        double rating = 3.5;
        statistics.addRating(rating);

        double returnedRating = statistics.getRating();
        assertEquals("RunningStatistics changes the value of rating", rating, returnedRating, doubleDelta);
    }

    @Test
    public void runningStatistics_readDataOnEmptyStatistics_returnsDescribedValues() {
        assertEquals("RunningStatistics.getLocation does not return null when not set", null, statistics.getLocation(endOfTimes));
        assertEquals("RunningStatistics.getSpeed does not return 0 when not set", 0, statistics.getSpeed(endOfTimes).getSpeed(), doubleDelta);
        assertEquals("RunningStatistics.getHeartrate does not return 0 when not set", 0, statistics.getHeartrate(endOfTimes).getHeartRate());
        assertEquals("RunningStatistics.getDistance does not return 0 when not set", 0, statistics.getDistance(endOfTimes).getDistance());
        assertEquals("RunningStatistics.getDuration does not return null when not set", null, statistics.getRunDuration());
        assertEquals("RunningStatistics.getRating does not return -1 when not set", -1, statistics.getRating(), doubleDelta);
    }

    @Test
    public void runningStatistics_addDataOnThreePointsInTime_readDataOnPointTwo_returnsExpectedValues() {
        // point one
        LatLng coords1 = new LatLng(10.5, 7.9);
        statistics.addLocation(coords1);
        RunSpeed speed1 = new RunSpeed(5.2);
        statistics.addSpeed(speed1);
        RunHeartRate heartRate1 = new RunHeartRate(45);
        statistics.addHeartrate(heartRate1);
        RunDistance distance1 = new RunDistance(10);
        statistics.addDistance(distance1);

        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            // who needs interruptedExceptions anyway
        }

        // point two
        LatLng coords2 = new LatLng(47.4, 5.9);
        statistics.addLocation(coords2);
        RunSpeed speed2 = new RunSpeed(0);
        statistics.addSpeed(speed2);
        RunHeartRate heartRate2 = new RunHeartRate(180);
        statistics.addHeartrate(heartRate2);
        RunDistance distance2 = new RunDistance(3);
        statistics.addDistance(distance2);

        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            //
        }

        long readTime = System.currentTimeMillis() - statistics.getStartTimeMillis();

        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            //
        }

        // point three
        LatLng coords3 = new LatLng(1, 89);
        statistics.addLocation(coords3);
        RunSpeed speed3 = new RunSpeed(10);
        statistics.addSpeed(speed3);
        RunHeartRate heartRate3 = new RunHeartRate(35);
        statistics.addHeartrate(heartRate3);
        RunDistance distance3 = new RunDistance(300);
        statistics.addDistance(distance3);

        // read data between point 2 and 3
        LatLng returnedCoords = statistics.getLocation(readTime);
        RunSpeed returnedSpeed = statistics.getSpeed(readTime);
        RunHeartRate returnedHeartRate = statistics.getHeartrate(readTime);
        RunDistance returnedDistance = statistics.getDistance(readTime);

        // returned data should be data of point 2
        assertEquals("RunningStatistics.getLocation does not return last time point", coords2, returnedCoords);
        assertEquals("RunningStatistics.getSpeed does not return last time point", speed2, returnedSpeed);
        assertEquals("RunningStatistics.getHeartrate does not return last time point", heartRate2, returnedHeartRate);
        assertEquals("RunningStatistics.getDistance does not return last time point", distance2, returnedDistance);
    }

    @Test
    public void runningStatistics_addLocations_readRoute_returnsExpectedValues() {
        // add a few locations
        LatLng coords1 = new LatLng(50, 50);
        statistics.addLocation(coords1);
        LatLng coords2 = new LatLng(4, 8.5);
        statistics.addLocation(coords2);
        LatLng coords3 = new LatLng(6.1, 8.1);
        statistics.addLocation(coords3);

        // read the route
        List<LatLng> returnedRoute = statistics.getRoute();

        // check if route is a list of the locations
        assertTrue("RunningStatistics.getRoute does not contain a passed location", returnedRoute.contains(coords1));
        assertTrue("RunningStatistics.getRoute does not contain a passed location", returnedRoute.contains(coords2));
        assertTrue("RunningStatistics.getRoute does not contain a passed location", returnedRoute.contains(coords3));
    }

    @Test
    public void runningStatistics_totalDistanceOfThreePoints_confirmsInterface() {
        RunDistance distance1 = new RunDistance(4587);
        statistics.addDistance(distance1);
        RunDistance distance2 = new RunDistance(45987);
        statistics.addDistance(distance2);
        RunDistance distance3 = new RunDistance(45689);
        statistics.addDistance(distance3);

        assertEquals("RunningStatistics.getTotalDistance does not return distance of last RunDistance object", distance3.getDistance(), statistics.getTotalDistance().getDistance());
    }

    @Test
    public void runningStatistics_totalDistanceOfEmpty_returnsZero() {
        assertEquals("RunningStatistics.getTotalDistance does not return 0 for empty statistics", 0, statistics.getTotalDistance().getDistance());
    }

    @Test
    public void runningStatistics_getStartTimeDate_confirmsInterface() {
        Date today = new Date(statistics.getStartTimeMillis());
        String dayOfWeek = (new SimpleDateFormat("EEEE")).format(today);
        String dayOfMonth = (new SimpleDateFormat("d")).format(today);
        String monthName = (new SimpleDateFormat("MMMM")).format(today);
        String year = (new SimpleDateFormat("yyyy")).format(today);

        String pattern = dayOfWeek + ", " + dayOfMonth + " " + monthName + " " + year;
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(statistics.getStartTimeDate());
        if (!matcher.find()) {
            fail("RunningStatistics.getStartTimeDate string formatting does not confirm the interface description: does not follow regex");
        }
    }
}
