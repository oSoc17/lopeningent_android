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

package com.dp16.runamicghent.DataProvider.MockUp;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.DataProvider.DataProvider;
import com.dp16.runamicghent.RunData.RunHeartRate;
import com.dp16.runamicghent.RunData.RunSpeed;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class publishes heart rates in function of the speed.
 * Created by hendrikdepauw on 20/03/2017.
 */

public class HeartBeatProviderMock implements DataProvider, EventListener {
    private ExecutorService worker;
    private static Random random = new Random();

    public HeartBeatProviderMock() {
        worker = Executors.newSingleThreadExecutor();
    }

    @Override
    public void start() {
        EventBroker.getInstance().addEventListener(Constants.EventTypes.SPEED, this);
    }

    @Override
    public void stop() {
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.SPEED, this);
    }

    @Override
    public void resume() {
        start();
    }

    @Override
    public void pause() {
        stop();
    }


    @Override
    public void handleEvent(String eventType, Object message) {
        Runnable task = new HeartBeatProviderMock.Worker((RunSpeed) message);
        worker.submit(task);
    }

    private class Worker implements Runnable, EventPublisher {
        private final RunSpeed runSpeed;

        private Worker(RunSpeed runSpeed) {
            this.runSpeed = runSpeed;
        }

        @Override
        public void run() {
            EventBroker.getInstance()
                    .addEvent(Constants.EventTypes.HEART_RESPONSE, generateHeartRate(runSpeed), this);
        }

        /**
         * Takes a runSpeed object and calculates a heart rate based on that. Gaussian noise is added.
         * The function to convert a speed into a heartrate is a * (base ^ speed) + b.
         * The noise is 0 average with deviation sd.
         *
         * @param runSpeed runspeed object to base calculations on
         * @return heartbeat calculated from speed with noise
         */
        private RunHeartRate generateHeartRate(RunSpeed runSpeed) {
            double a = 20;
            double b = 60;
            double base = 1.1;
            double sd = 4;

            double heartRate = a * Math.pow(base, runSpeed.getSpeed() * 3.6) + b;
            heartRate = heartRate + (random.nextGaussian() * sd);

            return new RunHeartRate((int) heartRate);
        }
    }
}
