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
import android.content.Context;
import android.preference.PreferenceManager;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.RunData.RunDuration;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for persistence.LocalStorage.
 * Created by Nick on 10-4-2017.
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class LocalStorageTests {
    private LocalStorage localStorage;
    private Application application;
    private String clientToken;
    private long dummyTime;

    @Before
    public void init() {
        dummyTime = 5;
        application = RuntimeEnvironment.application;
        localStorage = new LocalStorage(application);
        clientToken = "123e4567-e89b-12d3-a456-426655440000";
        // example UUID (format used by Firebase (https://groups.google.com/forum/#!searchin/firebase-talk/uid/firebase-talk/s9mv4S46Qs0/YPfpNz-VBgAJ))
        // copied from wikipedia (https://en.wikipedia.org/wiki/Universally_unique_identifier)
        PreferenceManager.getDefaultSharedPreferences(application).edit().putString("client token", clientToken).commit();
    }

    @Test
    public void convertRelease03_LeavesNewFilesAlone() {
        // make some files that confirm new standard
        String validFilename = "4587213431_something";
        String validContent = "some content";
        saveMockFile(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, validFilename, validContent);

        String alsoValidFilename = "4587_something_something_profit";
        String alsoValidContent = "some more content";
        saveMockFile(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, alsoValidFilename, alsoValidContent);

        // run convert
        localStorage.convertRelease0dot3Files();

        // check files unchanged
        File directory = application.getDir(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, Context.MODE_PRIVATE);
        File[] files = directory.listFiles();
        Map<String, File> filenames = new HashMap<>();
        assertNotNull("No files found in directory after running convertRelease0dot3Files() on valid files", files);
        assertEquals("Did not found 2 files in directory after running convertRelease0dot3Files() on 2 valid files", 2, files.length);
        for (File file : files) {
            filenames.put(file.getName(), file);
        }
        assertTrue("Did not find valid filename in directory after running convertRelease0dot3Files()", filenames.containsKey(validFilename));
        assertTrue("Did not find valid filename in directory after running convertRelease0dot3Files()", filenames.containsKey(alsoValidFilename));
        assertEquals("convertRelease0dot3Files() changed content of valid files", validContent, readMockFile(filenames.get(validFilename)));
        assertEquals("convertRelease0dot3Files() changed content of valid files", alsoValidContent, readMockFile(filenames.get(alsoValidFilename)));
    }

    @Test
    public void convertRelease03_ConvertsOldFilesAndLeavesContentAlone() {
        // make some old files (that don't confirm the standard)
        String oldFilename = "somefile";
        String oldContent = "someContent";
        String expectedFilename = clientToken + "_" + oldFilename;
        saveMockFile(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, oldFilename, oldContent);

        String anotherOldFilename = "12365745";
        String anotherOldContent = "JSON Bourne";
        String anotherExpectedFilename = clientToken + "_" + anotherOldFilename;
        saveMockFile(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, anotherOldFilename, anotherOldContent);

        // do convert
        localStorage.convertRelease0dot3Files();

        // check filenames changed with content the same
        File directory = application.getDir(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, Context.MODE_PRIVATE);
        File[] files = directory.listFiles();
        Map<String, File> filenames = new HashMap<>();
        assertNotNull("No files found in directory after running convertRelease0dot3Files() on old files", files);
        assertEquals("Did not found 2 files in directory after running convertRelease0dot3Files() on 2 old files", 2, files.length);
        for (File file : files) {
            filenames.put(file.getName(), file);
        }
        assertTrue("Did not find valid filename in directory after running convertRelease0dot3Files()", filenames.containsKey(expectedFilename));
        assertTrue("Did not find valid filename in directory after running convertRelease0dot3Files()", filenames.containsKey(anotherExpectedFilename));
        assertEquals("convertRelease0dot3Files() changed content of renamed files", oldContent, readMockFile(filenames.get(expectedFilename)));
        assertEquals("convertRelease0dot3Files() changed content of renamed files", anotherOldContent, readMockFile(filenames.get(anotherExpectedFilename)));
    }

    @Test
    public void getFilenamesRunningStatistics_2Valid2InValid_returns2Filenames() {
        // make some files, 2 with the correct name, 1 other
        String validFilename = "458721";
        saveMockFile(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, clientToken + "_" + validFilename, "mqksdjf");
        String anotherValidFilename = "fivesixseveneight";
        saveMockFile(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, clientToken + "_" + anotherValidFilename, "jqmsf");
        String invalidFilename = "onetwothreefour";
        saveMockFile(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, invalidFilename, "mqsdjf");

        // ask names from LocalStorage
        List<String> filenames = localStorage.getFilenamesRunningStatistics();

        // check it returns only the 2 valid ones
        assertNotNull("LocalStorage.getFilenamesRunningStatistics() returned null for valid files", filenames);
        assertEquals("LocalStorage.getFilenamesRunningStatistics() did not return 2 valid files", 2, filenames.size());
        assertTrue("LocalStorage.getFilenamesRunningStatistics() did not return valid filename", filenames.contains(validFilename));
        assertTrue("LocalStorage.getFilenamesRunningStatistics() did not return valid filename", filenames.contains(anotherValidFilename));
    }

    @Test
    public void getFilenamesRunningStatistics_emptyDirectory_returnsEmptyList() {
        List<String> filenames = localStorage.getFilenamesRunningStatistics();
        assertNotNull("LocalStorage.getFilenamesRunningStatistics() returned null empty directory", filenames);
        assertTrue("LocalStorage.getFilenamesRunningStatistics() did not return null for empty directory", filenames.isEmpty());
    }

    @Test
    public void getFilenamesRunningStatistics_twoUsers_doesOnlyReturnFileForCurrentUser() {
        // save mock file for current user
        String currentUserFilename = "44536";
        saveMockFile(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, clientToken + "_" + currentUserFilename, "Oh no, it's JSON Bourne");

        // save mock file for another user
        saveMockFile(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, "45787" + "_" + "somefilename", "I am the one and only.");

        // retrieve filenames
        List<String> filenames = localStorage.getFilenamesRunningStatistics();
        assertNotNull("getFilenamesRunningStatistics returned null on 2 files in directory", filenames);
        assertEquals("Did not find exactly one file for current user", 1, filenames.size());

        // check only current user is returned
        assertEquals("getFilenamesRunningStatistics did not return filename for correct user", currentUserFilename, filenames.get(0));
    }

    @Test
    public void saveRunningStatistics_loadRunningStatistics_returnsEqualObject() {
        // save a RunningStatistics object
        RunningStatistics runningStatistics = new RunningStatistics();
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);
        runningStatistics.addRating(5.0);
        localStorage.saveRunningStatistics(runningStatistics, System.currentTimeMillis());

        // find the object in the list of filenames
        List<String> filenames = localStorage.getFilenamesRunningStatistics();
        assertNotNull("getFilenamesRunningStatistics returned null after saving one file", filenames);
        assertEquals("Did not find exactly one file after saving only one file", 1, filenames.size());
        String filename = filenames.get(0);
        assertEquals("The edit time for the running statistics file is not within 1 second of the moment is was saved", System.currentTimeMillis(), localStorage.getRunningStatisticsEditTime(filename), 1000);


        // load the object with it's filename
        RunningStatistics retrievedRunningStatistics = localStorage.getRunningStatisticsFromFilename(filename);
        assertNotNull("getRunningStatisticsFromFilename returned null one loading an existing file");

        // check if object is the same
        // can't use assertEquals because Object.equals(Object) is not implemented
        assertEquals("Saved and loaded RunningStatistics did not have matching elapsed time", runningStatistics.getRunDuration().getSecondsPassed(), retrievedRunningStatistics.getRunDuration().getSecondsPassed());
        assertEquals("Saved and loaded RunningStatistics did not have matching starttime", runningStatistics.getStartTimeMillis(), retrievedRunningStatistics.getStartTimeMillis());
        assertEquals("Saved and loaded RunningStatistics did not have matching rating", runningStatistics.getRating(), retrievedRunningStatistics.getRating(), 0.01);
    }

    @Test(expected = ClassCastException.class)
    public void loadRunningStatistics_onGarbageFile_throwsClassCastException() {
        String filename = "456";
        saveMockFile(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, clientToken + "_" + filename, "garbage");
        localStorage.getRunningStatisticsFromFilename(filename);
    }

    @Test
    public void saveRunningStatistics_deleteRunningStatistics_returnsNoFilenames() {
        // save
        RunningStatistics runningStatistics = new RunningStatistics();
        localStorage.saveRunningStatistics(runningStatistics, dummyTime);

        // get filename
        List<String> filenames = localStorage.getFilenamesRunningStatistics();
        assertNotNull("getFilenamesRunningStatistics returned null after saving one file", filenames);
        assertEquals("Did not find exactly one file after saving only one file", 1, filenames.size());
        String filename = filenames.get(0);

        // delete
        localStorage.deleteRunningStatistics(filename);

        // check no more files
        filenames = localStorage.getFilenamesRunningStatistics();
        assertNotNull("getFilenamesRunningStatistics returned null on reading empty dir", filenames);
        assertEquals("Did not find empty directory after deletion of only file", 0, filenames.size());
    }

    @Test
    public void getAggregateRunningStatistics_onEmptyDirectory_returnsNull() {
        assertNull("LocalStorage.getAggregateRunningStatistics did not return null on empty directory", localStorage.getAggregateRunningStatistics());
        assertEquals("LocalStorage.getAggregateRunningStatisticsEditTime did not return 0 for empty directory", 0, localStorage.getAggregateRunningStatisticsEditTime());
    }

    @Test
    public void saveAggregateRunningStatistics_loadAggregateRunnningStatistics_returnsEqualObject() {
        // make an aggr. stats object
        AggregateRunningStatistics statistics = new AggregateRunningStatistics();
        RunningStatistics runningStatistics = new RunningStatistics();
        RunDuration runDuration = new RunDuration();
        runDuration.addSecond();
        runningStatistics.addRunDuration(runDuration);
        runningStatistics.addRating(5.0);
        statistics.handleRunningStatistics(runningStatistics);

        // save the aggr. stats object
        localStorage.saveAggregateRunningStatistics(statistics, System.currentTimeMillis());
        assertEquals("The edit time for the aggregate running statistics file is not within 1 second of the moment is was saved", System.currentTimeMillis(), localStorage.getAggregateRunningStatisticsEditTime(), 1000);

        // load the object
        AggregateRunningStatistics retrievedStatistics = localStorage.getAggregateRunningStatistics();
        assertNotNull("getAggregateRunningStatistics() returned null after call to saveAggregateRunningStatistics", retrievedStatistics);

        // check if object is the same
        // no assertEquals on the object itself as 'equals' is not implemented
        assertEquals("Saved and loaded AggregateRunningStatistics did not match on average duration", statistics.getAverageDuration().getSecondsPassed(), retrievedStatistics.getAverageDuration().getSecondsPassed());
        assertEquals("Saved and loaded AggregateRunningStatistics did not match on number runs", statistics.getNumberOfRuns(), retrievedStatistics.getNumberOfRuns());
    }

    @Test
    public void saveAggregateStatistics_deleteAggregateStatistics_returnsNullOnGetAggregateStatistics() {
        // save an aggr. stats object
        AggregateRunningStatistics statistics = new AggregateRunningStatistics();
        localStorage.saveAggregateRunningStatistics(statistics, dummyTime);

        // delete it
        localStorage.deleteAggregateRunningStatistics();

        // check if it is gone by calling getAggrStats and expecting null
        assertNull("Deleting a saved AggregateRunningStatistics did still return one on getAggregateRunningStatistics()", localStorage.getAggregateRunningStatistics());
    }

    @Test
    public void saveGhostRunning_filePresent_deleteGhostRunning_returnsNoFilenames() {
        // save ghost file
        String filename = "875422";
        localStorage.saveRunningStatisticsGhost(filename);

        // check if it exists
        List<String> filenames = localStorage.getFilenamesRunningStatisticsGhosts();
        assertNotNull("getFilenamesRunningStatisticsGhost returned null after saving one ghost file", filenames);
        assertEquals("Did not find exactly one ghost file after saving only one ghost file", 1, filenames.size());
        String returnedFilename = filenames.get(0);
        assertEquals("Saving ghost file does not return it's filename", filename, returnedFilename);

        // delete ghost file
        localStorage.deleteRunningStatisticsGhost(returnedFilename);

        // check if it still exists
        filenames = localStorage.getFilenamesRunningStatisticsGhosts();
        assertNotNull("getFilenamesRunningStatisticsGhosts returned null on reading empty dir", filenames);
        assertEquals("Did not find empty directory after deletion of only ghost file", 0, filenames.size());
    }

    @Test
    public void saveGhostRunning_listRunning_doesNotShowGhost() {
        // save ghost
        String filename = "46873";
        localStorage.saveRunningStatisticsGhost(filename);

        // check if (non-ghost) running statistics exist
        List<String> filenames = localStorage.getFilenamesRunningStatistics();
        assertNotNull("getFilenamesRunningStatistics returned null after saving one ghost file", filenames);
        assertEquals("Saving only a ghost did not return an empty (real) RunningStatistics directory", 0, filenames.size());
    }

    @Test
    public void saveAggregateRunningStatistics_listRunningStatistics_doesNotShowAggregate() {
        // save aggr. stats
        localStorage.saveAggregateRunningStatistics(new AggregateRunningStatistics(), dummyTime);

        // check if (non-ghost) running statistics exist
        List<String> filenames = localStorage.getFilenamesRunningStatistics();
        assertNotNull("getFilenamesRunningStatistics returned null after saving aggregate running statistics", filenames);
        assertEquals("Saving only aggregate running statistics did not return an empty RunningStatistics directory", 0, filenames.size());
    }


    private void saveMockFile(String directory, String filename, String contents) {
        File directoryFile = application.getDir(directory, Context.MODE_PRIVATE);
        File file = new File(directoryFile, filename);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(contents.getBytes());
        } catch (IOException e) {
            fail("Unable to save mock file with name " + filename + " to directory " + directory);
        }
    }

    private String readMockFile(File file) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            return new String(buffer);
        } catch (IOException e) {
            fail("Unable to read mock file with name " + file.getName() + " in directory " + file.getPath());
            return null; // because the java compiler doesn't know fail() will never return
        }
    }
}
