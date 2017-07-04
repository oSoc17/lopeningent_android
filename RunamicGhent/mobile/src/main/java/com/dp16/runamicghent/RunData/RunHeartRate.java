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

import java.io.Serializable;

/**
 * Simple data class with custom toString method that holds a heartrate value.
 * Created by hendrikdepauw on 16/03/2017.
 */

public class RunHeartRate implements Serializable {
    private int heartRate;

    public RunHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public int getHeartRate() {
        return heartRate;
    }

    /**
     * Custom toString function.
     *
     * @return A String representing the heartRate integer. Confirms the default integer to string conversion.
     */
    @Override
    public String toString() {
        return String.valueOf(heartRate);
    }

    public static String getDefaultString() {
        return "0";
    }
}
