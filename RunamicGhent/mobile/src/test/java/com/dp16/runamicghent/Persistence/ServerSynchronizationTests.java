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
import com.dp16.runamicghent.RunData.RunDuration;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for persistence.ServerSynchronization
 * Warning: these tests need an internet connection to the database.
 * Created by Nick on 12-4-2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ServerSynchronizationTests {
    private LocalStorage localStorage;
    private ServerStorage serverStorage;
    private ServerSynchronization serverSynchronization;
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

        // storage components init
        localStorage = new LocalStorage(application);
        serverStorage = new ServerStorage(clientToken);
        serverStorage.setDatabaseToUse(testDatabaseName);

        // unit under test init
        serverSynchronization = new ServerSynchronization(localStorage, serverStorage, new ScheduledThreadPoolExecutor(1));
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
    public void synchronize_emptyInput_reportsSuccess() {
        assertTrue("ServerSynchronization.synchronize() did not report success on empty local and server storage", serverSynchronization.synchronize());
    }

    @Test
    public void synchronize_emptyInput_leavesLocalAndServerStorageEmpty() {
        serverSynchronization.synchronize();

        // check local empty
        checkAggregateEmpty(localStorage);
        checkRunningEmpty(localStorage);
        checkGhostEmpty(localStorage);

        // check server empty
        serverStorage.connect();
        checkAggregateEmpty(serverStorage);
        checkRunningEmpty(serverStorage);
        checkGhostEmpty(serverStorage);
        serverStorage.disconnect();
    }

    @Test
    public void synchronize_localFile_isUploaded() {
        // variables
        long time = System.currentTimeMillis();
        AggregateRunningStatistics aggregateRunningStatistics = new AggregateRunningStatistics();
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);
        aggregateRunningStatistics.handleRunningStatistics(runningStatistics);

        // save local files
        localStorage.saveAggregateRunningStatistics(aggregateRunningStatistics, time);
        localStorage.saveRunningStatistics(runningStatistics, time);

        // do sync
        serverSynchronization.synchronize();

        // check local files still there and unchanged
        checkFile(localStorage, aggregateRunningStatistics);
        checkFile(localStorage, runningStatistics);

        // check server files are present and contain same content
        serverStorage.connect();
        checkFile(serverStorage, aggregateRunningStatistics);
        checkFile(serverStorage, runningStatistics);
        serverStorage.disconnect();
    }

    @Test
    public void synchronize_serverFile_isDownloaded() {
        // variables
        long time = System.currentTimeMillis();
        AggregateRunningStatistics aggregateRunningStatistics = new AggregateRunningStatistics();
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);
        aggregateRunningStatistics.handleRunningStatistics(runningStatistics);

        // save server files
        serverStorage.connect();
        serverStorage.saveAggregateRunningStatistics(aggregateRunningStatistics, time);
        serverStorage.saveRunningStatistics(runningStatistics, time);
        serverStorage.disconnect();

        // do sync
        serverSynchronization.synchronize();

        // check server files
        serverStorage.connect();
        checkFile(serverStorage, aggregateRunningStatistics);
        checkFile(serverStorage, runningStatistics);
        serverStorage.disconnect();

        // check local files
        checkFile(localStorage, aggregateRunningStatistics);
        checkFile(localStorage, runningStatistics);
    }

    @Test
    public void saveLocal_sync_deleteLocal_sync_serverBecomesGhost() {
        // variables
        long time = System.currentTimeMillis();
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);

        // save local files
        localStorage.saveRunningStatistics(runningStatistics, time);

        // do sync
        serverSynchronization.synchronize();

        // delete local: remove runningstatistics, save ghost
        String filename = String.valueOf(runningStatistics.getStartTimeMillis());
        localStorage.deleteRunningStatistics(filename);
        localStorage.saveRunningStatisticsGhost(filename);

        // do sync
        serverSynchronization.synchronize();

        // check local unchanged
        checkAggregateEmpty(localStorage);
        checkRunningEmpty(localStorage);
        checkFile(localStorage, filename);

        // check server: no runningstatistcs, one ghost
        serverStorage.connect();
        checkAggregateEmpty(serverStorage);
        checkRunningEmpty(serverStorage);
        checkFile(serverStorage, filename);
        serverStorage.disconnect();
    }

    @Test
    public void save2Local_sync_deleteOneLocal_sync_serverOneRealOneGhost() {
        // variables
        long time = System.currentTimeMillis();
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);

        try {
            // We need this sleep because a RunningStatistics object takes the current time (ms) as starttime of the run.
            // That starttime is then used as filename. This causes no conflicts in real use as the object is created on button press.
            // However, during testing...
            Thread.sleep(2);
        } catch (InterruptedException e) {
            // do nothing
        }

        RunningStatistics runningStatistics1V2 = new RunningStatistics();
        runningStatistics1V2.addRating(5);
        RunDuration runDurationV2 = new RunDuration();
        runDurationV2.addSecond();
        runDurationV2.addSecond();
        runningStatistics1V2.addRunDuration(runDurationV2);

        // save local files
        localStorage.saveRunningStatistics(runningStatistics, time);
        localStorage.saveRunningStatistics(runningStatistics1V2, time);

        // do sync
        serverSynchronization.synchronize();

        // delete local: remove runningstatistics, save ghost
        String filename = String.valueOf(runningStatistics.getStartTimeMillis());
        localStorage.deleteRunningStatistics(filename);
        localStorage.saveRunningStatisticsGhost(filename);

        // do sync
        serverSynchronization.synchronize();

        // check local unchanged
        checkAggregateEmpty(localStorage);
        checkFile(localStorage, runningStatistics1V2);
        checkNotFile(localStorage, runningStatistics);
        checkFile(localStorage, filename);

        // check server: one runningstatistcs, one ghost
        serverStorage.connect();
        checkAggregateEmpty(serverStorage);
        checkFile(serverStorage, runningStatistics1V2);
        checkNotFile(serverStorage, runningStatistics);
        checkFile(serverStorage, filename);
        serverStorage.disconnect();
    }

    @Test
    public void saveLocal_sync_deleteServer_sync_localDeleted() {
        // variables
        long time = System.currentTimeMillis();
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);

        // save local files
        localStorage.saveRunningStatistics(runningStatistics, time);

        // do sync
        serverSynchronization.synchronize();

        // delete server: remove runningstatistics, save ghost
        String filename = String.valueOf(runningStatistics.getStartTimeMillis());
        serverStorage.connect();
        serverStorage.deleteRunningStatistics(filename);
        serverStorage.saveRunningStatisticsGhost(filename);
        serverStorage.disconnect();

        // do sync
        serverSynchronization.synchronize();

        // check server unchanged
        serverStorage.connect();
        checkAggregateEmpty(serverStorage);
        checkRunningEmpty(serverStorage);
        checkFile(serverStorage, filename);
        serverStorage.disconnect();

        // check local: running removed, no ghost
        checkAggregateEmpty(localStorage);
        checkRunningEmpty(localStorage);
        checkGhostEmpty(localStorage);
    }

    @Test
    public void saveLocal_sync_deleteBoth_sync_onlyGhostOnServer() {
        // variables
        long time = System.currentTimeMillis();
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);

        // save local files
        localStorage.saveRunningStatistics(runningStatistics, time);

        // do sync
        serverSynchronization.synchronize();

        // delete local: remove runningstatistics, save ghost
        String filename = String.valueOf(runningStatistics.getStartTimeMillis());
        localStorage.deleteRunningStatistics(filename);
        localStorage.saveRunningStatisticsGhost(filename);

        // delete server: remove runningstatistics, save ghost
        serverStorage.connect();
        serverStorage.deleteRunningStatistics(filename);
        serverStorage.saveRunningStatisticsGhost(filename);
        serverStorage.disconnect();

        // do sync
        serverSynchronization.synchronize();

        // check local: empty
        checkAggregateEmpty(localStorage);
        checkRunningEmpty(localStorage);
        checkGhostEmpty(localStorage);

        // check server: no runningstatistcs, one ghost
        serverStorage.connect();
        checkAggregateEmpty(serverStorage);
        checkRunningEmpty(serverStorage);
        checkFile(serverStorage, filename);
        serverStorage.disconnect();
    }

    @Test
    public void saveLocal_deleteLocal_sync_everythingEmpty() {
        // variables
        long time = System.currentTimeMillis();
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);

        // save local files
        localStorage.saveRunningStatistics(runningStatistics, time);

        // delete local: remove runningstatistics, save ghost
        String filename = String.valueOf(runningStatistics.getStartTimeMillis());
        localStorage.deleteRunningStatistics(filename);
        localStorage.saveRunningStatisticsGhost(filename);

        // do sync
        serverSynchronization.synchronize();

        // check local empty
        checkAggregateEmpty(localStorage);
        checkRunningEmpty(localStorage);
        checkGhostEmpty(localStorage);

        // check server emtpy
        serverStorage.connect();
        checkAggregateEmpty(serverStorage);
        checkRunningEmpty(serverStorage);
        checkGhostEmpty(serverStorage);
        serverStorage.disconnect();
    }

    @Test
    public void saveServer_deleteServer_sync_onlyGhostServer() {
        // variables
        long time = System.currentTimeMillis();
        RunningStatistics runningStatistics = new RunningStatistics();
        runningStatistics.addRating(1);
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);

        // save server files
        serverStorage.connect();
        serverStorage.saveRunningStatistics(runningStatistics, time);

        // delete server: remove runningstatistics, save ghost
        String filename = String.valueOf(runningStatistics.getStartTimeMillis());
        serverStorage.deleteRunningStatistics(filename);
        serverStorage.saveRunningStatisticsGhost(filename);
        serverStorage.disconnect();

        // do sync
        serverSynchronization.synchronize();

        // check local empty
        checkAggregateEmpty(localStorage);
        checkRunningEmpty(localStorage);
        checkGhostEmpty(localStorage);

        // check server: only one ghost
        serverStorage.connect();
        checkAggregateEmpty(serverStorage);
        checkRunningEmpty(serverStorage);
        checkFile(serverStorage, filename);
        serverStorage.disconnect();
    }



    static void checkNotFile(StorageComponent component, RunningStatistics runningStatistics) {
        try {
            checkFile(component, runningStatistics);
            fail();
        } catch (AssertionError e) {
            // yay, code reuse
        }
    }

    static void checkNotFile(StorageComponent component, String ghostFilename) {
        try {
            checkFile(component, ghostFilename);
            fail();
        } catch (AssertionError e) {
            /// code reuse, anyone?
        }
    }

    /**
     * Checks if a component contains a file of given kind and if it equals the second argument.
     *
     * @param component                  StorageComponent to check.
     * @param aggregateRunningStatistics Object that should be present.
     */
    static void checkFile(StorageComponent component, AggregateRunningStatistics aggregateRunningStatistics) {
        // get item
        AggregateRunningStatistics returnedAggrStats = component.getAggregateRunningStatistics();
        assertNotNull(returnedAggrStats);

        // check equal
        assertEquals(aggregateRunningStatistics.getNumberOfRuns(), returnedAggrStats.getNumberOfRuns());
        assertEquals(aggregateRunningStatistics.getAverageDuration().getSecondsPassed(), returnedAggrStats.getAverageDuration().getSecondsPassed());
        assertEquals(aggregateRunningStatistics.getAverageDistance().getDistance(), returnedAggrStats.getAverageDistance().getDistance());
        assertEquals(aggregateRunningStatistics.getAverageRunSpeed().getSpeed(), returnedAggrStats.getAverageRunSpeed().getSpeed(), 0.01);
    }

    /**
     * Checks if a component contains a file of given kind and if it equals the second argument.
     *
     * @param component         StorageComponent to check.
     * @param runningStatistics Object that should be present.
     */
    static void checkFile(StorageComponent component, RunningStatistics runningStatistics) {
        // get list files
        List<String> returnedRunFiles = component.getFilenamesRunningStatistics();
        assertNotNull(returnedRunFiles);
        assertFalse(returnedRunFiles.isEmpty());

        // check wanted file in list
        String filename = String.valueOf(runningStatistics.getStartTimeMillis());
        boolean inList = false;
        for (String i : returnedRunFiles) {
            if (i.equals(filename)) {
                inList = true;
            }
        }
        assertTrue(inList);

        // get item
        RunningStatistics returnedRunningStats = component.getRunningStatisticsFromFilename(filename);
        assertNotNull(returnedRunningStats);

        // check equal
        assertEquals(runningStatistics.getRating(), returnedRunningStats.getRating(), 0.01);
        assertEquals(runningStatistics.getStartTimeMillis(), returnedRunningStats.getStartTimeMillis());
        assertEquals(runningStatistics.getAverageSpeed().getSpeed(), returnedRunningStats.getAverageSpeed().getSpeed(), 0.01);
        assertEquals(runningStatistics.getTotalDistance().getDistance(), returnedRunningStats.getTotalDistance().getDistance());
    }

    /**
     * Checks if a component contains a ghost file and if it's filename equals the second argument.
     *
     * @param component     StorageComponent to check.
     * @param ghostFilename Name of file that should be present.
     */
    static void checkFile(StorageComponent component, String ghostFilename) {
        // get fist files
        List<String> returnedGhostFiles = component.getFilenamesRunningStatisticsGhosts();
        assertNotNull(returnedGhostFiles);
        assertFalse(returnedGhostFiles.isEmpty());

        // check if wanted file in list
        for (String i : returnedGhostFiles) {
            if (i.equals(ghostFilename)) {
                return;
            }
        }

        // we only get here if the filename was not present in the returned items
        fail("Filename not in returned files");
    }

    /**
     * Verifies that a component does not contain AggregateRunningStatistics.
     *
     * @param component Component to check.
     */
    static void checkAggregateEmpty(StorageComponent component) {
        assertNull("AggregateRunningStatistics was not empty", component.getAggregateRunningStatistics());
    }

    /**
     * Verifies that a component does not contain RunningStatistics.
     *
     * @param component Component to check
     */
    static void checkRunningEmpty(StorageComponent component) {
        List<String> returnedFiles = component.getFilenamesRunningStatistics();
        assertNotNull(returnedFiles);
        assertTrue(returnedFiles.isEmpty());
    }

    /**
     * Verifies that a component does not contain RunningStatistics ghost files.
     *
     * @param component Component to check.
     */
    static void checkGhostEmpty(StorageComponent component) {
        List<String> returnedGhostFiles = component.getFilenamesRunningStatisticsGhosts();
        assertNotNull(returnedGhostFiles);
        assertTrue(returnedGhostFiles.isEmpty());
    }

    static void wipeTestDatabase(String testDatabaseName){
        String databaseURI = "mongodb://" + Constants.Storage.MONGOUSER + ":" + Constants.Storage.MONGOPASS + "@" + Constants.Server.ADDRESS + ":" + Constants.Server.MONGOPORT + "/DRIG_unittest";
        MongoClient mongoClient = new MongoClient(new MongoClientURI(databaseURI));
        MongoDatabase mongoDatabase = mongoClient.getDatabase(testDatabaseName);
        MongoCollection<Document> runningCollection = mongoDatabase.getCollection(Constants.Storage.RUNNINGSTATISTICSCOLLECTION);
        MongoCollection<Document> aggrCollection = mongoDatabase.getCollection(Constants.Storage.AGGREGATERUNNINGSTATISTICSCOLLECTION);
        MongoCollection<Document> ghostCollection = mongoDatabase.getCollection(Constants.Storage.RUNNINGSTATISTICSGHOSTCOLLECTION);
        runningCollection.drop();
        aggrCollection.drop();
        ghostCollection.drop();
    }
}
