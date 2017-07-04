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

import com.dp16.runamicghent.RunData.RunHeartRate;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for RunData.RunHeartRate.
 * Created by Nick on 25-3-2017.
 */

public class RunHeartRateTests {
    @Test
    public void runHeartRate_doesNotChangeData() {
        int heartRate = 1574;
        RunHeartRate runHeartRate = new RunHeartRate(heartRate);
        assertEquals("RunHeartRate changes the data is receives", heartRate, runHeartRate.getHeartRate());
    }

    @Test
    public void runHeartRate_toString_formatsCorrectly() {
        int heartRate = 45873;
        RunHeartRate runHeartRate = new RunHeartRate(heartRate);
        assertEquals("RunHeartRate toString formatting does not confirm the interface description", Integer.toString(heartRate), runHeartRate.toString());
    }
}
