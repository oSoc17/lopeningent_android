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

import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;

import java.util.List;

/**
 * Interface for a storage component. This describes what a storage component should be capable of.
 * Typical examples of StorageComponents are LocalStorage and ServerStorage.
 * Created by Nick on 9-4-2017.
 */

interface StorageComponent {
    /**
     * Lists the names of all the available {@link RunningStatistics} files/documents/objects (whatever you want to call it) for the current logged-in user.
     *
     * @return All filenames or null if something went wrong. If there are no files, an empty list is returned.
     */
    @Nullable
    List<String> getFilenamesRunningStatistics();

    /**
     * Lists all ghost files for the current user.
     *
     * @return List of all ghost files. Empty list if no ghost files. Null if something went wrong.
     */
    List<String> getFilenamesRunningStatisticsGhosts();

    /**
     * Retrieves the contents of the file/document/object with the given name.
     *
     * @param filename Name of the file of which to read the contents.
     * @return The contents of the file cast to a RunningStatistics object or null if something went wrong.
     * @throws ClassCastException if the contents could not be cast to {@link RunningStatistics}.
     */
    @Nullable
    RunningStatistics getRunningStatisticsFromFilename(String filename);

    /**
     * Saves a {@link RunningStatistics} object to file/document/object.
     *
     * @param runningStatistics Object to save.
     * @param editTime          Time this object was last edited.
     * @return true upon success, false upon failure.
     */
    boolean saveRunningStatistics(RunningStatistics runningStatistics, long editTime);

    /**
     * Saves an empty ghost file with the given filename.
     *
     * @param filename Name of file to save.
     * @return true upon success, false upon failure.
     */
    boolean saveRunningStatisticsGhost(String filename);

    /**
     * @param filename File for which to get the edit time
     * @return time (in ms since EPOCH) the RunningStatistics file was last edited. 0 if file does not exist.
     */
    long getRunningStatisticsEditTime(String filename);

    /**
     * Deletes a file/document/object with the given name.
     *
     * @param filename Name of file to delete.
     * @return true upon success, false upon failure.
     */
    boolean deleteRunningStatistics(String filename);

    /**
     * Deletes a ghost file with given name.
     *
     * @param filename Name of file to delete.
     * @return true upon success, false upon failure.
     */
    boolean deleteRunningStatisticsGhost(String filename);


    /**
     * Retrieves the {@link AggregateRunningStatistics} for the logged in user.
     *
     * @return The AggregateRunningStatistics or null if something went wrong.
     * @throws ClassCastException if the contents could not be cast to {@link AggregateRunningStatistics}.
     */
    @Nullable
    AggregateRunningStatistics getAggregateRunningStatistics();

    /**
     * Saves the {@link AggregateRunningStatistics}.
     *
     * @param aggregateRunningStatistics Object to save.
     * @param editTime                   Time this object was last edited.
     * @return true upon success, false upon failure.
     */
    boolean saveAggregateRunningStatistics(AggregateRunningStatistics aggregateRunningStatistics, long editTime);

    /**
     * Deletes the saved instance of {@link AggregateRunningStatistics}.
     *
     * @return true upon success, false upon failure.
     */
    boolean deleteAggregateRunningStatistics();
}
