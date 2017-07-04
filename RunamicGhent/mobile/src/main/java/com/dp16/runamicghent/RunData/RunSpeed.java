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

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.Serializable;
import java.text.DecimalFormat;

/**
 * Simple data class with custom toString method that holds a speed value.
 * Created by hendrikdepauw on 16/03/2017.
 */

public class RunSpeed implements Serializable {
    private double speed; //in m/s
    private static double zeroCompareError = 0.1;

    public RunSpeed(double runSpeed) {
        this.speed = runSpeed;
    }

    public double getSpeed() {
        return speed;
    }

    /**
     * Custom toString method.
     *
     * @return If pace is not set: speed in the format "x.x km/h". If pace is set: in the format "x.x min/km".
     */
    public String toString(Context context) {
        String speedPaceSetting = "";
        try {
            speedPaceSetting = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_key_speed_pace", "0");
        } catch (NullPointerException npe) {
            Log.e(this.getClass().getSimpleName(), npe.getMessage(), npe);
            speedPaceSetting = "0";
        }

        String speedString;

        if ("0".equals(speedPaceSetting)) {
            DecimalFormat df = new DecimalFormat("#.##");
            speedString = df.format(speed * 3.6f);
            speedString = speedString.concat(" km/h");
        } else {
            speedString = RunDuration.toString((int) speedToPace(speed));
            speedString = speedString.concat(" min/km");
        }

        return speedString;
    }

    public static String getDefaultString(Context context) {
        String speedPaceSetting = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_key_speed_pace", "0");

        if ("0".equals(speedPaceSetting)) {
            return "0.00 km/h";
        } else {
            return "0:00 min/km";
        }

    }

    private static double speedToPace(Double speed) {
        if (Math.abs(speed) > zeroCompareError) {
            return (1.0 / speed) * 1000;
        } else {
            return 0;
        }
    }
}