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

package com.dp16.runamicghent.DynamicCheckers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.dp16.runamicghent.Activities.Utils;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.RunData.RunAudio;
import com.dp16.runamicghent.RunData.RunHeartRate;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class that checks if the heart rate of the user is too high or too low.
 * If it is, the route can be dynamically shortened or lengthened.
 *
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#AUDIO}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> None.
 * </p>
 *
 * Created by lorenzvanherwaarden on 12/04/2017.
 */
public class HeartRateChecker implements EventListener, EventPublisher {
    private ExecutorService worker;

    // Rolling average for heart rate
    Utils.RollingAvg rollingAvg = new Utils.RollingAvg(Constants.DynamicRouting.ROLLING_SIZE);

    Long timestampBegin = -1L;

    int upperLimit;
    int lowerLimit;

    public HeartRateChecker(Context context) {
        // Make worker thread
        worker = Executors.newSingleThreadExecutor();

        // retrieve upper and lower heart rate limit from the preferences. We need a context for this.
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        lowerLimit = preference.getInt("pref_key_dynamic_heart_rate_lower", Constants.DynamicRouting.HEART_RATE_LOWER);
        upperLimit = preference.getInt("pref_key_dynamic_heart_rate_upper", Constants.DynamicRouting.HEART_RATE_UPPER);
    }

    /**
     * Start HeartRateChecker
     */
    public void start(){
        // Subscribe to EventBroker as listener of Heart Response
        EventBroker.getInstance().addEventListener(Constants.EventTypes.HEART_RESPONSE, this);
    }

    /**
     * Stop HeartRateChecker
     */
    public void stop(){
        // Remove EventListener of Heart Reponse events
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.HEART_RESPONSE, this);
    }

    /**
     * Handles a received event. Starts a new thread to process the user's heart rate.
     * Should not be called too frequently because it will create a lot of threads.
     *
     * @param eventType Should always be HEART_RESPONSE
     * @param message   A RunHeartRate containing the user's heart rate
     */
    @Override
    public void handleEvent(String eventType, Object message) {
        Runnable task = new Worker((RunHeartRate) message);
        worker.submit(task);
    }

    /*
     * This is the worker thread.
     * It will add the new heart rate to the rolling average.
     * If that rolling average is above the upper or below the lower limit for a certain
     * amount of time, an ABNORMAL_HEART_RATE event with a lower or upper tag.
     */
    private class Worker implements Runnable, EventPublisher {
        RunHeartRate runHeartRate;

        public Worker(RunHeartRate runHeartRate) {
            this.runHeartRate = runHeartRate;
        }

        @Override
        public void run() {
            // Add new heart rate to the rolling average
            rollingAvg.add(runHeartRate.getHeartRate());

            // if size amount of heart rates have been added to the rolling average
            if (rollingAvg.isPopulated()) {
                if (rollingAvg.getAverage() > upperLimit) {
                    timestampSetAndEnoughTimePassed(Constants.DynamicRouting.TAG_UPPER);
                } else if (rollingAvg.getAverage() < lowerLimit) {
                    timestampSetAndEnoughTimePassed(Constants.DynamicRouting.TAG_LOWER);
                } else {
                    // reset timestamp if rolling average goes between thresholds
                    timestampBegin = -1L;
                }
            }
        }
    }

    /*
     * Sets timestamp if needed and send ABNORMAL_HEART_RATE event if enough time has elapsed since
     * the rolling average first went under the lower limit or above the upper limit.
     */
    private void timestampSetAndEnoughTimePassed(String limitTag) {
        if (timestampBegin == -1L) {
            timestampBegin = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - timestampBegin > Constants.DynamicRouting.HEART_RATE_MIN_TIME) {
            EventBroker.getInstance().addEvent(Constants.EventTypes.ABNORMAL_HEART_RATE, limitTag, this);

            // Create string to send to AudioPlayer
            String textToSpeek = limitTag.equals(Constants.DynamicRouting.TAG_UPPER) ? GuiController.getInstance().getContext().getString(R.string.audio_heartrate_high) : GuiController.getInstance().getContext().getString(R.string.audio_heartrate_low);
            RunAudio runAudio = new RunAudio(textToSpeek);
            EventBroker.getInstance().addEvent(Constants.EventTypes.AUDIO, runAudio, this);

            /*
             set timestamp such that minimally HEART_RATE_WAIT_TIME will need to elapse before the
             heart rate checker would consider an abnormal heart rate again.
              */
            timestampBegin = System.currentTimeMillis() + Constants.DynamicRouting.HEART_RATE_WAIT_TIME;
        }
    }
}
