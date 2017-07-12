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

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage component that takes care of the server side storage on the PostgreSQL database.
 * This file should be similar to the existing ServerStorage.java file, but using the new Postgres database
 *
 * Created by Redouane Arroubai 11-07-2017
 */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class ServerStorageSQL implements StorageComponent {
    private boolean connected;
    /*private MongoClient mongoClient;
    private MongoCollection<Document> runningStatisticsCollection;
    private MongoCollection<Document> runningStatisticsGhostCollection;
    private MongoCollection<Document> aggregateRunningStatisticsCollection;    needs to be SQL*/
    private String databaseToUse;
    private String userId;
    private static final String FILENAME_KEY = "filename";
    private static final String EDIT_TIME_KEY = "edittime";
    private static final String USER_KEY = "user";
    private static final String TAG = "ServerStorage";

    ServerStorageSQL(String clientToken) {
        connected = false;
        databaseToUse = Constants.Storage.MONGODBNAME;/// need to change to SQL database name
        userId = clientToken;
    }

    void setUserId(String clientToken) {
        userId = clientToken;
    }


    /**
     * For testing purposes only. Overrides the database in Constants.Storage.MONGODBNAME.
     * <p>
     * Must be called <i>before</i> {@link #connect()} to have any effect.
     *
     * @param database New database to use.
     */
    @VisibleForTesting
    public void setDatabaseToUse(String database) {
        databaseToUse = database;
    }

    /**
     * Sets up the database connection.<br>
     * <b>Must</b> be called before any method that accesses the database. Otherwise the other methods will report failure.
     * <p>
     * Does nothing if already connected.
     */
    void connect() {
        if (!connected) {
            /*
            connection to Postgresql server/ python middleware
             */
            connected = true;
        }
    }

    /**
     * Cleans up the database connection. Should be called when done with the database.
     * <p>
     * Does nothing if not connected.
     */
    void disconnect() {
        if (connected) {
            //mongoClient.close();
            connected = false;
        }
    }

    @Nullable
    @Override
    public List<String> getFilenamesRunningStatistics() {
        return null;
    }

    @Override
    public List<String> getFilenamesRunningStatisticsGhosts() {
        return null;
    }

    @Nullable
    @Override
    public RunningStatistics getRunningStatisticsFromFilename(String filename) {
        return null;
    }

    @Override
    public boolean saveRunningStatistics(RunningStatistics runningStatistics, long editTime) {
        return false;
    }

    @Override
    public boolean saveRunningStatisticsGhost(String filename) {
        return false;
    }

    @Override
    public long getRunningStatisticsEditTime(String filename) {
        return 0;
    }

    @Override
    public boolean deleteRunningStatistics(String filename) {
        return false;
    }

    @Override
    public boolean deleteRunningStatisticsGhost(String filename) {
        return false;
    }

    @Nullable
    @Override
    public AggregateRunningStatistics getAggregateRunningStatistics() {
        return null;
    }

    @Override
    public boolean saveAggregateRunningStatistics(AggregateRunningStatistics aggregateRunningStatistics, long editTime) {
        return false;
    }

    @Override
    public boolean deleteAggregateRunningStatistics() {
        return false;
    }
}
