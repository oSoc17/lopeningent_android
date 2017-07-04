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

package com.dp16.runamicghent.utilTests;

import com.dp16.runamicghent.Activities.Utils;
import com.dp16.runamicghent.Constants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the Activities.Utils.RollingAvg class.
 * Created by lorenzvanherwaarden on 18/04/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class RollingAvgTests {
    double margin = 0.1;

    @Test
    public void rollingLatLon_avgZeroWhenInit(){
        double zero = 0.0;
        Utils.RollingAvg rollingAvg = new Utils.RollingAvg(Constants.DynamicRouting.ROLLING_SIZE);
        assertEquals("Average is zero when initialised", zero, rollingAvg.getAverage(), margin);
    }

    @Test
    public void rollingAvg_addWorksCorrectly(){
        int test = 50;
        double control = test/(1.0*Constants.DynamicRouting.ROLLING_SIZE);
        Utils.RollingAvg rollingAvg = new Utils.RollingAvg(Constants.DynamicRouting.ROLLING_SIZE);
        rollingAvg.add(test);
        assertEquals("Adding an int to rollingAvg works correctly: avg is correct", control, rollingAvg.getAverage(), margin);
    }

    @Test
    public void rollingAvg_permutationIndexWorks(){
        Random random = new Random();
        Utils.RollingAvg rollingAvg = new Utils.RollingAvg(Constants.DynamicRouting.ROLLING_SIZE);
        double totalTest= 0.0;

        // Add more LatLng's than the rolling size, to test whether the permutation of the index works correctly
        for (int i = 0; i <= Constants.DynamicRouting.ROLLING_SIZE; i++){
            int test = random.nextInt(10);
            if (i != 0) {
                totalTest += test;
            }
            rollingAvg.add(test);
        }
        assertEquals("Adding more ints than the size still gives correct result: ", totalTest/Constants.DynamicRouting.ROLLING_SIZE, rollingAvg.getAverage(), margin);
    }


}
