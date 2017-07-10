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

/**
 * This class represents the amount of seconds a user is running.
 * Created by hendrikdepauw on 08/03/2017.
 */

public class RunDuration implements Serializable {
    private int secondsPassed;

    /**
     * Constructor. Initializes the amount of seconds to zero.
     */
    public RunDuration() {
        secondsPassed = 0;
    }

    public RunDuration(int secondsPassed) {
        this.secondsPassed = secondsPassed;
    }

    /**
     * This method increases the amount of seconds by one. Should be called
     * by the TimingProvider every time a second has passed.
     */
    public void addSecond() {
        secondsPassed++;
    }

    public void add(RunDuration runDuration) {
        secondsPassed += runDuration.getSecondsPassed();
    }

    public int getSecondsPassed() {
        return secondsPassed;
    }

    @Override
    public String toString() {
        return toString(secondsPassed);
    }

    public String toAudioString(){
        return toAudioString(secondsPassed);
    }

    /**
     * Static method that converts an amount of seconds to a string.
     * If the amount of time is shorter than 1h, it is displayed as m:ss.
     * Seconds is always 2 numbers, a zero might be padded in front.
     * In case it is longer than 1h, time is displayed as h:mm:ss.
     * The amount of hours is not padded.
     *
     * @param secondsPassed Amount of seconds to convert
     * @return String with time
     */
    public static String toString(int secondsPassed) {
        int hours = secondsPassed / 3600;
        int minutes = (secondsPassed % 3600) / 60;
        int seconds = secondsPassed % 60;

        String returnString = "";

        if (hours != 0) {
            returnString = returnString.concat(hours + ":");

            if (minutes < 10) {
                returnString = returnString.concat("0" + minutes + ":");
            } else {
                returnString = returnString.concat(minutes + ":");
            }
        } else {
            returnString = returnString.concat(minutes + ":");
        }

        if (seconds < 10) {
            returnString = returnString.concat("0" + seconds);
        } else {
            returnString = returnString.concat(Integer.toString(seconds));
        }

        return returnString;
    }

    /**
     * Static method that converts an amount of seconds to a String, ready for TextToSpeech.
     * Hours are not added to the string if zero.
     * Minutes and seconds are always added to the string, also if zero.
     * (non) plural is taken into account.
     *
     * @param secondsPassed Amount of seconds to be converted to a string
     * @return String ready for TextToSpeech
     */
    public static String toAudioString(int secondsPassed){
        int hours = secondsPassed / 3600;
        int minutes = (secondsPassed % 3600) / 60;
        int seconds = secondsPassed % 60;

        String returnString = "";

        if (hours == 1) {
            returnString = returnString.concat(hours + GuiController.getInstance().getContext().getString(R.string.audio_hour));
        } else if (hours > 1) {
            returnString = returnString.concat(hours + GuiController.getInstance().getContext().getString(R.string.audio_hours));
        }

        if (minutes == 1) {
            returnString = returnString.concat(minutes + GuiController.getInstance().getContext().getString(R.string.audio_minute));
        } else {
            returnString = returnString.concat(minutes + GuiController.getInstance().getContext().getString(R.string.audio_minutes));
        }

        if (seconds == 1) {
            returnString = returnString.concat(seconds + GuiController.getInstance().getContext().getString(R.string.audio_second));
        } else {
            returnString = returnString.concat(seconds + GuiController.getInstance().getContext().getString(R.string.audio_seconds));
        }

        return returnString;
    }

    public static String getDefaultString() {
        return "0:00";
    }
}
