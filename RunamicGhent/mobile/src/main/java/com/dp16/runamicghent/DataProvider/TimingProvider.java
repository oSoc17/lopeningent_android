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

package com.dp16.runamicghent.DataProvider;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.RunData.RunDuration;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventPublisher;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This class provides a timer to time a run. Every second a DURATION event will be published.
 * This timing is contained in a runDuration object.
 *
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#DURATION}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> None.
 * </p>
 *
 * Created by hendrikdepauw on 08/03/2017.
 */

public class TimingProvider implements DataProvider {
    private RunDuration runDuration;
    private Timer timer;

    public TimingProvider() {
        runDuration = new RunDuration();
    }

    /**
     * Starts the timer with a delay of 1 second. The timer goes off every second (1000 ms)
     */
    @Override
    public void start() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new IncreaseTime(), 1000, 1000);
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void resume() {
        start();
    }

    @Override
    public void pause() {
        stop();
    }

    public RunDuration getRunDuration() {
        return runDuration;
    }

    /**
     * This class/method is called every second. It increases the timer by one second.
     * Then publishes the DURATION event with the runDuration object.
     */
    private class IncreaseTime extends TimerTask implements EventPublisher {
        @Override
        public void run() {
            runDuration.addSecond();
            EventBroker.getInstance().addEvent(Constants.EventTypes.DURATION, runDuration, this);
        }
    }
}
