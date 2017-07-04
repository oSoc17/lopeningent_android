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

package com.dp16.runamicghent;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

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
import java.util.concurrent.TimeUnit;

/**
 * Created by Stiaan on 7/03/2017.
 * The WearComm class offers an interface with the android mobile device via bluetooth.
 * It sends and receives messages from the mobile device. The received message contains status updates from the mobile app (speed, time, running or not).
 * The sent messages are updates providing heartbeat data for the mobile app and requests for status updates
 */

public class WearComm extends WearableListenerService implements EventPublisher, EventListener, MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "commWithWear";

    //these objects need to be static for the correct execution of the wear app (otherwise problems will occur in onMessageReceived events)
    private static volatile Node mNode; // the connected device to send the message to

    //Api providing the necessary features for communicating with android wear device
    private static volatile GoogleApiClient mGoogleApiClient;

    //the countdownlatch is used to make sure a node has been found before messages are sent to it.
    private static CountDownLatch latch;

    /**
     * adding event listeners to the class
     */
    public void setEventListeners() {

        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.HEART_RESPONSE, this);
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.REQUEST_STATE_WEAR, this);
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.ON_STOP, this);
    }

    public static void setLatch(){
        latch = new CountDownLatch(1);
    }

    /**
     * forward messages received from the mobile device to the handler and if the API is not yet initialized
     * initialize it
     * @param messageEvent
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String event = messageEvent.getPath();
        byte[] message = messageEvent.getData();
        if (mGoogleApiClient == null) {
            Log.e(TAG, "no google api found when receiving message");
            WearComm.initializeApi(this);
        }

        Log.d(TAG, "Received: " + event);
        handleReceivedMessage(event, message);

    }

    /**
     * handle received message events
     * @param event
     * @param message
     */
    private void handleReceivedMessage(String event, byte[] message) {
        switch (event) {
            case ConstantsWatch.WearMessageTypes.START_RUN_MOBILE:
            case ConstantsWatch.WearMessageTypes.RUN_START_STATE_MESSAGE_MOBILE:
                startRun();
                Log.d(TAG, "START_RUN_MOBILE OR RUN_START_STATE_MESSAGE_MOBILE");
                break;
            case ConstantsWatch.WearMessageTypes.STOP_RUN_MOBILE:
                stopRun();
                Log.d(TAG, "STOP_RUN_MOBILE");
                break;
            case ConstantsWatch.WearMessageTypes.PAUSE_RUN_MOBILE:
                pauseRun();
                Log.d(TAG, "PAUSE_RUN_MOBILE");
                break;
            case ConstantsWatch.WearMessageTypes.NAVIGATE_LEFT:
                showNavigation(-1);
                Log.d(TAG, "NAVIGATE_LEFT");
                break;
            case ConstantsWatch.WearMessageTypes.NAVIGATE_RIGHT:
                showNavigation(1);
                Log.d(TAG, "NAVIGATE_RIGHT");
                break;
            case ConstantsWatch.WearMessageTypes.NAVIGATE_STRAIGHT:
                showNavigation(0);
                Log.d(TAG, "NAVIGATE_STRAIGHT");
                break;
            case ConstantsWatch.WearMessageTypes.NAVIGATE_UTURN:
                showNavigation(2);
                Log.d(TAG, "NAVIGATE_UTURN");
                break;
            case ConstantsWatch.WearMessageTypes.RUN_STATE_PAUSED_MESSAGE_MOBILE:
                EventBroker.getInstance().addEvent(ConstantsWatch.EventTypes.RUN_STATE_PAUSED_MOBILE, "", this);
                Log.d(TAG, "RUN_STATE_PAUSED_MOBILE");
                break;
            case ConstantsWatch.WearMessageTypes.TIME_UPDATE_MESSAGE_MOBILE:
                EventBroker.getInstance().addEvent(ConstantsWatch.EventTypes.TIME_MOBILE, Integer.parseInt(new String(message)) * 1000, this);
                Log.d(TAG, "TIME_UPDATE_MESSAGE_MOBILE");
                break;
            case ConstantsWatch.WearMessageTypes.SPEED_UPDATE_MESSAGE_MOBILE:
                EventBroker.getInstance().addEvent(ConstantsWatch.EventTypes.SPEED_MOBILE, new String(message), this);
                Log.d(TAG, "SPEED_UPDATE_MESSAGE_MOBILE");
                break;
            case ConstantsWatch.WearMessageTypes.DISTANCE_UPDATE_MESSAGE_MOBILE:
                EventBroker.getInstance().addEvent(ConstantsWatch.EventTypes.DISTANCE_MOBILE, new String(message), this);
                Log.d(TAG, "DISTANCE_UPDATE_MESSAGE_MOBILE");
                break;
            default:
                Log.e(TAG, "message not recognized");
        }
    }

    /**
     * Initialize the google API client and connect it
     * @param wearComm
     */
    private static void initializeApi(WearComm wearComm) {
        mGoogleApiClient = new GoogleApiClient.Builder(wearComm)
                .addApi(Wearable.API)
                .addConnectionCallbacks(wearComm)
                .addOnConnectionFailedListener(wearComm)
                .build();

        mGoogleApiClient.connect();
    }

    /**
     * publish event for showing the navigation arrow on the watch
     * @param i int indicating the direction (left = -1, forward = 0, right = 1)
     */
    private void showNavigation(int i) {
        EventBroker.getInstance().addEvent(ConstantsWatch.EventTypes.NAVIGATE, i, this);
    }

    //not used currently
    private void pauseRun() {
        EventBroker.getInstance().addEvent(ConstantsWatch.EventTypes.PAUSE_MOBILE, "", this);
    }

    private void stopRun() {

        EventBroker.getInstance().addEvent(ConstantsWatch.EventTypes.STOP_MOBILE, "", this);
    }

    private void startRun() {
        if(!WearActivity.isActive()){
            Intent intent = new Intent(this, WearActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        EventBroker.getInstance().addEvent(ConstantsWatch.EventTypes.START_MOBILE, "", this);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

        WearComm.resolveNode();
    }

    @Override
    public void onConnectionSuspended(int i) {
        //not implemented
    }

    /**
     * find the mobile device
     */
    private static void resolveNode() {
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient)
                .setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult nodes) {
                        for (Node node : nodes.getNodes()) {
                            Log.i(TAG, node.getDisplayName());
                            mNode = node;
                            latch.countDown();
                            //if a node has been found, the latch can count down, allowing messages to be sent
                        }
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //not implemented
    }
    public void disconnectApi(){
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * send a message to the mobile device
     * @param path string indicating the type of message
     * @param key string containing a value for the message
     */
    public void sendMessage(String path, String key) {

        try {
            //if the node has not yet been found, wait a while, the resolveNode method may still be running
            boolean countDownHappened = latch.await(1000, TimeUnit.MILLISECONDS);
            if(!countDownHappened){
                Log.e(TAG, "no node found");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        Log.d(TAG, "sending message: " +path);
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
                            } else Log.i(TAG, "Message succes");
                        }
                    }
            );
        } else Log.e(TAG, "Sending message failed");

    }

    public static void setGoogleApiClient(GoogleApiClient mGAC) {
        mGoogleApiClient = mGAC;
    }

    /**
     * handle events received from components of the wearable
     * @param eventType String indicating what event happened
     * @param message Object containing data for the event
     */
    @Override
    public void handleEvent(String eventType, Object message) {
        switch (eventType) {
            case ConstantsWatch.EventTypes.HEART_RESPONSE:
                //update mobile with current heartrate
                sendMessage(ConstantsWatch.WearMessageTypes.HEART_RATE_MESSAGE_WEAR, Integer.toString((int) message));
                break;
            case ConstantsWatch.EventTypes.REQUEST_STATE_WEAR:
                //send request for current state of mobile (time, runningState)
                sendMessage(ConstantsWatch.WearMessageTypes.REQUEST_STATE_MESSAGE_WEAR, "");
                break;
            case ConstantsWatch.EventTypes.ON_STOP:
                //stop the wearComm
                disconnectApi();
                EventBroker.getInstance().removeEventListener(this);
                break;
            default:
                break;
        }
    }
}

