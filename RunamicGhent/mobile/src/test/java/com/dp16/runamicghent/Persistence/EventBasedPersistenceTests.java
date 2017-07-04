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

import android.app.Application;
import android.preference.PreferenceManager;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisherClass;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dp16.runamicghent.util.ThreadingUtils.waitOneSecUntilAtomicVariableReachesValue;
import static org.junit.Assert.assertEquals;

/**
 * Tests for Persistence.EventBasedPersistence
 * Created by Nick on 30-4-2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class EventBasedPersistenceTests {
    EventBasedPersistence underTest;
    EventBroker broker;
    Application application;
    PersistenceControllerMock mock;
    EventPublisherClass publisher;

    @Before
    public void setup(){
        broker = EventBroker.getInstance();
        publisher = new EventPublisherClass();

        application = RuntimeEnvironment.application;
        String clientToken = "123e4567-e89b-12d3-a456-426655440000";
        PreferenceManager.getDefaultSharedPreferences(application).edit().putString("client token", clientToken).commit();

        mock = new PersistenceControllerMock();

        underTest = new EventBasedPersistence(application);
        underTest.setController(mock);

        broker.start();
        underTest.start();
    }

    @After
    public void cleanUp(){
        underTest.stop();
        broker.stop();
    }

    @Test
    public void saveRun(){
        // publish a store run event
        publisher.publishEvent(Constants.EventTypes.STORE_RUNNINGSTATISTICS, null);

        // wait until it is passed to the mock controller
        waitOneSecUntilAtomicVariableReachesValue("", mock.saveRun, 1);

        // check other methods not called
        assertEquals(0, mock.delRun.get());
        assertEquals(0, mock.getRun.get());
        assertEquals(0, mock.saveAgg.get());
        assertEquals(0, mock.delAgg.get());
        assertEquals(0, mock.getAgg.get());
        assertZero(mock.compa);
        assertZero(mock.sync);
    }

    @Test
    public void deleteRun(){
        publisher.publishEvent(Constants.EventTypes.DELETE_RUNNINGSTATISTICS, null);
        waitOneSecUntilAtomicVariableReachesValue("", mock.delRun, 1);
        assertEquals(0, mock.saveRun.get());
        assertZero(mock.getRun);
        assertZero(mock.saveAgg);
        assertZero(mock.delAgg);
        assertZero(mock.getAgg);
        assertZero(mock.compa);
        assertZero(mock.sync);
    }

    @Test
    public void getRun(){
        // make a listener for the LOADED_RUN event
        final AtomicInteger received = new AtomicInteger(0);
        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                received.incrementAndGet();
            }
        };
        broker.addEventListener(Constants.EventTypes.LOADED_RUNNINGSTATISTICS, listener);

        // publish a 'load run' event
        publisher.publishEvent(Constants.EventTypes.LOAD_RUNNINGSTATISTICS, null);

        // wait until it is passed to the mock controller
        waitOneSecUntilAtomicVariableReachesValue("", mock.getRun, 1);

        // wait until it is received in the listener
        waitOneSecUntilAtomicVariableReachesValue("", received, 1);

        // check other methods not called
        assertZero(mock.saveRun);
        assertZero(mock.delRun);
        assertZero(mock.saveAgg);
        assertZero(mock.delAgg);
        assertZero(mock.getAgg);
        assertZero(mock.compa);
        assertZero(mock.sync);
    }

    @Test
    public void saveAgg(){
        publisher.publishEvent(Constants.EventTypes.STORE_AGGREGATESTATISTICS, null);
        waitOneSecUntilAtomicVariableReachesValue("", mock.saveAgg, 1);
        assertZero(mock.saveRun);
        assertZero(mock.delRun);
        assertZero(mock.getRun);
        assertZero(mock.delAgg);
        assertZero(mock.getAgg);
        assertZero(mock.compa);
        assertZero(mock.sync);
    }

    @Test
    public void delAgg(){
        publisher.publishEvent(Constants.EventTypes.DELETE_AGGREGATESTATISTICS, null);
        waitOneSecUntilAtomicVariableReachesValue("", mock.delAgg, 1);
        assertZero(mock.saveRun);
        assertZero(mock.delRun);
        assertZero(mock.getRun);
        assertZero(mock.getAgg);
        assertZero(mock.saveAgg);
        assertZero(mock.compa);
        assertZero(mock.sync);
    }

    @Test
    public void getAgg(){
        final AtomicInteger received = new AtomicInteger(0);
        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                received.incrementAndGet();
            }
        };
        broker.addEventListener(Constants.EventTypes.LOADED_AGGREGATESTATISTICS, listener);
        publisher.publishEvent(Constants.EventTypes.LOAD_AGGREGATESTATISTICS, null);
        waitOneSecUntilAtomicVariableReachesValue("", mock.getAgg, 1);
        waitOneSecUntilAtomicVariableReachesValue("", received, 1);
        assertZero(mock.saveRun);
        assertZero(mock.delRun);
        assertZero(mock.getRun);
        assertZero(mock.saveAgg);
        assertZero(mock.delAgg);
        assertZero(mock.compa);
        assertZero(mock.sync);
    }

    @Test
    public void sync(){
        publisher.publishEvent(Constants.EventTypes.SYNC_WITH_DATABASE, null);
        waitOneSecUntilAtomicVariableReachesValue("", mock.compa, 1);
        waitOneSecUntilAtomicVariableReachesValue("", mock.sync, 1);
        assertZero(mock.saveRun);
        assertZero(mock.delRun);
        assertZero(mock.getRun);
        assertZero(mock.saveAgg);
        assertZero(mock.delAgg);
        assertZero(mock.getAgg);
    }


    /**
     * Does 'assertEsuals(0, atomicInteger.get());'
     * Is a bit shorter.
     */
    private void assertZero(AtomicInteger atomicInteger){
        assertEquals(0, atomicInteger.get());
    }

    /**
     * Mock class for PeristenceController.
     */
    private class PersistenceControllerMock extends PersistenceController{
        AtomicInteger saveRun = new AtomicInteger(0);
        AtomicInteger delRun = new AtomicInteger(0);
        AtomicInteger getRun = new AtomicInteger(0);
        AtomicInteger saveAgg = new AtomicInteger(0);
        AtomicInteger delAgg = new AtomicInteger(0);
        AtomicInteger getAgg = new AtomicInteger(0);
        AtomicInteger compa = new AtomicInteger(0);
        AtomicInteger sync = new AtomicInteger(0);

        public PersistenceControllerMock(){
            super(application);
        }

        @Override
        void saveRunningStatistics(RunningStatistics runningStatistics) {
            saveRun.incrementAndGet();
        }

        @Override
        boolean deleteRunningStatistics(RunningStatistics runningStatistics) {
            delRun.incrementAndGet();
            return true;
        }

        @Override
        List<RunningStatistics> getRunningStatistics() {
            getRun.incrementAndGet();
            return new ArrayList<>();
        }

        @Override
        void saveAggregateStatistics(AggregateRunningStatistics aggregateRunningStatistics) {
            saveAgg.incrementAndGet();
        }

        @Override
        void deleteAggregateStatistics() {
            delAgg.incrementAndGet();
        }

        @Override
        AggregateRunningStatistics getAggregateRunningStatistics() {
            getAgg.incrementAndGet();
            return new AggregateRunningStatistics();
        }

        @Override
        void doBackwardsCompatibility(){
            compa.incrementAndGet();
        }

        @Override
        void synchronizeWithServer(){
            sync.incrementAndGet();
        }
    }
}





