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

package com.dp16.runamicghent.StatTracker;

import android.util.Log;

import com.dp16.runamicghent.Constants;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.dp16.eventbroker.EventPublisherClass;

/**
 * Class that handles the AggregateRunningStatistics.
 * This class should always be used to modify or read the AggregateRunningStatistics.
 * Use it by creating this class, then automatically the statistics are loaded from memory
 * using the persistence module.
 * After creation of this object you can either addRunningStatistics or getRunningStatistics.
 * These methods can take some time as they have to wait for the object to be loaded, they
 * do not return immediately. Use an AsyncTask if using this from an Activity or Fragment.
 * <p>
 *     <b>Messages Produced: </b>
 *     {@link com.dp16.runamicghent.Constants.EventTypes#LOAD_AGGREGATESTATISTICS},
 *     {@link com.dp16.runamicghent.Constants.EventTypes#DELETE_AGGREGATESTATISTICS},
 *     {@link com.dp16.runamicghent.Constants.EventTypes#STORE_AGGREGATESTATISTICS}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#LOADED_AGGREGATESTATISTICS}
 * </p>
 *
 * Created by hendrikdepauw on 30/03/2017.
 */

public class AggregateRunningStatisticsHandler implements EventListener, EventPublisher {
    private AggregateRunningStatistics aggregateRunningStatistics;

    //Objects used for synchronization. Have to make sure statistics are loaded before modifying them
    private final Object lock = new Object();
    private volatile Boolean aggregateRunningStatisticsLoaded = false;

    /**
     * Constructor registers with the eventBroker for LOADED_AGGREGATEDSTATISTICS events and
     * send a LOAD_AGGREGATEDSTATISTICS event so they can be loaded from memory.
     */
    public AggregateRunningStatisticsHandler() {
        EventBroker.getInstance().addEventListener(Constants.EventTypes.LOADED_AGGREGATESTATISTICS, this);
        EventBroker.getInstance().addEvent(Constants.EventTypes.LOAD_AGGREGATESTATISTICS, null, this);
    }

    /**
     * This method updates the aggregateRunningStatistics with new RunningStatistics.
     * All logic is done in the AggregateRuningStatistics object.
     * It first waits for the aggregateRunningStatistics to be loaded.
     * After the updates are done, they are saved by DataPersistenceLocal.
     *
     * @param runningStatistics
     */
    public void addRunningStatistics(RunningStatistics runningStatistics) {
        waitOnAggregateRunningStatistics();

        aggregateRunningStatistics.handleRunningStatistics(runningStatistics);

        EventBroker.getInstance().addEvent(Constants.EventTypes.STORE_AGGREGATESTATISTICS, aggregateRunningStatistics, this);
    }

    /**
     * Waits on the aggregateRunningStatistics to be loaded and then returns them.
     *
     * @return AggregateRunningStatistics
     */
    public AggregateRunningStatistics getAggregateRunningStatistics() {
        waitOnAggregateRunningStatistics();

        return aggregateRunningStatistics;
    }

    /**
     * This method waits until the aggregateRunningStatistics are loaded.
     * Uses a boolean that is set to true by the handleEvent method.
     */
    private void waitOnAggregateRunningStatistics() {
        synchronized (lock) {
            while (!aggregateRunningStatisticsLoaded) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Log.e("AggregateHandler", e.getMessage(), e); // Not full name because max 23 characters...
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * This method will fire an event to delete the aggregateRunningStatistics.
     * It can be used from a static context because there will only be one aggregateRunningStatistics for the current user.
     */
    public static void deleteAggregateRunningStatistics() {
        EventPublisherClass publisher = new EventPublisherClass();
        publisher.publishEvent(Constants.EventTypes.DELETE_AGGREGATESTATISTICS, null);
    }

    @Override
    public void handleEvent(String eventType, Object message) {
        //Unsubscribe from the eventBroker
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.LOADED_AGGREGATESTATISTICS, this);

        /*
         * If null is returned by DataRetrievalLocal, this means no AggregateRunningStatistics are stored
         * and a new one should be made.
         */
        if (message != null) {
            aggregateRunningStatistics = (AggregateRunningStatistics) message;
        } else {
            aggregateRunningStatistics = new AggregateRunningStatistics();
            EventBroker.getInstance().addEvent(Constants.EventTypes.STORE_AGGREGATESTATISTICS, aggregateRunningStatistics, this);
        }

        synchronized (lock) {
            //Notify other methods that the statistics are loaded.
            aggregateRunningStatisticsLoaded = true;
            lock.notify();
        }
    }
}
