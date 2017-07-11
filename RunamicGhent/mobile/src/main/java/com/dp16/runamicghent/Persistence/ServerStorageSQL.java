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
