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

package com.dp16.runamicghent.StatTracker;

import android.app.Activity;
import android.preference.PreferenceManager;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.DataProvider.AndroidWearProvider;
import com.dp16.runamicghent.DataProvider.DataProvider;
import com.dp16.runamicghent.DataProvider.DistanceProvider;
import com.dp16.runamicghent.DataProvider.MockUp.HeartBeatProviderMock;
import com.dp16.runamicghent.DataProvider.SpeedProvider;
import com.dp16.runamicghent.DataProvider.TimingProvider;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.RunData.RunAudio;
import com.dp16.runamicghent.RunData.RunDistance;
import com.dp16.runamicghent.RunData.RunDuration;
import com.dp16.runamicghent.RunData.RunHeartRate;
import com.dp16.runamicghent.RunData.RunSpeed;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class collects statistics while running and stores them in a RunningStatistics object.
 *
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#STORE_RUNNINGSTATISTICS}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#SPEED}, {@link com.dp16.runamicghent.Constants.EventTypes#LOCATION}, {@link com.dp16.runamicghent.Constants.EventTypes#DISTANCE}
 * </p>
 *
 * Created by hendrikdepauw on 07/03/2017.
 */

public class StatTracker implements EventListener, EventPublisher {

    private static final String TAG = "StatTracker";

    private RunningStatistics runningStatistics;
    private ExecutorService worker;

    private ArrayList<DataProvider> dataProviders;
    private TimingProvider timingProvider;
    private Activity activity;
    private boolean canStillEdit;
    private long startTimePause;

    private RunDuration lastRunDuration;

    private boolean distanceFeedback;
    private int distanceFeedbackInterval;
    private int nextDistanceFeedback;
    private boolean durationFeedback;
    private int durationFeedbackInterval;
    private int nextDurationFeedback;

    public StatTracker(Activity activity) {
        dataProviders = new ArrayList<>();
        canStillEdit = true;
        lastRunDuration = new RunDuration(0);

        this.activity = activity;

        //Add all the dataproviders to a list.
        dataProviders.add(new DistanceProvider());
        dataProviders.add(new SpeedProvider());

        // Decide whether to use real heartbeatprovider or the mock based on preferences. By default the real one is chosen.
        if (PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean("pref_key_debug_heartbeat_mock", false)) {
            dataProviders.add(new HeartBeatProviderMock());
        } else {
            dataProviders.add(new AndroidWearProvider(activity));
        }

        //Timing provider is special in the sense that it returns a value when stopped. This is why it is not kept in the list.
        timingProvider = new TimingProvider();

        // Initialize everything for feedback
        distanceFeedback = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean("pref_key_audio_feedback_distance", true);
        distanceFeedbackInterval = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getInt("pref_key_audio_feedback_distance_interval", 1) * 1000; // In meters
        nextDistanceFeedback = distanceFeedbackInterval;
        durationFeedback = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean("pref_key_audio_feedback_duration", false);
        durationFeedbackInterval = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getInt("pref_key_audio_feedback_duration_interval", 1) * 60; // In seconds
        nextDurationFeedback = durationFeedbackInterval;

        //Make worker thread
        worker = Executors.newSingleThreadExecutor();
    }

    public void startStatTracker() {
        //Register at the eventbroker
        subscribeToEventBroker();

        runningStatistics = new RunningStatistics();

        //Start all dataproviders
        timingProvider.start();
        for (DataProvider i : dataProviders) {
            i.start();
        }
    }

    /**
     * Stops the statTracker and adds total runDuration to the runningStatistics.
     * Beware, this method does not store the runningstatistics.
     * To do this you have to call saveRunningStatistics().
     * I think it is save to call this method more than once.
     */
    public void stopStatTracker() {
        //Unsubscribe from the EventBroker
        unsubscribeFromEventBroker();

        //Stop timingprovider and get the runduration
        timingProvider.stop();
        if (runningStatistics != null) {
            runningStatistics.addRunDuration(timingProvider.getRunDuration());
        }

        //Stop all other dataproviders
        for (DataProvider i : dataProviders) {
            i.stop();
        }
    }

    public void resumeStatTracker() {
        //Register at the eventbroker
        subscribeToEventBroker();

        //Update the starttime of the runningstatistics
        runningStatistics.includePause(System.currentTimeMillis() - startTimePause);

        //Resume all dataproviders
        timingProvider.resume();
        for (DataProvider i : dataProviders) {
            i.resume();
        }
    }

    public void pauseStatTracker() {
        //Unregister from the eventbroker
        unsubscribeFromEventBroker();

        startTimePause = System.currentTimeMillis();

        //Pause all dataproviders
        timingProvider.pause();
        for (DataProvider i : dataProviders) {
            i.pause();
        }
    }

    /**
     * This method subscribes to all events in the eventbroker
     */
    private void subscribeToEventBroker() {
        EventBroker.getInstance().addEventListener(Constants.EventTypes.LOCATION, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.SPEED, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.HEART_RESPONSE, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.DISTANCE, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.DURATION, this);
    }

    /**
     * This method unsubscribes from all events in the eventbroker
     */
    private void unsubscribeFromEventBroker() {
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.LOCATION, this);
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.SPEED, this);
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.HEART_RESPONSE, this);
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.DISTANCE, this);
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.DURATION, this);
    }

    /**
     * This method does three things:
     * 1. Save the runningStatistics locally
     * 2. Save the runningStatistics on the server
     * 3. Add the runningStatistics to the AggregatedRunningStatistics
     */
    public void saveRunningStatistics() {
        //Can not edit RunningStatistics anymore
        canStillEdit = false;

        //Send runningStatistics to persistence component
        EventBroker.getInstance().addEvent(Constants.EventTypes.STORE_RUNNINGSTATISTICS, runningStatistics, this);

        /*
         * Pass the runningStatistics to the AggregateRunningStatistics
         * This method can take some time as it has to wait until the AggregateRunningStatistics
         * are loaded from memory.
         */
        AggregateRunningStatisticsHandler aggregateRunningStatisticsHandler = new AggregateRunningStatisticsHandler();
        aggregateRunningStatisticsHandler.addRunningStatistics(runningStatistics);
    }

    // Returns RunningStatistics object that corresponds to this stattracker session
    public RunningStatistics getRunningStatistics() {
        return runningStatistics;
    }

    /**
     * Add a rating to the runningstatistics. Can only do so if they are not written to a file yet.
     * Maybe throw an exception if this is the case?
     *
     * @param rating rating to be added.
     */
    public void addRating(double rating) {
        if (canStillEdit) {
            runningStatistics.addRating(rating);
        }
    }

    /**
     * Called when an event enters the StatTracker.
     * Passes work to the worker thread.
     *
     * @param eventType
     * @param message
     */
    @Override
    public void handleEvent(String eventType, Object message) {
        Runnable task = new StatTracker.Worker(eventType, message);
        worker.submit(task);
    }

    private class Worker implements Runnable, EventPublisher {
        private final String eventType;
        private final Object message;

        public Worker(String eventType, Object message) {
            this.eventType = eventType;
            this.message = message;
        }

        @Override
        public void run() {
            /*
             * Determine the type of message received and store it in runningStatistics
             */
            switch (eventType) {
                case Constants.EventTypes.LOCATION:
                    runningStatistics.addLocation((LatLng) message);
                    break;
                case Constants.EventTypes.SPEED:
                    runningStatistics.addSpeed((RunSpeed) message);
                    break;
                case Constants.EventTypes.HEART_RESPONSE:
                    runningStatistics.addHeartrate((RunHeartRate) message);
                    break;
                case Constants.EventTypes.DISTANCE:
                    runningStatistics.addDistance((RunDistance) message);
                    if(distanceFeedback){
                        checkForFeedbackDistance((RunDistance) message);
                    }
                    break;
                case Constants.EventTypes.DURATION:
                    lastRunDuration = (RunDuration) message;
                    if(durationFeedback){
                        checkForFeedbackDuration((RunDuration) message);
                    }
                    break;
                default:
                    break;
            }
        }

        private void checkForFeedbackDistance(RunDistance runDistance){
            if(runDistance.getDistance() > nextDistanceFeedback){
                nextDistanceFeedback += distanceFeedbackInterval;
                publishFeedback();
            }
        }

        private void checkForFeedbackDuration(RunDuration runDuration){
            if(runDuration.getSecondsPassed() > nextDurationFeedback){
                nextDurationFeedback += durationFeedbackInterval;
                publishFeedback();
            }
        }

        /**
         * This method publishes an AUDIO event giving feedback to the user about his statistics.
         * The user can choose in the preferences what he wants to hear.
         * All feedback is put in a RunAudio object that is then published.
         */
        private void publishFeedback(){
            String string = "";

            if(PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean("pref_key_audio_feedback_contents_distance", true)){
                string = string.concat(GuiController.getInstance().getContext().getString(R.string.audio_totaldistance) + runningStatistics.getTotalDistance().toAudioString() + ". ");
            }
            if(PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean("pref_key_audio_feedback_contents_duration", true)){
                string = string.concat(GuiController.getInstance().getContext().getString(R.string.audio_totalduration) + lastRunDuration.toAudioString() + ". ");
            }
            if(PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean("pref_key_audio_feedback_contents_avg_speed", true)){
                string = string.concat(GuiController.getInstance().getContext().getString(R.string.audio_average_speed) + runningStatistics.getAverageSpeed().toString(activity.getApplicationContext()) + ". ");
            }
            if(PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean("pref_key_audio_feedback_contents_avg_heartrate", false)){
                string = string.concat(GuiController.getInstance().getContext().getString(R.string.audio_average_heartrate) + runningStatistics.getAverageHeartRate().toString() + ". ");
            }

            RunAudio runAudio = new RunAudio(string);

            EventBroker.getInstance().addEvent(Constants.EventTypes.AUDIO, runAudio, this);
        }
    }
}
