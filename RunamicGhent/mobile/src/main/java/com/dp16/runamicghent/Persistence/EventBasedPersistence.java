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

package com.dp16.runamicghent.Persistence;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisherClass;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Public interface for the persistence package. This package is responsible for saving files.
 * The files are saved on the device. When internet connection is available, the local storage is sync'd with the server database.
 * <p>
 * Communication with the persistence component is done via the eventbroker.
 * This class listens for the following event types:
 * <ul>
 * <li>Constants.EventTypes.STORE_RUNNINGSTATISTICS</li>
 * <li>Constants.EventTypes.STORE_AGGREGATESTATISTICS</li>
 * <li>Constants.EventTypes.LOAD_RUNNINGSTATISTICS</li>
 * <li>Constants.EventTypes.LOAD_AGGREGATESTATISTICS</li>
 * <li>Constants.EventTypes.DELETE_RUNNINGSTATISTICS</li>
 * <li>Constants.EventTypes.DELETE_AGGREGATESTATISTICS</li>
 * <li>Constants.EventTypes.SYNC_WITH_DATABASE</li>
 * </ul>
 * and publishes the following event types:
 * <ul>
 * <li>Constants.EventTypes.LOADED_RUNNINGSTATISTICS</li>
 * <li>Constants.EventTypes.LOADED_AGGREGATESTATISTICS</li>
 * </ul>
 * <p>
 * Created by Nick on 9-4-2017.
 */

public class EventBasedPersistence extends EventPublisherClass implements EventListener {
    private ExecutorService worker;
    private EventBroker broker;
    private PersistenceController controller;
    private String tag = "EventBasedPersistence";

    public EventBasedPersistence(Context context) {
        worker = Executors.newSingleThreadExecutor();
        broker = EventBroker.getInstance();
        controller = new PersistenceController(context);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public PersistenceController getController() {
        return controller;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void setController(PersistenceController controller) {
        this.controller = controller;
    }

    /**
     * Subscribes to EventBroker.
     */
    public void start() {
        broker.addEventListener(Constants.EventTypes.STORE_RUNNINGSTATISTICS, this);
        broker.addEventListener(Constants.EventTypes.STORE_AGGREGATESTATISTICS, this);
        broker.addEventListener(Constants.EventTypes.LOAD_RUNNINGSTATISTICS, this);
        broker.addEventListener(Constants.EventTypes.LOAD_AGGREGATESTATISTICS, this);
        broker.addEventListener(Constants.EventTypes.DELETE_RUNNINGSTATISTICS, this);
        broker.addEventListener(Constants.EventTypes.DELETE_AGGREGATESTATISTICS, this);
        broker.addEventListener(Constants.EventTypes.SYNC_WITH_DATABASE, this);
    }

    /**
     * Detaches from EventBroker.
     */
    public void stop() {
        broker.removeEventListener(this);
    }

    @Override
    public void handleEvent(String eventType, Object message) {
        Runnable task = new Worker(eventType, message);
        worker.submit(task);
    }

    private class Worker implements Runnable {
        private String eventType;
        private Object message;

        public Worker(String eventType, Object message) {
            this.eventType = eventType;
            this.message = message;
        }

        @Override
        public void run() {
            // check which eventType it is and pass to controller
            switch (eventType) {
                case Constants.EventTypes.STORE_RUNNINGSTATISTICS:
                    controller.saveRunningStatistics((RunningStatistics) message);
                    break;
                case Constants.EventTypes.STORE_AGGREGATESTATISTICS:
                    controller.saveAggregateStatistics((AggregateRunningStatistics) message);
                    break;
                case Constants.EventTypes.LOAD_RUNNINGSTATISTICS:
                    publishEvent(Constants.EventTypes.LOADED_RUNNINGSTATISTICS, controller.getRunningStatistics());
                    break;
                case Constants.EventTypes.LOAD_AGGREGATESTATISTICS:
                    publishEvent(Constants.EventTypes.LOADED_AGGREGATESTATISTICS, controller.getAggregateRunningStatistics());
                    break;
                case Constants.EventTypes.DELETE_RUNNINGSTATISTICS:
                    controller.deleteRunningStatistics((RunningStatistics) message);
                    break;
                case Constants.EventTypes.DELETE_AGGREGATESTATISTICS:
                    controller.deleteAggregateStatistics();
                    break;
                case Constants.EventTypes.SYNC_WITH_DATABASE:
                    controller.doBackwardsCompatibility();
                    controller.synchronizeWithServer();
                    break;
                default:
                    Log.e(tag, "EventBasedPersistence received event for which not subscribed: " + eventType);
            }
        }
    }

}
