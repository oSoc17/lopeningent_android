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
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Storage component that takes care of the storage on the filesystem of the local device.
 * <p>
 * This class saves files with an additional userid. This userid is retrieved from SharedPreferences("client token").
 * Consumers of this class should not care about this userid. All filenames accepted and returned by {@link LocalStorage} are the plain filenames <b>without</b> the userid.
 * If you wish to access the filesystem in another way than this class, you should take the userid into account.
 * <p>
 * Created by Nick on 9-4-2017.
 */

class LocalStorage implements StorageComponent {
    private Context context;
    private static final String AGGREGATE_STATS_FILENAME = "AggregateRunningStatistics";
    private static final String DELIMITER = "_";
    private static final String TAG = "LocalStorage";


    /**
     * This constructor needs a context to access the local storage of this app.
     *
     * @param context Context of the app.
     */
    LocalStorage(Context context) {
        this.context = context;
    }

    /**
     * Method that looks up all the release 0.3 runningstatistics files (without userid) and converts them to the current storage system (with userid).
     * The current storage system following the following convention for filenames: "<userid>_<oldFileName>".
     * All present files are assigned to the current logged-in user.
     */
    void convertRelease0dot3Files() {
        File directory = context.getDir(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, Context.MODE_PRIVATE);
        File[] files = directory.listFiles();

        if (files == null) {
            // no files where found, so no conversion is needed
            return;
        }

        for (File file : files) {
            String filename = file.getName();
            if (!filename.contains(DELIMITER)) {
                // if the filename does not contain the DELIMITER, it is not linked to a user
                // so we link it to the current user
                File newFile = new File(directory, appendUserIdToFilename(filename));
                if (!file.renameTo(newFile) || !file.delete()) {
                    // if the renaming failed: can't to anything with the file, so we delete it
                    // if the delete failed, there is nothing we can do...
                    Log.e(TAG, "Unable to delete the file " + filename);
                }
            }
        }
    }

    @Nullable
    @Override
    public List<String> getFilenamesRunningStatistics() {
        return getFilenamesFromDirectory(Constants.Storage.RUNNINGSTATISTICSDIRECTORY);
    }

    @Override
    public List<String> getFilenamesRunningStatisticsGhosts() {
        return getFilenamesFromDirectory(Constants.Storage.RUNNINGSTATISTICSGHOSTDIRECTORY);
    }

    @Nullable
    @Override
    public RunningStatistics getRunningStatisticsFromFilename(String filename) {
        return (RunningStatistics) getObjectFromFilename(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, filename, RunningStatistics.class);
    }

    /**
     * Saves a {@link RunningStatistics} object to file/document/object.
     *
     * @param runningStatistics Object to save.
     * @param editTime          <b>Ignored</b> Overwritten by System.currentTimeMillis()
     * @return true upon success, false upon failure.
     */
    @Override
    public boolean saveRunningStatistics(RunningStatistics runningStatistics, long editTime) {
        return saveObjectToFile(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, String.valueOf(runningStatistics.getStartTimeMillis()), runningStatistics);
    }

    @Override
    public boolean saveRunningStatisticsGhost(String filename) {
        return saveObjectToFile(Constants.Storage.RUNNINGSTATISTICSGHOSTDIRECTORY, filename, "");
    }

    @Override
    public long getRunningStatisticsEditTime(String filename) {
        return getEditTimeFromFilename(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, filename);
    }

    @Override
    public boolean deleteRunningStatistics(String filename) {
        return deleteFile(Constants.Storage.RUNNINGSTATISTICSDIRECTORY, filename);
    }

    @Override
    public boolean deleteRunningStatisticsGhost(String filename) {
        return deleteFile(Constants.Storage.RUNNINGSTATISTICSGHOSTDIRECTORY, filename);
    }

    @Nullable
    @Override
    public AggregateRunningStatistics getAggregateRunningStatistics() {
        return (AggregateRunningStatistics) getObjectFromFilename(Constants.Storage.AGGREGATERUNNINGSTATISTICSDIRECTORY, AGGREGATE_STATS_FILENAME, AggregateRunningStatistics.class);
    }

    /**
     * @return time (in ms since EPOCH) the AggregateRunningStatistics file was last edited. 0 is file does not exist.
     */
    long getAggregateRunningStatisticsEditTime() {
        return getEditTimeFromFilename(Constants.Storage.AGGREGATERUNNINGSTATISTICSDIRECTORY, AGGREGATE_STATS_FILENAME);
    }

    /**
     * Saves the {@link AggregateRunningStatistics}.
     *
     * @param aggregateRunningStatistics Object to save.
     * @param editTime                   <b>Ignored</b> Overwritten by System.currentTimeMillis()
     * @return true upon success, false upon failure.
     */
    @Override
    public boolean saveAggregateRunningStatistics(AggregateRunningStatistics aggregateRunningStatistics, long editTime) {
        return saveObjectToFile(Constants.Storage.AGGREGATERUNNINGSTATISTICSDIRECTORY, AGGREGATE_STATS_FILENAME, aggregateRunningStatistics);
    }


    /**
     * Warning: really deleting the AggregateRunningStatistics will cause problems with server synchronization.
     * It is recommended to just do {@link #saveAggregateRunningStatistics(AggregateRunningStatistics, long)} with an empty object.
     */
    @Override
    public boolean deleteAggregateRunningStatistics() {
        return deleteFile(Constants.Storage.AGGREGATERUNNINGSTATISTICSDIRECTORY, AGGREGATE_STATS_FILENAME);
    }

    /**
     * Lists the names of all the available files in the directory for the current logged-in user. Strips the userid from the filenames.
     *
     * @param directory Directory to list the files from.
     * @return Names of all files in the directory. An empty list if there were no files. Null if something went wrong.
     */
    private List<String> getFilenamesFromDirectory(String directory) {
        List<String> result = new ArrayList<>();
        File directoryFile = context.getDir(directory, Context.MODE_PRIVATE);

        File[] files = directoryFile.listFiles();

        if (files == null) {
            // return the empty list when no files where found
            return result;
        }

        for (File file : files) {
            // filter on userid and remove userid from filename
            String decodedFilename = removeAndFilterOnUserIdFromFileName(file.getName());
            if (decodedFilename != null) {
                result.add(decodedFilename);
            }
        }

        return result;
    }

    /**
     * Reads a file from a directory and parses it to an object.
     *
     * @param directory Directory the file lives in.
     * @param filename  The name of the file (without userid).
     * @param clazz     Class to parse to.
     * @return an Object of the Class clazz.
     * @throws ClassCastException When the contents of the file can not be parsed to the clazz.
     */
    private Object getObjectFromFilename(String directory, String filename, Class clazz) {
        // get a reference to the file
        File directoryFile = context.getDir(directory, Context.MODE_PRIVATE);
        File file = new File(directoryFile, appendUserIdToFilename(filename));

        // read the contents into a string
        String fileContents = "";
        try {
            fileContents = readFileContentsToString(file);
        } catch (IOException e) {
            // something went wrong reading the file, so we return null
            Log.e(TAG, e.getMessage(), e);
            return null;
        }

        // convert the JSON content to an instance of RunningStatistics using the Gson API.
        try {
            return new Gson().fromJson(fileContents, clazz);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, e.getMessage(), e);
            throw new ClassCastException("The file contents could not be cast to a " + clazz.getName() + " object");
        }
    }

    /**
     * Saves an object to a file in a directory.
     *
     * @param directory Directory to save to.
     * @param filename  Name of file to save to (without userid).
     * @param object    Object to save.
     * @return true upon success, false upon failure.
     */
    private boolean saveObjectToFile(String directory, String filename, Object object) {
        // get a reference to the file we want to save in
        File directoryFile = context.getDir(directory, Context.MODE_PRIVATE);
        File file = new File(directoryFile, appendUserIdToFilename(filename));

        // convert the RunningStatistics object to a JSON string
        String contents = new Gson().toJson(object);

        // save the string to the file
        try {
            writeStringToFile(contents, file);
        } catch (IOException e) {
            // report a failure upon an exception
            Log.e(TAG, e.getMessage(), e);
            return false;
        }

        // report success
        return true;
    }

    /**
     * @param directory Directory to find the file in
     * @param filename  File to get edit time from
     * @return time (in ms since EPOCH) the file was last edited. 0 is file does not exist.
     */
    private long getEditTimeFromFilename(String directory, String filename) {
        File direcotyFile = context.getDir(directory, Context.MODE_PRIVATE);
        File file = new File(direcotyFile, appendUserIdToFilename(filename));
        return file.lastModified();
    }

    /**
     * Deletes a file from storage.
     *
     * @param directory Directory of the file.
     * @param filename  Name of file to delete (without userid).
     * @return true upon success, false upon failure.
     */
    private boolean deleteFile(String directory, String filename) {
        File directoryFile = context.getDir(directory, Context.MODE_PRIVATE);
        File file = new File(directoryFile, appendUserIdToFilename(filename));
        return file.delete();
    }

    /**
     * Reads the contents of a file to a string. All IOExceptions should be handled by the caller.
     *
     * @param file File to read from.
     * @return File contents as a String. Empty string if file was empty.
     * @throws IOException if an error occurs during the reading
     */
    private String readFileContentsToString(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            int amountRead = inputStream.read(buffer);
            if (amountRead == 0) {
                Log.d(TAG, "Read empty file: " + file.getName());
            }
            // no checks on file content here, if the content is corrupted, it will get catched by the JSON conversion in the caller
            return new String(buffer);
        }
    }

    /**
     * Writes a string to a file. All IOExceptions should be handled by the caller.
     *
     * @param string String to save
     * @param file   File to save to
     * @throws IOException if an error occurs during the writing
     */
    private void writeStringToFile(String string, File file) throws IOException {
        try (FileWriter fileWriter = new FileWriter(file, false)) {
            Writer output = new BufferedWriter(fileWriter);
            output.write(string);
            output.flush();
            output.close();
        }
    }

    /**
     * Converts a filename to a filename with the userid
     *
     * @param uncoded Normal filename
     * @return Filename with userid
     */
    private String appendUserIdToFilename(String uncoded) {
        String userId = PreferenceManager.getDefaultSharedPreferences(context).getString("client token", "");
        return userId + DELIMITER + uncoded;
    }

    /**
     * Converts a filename with userid to a filename and filters on userId.
     * The userId is fetched from SharedPreferences("client token").
     *
     * @param coded Filename with userid
     * @return Filename without userid. Returns null if this file is not related to the current user.
     */
    @Nullable
    private String removeAndFilterOnUserIdFromFileName(String coded) {
        String userId = PreferenceManager.getDefaultSharedPreferences(context).getString("client token", "");
        if (coded.startsWith(userId)) {
            int positionDelimiter = coded.indexOf(DELIMITER);
            return coded.substring(positionDelimiter + 1);
        }
        // the file was not for this user
        return null;
    }
}
