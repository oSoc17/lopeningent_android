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
import com.google.android.gms.maps.model.LatLng;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the Activities.Utils.CalculateBearing class.
 * Created by lorenzvanherwaarden on 11/05/2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BearingCalculatorTests {
    double margin = 0.1;

    @Test
    public void bearingCalc_oldBearingWhenSmallDifferenceInDistance(){
        LatLng firstLocation = new LatLng(4.5000000, 51.0000000);
        LatLng secondLocation = new LatLng(4.5000001, 51.0000001);

        Utils.BearingCalculator bearingCalculator = new Utils.BearingCalculator();
        // First time firstLocation is only one added so bearing such be 0.
        assertEquals(0.0, bearingCalculator.calculateBearing(firstLocation), margin);

        // Second time secondLocation is almost on exact spot (so not 8 meters apart) and bearingCalculator won't give a new bearing.
        // As old one was 0.0, it should still return 0.0
        assertEquals(0.0, bearingCalculator.calculateBearing(secondLocation), margin);
    }

    @Test
    public void bearingCalc_testBearingWithPredefinedOne(){
        LatLng firstLocation = new LatLng(4.12345, 51.12345);
        LatLng secondLocation = new LatLng(4.11892839, 51.12345);

        // bearing between locations is -180.0. But we have to take into account the
        // DISCOUNT_FACTOR that the bearingcalculator uses.
        float bearingCheck = (1.0f - Constants.MapSettings.DISCOUNT_FACTOR) * (-180.0f);

        Utils.BearingCalculator bearingCalculator = new Utils.BearingCalculator();
        bearingCalculator.calculateBearing(firstLocation);
        assertEquals(bearingCheck, bearingCalculator.calculateBearing(secondLocation), margin);
    }
}
