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

package com.dp16.runamicghent.DataProvider;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.RunData.RunAudio;
import com.dp16.runamicghent.RunData.RunDistance;
import com.dp16.runamicghent.RunData.RunDuration;
import com.dp16.runamicghent.RunData.RunHeartRate;
import com.dp16.runamicghent.RunData.RunSpeed;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This dataprovider offers an interface with an android wearable via bluetooth.
 * It sends and receives messages from the android wear device. The received message contains heartrates or requests for the state of the mobile app.
 * The sent messages are updates providing data to be displayed on the wearable.
 * Created by Stiaan on 7/03/2017.
 */

public class AndroidWearProvider extends WearableListenerService implements EventListener, DataProvider, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, EventPublisher {

    protected Activity activity;
    private static final String TAG = "androidWearProvider";

    //the countdownlatch is used to make sure a node has been found before messages are sent to it.
    private static CountDownLatch latch = new CountDownLatch(1);

    //Api providing the necessary features for communicating with android wear device
    private static GoogleApiClient mGoogleApiClient;

    //these objects need to be static for the correct execution of the wear app (otherwise problems will occur in onMessageReceived events)
    private static volatile Node mNode; //the android wear device
    private static volatile RunDuration duration = new RunDuration(); //the current duration of the run for updating the wearable
    private static volatile boolean runningState = false; // state in which the user is (running or not)
    private static volatile double currentSpeed = -1; //current speed for updating wearable
    private static volatile String currentDistanceRan = "0 m"; //current distance for updating wearable

    //worker thread for handling events for androidWearProvider
    private ExecutorService worker;

    public AndroidWearProvider() {
        //default constructor is required for android manifest
    }

    public AndroidWearProvider(Activity activity) {

        this.activity = activity;

        // Make worker thread
        worker = Executors.newSingleThreadExecutor();

        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public static void setRunningState(boolean runningState) {
        AndroidWearProvider.runningState = runningState;
    }

    public static void setDuration(RunDuration duration) {
        AndroidWearProvider.duration = duration;
    }

    public static void setCurrentSpeed(double currentSpeed) {
        AndroidWearProvider.currentSpeed = currentSpeed;
    }

    public static void setCurrentDistanceRan(String currentDistanceRan) {
        AndroidWearProvider.currentDistanceRan = currentDistanceRan;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //find the android wear device
        AndroidWearProvider.resolveNode();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Not implemented
        Log.d(TAG, "connection suspended");
    }

    /**
     * Finding the wearable device.
     */
    private static void resolveNode() {

        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient)
                .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                        for (Node node : nodes.getNodes()) {
                            Log.d(TAG, node.getDisplayName());
                            mNode = node;
                            //if a node has been found, the latch can count down, allowing messages to be sent
                            latch.countDown();
                        }
                    }
                });

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Not implemented
        Log.e(TAG, "could not connect to google API");
    }

    /**
     * Sending a message to the node.
     *
     * @param path message path
     * @param key  value to be sent paired with the path
     */
    public void sendMessage(String path, String key) {

        //if the node has not yet been found, wait for a second for the resolve node method
        //if after a second still no node has been found, no node will be found.
        if (mNode == null) {
            Log.e(TAG, "node not yet found, waiting for response");
            try {
                boolean countDownhappened = latch.await(1000, TimeUnit.MILLISECONDS);
                if (mNode == null || !countDownhappened)
                    Log.d(TAG, "No node was found");
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }

        Log.d(TAG, "sending message: " + path);
        if (mNode != null && mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(TAG, "-- " + mGoogleApiClient.isConnected());
            Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, mNode.getId(), path, key.getBytes()).setResultCallback(

                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {

                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e(TAG, "Failed to send message with status code: "
                                        + sendMessageResult.getStatus().getStatusCode());
                            } else Log.d(TAG, "Message succes");
                        }
                    }
            );
        } else Log.e(TAG, "Message not sent, failed to find an android wear device");

    }

    /**
     * handle a message received from the wearable
     *
     * @param messageEvent Object containing message path and data
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        String event = messageEvent.getPath();
        byte[] message = messageEvent.getData();

        Log.d(TAG, "Received: " + event);

        switch (event) {
            case Constants.WearMessageTypes.HEART_RATE_MESSAGE_WEAR:
                Log.e(TAG, new String(message));
                EventBroker.getInstance().addEvent(Constants.EventTypes.HEART_RESPONSE, new RunHeartRate(Integer.parseInt(new String(message))), this);
                break;
            case Constants.WearMessageTypes.REQUEST_STATE_MESSAGE_WEAR:
                checkAndHandleState();
                break;
            default:
                Log.e(TAG, "Message not recognized");
                break;
        }
    }

    /**
     * method that checks in which state the runner is currently and sends a message containing that state to the wearable
     * It notifies the wearable of the current time and distance of the run
     */
    private void checkAndHandleState() {
        if (runningState)
            sendMessage(Constants.WearMessageTypes.RUN_STATE_START_MESSAGE_MOBILE, "");
        else
            sendMessage(Constants.WearMessageTypes.RUN_STATE_PAUSED_MESSAGE_MOBILE, "");

        sendMessage(Constants.WearMessageTypes.TIME_UPDATE_MESSAGE_MOBILE, Integer.toString(duration.getSecondsPassed()));
        sendMessage(Constants.WearMessageTypes.DISTANCE_UPDATE_MESSAGE_MOBILE, currentDistanceRan);
    }

    /**
     * submit a received event to a worker
     *
     * @param eventType String containing the type of the event
     * @param message   Object containing a value paired with the event if there is one, else an empty string
     */
    @Override
    public void handleEvent(String eventType, Object message) {

        Runnable task = new Worker(eventType, message);
        worker.submit(task);

    }

    /**
     * Connect to the Google Api
     */
    public void connectApi() {
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Disconnect from the Google Api
     */
    public void disconnectApi() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

    }

    /**
     * initialize the AndroidWearProvider by connecting to the api, resetting the latch and adding the listeners
     */
    @Override
    public void start() {
        //make sure the latch will wait for connection with the node
        AndroidWearProvider.resetLatch();
        connectApi();
        EventBroker.getInstance().addEventListener(Constants.EventTypes.START_WEAR, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.STOP_WEAR, this);
       // EventBroker.getInstance().addEventListener(Constants.EventTypes.NAVIGATION_DIRECTION, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.AUDIO, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.DURATION, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.SPEED, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.DISTANCE, this);
    }

    /**
     * Reset the latch so that when a new node has to be found, make sure the latch will wait for connection with that node
     */
    private static void resetLatch() {
        latch = new CountDownLatch(1);
    }

    /**
     * pause the AndroidWearProvider
     */
    @Override
    public void pause() {
        AndroidWearProvider.setRunningState(false);
        sendMessage(Constants.WearMessageTypes.PAUSE_RUN_MOBILE, "");
        EventBroker.getInstance().removeEventListener(this);
    }

    /**
     * stop the androidWearProvider
     */
    @Override
    public void stop() {
        AndroidWearProvider.setRunningState(false);
        AndroidWearProvider.setCurrentDistanceRan("0 m");
        AndroidWearProvider.setCurrentSpeed(-1);
        AndroidWearProvider.setDuration(new RunDuration());
        sendMessage(Constants.WearMessageTypes.STOP_RUN_MOBILE, "");
        AndroidWearProvider.resetStaticValues();
        EventBroker.getInstance().removeEventListener(this);
        disconnectApi();
    }


    /**
     * reset the static values to prevent wrong static values to be kept on stop
     */
    private static void resetStaticValues() {


        mNode = null;
    }

    /**
     * Resume androidWearProvider functions
     */
    @Override
    public void resume() {
        EventBroker.getInstance().addEventListener(Constants.EventTypes.START_WEAR, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.STOP_WEAR, this);
        //EventBroker.getInstance().addEventListener(Constants.EventTypes.NAVIGATION_DIRECTION, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.AUDIO, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.DURATION, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.SPEED, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.DISTANCE, this);

        AndroidWearProvider.setRunningState(true);
        sendMessage(Constants.WearMessageTypes.START_RUN_MOBILE, "");
        sendMessage(Constants.WearMessageTypes.TIME_UPDATE_MESSAGE_MOBILE, Integer.toString(duration.getSecondsPassed()));
        sendMessage(Constants.WearMessageTypes.DISTANCE_UPDATE_MESSAGE_MOBILE, currentDistanceRan);
    }

    /**
     * The worker class handling events for the wearable in a separate thread
     */
    private class Worker implements Runnable, EventPublisher {
        String eventType;
        Object message;

        /**
         * add an event to the worker
         *
         * @param eventType String with type of event
         * @param message   Object containing data paired with the event
         */
        public Worker(String eventType, Object message) {
            this.eventType = eventType;
            this.message = message;
        }

        /**
         * handle events submitted to the worker
         */
        @Override
        public void run() {
            switch (eventType) {
                case Constants.EventTypes.START_WEAR:
                    //tell the wearable the run has started
                    AndroidWearProvider.setRunningState(true);
                    sendMessage(Constants.WearMessageTypes.START_RUN_MOBILE, "");
                    break;
                case Constants.EventTypes.STOP_WEAR:
                    //tell the wearable the run has stopped
                    sendMessage(Constants.WearMessageTypes.STOP_RUN_MOBILE, "");
                    AndroidWearProvider.setRunningState(false);
                    break;
                case Constants.EventTypes.PAUSE_WEAR:
                    //tell the wearable the run has paused
                    sendMessage(Constants.WearMessageTypes.PAUSE_RUN_MOBILE, "");
                    AndroidWearProvider.setRunningState(false);
                    break;
                /*case Constants.EventTypes.NAVIGATION_DIRECTION:
                    sendNavigationMessage((RunDirection) message);
                    break;*/
                case Constants.EventTypes.AUDIO:
                    sendNavigationMessage((RunAudio) message);
                    break;
                case Constants.EventTypes.DURATION:
                    //update the current time for when the wearable requests it
                    AndroidWearProvider.setDuration((RunDuration) message);
                    break;
                case Constants.EventTypes.SPEED:
                    updateRunSpeed(message);
                    break;
                case Constants.EventTypes.DISTANCE:
                    updateDistanceRan(message);
                    break;
                default:
                    Log.e(TAG, "Event not recognized");
                    break;
            }
        }

        /**
         * this class will notify the wearable of the direction the user has to take by extracting
         * the direction from the message that was received in the event from the RouteEngine
         *
         * @param message RunDirection object containing a direction
         */
        private void sendNavigationMessage(RunAudio message) {

            if ("turn left".equals(message.getAudioString())) {
                //tell the wearable to show navigation arrow left
                sendMessage(Constants.WearMessageTypes.NAVIGATE_LEFT, "");
            } else if ("turn right".equals(message.getAudioString())) {
                //tell the wearable to show navigation arrow right
                sendMessage(Constants.WearMessageTypes.NAVIGATE_RIGHT, "");
            } else if ("please turnaround".equals(message.getAudioString())) {
                //tell the wearable to show navigation arrow right
                sendMessage(Constants.WearMessageTypes.NAVIGATE_UTURN, "");
            }
        }

        /**
         * this method will inform the wearable of the current running speed if this speed differs
         * from the last update by at least 0.5
         *
         * @param message Object that can be cast to RunSpeed
         */
        private void updateRunSpeed(Object message) {
            RunSpeed speed = (RunSpeed) message;
            if (Math.abs(speed.getSpeed() - currentSpeed) > 0.5) {
                AndroidWearProvider.setCurrentSpeed(speed.getSpeed());
                sendMessage(Constants.WearMessageTypes.SPEED_UPDATE_MESSAGE_MOBILE, speed.toString(activity));
            }
        }

        private void updateDistanceRan(Object message) {
            RunDistance distance = (RunDistance) message;
            AndroidWearProvider.setCurrentDistanceRan(distance.toString());
            sendMessage(Constants.WearMessageTypes.DISTANCE_UPDATE_MESSAGE_MOBILE, distance.toString());
        }

    }
}
