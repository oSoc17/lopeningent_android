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

import com.dp16.runamicghent.RunData.RunDuration;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.dp16.runamicghent.Persistence.ServerSynchronizationTests.checkAggregateEmpty;
import static com.dp16.runamicghent.Persistence.ServerSynchronizationTests.checkFile;
import static com.dp16.runamicghent.Persistence.ServerSynchronizationTests.checkGhostEmpty;
import static com.dp16.runamicghent.Persistence.ServerSynchronizationTests.checkRunningEmpty;
import static com.dp16.runamicghent.Persistence.ServerSynchronizationTests.wipeTestDatabase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for persistence.PersistenceController.
 * Warning: these tests need an internet connection with the database.
 * Created by Nick on 13-4-2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PersistenceControllerTests {
    private LocalStorage localStorage;
    private ServerStorage serverStorage;
    private PersistenceController persistenceController;
    private String clientToken;
    private String testDatabaseName;
    private Application application;


    @Before
    public void init() {
        // framework init
        application = RuntimeEnvironment.application;
        clientToken = "123e4567-e89b-12d3-a456-426655440000";
        testDatabaseName = "DRIG_unittest";
        PreferenceManager.getDefaultSharedPreferences(application).edit().putString("client token", clientToken).commit();

        // unit under test init
        persistenceController = new PersistenceController(application);
        localStorage = persistenceController.getLocalStorage();
        serverStorage = persistenceController.getServerStorage();
        serverStorage.setDatabaseToUse(testDatabaseName);
    }

    @After
    public void closeServerConnection() {
        // is not needed 99% of the time, but is safe to call it twice
        serverStorage.disconnect();
    }

    @After
    public void wipeDatabase() {
        wipeTestDatabase(testDatabaseName);
    }

    @Test
    public void saveRunningStatistics_savedOnClientAndServer() {
        // make object
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);

        // call save
        persistenceController.saveRunningStatistics(runningStatistics);

        wait10SecsForTasksToFinish();

        // check file on client
        checkAggregateEmpty(localStorage);
        checkFile(localStorage, runningStatistics);
        checkGhostEmpty(localStorage);

        // check file on server
        serverStorage.connect();
        checkAggregateEmpty(serverStorage);
        checkFile(serverStorage, runningStatistics);
        checkGhostEmpty(serverStorage);
        serverStorage.disconnect();
    }

    @Test
    public void saveRunningStatistics_deleteRunningStatistics_nothingClientGhostServer() {
        // make object
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);

        // call save
        persistenceController.saveRunningStatistics(runningStatistics);

        // call delete
        persistenceController.deleteRunningStatistics(runningStatistics);

        wait10SecsForTasksToFinish();
        persistenceController.getThreadPool();

        // check file on client
        checkAggregateEmpty(localStorage);
        checkRunningEmpty(localStorage);
        checkGhostEmpty(localStorage);

        // check file on server
        serverStorage.connect();
        checkAggregateEmpty(serverStorage);
        checkRunningEmpty(serverStorage);
        // a little finicky here. As the sync with the server is handled asynchronously, the server might contain a ghost (sync before delete), or be empty (sync after delete)
        // so we will just check on 'at most one ghostfile'
        List<String> ghostfiles = serverStorage.getFilenamesRunningStatisticsGhosts();
        assertNotNull(ghostfiles);
        assertTrue(ghostfiles.size() <= 1);
        serverStorage.disconnect();
    }

    @Test
    public void save2RunningStatistics_getRunningStatisticsReturns2Sorted_checkServerReturns2(){
        // make objects
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);

        try {
            Thread.sleep(3); // to separate starttime runningstatistics
        } catch (InterruptedException e){
            // dodo
        }

        RunningStatistics statistics = new RunningStatistics();
        statistics.addRating(4);
        RunDuration duration = new RunDuration();
        duration.addSecond();
        duration.addSecond();
        statistics.addRunDuration(duration);

        // save both
        persistenceController.saveRunningStatistics(runningStatistics);
        persistenceController.saveRunningStatistics(statistics);

        // retrieve
        List<RunningStatistics> returnedRunningStats = persistenceController.getRunningStatistics();
        assertEquals(2, returnedRunningStats.size());

        wait10SecsForTasksToFinish();

        // check sorted
        if(returnedRunningStats.get(0).getStartTimeMillis() < returnedRunningStats.get(1).getStartTimeMillis()){
            // if the first is strictly older than the second: not good
            fail("getRunningStatistics() did not return ");
        }

        // check server
        serverStorage.connect();
        checkFile(serverStorage, runningStatistics);
        checkFile(serverStorage, statistics);
        serverStorage.disconnect();
    }

    @Test
    public void saveAggrStats_savedOnClientAndServer(){
        // make object
        AggregateRunningStatistics aggregateRunningStatistics = new AggregateRunningStatistics();
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);
        aggregateRunningStatistics.handleRunningStatistics(runningStatistics);

        // call save
        persistenceController.saveAggregateStatistics(aggregateRunningStatistics);

        wait10SecsForTasksToFinish();

        // check file on client
        checkFile(localStorage, aggregateRunningStatistics);
        checkRunningEmpty(localStorage);
        checkGhostEmpty(localStorage);

        // check file on server
        serverStorage.connect();
        checkFile(serverStorage, aggregateRunningStatistics);
        checkRunningEmpty(serverStorage);
        checkGhostEmpty(serverStorage);
        serverStorage.disconnect();
    }

    @Test
    public void saveAggrStats_deleteAggrStats_nothingClientServer(){
        // make object
        AggregateRunningStatistics aggregateRunningStatistics = new AggregateRunningStatistics();
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);
        aggregateRunningStatistics.handleRunningStatistics(runningStatistics);

        // call save
        persistenceController.saveAggregateStatistics(aggregateRunningStatistics);

        // call delete
        persistenceController.deleteAggregateStatistics();

        wait10SecsForTasksToFinish();

        AggregateRunningStatistics empty = new AggregateRunningStatistics();

        // check file on client
        checkFile(localStorage, empty);
        checkRunningEmpty(localStorage);
        checkGhostEmpty(localStorage);

        // check file on server
        serverStorage.connect();
        checkFile(serverStorage, empty);
        checkRunningEmpty(serverStorage);
        checkGhostEmpty(serverStorage);
        serverStorage.disconnect();
    }

    @Test
    public void saveAggrStats_retrieveAggrStats_equalObjects(){
        // make object
        AggregateRunningStatistics aggregateRunningStatistics = new AggregateRunningStatistics();
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);
        aggregateRunningStatistics.handleRunningStatistics(runningStatistics);

        // call save
        persistenceController.saveAggregateStatistics(aggregateRunningStatistics);

        // retrieve
        AggregateRunningStatistics returnedStats = persistenceController.getAggregateRunningStatistics();

        // check equal
        assertEquals(aggregateRunningStatistics.getAverageDuration().getSecondsPassed(), returnedStats.getAverageDuration().getSecondsPassed());
        assertEquals(aggregateRunningStatistics.getNumberOfRuns(), returnedStats.getNumberOfRuns());

        wait10SecsForTasksToFinish();
    }

    /**
     * Note: this will <b>kill</b> the PersistenceController as it blocks all further submissions of sync requests. Only use at end of test.
     */
    private void wait10SecsForTasksToFinish() {
        persistenceController.getThreadPool().shutdown();
        try {
            if (!persistenceController.getThreadPool().awaitTermination(10, TimeUnit.SECONDS)) {
                fail("Tasks in PersistenceController threadpool did not terminate within 10 seconds.");
            }
        } catch (InterruptedException e) {
            // nobody cares
        }
    }
}
