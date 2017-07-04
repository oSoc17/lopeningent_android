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

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.dp16.runamicghent.RunData.RunSpeed;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for RunData.RunSpeed.
 * Created by Nick on 25-3-2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RunSpeedTests {
    private static final double doubleDelta = 0.0001;
    private static final double doubleDeltaInToString = 0.01;

    @Test
    public void runSpeed_noPace_doesNotChangeData() {
        double speed = 456.54;
        RunSpeed runSpeed = new RunSpeed(speed);
        assertEquals("RunSpeed changes the data received", speed, runSpeed.getSpeed(), doubleDelta);
    }

    @Test
    public void runSpeed_noPaceToString_confirmsInterface() {
        double speed = 4574.4587;
        RunSpeed runSpeed = new RunSpeed(speed);
        // we only check if a delimiter character is found because this delimiter can change with localization
        String pattern = "(\\d+).(\\d*) km/h";
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(runSpeed.toString(RuntimeEnvironment.application));
        if (matcher.find()) {
            String receivedSpeedString = matcher.group(1) + "." + matcher.group(2);
            double receivedSpeed = Double.parseDouble(receivedSpeedString);
            assertEquals("RunSpeed string formatting does not confirm the interface description", speed * 3.6, receivedSpeed, doubleDeltaInToString);
        } else {
            fail("RunSpeed string formatting does not confirm the interface description: does not follow regex");
        }
    }

    @Test
    public void runSpeed_paceToString_confirmsInterface() {
        Application application = RuntimeEnvironment.application;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        sharedPreferences.edit().putString("pref_key_speed_pace", "1").commit();

        double speed = 786454.1;
        RunSpeed runSpeed = new RunSpeed(speed);
        String pattern = ".* min/km";
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(runSpeed.toString(RuntimeEnvironment.application));
        if (!matcher.find()) {
            fail("RunSpeed string formatting does not confirm the interface description: does not follow regex");
        }
    }

    @Test
    public void runSpeed_paceToString_noSpeed_confirmsInterface() {
        Application application = RuntimeEnvironment.application;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        sharedPreferences.edit().putString("pref_key_speed_pace", "1").commit();

        double speed = 0;
        RunSpeed runSpeed = new RunSpeed(speed);
        String pattern = ".* min/km";
        Pattern regexPattern = Pattern.compile(pattern);
        Matcher matcher = regexPattern.matcher(runSpeed.toString(RuntimeEnvironment.application));
        if (!matcher.find()) {
            fail("RunSpeed string formatting does not confirm the interface description: does not follow regex");
        }
    }

    @Test
    public void runSpeed_getDefaultString_correctFormat(){
        Application application = RuntimeEnvironment.application;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);

        sharedPreferences.edit().putString("pref_key_speed_pace", "0").commit();
        Assert.assertEquals("RunSpeed does not return correct defaulString in km/h", RunSpeed.getDefaultString(RuntimeEnvironment.application), "0.00 km/h");

        sharedPreferences.edit().putString("pref_key_speed_pace", "1").commit();
        Assert.assertEquals("RunSpeed does not return correct defaulString in min/km", RunSpeed.getDefaultString(RuntimeEnvironment.application), "0:00 min/km");
    }
}
