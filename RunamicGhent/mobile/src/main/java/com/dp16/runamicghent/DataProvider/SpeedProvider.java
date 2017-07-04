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

import android.location.Location;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.RunData.RunSpeed;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class calculates and publishes a speed value based on location values.
 * It uses a Kalman filter to smooth the values.
 *
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#SPEED}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#RAW_LOCATION}
 * </p>
 *
 * Created by hendrikdepauw on 06/03/2017.
 */

public class SpeedProvider implements EventListener, DataProvider {

    private ExecutorService worker;

    //kalman filter required values
    private double estimateError;
    private double currentEstimate;
    private double previousEstimate;

    public SpeedProvider() {
        //kalman filter initial values
        estimateError = 0.0;
        previousEstimate = 0.0;

        // Make worker thread
        worker = Executors.newSingleThreadExecutor();
    }

    @Override
    public void start() {
        EventBroker.getInstance().addEventListener(Constants.EventTypes.RAW_LOCATION, this);
    }

    @Override
    public void stop() {
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.RAW_LOCATION, this);
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
        Runnable task = new Worker((Location) message);
        worker.submit(task);
    }

    private class Worker implements Runnable, EventPublisher {
        private final Location location;

        public Worker(Location location) {
            this.location = location;
        }

        /**
         * This method applies the kalman filter
         * It uses a defaultEstimateError of 2, which is arbitrary.
         * It makes sure the estimate keeps adapting to the changing speed.
         */
        @Override
        public void run() {
            double defaultEstimateError = 2.0;
            double measurementError = location.getAccuracy();
            double gain = estimateError / (estimateError + measurementError);

            //Calculate the new estimate and estimate error
            currentEstimate = previousEstimate + gain * (location.getSpeed() - previousEstimate);
            estimateError = (1 - gain) * estimateError + defaultEstimateError;

            //Update the previous estimate
            previousEstimate = currentEstimate;

            //publish the speed event
            EventBroker.getInstance().addEvent(Constants.EventTypes.SPEED, new RunSpeed(currentEstimate), this);
        }
    }
}

