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
import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class that handles the synchronization between {@link LocalStorage} and {@link ServerStorage} for the {@link PersistenceController}.
 * Created by Nick on 10-4-2017.
 */

class ServerSynchronization implements Runnable {
    private LocalStorage localStorage;
    private ServerStorage serverStorage;
    private ScheduledThreadPoolExecutor threadPool;

    ServerSynchronization(LocalStorage localStorage, ServerStorage serverStorage, ScheduledThreadPoolExecutor threadPool) {
        this.localStorage = localStorage;
        this.serverStorage = serverStorage;
        this.threadPool = threadPool;
    }

    @Override
    public void run() {
        if (!synchronize()) {
            // the sync failed -> reschedule sync operation
            threadPool.schedule(this, Constants.Storage.MINUTES_BETWEEN_SERVER_SYNC_TRIES, TimeUnit.MINUTES);
        }
    }

    /**
     * Does the synchronization between local and server
     *
     * @return true on success, false on failure
     */
    boolean synchronize() {
        try {
            serverStorage.connect();
            boolean success = synchronizeAggregateStatistics();
            if (!success) {
                // if first part was not successful, whole sync failed
                return false;
            }
            return synchronizeRunningStatics();
        } finally {
            serverStorage.disconnect();
        }
    }

    /**
     * Does the synchronization of the aggregate statistics
     *
     * @return true on success, false on failure
     */
    private boolean synchronizeAggregateStatistics() {
        // get edit time of local and server
        long localEditTime = localStorage.getAggregateRunningStatisticsEditTime();
        long serverEditTime = serverStorage.getAggregateRunningStatisticsEditTime();

        // edit times are in sync -> nothing has to be done
        if (localEditTime == serverEditTime) {
            return true;
        }

        // server has newest version -> download
        if (localEditTime < serverEditTime) {
            AggregateRunningStatistics aggregateRunningStatistics = serverStorage.getAggregateRunningStatistics();
            if (aggregateRunningStatistics == null) {
                // something went wrong reading server data (eg., no internet) -> report sync failed
                return false;
            }
            return localStorage.saveAggregateRunningStatistics(aggregateRunningStatistics, serverEditTime);
        }

        // when we get here: localEditTime > serverEditTime
        // local has newest version -> upload
        AggregateRunningStatistics aggregateRunningStatistics = localStorage.getAggregateRunningStatistics();
        if (aggregateRunningStatistics == null) {
            // something went wrong reading local data -> report sync failed
            return false;
        }
        // remove old one
        serverStorage.deleteAggregateRunningStatistics();
        // upload new
        return serverStorage.saveAggregateRunningStatistics(aggregateRunningStatistics, localEditTime);

    }

    /**
     * Does the synchronization of the running statistics
     *
     * @return true on success, false on failure
     */
    private boolean synchronizeRunningStatics() {
        // get real and ghost file lists for both local and server
        List<String> realFilesLocal = localStorage.getFilenamesRunningStatistics();
        List<String> ghostFilesLocal = localStorage.getFilenamesRunningStatisticsGhosts();
        List<String> realFilesServer = serverStorage.getFilenamesRunningStatistics();
        List<String> ghostFilesServer = serverStorage.getFilenamesRunningStatisticsGhosts();

        // if any of above methods returned null (eg., no internet) -> report sync failed
        if (realFilesLocal == null || ghostFilesLocal == null || realFilesServer == null || ghostFilesServer == null) {
            return false;
        }

        // for each file there are now <b>9</b> possibilities
        // 1. in the case of a ghost on the server and nothing on the client: do nothing
        // 2. in the case of nothing on client and nothing on server: do nothing

        // 3. in the case of an item on the client and an item on the server: compare edit times
        for (String filename : realServerAndRealClient(realFilesServer, realFilesLocal)) {
            if (!compareEditTimes(filename)) {
                return false;
            }
        }

        // 4. in the case of an item on the server and a ghost on the client: delete local, put ghost on server
        for (String filename : realServerAndGhostClient(realFilesServer, ghostFilesLocal)) {
            if (!deleteLocalAndGhostServer(filename)) {
                return false;
            }
        }

        // 5. in the case of an item on the server and no item on the client: download
        for (String filename : realServerAndNothingClient(realFilesServer, realFilesLocal, ghostFilesLocal)) {
            if (!download(filename)) {
                return false;
            }
        }

        // 6. in the case of a ghost on the server and an item on the client: delete local
        for (String filename : ghostServerAndRealClient(ghostFilesServer, realFilesLocal)) {
            if (!deleteLocal(filename)) {
                return false;
            }
        }

        // 7. in the case of a ghost on the server and a ghost on the client: delete client ghost
        for (String filename : ghostServerAndGhostClient(ghostFilesServer, ghostFilesLocal)) {
            if (!deleteLocalGhost(filename)) {
                return false;
            }
        }

        // 8. in the case of nothing on the server and an item on the client: upload
        for (String filename : nothingServerAndRealClient(realFilesServer, ghostFilesServer, realFilesLocal)) {
            if (!upload(filename)) {
                return false;
            }
        }

        // 9. in the case of nothing on the server and a ghost on the client: delete local ghost
        for (String filename : nothingServerAndGhostClient(realFilesServer, ghostFilesServer, ghostFilesLocal)) {
            if (!deleteLocalGhost(filename)) {
                return false;
            }
        }

        // when we get here all sync operations were successful
        return true;
    }

    private List<String> realServerAndRealClient(List<String> realServer, List<String> realClient) {
        return listCrossSection(realServer, realClient);
    }

    private List<String> realServerAndGhostClient(List<String> realServer, List<String> ghostClient) {
        return listCrossSection(realServer, ghostClient);
    }

    private List<String> realServerAndNothingClient(List<String> realServer, List<String> realClient, List<String> ghostClient) {
        List<String> allClient = new ArrayList<>();
        allClient.addAll(realClient);
        allClient.addAll(ghostClient);
        return listNotContains(realServer, allClient);
    }

    private List<String> ghostServerAndRealClient(List<String> ghostServer, List<String> realClient) {
        return listCrossSection(ghostServer, realClient);
    }

    private List<String> ghostServerAndGhostClient(List<String> ghostServer, List<String> ghostClient) {
        return listCrossSection(ghostServer, ghostClient);
    }

    private List<String> nothingServerAndRealClient(List<String> realServer, List<String> ghostServer, List<String> realClient) {
        List<String> allServer = new ArrayList<>();
        allServer.addAll(realServer);
        allServer.addAll(ghostServer);
        return listNotContains(realClient, allServer);
    }

    private List<String> nothingServerAndGhostClient(List<String> realServer, List<String> ghostServer, List<String> ghostClient) {
        List<String> allServer = new ArrayList<>();
        allServer.addAll(realServer);
        allServer.addAll(ghostServer);
        return listNotContains(ghostClient, allServer);
    }


    /**
     * @return List of items that are in List1 and List2
     */
    private List<String> listCrossSection(List<String> list1, List<String> list2) {
        List<String> result = new ArrayList<>();
        for (String i : list1) {
            if (list2.contains(i)) {
                result.add(i);
            }
        }
        return result;
    }

    /**
     * @return List of items that are in 'list' but not in 'filter'.
     */
    private List<String> listNotContains(List<String> list, List<String> filter) {
        List<String> result = new ArrayList<>();
        result.addAll(list);
        for (String i : filter) {
            if (list.contains(i)) {
                result.remove(i);
            }
        }
        return result;
    }

    /**
     * Compares the edit times for 2 RunningStatistics files and keeps the one with the highest one.
     *
     * @param filename File to compare the times of.
     * @return True on success, false if something went wrong.
     */
    private boolean compareEditTimes(String filename) {
        // get edit time of local and server
        long localEditTime = localStorage.getRunningStatisticsEditTime(filename);
        long serverEditTime = serverStorage.getRunningStatisticsEditTime(filename);

        // edit times are in sync -> nothing has to be done
        if (localEditTime == serverEditTime) {
            return true;
        }

        // server has newest version -> download
        if (localEditTime < serverEditTime) {
            download(filename);
            return true;
        }

        // when we get here: localEditTime > serverEditTime
        // local has newest version -> upload
        upload(filename);
        return true;
    }

    /**
     * Puts a ghost file on the server. Then deletes the file on the server. Then deletes the client ghost.
     *
     * @param filename Name of file to ghost/delete.
     * @return True on success, false on failure.
     */
    private boolean deleteLocalAndGhostServer(String filename) {
        if (!serverStorage.saveRunningStatisticsGhost(filename)) {
            return false;
        }
        if (!serverStorage.deleteRunningStatistics(filename)) {
            return false;
        }
        return localStorage.deleteRunningStatistics(filename);
    }

    /**
     * Loads a file from local storage, then saves it on the server.
     *
     * @param filename File to upload.
     * @return True on success, false on failure.
     */
    private boolean upload(String filename) {
        RunningStatistics runningStatistics = localStorage.getRunningStatisticsFromFilename(filename);
        long editTime = localStorage.getRunningStatisticsEditTime(filename);
        if (runningStatistics == null || editTime == 0) {
            return false;
        }
        // remove old one
        serverStorage.deleteRunningStatistics(filename);
        return serverStorage.saveRunningStatistics(runningStatistics, editTime);
    }

    /**
     * Deletes a file from local storage.
     *
     * @param filename File to delete.
     * @return True on success, false on failure.
     */
    private boolean deleteLocal(String filename) {
        return localStorage.deleteRunningStatistics(filename);
    }

    /**
     * Deletes a ghost from local storage.
     *
     * @param filename Ghost to delete.
     * @return True on success, false on failure.
     */
    private boolean deleteLocalGhost(String filename) {
        return localStorage.deleteRunningStatisticsGhost(filename);
    }

    /**
     * Loads a file from server, then saves it to local.
     *
     * @param filename File to download.
     * @return True on success, false on failure.
     */
    private boolean download(String filename) {
        RunningStatistics runningStatistics = serverStorage.getRunningStatisticsFromFilename(filename);
        long editTime = serverStorage.getRunningStatisticsEditTime(filename);
        if (runningStatistics == null || editTime == 0) {
            return false;
        }
        return localStorage.saveRunningStatistics(runningStatistics, editTime);
    }
}
