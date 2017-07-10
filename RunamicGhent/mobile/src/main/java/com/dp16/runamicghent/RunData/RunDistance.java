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

import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;

import java.io.Serializable;
import java.text.DecimalFormat;

/**
 * Simple data class with custom {@link #toString()} method that holds a distance value.
 * Created by hendrikdepauw on 16/03/2017.
 */

public class RunDistance implements Serializable {
    private int distance; // in meters

    public RunDistance(int distance) {
        this.distance = distance;
    }

    public int getDistance() {
        return distance;
    }

    public void add(RunDistance runDistance) {
        this.distance += runDistance.getDistance();
    }

    public String toString(){
        return toString(this.distance);
    }

    public String toAudioString(){
        return toAudioString(this.distance);
    }

    /**
     * Custom toString implementation
     *
     * @return A String in the "xxx m" format for a distance lower than 1000 meters.
     * A String in the "x.xx km" format for a distance higher or equal to 1000 meter.
     */
    public static String toString(int distance) {
        String returnString;

        if (distance < 1000) {
            returnString = String.valueOf(distance) + " m";
        } else {
            DecimalFormat df = new DecimalFormat("#.##");
            returnString = df.format(((double) distance) / 1000);
            returnString = returnString.concat(" km");
        }

        return returnString;
    }

    /**
     * Static method that converts a distance in meters to a string that is ready for TextToSpeech.
     * This is almost the same as the toString method, but instead of 'm' or 'km' in the end
     * it says 'meters' or 'kilometers'.
     * @param distance distance in meters to be converted to audio string
     * @return String ready for TextToSpeech
     */
    public static String toAudioString(int distance){
        String returnString;

        if (distance < 1000) {
            returnString = String.valueOf(distance) + GuiController.getInstance().getContext().getString(R.string.audio_meters);
        } else {
            DecimalFormat df = new DecimalFormat("#.##");
            returnString = df.format(((double) distance) / 1000);
            returnString = returnString.concat(GuiController.getInstance().getContext().getString(R.string.audio_kilometers));
        }

        return returnString;
    }

    public static String getDefaultString() {
        return "0 m";
    }

}
