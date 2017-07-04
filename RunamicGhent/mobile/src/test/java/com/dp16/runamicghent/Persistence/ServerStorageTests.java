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
import org.robolectric.annotation.Config;

import java.util.List;

import static com.dp16.runamicghent.Persistence.ServerSynchronizationTests.wipeTestDatabase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for persistence.ServerStorage
 * Warning: these tests assume the mongo docker instance is accessible.
 * Created by Nick on 10-4-2017.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ServerStorageTests {
    private ServerStorage serverStorage;
    private String clientToken;
    private String testDatabaseName;
    private long dummyTime;

    @Before
    public void init() {
        dummyTime = 5;
        clientToken = "123e4567-e89b-12d3-a456-426655440000";
        serverStorage = new ServerStorage(clientToken);
        testDatabaseName = "DRIG_unittest";
        serverStorage.setDatabaseToUse(testDatabaseName);
        serverStorage.connect();
    }


    @After
    public void cleanup() {
        serverStorage.disconnect();
    }

    @After
    public void wipeDatabase() {
        wipeTestDatabase(testDatabaseName);
    }

    @Test
    public void ServerStorage_disconnect_allMethodsReturnFailures() {
        serverStorage.connect();
        serverStorage.disconnect();
        assertNull("No reported failure when disconnected", serverStorage.getFilenamesRunningStatistics());
        assertNull("No reported failure when disconnected", serverStorage.getFilenamesRunningStatisticsGhosts());
        assertNull("No reported failure when disconnected", serverStorage.getRunningStatisticsFromFilename("123456"));
        assertFalse("No reported failure when disconnected", serverStorage.saveRunningStatistics(new RunningStatistics(), dummyTime));
        assertFalse("No reported failure when disconnected", serverStorage.saveRunningStatisticsGhost("123546"));
        assertEquals("No reported failure when disconnected", 0, serverStorage.getRunningStatisticsEditTime("123456"));
        assertFalse("No reported failure when disconnected", serverStorage.deleteRunningStatistics("123456"));
        assertFalse("No reported failure when disconnected", serverStorage.deleteRunningStatisticsGhost("123546"));
        assertNull("No reported failure when disconnected", serverStorage.getAggregateRunningStatistics());
        assertFalse("No reported failure when disconnected", serverStorage.saveAggregateRunningStatistics(new AggregateRunningStatistics(), dummyTime));
        assertFalse("No reported failure when disconnected", serverStorage.deleteAggregateRunningStatistics());
        assertEquals("No reported failure when disconnected", 0, serverStorage.getAggregateRunningStatisticsEditTime());
    }

    @Test
    public void getFilenamesRunningStatistics_emptyDatabase_returnsEmptyList() {
        List<String> filenames = serverStorage.getFilenamesRunningStatistics();
        assertNotNull(filenames);
        assertTrue(filenames.isEmpty());
    }

    @Test
    public void saveRunningStatistics_listRunningStatistics_showsOnlyThatDocument() {
        RunningStatistics runningStatistics = new RunningStatistics();
        serverStorage.saveRunningStatistics(runningStatistics, dummyTime);
        List<String> filenames = serverStorage.getFilenamesRunningStatistics();
        assertNotNull("getFilenamesRunningStatistics returned null when saving one valid document", filenames);
        assertEquals("getFilenamesRunningStatistics did not return one filename when saving one valid document", 1, filenames.size());
        assertEquals("Returned filename from getFilenamesRunningStatistics is not RunningStatistics.getStartTimeMillis",
                String.valueOf(runningStatistics.getStartTimeMillis()), filenames.get(0));
    }

    @Test
    public void getFilenamesRunningStatisticsGhost_emptyDatabase_returnsEmptyList() {
        List<String> filenames = serverStorage.getFilenamesRunningStatisticsGhosts();
        assertNotNull(filenames);
        assertTrue(filenames.isEmpty());
    }

    @Test
    public void saveRunningStatisticsGhost_listRunningStatisticsGhost_showsOnlyThatDocument() {
        String filename = "123";
        serverStorage.saveRunningStatisticsGhost(filename);
        List<String> filenames = serverStorage.getFilenamesRunningStatisticsGhosts();
        assertNotNull("getFilenamesRunningStatisticsGhost returned null when saving one valid document", filenames);
        assertEquals("getFilenamesRunningStatisticsGhost did not return one filename when saving one valid document", 1, filenames.size());
        assertEquals("Returned filename from getFilenamesRunningStatisticsGhost is not name of file saved", filename, filenames.get(0));
    }

    @Test
    public void saveRunningStatistics_loadRunningStatistics_equalObject() {
        // make a runningstatistics object
        RunningStatistics runningStatistics = new RunningStatistics();
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);
        runningStatistics.addRating(4.2);

        // save the object
        serverStorage.saveRunningStatistics(runningStatistics, dummyTime);

        // list all saved documents
        List<String> filenames = serverStorage.getFilenamesRunningStatistics();
        assertNotNull("getFilenamesRunningStatistics returned null when saving one valid document", filenames);
        assertEquals("getFilenamesRunningStatistics did not return one filename when saving one valid document", 1, filenames.size());

        // load the document
        RunningStatistics retrievedRunningStatistics = serverStorage.getRunningStatisticsFromFilename(filenames.get(0));

        // check it is the same (no equals on RunningStatistics object, because not implemented)
        assertEquals("Saving and loading a RunningStatistics object did not return an equal object",
                runningStatistics.getRunDuration().getSecondsPassed(), retrievedRunningStatistics.getRunDuration().getSecondsPassed());
        assertEquals("Saving and loading a RunningStatistics object did not return an equal object",
                runningStatistics.getStartTimeMillis(), retrievedRunningStatistics.getStartTimeMillis());
        assertEquals("Saving and loading a RunningStatistics object did not return an equal object",
                runningStatistics.getRating(), retrievedRunningStatistics.getRating(), 0.01);
    }

    @Test
    public void saveRunningStatistics_deleteRunningStatistics_returnsEmptyDatabase() {
        RunningStatistics runningStatistics = new RunningStatistics();
        assertTrue("saveRuningStatistics did not report success", serverStorage.saveRunningStatistics(runningStatistics, dummyTime));

        assertTrue("deleteRunningStatistics did not report success on correct filename", serverStorage.deleteRunningStatistics(String.valueOf(runningStatistics.getStartTimeMillis())));

        List<String> filenames = serverStorage.getFilenamesRunningStatistics();
        assertNotNull("getFilenamesRunningStatistics returned null when saving and deleting one valid document", filenames);
        assertEquals("getFilenamesRunningStatistics did not return empty database when saving and deleting one valid document", 0, filenames.size());
    }

    @Test
    public void deleteRunningStatistics_doesNotDeleteWrongFile() {
        RunningStatistics runningStatistics = new RunningStatistics();
        String wrongFilename = "wrongFilename";
        serverStorage.saveRunningStatistics(runningStatistics, dummyTime);

        assertFalse("deleteRunningStatistics did not report failure on wrong filename", serverStorage.deleteRunningStatistics(wrongFilename));

        List<String> filenames = serverStorage.getFilenamesRunningStatistics();
        assertNotNull("getFilenamesRunningStatistics returned null when saving one valid document", filenames);
        assertEquals("getFilenamesRunningStatistics did not return one filename when saving one valid document and deleting a non-existed", 1, filenames.size());
    }

    @Test
    public void saveRunningStatisticsGhost_deleteRunningStatisticsGhost_returnsEmptyDatabase() {
        String filename = "123456";
        assertTrue("saveRunningStatisticsGhost did not report success", serverStorage.saveRunningStatisticsGhost(filename));

        assertTrue("deleteRunningStatisticsGhost did not report success on correct filename", serverStorage.deleteRunningStatisticsGhost(filename));

        List<String> filenames = serverStorage.getFilenamesRunningStatisticsGhosts();
        assertNotNull("getFilenamesRunningStatisticsGhosts returned null when saving and deleting one valid document", filenames);
        assertEquals("getFilenamesRunningStatisticsGhosts did not return empty database when saving and deleting one valid document", 0, filenames.size());
    }

    @Test
    public void deleteRunningStatisticsGhost_doesNotDeleteWrongFile() {
        String filenameOne = "123456";
        String filenameTwo = "987654";
        serverStorage.saveRunningStatisticsGhost(filenameOne);

        assertFalse("deleteRunningStatisticsGhost did not report failure on wrong filename", serverStorage.deleteRunningStatisticsGhost(filenameTwo));

        List<String> filenames = serverStorage.getFilenamesRunningStatisticsGhosts();
        assertNotNull("getFilenamesRunningStatisticsGhosts returned null when saving one valid document", filenames);
        assertEquals("getFilenamesRunningStatisticsGhosts did not return one filename when saving one valid document and deleting a non-existed", 1, filenames.size());
    }

    @Test
    public void getAggregateRunningStatistics_emptyDatabase_returnsNull() {
        assertNull("getAggregateRunningStatistics on empty database did not return null", serverStorage.getAggregateRunningStatistics());
    }

    @Test
    public void save_load_AggregateRunningStatistics_equalObject() {
        // make aggr. stats object
        AggregateRunningStatistics aggregateRunningStatistics = new AggregateRunningStatistics();
        RunningStatistics runningStatistics = new RunningStatistics();
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);
        runningStatistics.addRating(4.2);
        aggregateRunningStatistics.handleRunningStatistics(runningStatistics);

        // save
        assertTrue("saveAggregateRunningStatistics did not return success", serverStorage.saveAggregateRunningStatistics(aggregateRunningStatistics, dummyTime));

        // load
        AggregateRunningStatistics returnedStats = serverStorage.getAggregateRunningStatistics();
        assertNotNull("getAggregateRunningStatistics returned null after successful save", returnedStats);

        // test equality
        assertEquals("Save and load AggregateRunningStatistics did not return equal objects",
                aggregateRunningStatistics.getNumberOfRuns(), returnedStats.getNumberOfRuns());
        assertEquals("Save and load AggregateRunningStatistics did not return equal objects",
                aggregateRunningStatistics.getAverageDuration().getSecondsPassed(), returnedStats.getAverageDuration().getSecondsPassed());
        assertEquals("Save and load AggregateRunningStatistics did not return equal objects",
                aggregateRunningStatistics.getTotalDuration().getSecondsPassed(), returnedStats.getTotalDuration().getSecondsPassed());
    }

    @Test
    public void getAggregateRunningStatisticsEditTime_withinSecondsOfSave() {
        AggregateRunningStatistics aggregateRunningStatistics = new AggregateRunningStatistics();

        serverStorage.saveAggregateRunningStatistics(aggregateRunningStatistics, System.currentTimeMillis());

        assertEquals("getAggregateRunningStatisticsEditTime is not within 10 seconds of save time",
                System.currentTimeMillis(), serverStorage.getAggregateRunningStatisticsEditTime(), 10000);
    }

    @Test
    public void getRunningStatisticsEditTime_withinSecondsOfSave() {
        RunningStatistics runningStatistics = new RunningStatistics();

        serverStorage.saveRunningStatistics(runningStatistics, System.currentTimeMillis());

        assertEquals("getRunningStatisticsEditTime is not within 10 seconds of save time",
                System.currentTimeMillis(), serverStorage.getRunningStatisticsEditTime(String.valueOf(runningStatistics.getStartTimeMillis())), 10000);
    }

    @Test
    public void save_delete_AggregateRunningStatistics_returnsNullOnLoad() {
        AggregateRunningStatistics aggregateRunningStatistics = new AggregateRunningStatistics();

        serverStorage.saveAggregateRunningStatistics(aggregateRunningStatistics, dummyTime);

        assertTrue("deleteAggregateRunningStatistics did not report success one valid input", serverStorage.deleteAggregateRunningStatistics());

        assertNull("getAggregateRunningStatistics did not return null after deleting the document", serverStorage.getAggregateRunningStatistics());
    }

    @Test
    public void databaseInterferenceTest_runningStatistics_runningStatisticsGhost_aggregateRunningStatistics() {
        // insert some running, check if it shows up in ghost and aggr.
        serverStorage.saveRunningStatistics(new RunningStatistics(), dummyTime);
        assertTrue("RunningStatistics interferes with RunningStatisticsGhost", serverStorage.getFilenamesRunningStatisticsGhosts().isEmpty());
        assertNull("RunningStatistics interferes with AggregateRunningStatistics", serverStorage.getAggregateRunningStatistics());

        wipeDatabase();

        // insert some ghost, check if it shows up in running and aggr.
        serverStorage.saveRunningStatisticsGhost("blabla");
        assertTrue("RunningStatisticsGhost interferes with RunningStatistics", serverStorage.getFilenamesRunningStatistics().isEmpty());
        assertNull("RunningStatisticsGhost interferes with AggregateRunningStatistics", serverStorage.getAggregateRunningStatistics());

        wipeDatabase();

        // insert some aggr., check if it shows up in running and ghost
        serverStorage.saveAggregateRunningStatistics(new AggregateRunningStatistics(), dummyTime);
        assertTrue("AggregateRunningStatistics interferes with RunningStatistics", serverStorage.getFilenamesRunningStatistics().isEmpty());
        assertTrue("AggregateRunningStatistics interferes with RunningStatisticsGhost", serverStorage.getFilenamesRunningStatisticsGhosts().isEmpty());
    }

    @Test
    public void userInterferenceTest_runningStatisticsGhost() {
        // get a handle on the database
        String databaseURI = "mongodb://" + Constants.Storage.MONGOUSER + ":" + Constants.Storage.MONGOPASS + "@" + Constants.Server.ADDRESS + ":" + Constants.Server.MONGOPORT + "/DRIG_unittest";
        MongoClient mongoClient = new MongoClient(new MongoClientURI(databaseURI));
        MongoDatabase mongoDatabase = mongoClient.getDatabase(testDatabaseName);
        MongoCollection<Document> ghostCollection = mongoDatabase.getCollection(Constants.Storage.RUNNINGSTATISTICSGHOSTCOLLECTION);

        // (manually) save something on wrong userId
        Document docWithWrongId = new Document("user", "wrongId");
        docWithWrongId.put("filename", "123546");
        ghostCollection.insertOne(docWithWrongId);

        // check if it shows up
        List<String> filenames = serverStorage.getFilenamesRunningStatisticsGhosts();
        assertTrue("A document from the wrong user showed up", filenames.isEmpty());

        // (manually) save something on right userId
        Document docWithRightId = new Document("user", clientToken);
        String rightFilename = "789";
        docWithRightId.put("filename", rightFilename);
        ghostCollection.insertOne(docWithRightId);

        // check if it shows up
        filenames = serverStorage.getFilenamesRunningStatisticsGhosts();
        assertEquals("The document from the right user did not show up", 1, filenames.size());
        assertEquals("The document from the right user did not have the right name", rightFilename, filenames.get(0));
    }
}

