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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.dp16.runamicghent.Activities.Utils;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mongodb.util.JSON;

import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Storage component that takes care of the server side storage on the PostgreSQL database.
 * This file should be similar to the existing ServerStorage.java file, but using the new Postgres database and middleware (api)
 *
 * Created by Redouane Arroubai 11-07-2017
 */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class ServerStorage implements StorageComponent {
    private boolean connected;
    private String userToken;
    private JSONObject serverStats;

    ServerStorage() {
        setUserToken();
    }


    void setUserToken() {
        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
        try {
            mUser.getToken(true)
                    .addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                        public void onComplete(@NonNull Task<GetTokenResult> task) {

                            if (task.isSuccessful()) {
                                userToken = task.getResult().getToken();
                                Log.d("userToken", userToken);
                            } else {
                                userToken = "";
                            }


                        }
                    });
        }catch (Exception e){
            userToken = "";
        }
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

    }

    /**
     * Sets up the database connection.<br>
     * <b>Must</b> be called before any method that accesses the database. Otherwise the other methods will report failure.
     * <p>
     * Does nothing if already connected.
     */
    void connect() {
        if (!connected ) {
            setUserToken();
            if(userToken != ""){

                String body = null;
                try {
                    body = URLEncoder.encode("android_token", "UTF-8")
                            + "=" + URLEncoder.encode(userToken, "UTF-8");

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                JSONObject result = Utils.PostRequest(body,"http://" + Constants.Server.ADDRESS + "/stats/check/");
                if (result!=null){
                    try {
                        serverStats = new JSONObject((result.get("values").toString()));
                        connected = true;

                    } catch (JSONException e) {
                        e.printStackTrace();
                        connected = false;
                    }
                }else{connected = false;}

            }else{
                connected = false;
            }

        }

    }


    /**
     * Cleans up the database connection. Should be called when done with the database.
     * <p>
     * Does nothing if not connected.
     */
    void disconnect() {
        if (connected){
            serverStats = null;
            connected = false;
        }


    }

    @Nullable
    @Override
    public List<String> getFilenamesRunningStatistics() {
        return new ArrayList<String>(){};

    }

    @Override
    public List<String> getFilenamesRunningStatisticsGhosts() {
        return new ArrayList<String>(){};
    }

    @Nullable
    @Override
    public RunningStatistics getRunningStatisticsFromFilename(String filename) {

        return new RunningStatistics();
    }


    @Override
    public boolean saveRunningStatistics(RunningStatistics runningStatistics, long editTime) {

        return true;
    }

    @Override
    public boolean saveRunningStatisticsGhost(String filename) {

        return true;
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

        return true;
    }

    @Nullable
    @Override
    public AggregateRunningStatistics getAggregateRunningStatistics() {
        if (!connected) {
            return null;
        }
        try {
            String jsonAgg = "{ \"averageDistance\" : { \"distance\" : "+ serverStats.get("avg_distance") +"} " +
                             ", \"averageDuration\" : { \"secondsPassed\" : "+ serverStats.get("avg_duration")+
                             "} , \"averageHeartRate\" : { \"heartRate\" : " + serverStats.get("avg_heartrate") +
                             "} , \"averageRunSpeed\" : { \"speed\" : "+ serverStats.get("avg_speed") +
                             "} , \"numberOfRuns\" : " + serverStats.get("runs") +
                             " , \"totalDistance\" : { \"distance\" : "+ serverStats.get("tot_distance") +
                             "} , \"totalDuration\" : { \"secondsPassed\" : "+ serverStats.get("tot_duration") +"}}";
            return new Gson().fromJson(jsonAgg, AggregateRunningStatistics.class);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public boolean saveAggregateRunningStatistics(AggregateRunningStatistics aggregateRunningStatistics, long editTime) {
        setUserToken();
        if (!connected || userToken == "") {
            return false;
        }
        boolean isPosted = false;

        String body = null;
        try {

            body = URLEncoder.encode("android_token", "UTF-8")
                    + "=" + URLEncoder.encode(userToken, "UTF-8");
            body += "&" + URLEncoder.encode("avg_speed", "UTF-8")
                    + "=" + URLEncoder.encode(aggregateRunningStatistics.getAverageRunSpeed().getSpeed() + "", "UTF-8");
            body += "&" + URLEncoder.encode("avg_duration", "UTF-8")
                    + "=" + URLEncoder.encode(aggregateRunningStatistics.getAverageDuration().getSecondsPassed() + "", "UTF-8");
            body += "&" + URLEncoder.encode("tot_duration", "UTF-8")
                    + "=" + URLEncoder.encode(aggregateRunningStatistics.getTotalDuration().getSecondsPassed() + "", "UTF-8");
            body += "&" + URLEncoder.encode("avg_distance", "UTF-8")
                    + "=" + URLEncoder.encode(aggregateRunningStatistics.getAverageDistance().getDistance() + "", "UTF-8");
            body += "&" + URLEncoder.encode("tot_distance", "UTF-8")
                    + "=" + URLEncoder.encode(aggregateRunningStatistics.getTotalDistance().getDistance() + "", "UTF-8");
            body += "&" + URLEncoder.encode("runs", "UTF-8")
                    + "=" + URLEncoder.encode(aggregateRunningStatistics.getNumberOfRuns() + "", "UTF-8");
            body += "&" + URLEncoder.encode("avg_heartrate", "UTF-8")
                    + "=" + URLEncoder.encode(aggregateRunningStatistics.getAverageHeartRate().getHeartRate() + "", "UTF-8");
            body += "&" + URLEncoder.encode("edit_time", "UTF-8")
                    + "=" + URLEncoder.encode( editTime + "", "UTF-8");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        JSONObject response = Utils.PostRequest(body,"http://" + Constants.Server.ADDRESS + "/stats/update/");

        if (response!=null){
            isPosted = true;
        }else{
            isPosted=false;
        }
        return isPosted;
    }

    /**
     * @return time (in ms since EPOCH) the AggregateRunningStatistics file was last edited. 0 if file does not exist.
     */
    long getAggregateRunningStatisticsEditTime() {
        if (!connected) {
            return 0;
        }
        try {
            return (int)serverStats.get("edit_time");
        } catch (JSONException e) {
            e.printStackTrace();
            return 0;

        }
    }

    /**
     * Warning: really deleting the AggregateRunningStatistics will cause problems with server synchronization.
     * For legal reasons this will need to be implemented if this app goes public in the play store
     * It is recommended to just do {@link #saveAggregateRunningStatistics(AggregateRunningStatistics, long)} with an empty object.
     */
    @Override
    public boolean deleteAggregateRunningStatistics() {
        return true;
    }




}
