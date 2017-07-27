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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * Central class from the persistence package. This class handles the general control flow.
 * All external requests that arrive from public interface classes (like {@link EventBasedPersistence}) call a method from this class.
 * Created by Nick on 9-4-2017.
 */

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class PersistenceController {
    private LocalStorage localStorage;
    private ServerStorage serverStorage;
    private ScheduledThreadPoolExecutor threadPool;
    private ServerSynchronization sync;
    private Context context;

    PersistenceController(Context context) {
        this.context = context;
        localStorage = new LocalStorage(context);
        String clientToken = PreferenceManager.getDefaultSharedPreferences(context).getString("client token", "");
        serverStorage = new ServerStorage();

        // make a threadpool that allows to schedule tasks
        // this threadpool has only one thread and it is a daemon, meaning that it won't stall the app on shutdown
        threadPool = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setDaemon(true);
                return thread;
            }
        });

        // make an instance of the class that will do the synchronization
        sync = new ServerSynchronization(localStorage, serverStorage, threadPool);
    }

    private void renewClientToken() {
        serverStorage.setUserToken();
    }

    /**
     * For testing purposes only.
     */
    @VisibleForTesting
    public ServerStorage getServerStorage() {
        return serverStorage;
    }

    /**
     * For testing purposes only.
     */
    @VisibleForTesting
    LocalStorage getLocalStorage() {
        return localStorage;
    }

    /**
     * For testing purposes only.
     */
    @VisibleForTesting
    ScheduledThreadPoolExecutor getThreadPool() {
        return threadPool;
    }

    /**
     * Does all kinds of needed backwards compatibility tasks. Mostly related to changing the names of files.
     * Should be called on app startup.
     */
    void doBackwardsCompatibility() {
        // convert files from release 0.3 to new format
        localStorage.convertRelease0dot3Files();
    }

    /**
     * Saves a {@link RunningStatistics} object to local storage and initiates a {@link #synchronizeWithServer()}.
     *
     * @param runningStatistics object to save.
     */
    void saveRunningStatistics(RunningStatistics runningStatistics) {
        boolean success = localStorage.saveRunningStatistics(runningStatistics, System.currentTimeMillis());
        if (success) {
            // we only contact the server upon successful local save
            synchronizeWithServer();
        }
    }

    /**
     * Deletes a {@link RunningStatistics} object from local storage and initiates a {@link #synchronizeWithServer()}.
     *
     * @param runningStatistics object to delete.
     * @return true on success, false on failure
     */
    boolean deleteRunningStatistics(RunningStatistics runningStatistics) {
        String filename = String.valueOf(runningStatistics.getStartTimeMillis());
        boolean success = localStorage.saveRunningStatisticsGhost(filename);
        if (success) {
            // we only delete when we can save a ghost image
            success = localStorage.deleteRunningStatistics(filename);
            if (success) {
                // we only contact the server upon successful local delete
                synchronizeWithServer();
            }
        }
        return success;
    }

    /**
     * Retrieves all saved {@link RunningStatistics} from local storage. Does NOT care about anything server related.
     *
     * @return List of all saved statistics sorted new to old. Empty list if nothing was found in local storage.
     */
    List<RunningStatistics> getRunningStatistics() {
        List<RunningStatistics> result = new ArrayList<>();
        List<String> filenames = localStorage.getFilenamesRunningStatistics();
        if (filenames == null) {
            // when the local storage returns an error, we return an empty list.
            return result;
        }
        for (String filename : filenames) {
            result.add(localStorage.getRunningStatisticsFromFilename(filename));
        }

        //Sort the list of RunningStatistics from new to old
        Collections.sort(result, new Comparator<RunningStatistics>() {
            /**
             * Sorts from most recent (biggest) to oldest (smallest)
             * @param o1 object 1
             * @param o2 object 2
             * @return a negative integer, zero, or a positive integer as the first argument
             *          is greater than, equal to, or less than the second.
             */
            @Override
            public int compare(RunningStatistics o1, RunningStatistics o2) {
                if (o1.getStartTimeMillis() > o2.getStartTimeMillis()) {
                    return -1;
                } else if (o1.getStartTimeMillis() < o2.getStartTimeMillis()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        synchronizeWithServer();
        return result;
    }

    /**
     * Saves the {@link AggregateRunningStatistics} to local storage and initiates a {@link #synchronizeWithServer()}.
     *
     * @param aggregateRunningStatistics object to save.
     */
    void saveAggregateStatistics(AggregateRunningStatistics aggregateRunningStatistics) {
        boolean success = localStorage.saveAggregateRunningStatistics(aggregateRunningStatistics, System.currentTimeMillis());
        if (success) {
            // we only contact the server upon successful local save
            synchronizeWithServer();
        }
    }

    /**
     * Deletes the {@link AggregateRunningStatistics} from local and server storage.
     */
    void deleteAggregateStatistics() {
        AggregateRunningStatistics empty = new AggregateRunningStatistics();
        boolean success = localStorage.saveAggregateRunningStatistics(empty, System.currentTimeMillis());
        if (success) {
            renewClientToken();
            // we only contact the server upon successful local delete
            synchronizeWithServer();
        }
    }

    /**
     * Retrieves the saved {@link AggregateRunningStatistics} from local storage. Does NOT care about anything server related.
     *
     * @return AggregateRunningStatistics of the logged in user.
     */
    @Nullable
    AggregateRunningStatistics getAggregateRunningStatistics() {
        AggregateRunningStatistics result = localStorage.getAggregateRunningStatistics();
        synchronizeWithServer();
        return result;
    }

    /**
     * Performs the synchronization between the local storage and the database on the server.
     */
    void synchronizeWithServer() {
        renewClientToken();
        // schedule the synchronization to run now, if it is not already running
        if (threadPool.getActiveCount() < 1) {
            threadPool.execute(sync);
        }
    }


}
