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

import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;
import android.preference.PreferenceManager;

import com.dp16.runamicghent.Activities.RunningScreen.RunningActivity;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.RunData.RunAudio;
import com.dp16.runamicghent.RunData.RunDirection;
import com.dp16.runamicghent.RunData.RunDistance;
import com.dp16.runamicghent.RunData.RunRoute;
import com.dp16.runamicghent.RunData.RunRoutePoint;
import com.dp16.runamicghent.TrackRequest;
import com.dp16.runamicghent.TrackResponse;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The purpose of this class is to provide navigation directions at the appropriate time.
 * This class listens for location events and checks them with the route.
 * If it is time to send a direction, a NAVIGATION_DIRECTION event is published
 * with as payload a RunDirection object. This object tells the listener what direction he should go.
 *
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#TRACK_REQUEST}, {@link com.dp16.runamicghent.Constants.EventTypes#TRACK_LOADED}, {@link com.dp16.runamicghent.Constants.EventTypes#AUDIO}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#LOCATION}, {@link com.dp16.runamicghent.Constants.EventTypes#TRACK}
 * </p>
 *
 * Created by hendrikdepauw on 03/04/2017.
 */

public class RouteEngine implements EventListener, EventPublisher {
    private LatLng currentLocation;

    private RunRoute[] routeList = new RunRoute[2]; //this route list will not be longer than 2 items, only the current route and the dynamic route are saved
    private int progress;//progress of the current route that we are running
    private int offset;//offset in the route, this attribute will be used if we want to look to the future instructions

    private int onRoute ;//this variable points to the element in the routeList where we are currently on
    private int notOnRoute ;//this variable points to the element in the routeList where we are not currently on

    private boolean onSplitPoint = false;//this boolean will be set to false if a new dynamic route comes in and we have not found the splitpoint
    private int splitPoint = 0;

    private int requestNumber;
    private boolean newRoute = false;

    private RunningActivity activity;
    private ExecutorService worker;

    private static final int NAVIGATION_RADIUS = 30;
    private static final int FUTURE_INSTRUCTIONS = 20;

    public enum DynamicRouteType {LONGER, SHORTER, HOME}

    public RouteEngine(RunningActivity activity) {
        this.activity = activity;

        // Make worker thread
        worker = Executors.newSingleThreadExecutor();
    }

    public void start() {
        EventBroker.getInstance().addEventListener(Constants.EventTypes.TRACK, this);
    }

    public void startRunning() {
        progress = 0;

        EventBroker.getInstance().addEventListener(Constants.EventTypes.LOCATION, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.ABNORMAL_HEART_RATE, this);
    }

    public void stop() {
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.LOCATION, this);
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.ABNORMAL_HEART_RATE, this);
    }

    @Override
    public void handleEvent(String eventType, Object message) {
        switch (eventType){
            case Constants.EventTypes.LOCATION:
                if (progress < routeList[onRoute].getRoute().size()) {
                    currentLocation = (LatLng) message;
                    worker.submit(new Worker());
                }
                break;
            case Constants.EventTypes.TRACK:
                // When a track is received, its response number is compared with the request number.
                // If these are equal, the track is displayed on the map and a TRACK_LOADED event is published
                if (((TrackResponse) message).getResponseNumber() == requestNumber) {
                    if(((TrackResponse) message).getDynamic()){
                        /* A route alternative was requested and has now arrived.
                         It is stored in runRouteAlternative. This should become the main runRoute
                         when the user decides to follow it. Otherwise it should be discarded.
                         When multiple route alternatives arrive, only the newest one is kept.
                         */
                        RunRoute dynamicRoute = new RunRoute(((TrackResponse) message).getTrack());
                        routeList[notOnRoute] = dynamicRoute;
                        removeEqualStart();
                        activity.getMapRunningFragment().setSecondaryRoute(routeList[notOnRoute].getRouteCoordinates().subList(splitPoint, routeList[notOnRoute].getRouteCoordinates().size() - 1));
                        activity.getMapRunningFragment().displaySecondaryRoute();
                        newRoute = true;
                        onSplitPoint = false;
                    } else {
                        // A normal route was requested.
                        onRoute = 0;
                        notOnRoute = 1;
                        offset = 0;
                        RunRoute route = new RunRoute(((TrackResponse) message).getTrack());
                        routeList[onRoute] = route;

                        activity.getMapRunningFragment().setRoute(routeList[onRoute].getRouteCoordinates());
                        activity.getMapRunningFragment().displayRoute();
                        activity.setRunRoute(routeList[onRoute]);

                        EventBroker.getInstance().addEvent(Constants.EventTypes.TRACK_LOADED, null, this);
                    }
                }
                break;
            case Constants.EventTypes.ABNORMAL_HEART_RATE:
                // Request dynamic change to route based on the TAG the heart rate checker sends
                requestTrackDynamic(message.equals(Constants.DynamicRouting.TAG_UPPER) ? RouteEngine.DynamicRouteType.SHORTER : RouteEngine.DynamicRouteType.LONGER);
                break;
            default:
                break;
        }
    }

    /**
     * Whenever a new dynamic route comes in, we want to remove the overlapping part from it. This method will take care of that.
     */
    private void removeEqualStart(){
        //TODO chance this method
        boolean equal = true;
        int iterator = 0;
        float[] distance = new float[1];
        splitPoint = 0;

        float threshold = 0.000001f;

        while(equal){
            //if instructions are similar, then remove this instruction from the list
            RunRoutePoint onRoutePoint = routeList[onRoute].getRoute().get(iterator);
            RunRoutePoint dynamicRoutePoint = routeList[notOnRoute].getRoute().get(iterator);
            Location.distanceBetween(onRoutePoint.getLocation().latitude, onRoutePoint.getLocation().longitude,
                    dynamicRoutePoint.getLocation().latitude, dynamicRoutePoint.getLocation().longitude,
                    distance);
            if(distance[0] < threshold && onRoutePoint.getDirection().getDirection() == dynamicRoutePoint.getDirection().getDirection()){
                splitPoint++;
                iterator++;
            }else{
                equal = false;
                Log.d("ROUTEENGINE", "EQUAL POINT FOUND");
            }
        }
    }


    public void requestTrackStatic(LatLng location){
       SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        Boolean timeChecked = preferences.getBoolean("Time",false);
        double km;
        if (!timeChecked){

            km = Double.parseDouble(preferences.getString("distanceValue", "5"));
        }
        else {
            //time values
            String time = preferences.getString("timeValue", "00:10");
            int h = Integer.parseInt(time.substring(0,2));
            int m = Integer.parseInt(time.substring(3,5)) + (h/60) ;

            //difficulty
            int difficulty = preferences.getInt("difficulty", 0);
            switch (difficulty){
                case 0: km = (Constants.RouteGenerator.BEGINNER_SPEED/60) *m;
                    break;
                case 1: km = (Constants.RouteGenerator.AVERAGE_SPEED/60) *m;
                    break;
                case 2: km = (Constants.RouteGenerator.EXPERT_SPEED/60) *m;
                    break;
                default: km = (Constants.RouteGenerator.AVERAGE_SPEED/60) *m;
            }

        }
        RunDistance distance = new RunDistance((int) (km*1000));
        requestTrack(location,distance,false,"");
        /*RunDistance distance = new RunDistance(PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getInt("pref_key_routing_length", Constants.RouteGenerator.DEFAULT_LENGTH));
        requestTrack(location, distance, false, "");*/
    }

    public void requestTrackDynamic(DynamicRouteType dynamicRouteType){
        RunRoute route = routeList[onRoute];
        double currentDistance = route.getRouteLength().getDistance();
        RunDistance distance;

        switch (dynamicRouteType){
            case SHORTER:
                distance = new RunDistance((int) (currentDistance * 0.8));
                break;
            case LONGER:
                distance = new RunDistance((int) (currentDistance * 1.2));
                break;
            case HOME:
            default:
                distance = new RunDistance(0);
                break;
        }

        LatLng loc;
        RunRoutePoint point = route.getRoute().get(progress+1);
        loc = point.getLocation();
        requestTrack(loc, distance, true, route.getTag());
    }

    public void requestTrackDynamicTime(double avgSpeed){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        //time values
        String time = preferences.getString("timeValue", "00:10");
        int h = Integer.parseInt(time.substring(0,2));
        int m = Integer.parseInt(time.substring(3,5)) + (h/60) ;


        //distance
        RunRoute route = routeList[onRoute];
        RunDistance distance;
        //Calc
            distance = new RunDistance((int) ((avgSpeed/60)*m));

        LatLng loc;
        RunRoutePoint point = route.getRoute().get(progress+1);
        loc = point.getLocation();
        requestTrack(loc, distance, true, route.getTag());
    }

    private void requestTrack(LatLng location, RunDistance distance, boolean dynamic, String tag){
        // Create new TrackRequest, request number will be generated by TrackRequest
        TrackRequest trackRequest = new TrackRequest(location, distance, dynamic, tag);
        requestNumber = trackRequest.getRequestNumber();
        // Set request number of MapRunningFragment
        EventBroker.getInstance().addEvent(Constants.EventTypes.TRACK_REQUEST, trackRequest, this);
    }

    private class Worker implements Runnable, EventPublisher {
        /**
         * Checks if the current location is within a certain radius of the next instruction.
         * This method of checking is not ideal for several reasons:
         * 1. If the GPS coordinates are very inaccurate, an instruction point might be skipped.
         * This prevents any other instructions from being loaded because the algorithm is still
         * looking for that location that is already passed.
         * 2. Multiple instructions can be very near each other. If only one location
         * is in the radius of both instructions, one will be missed and the algorithm is stuck.
         * Probably more reasons can be found. But it is the best I could think of. It is slightly
         * inspired on: https://github.com/mapzen/open/blob/master/src/main/java/com/mapzen/open/route/RouteEngine.java
         * This is a class that does exactly what we want this thing to do. I just don't know how.
         * Grtz Hendrik.
         *
         * New problem: above is fixed but a new problem arrised where the Uturn can cause a stop in the rundirections
         * if it is skipped. This is because we need to wait to pass the UTURN otherwise we could have two directions at
         * the same time if we need to take the same rout multiple times
         * Grtz Maxim
         */
        @Override
        public void run() {
            // Get the distance between the next instruction point and the current location.
            float[] distanceToNextInstruction = new float[1];
            offset = 0;
            boolean stop = false;
            if(newRoute && onSplitPoint){
                decidePath(); // this method will set the onRoute variable the closest route to the user
            }
            while(decideStoppingCriteria() && !stop){
                if(newRoute && !onSplitPoint){
                    checkOnSplitPoint();
                }

                calculateDistances(distanceToNextInstruction);
                // If they are close enough, publish a NAVIGATION_DIRECTION event.
                if (!onSplitPoint && distanceToNextInstruction[0] < NAVIGATION_RADIUS) {
                    RunDirection.Direction direction = routeList[onRoute].getRoute().get(progress + offset).getDirection().getDirection();
                    if(direction!= RunDirection.Direction.NONE)//we do not want to overload the eventbroker with NONE requests that are doing nothing
                        publishRunAudioEvent(new RunAudio(routeList[onRoute].getRoute().get(progress + offset).getDirection()));
                    progress = progress + offset + 1;
                    stop = true;
                }else if(onSplitPoint){
                    progress = progress + offset;
                    stop = true;
                }else if(routeList[onRoute].getRoute().get(progress + offset).getDirection().getDirection() == RunDirection.Direction.UTURN){
                    stop = true;
                }else {
                    offset++;
                }
            }

        }

        private void calculateDistances(float[] distanceToNextInstruction){
            Location.distanceBetween(currentLocation.latitude, currentLocation.longitude,
                    routeList[onRoute].getRoute().get(progress + offset).getLocation().latitude, routeList[onRoute].getRoute().get(progress + offset).getLocation().longitude,
                    distanceToNextInstruction);
        }

        private boolean decideStoppingCriteria(){
            return progress <= routeList[onRoute].getRoute().size() && offset < FUTURE_INSTRUCTIONS;
        }

        /**
         * This method decides on which route the runner is currently on
         * Do not call this method if no dynamic route has been added
         */
        private void decidePath(){
            LatLng p = new LatLng(currentLocation.latitude, currentLocation.longitude);
            int index = progress;

            //if the distance gets to big, then we need to look further because no distinct could be made if the two routes are to close to each other
            //this can be done by looking to further route lines

            double distanceCurrentRoute = Double.MAX_VALUE;
            double distanceSecondRoute = Double.MAX_VALUE;
            int offset1 = 0;
            int offset2 = 0;
            int i = 0;
            while(i<5 && (index+i+1)<routeList[onRoute].getRoute().size()){
                double hulp1 = distanceToLine(p,routeList[onRoute].getRoute().get(index + i).getLocation(),routeList[onRoute].getRoute().get(index + i +1).getLocation());
                double hulp2 = distanceToLine(p,routeList[notOnRoute].getRoute().get(index + i).getLocation(), routeList[notOnRoute].getRoute().get(index + i + 1).getLocation());
                if(hulp1<distanceCurrentRoute){
                    distanceCurrentRoute = hulp1;
                    offset1 = i;
                }
                if(hulp2<distanceSecondRoute){
                    distanceSecondRoute = hulp2;
                    offset2 = i;
                }
                i++;
            }

            if(distanceCurrentRoute - distanceSecondRoute>NAVIGATION_RADIUS){//runner swtichs to dynamic route
                int hulp = onRoute;
                onSplitPoint = false;//now we know which route has been taken, runner is no longer on split point
                onRoute = notOnRoute;
                notOnRoute = hulp;
                newRoute = false;
                progress = index + offset2;
                activity.getMapRunningFragment().setRoute(routeList[onRoute].getRouteCoordinates());
                activity.getMapRunningFragment().displayRoute();
                activity.getWhileRunningFragment().getRouteTotalText().setText("/ " + routeList[onRoute].getRouteLength().toString());
            }else if(distanceSecondRoute - distanceCurrentRoute>NAVIGATION_RADIUS) {//runner stays on primary route, no swap needed of route
                newRoute = false;
                onSplitPoint = false;
                progress = index + offset1;
                activity.getMapRunningFragment().setRoute(routeList[onRoute].getRouteCoordinates());
                activity.getMapRunningFragment().displayRoute();
                activity.getWhileRunningFragment().getRouteTotalText().setText(routeList[onRoute].getRouteLength().toString());
            }
        }

        /**
         * this method checks if the runner is on a split point
         */
        private void checkOnSplitPoint(){
            RunRoutePoint pointNew = routeList[notOnRoute].getRoute().get(splitPoint);
            float[] distance = new float[1];
            Location.distanceBetween(currentLocation.latitude, currentLocation.longitude,
                    pointNew.getLocation().latitude, pointNew.getLocation().longitude,
                    distance);
            if(distance[0]<NAVIGATION_RADIUS) {//if this is true, then we are on a split point
                ArrayList<RunDirection> directions = new ArrayList<>();
                directions.add(pointNew.getDirection());
                onSplitPoint = true;
                progress = splitPoint;

                publishRunAudioEvent(new RunAudio(routeList[onRoute].getRoute().get(splitPoint).getDirection(), pointNew.getDirection()));
            }
        }

        private double distanceToLine(LatLng p, LatLng start, LatLng end){
            return PolyUtil.distanceToLine(p,start,end);
        }

        private void publishRunAudioEvent(RunAudio runAudio){
            if(PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()).getBoolean("pref_key_audio_directions", true)){
                EventBroker.getInstance().addEvent(Constants.EventTypes.AUDIO, runAudio, this);
            }
        }

    }
}
