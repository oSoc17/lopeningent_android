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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dp16.runamicghent.Activities.Utils;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.TrackRequest;
import com.dp16.runamicghent.TrackResponse;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class that allows fetching a JSONObject. In this case, the JSONObject should contain a generated route.
 * <p>
 * <p>
 * <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#STATUS_CODE}
 * </p>
 * <p>
 * <b>Messages Consumed: </b> , {@link com.dp16.runamicghent.Constants.EventTypes#TRACK_REQUEST}
 * </p>
 *
 * @see DataProvider
 * @see EventPublisher
 * @see EventListener
 * Created by Nick on 25-2-2017. (and Simon?)
 * Edited by Redouane Arroubai on 17-07-2017 (new API/ post requests)
 */
public class RouteProvider implements EventListener, EventPublisher, DataProvider {

    private ExecutorService worker;
    private Context context;

    /**
     * Get Singleton EventBroker. Start listening for TRACK_REQUESTS.
     */
    public RouteProvider(Context context) {
        worker = Executors.newSingleThreadExecutor();
        this.context = context;
        this.start();
    }

    @Override
    public void start() {
        EventBroker.getInstance().addEventListener(Constants.EventTypes.TRACK_REQUEST, this);
    }

    @Override
    public void stop() {
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.TRACK_REQUEST, this);
    }

    @Override
    public void resume() {
        start();
    }

    @Override
    public void pause() {
        stop();
    }

    /**
     * Handles the reception of TRACK_REQUEST events.
     *
     * @param eventType: should be of type Constants.EventTypes.TRACK_REQUEST.
     * @param message:   the (JSON) description of the requested track.
     */
    @Override
    public void handleEvent(String eventType, Object message) {
        Runnable task = new RouteProvider.Worker(message);
        worker.submit(task);
    }

    /**
     * Asynchronous fetching of a track from the server.
     * Once a result is fetched, a new TRACK event is published.
     * This event contains the fetched JSON track object.
     */
    private class Worker implements Runnable {
        private TrackResponse trackResponse;
        private TrackRequest trackRequest;
        private RouteProvider routeProvider;

        Worker(Object message) {
            this.trackRequest = (TrackRequest) message;
        }

        /**
         * Publishes a request for a specific track to the EventBroker.
         *
         * @param track: JSON representation of the received track.
         */
        private void publishEvent(JSONObject track) {
            trackResponse = new TrackResponse(track, trackRequest.getDynamic(), trackRequest.getRequestNumber());
            EventBroker.getInstance().addEvent(Constants.EventTypes.TRACK, trackResponse, this.routeProvider);
        }

        /*
          Open connection and fetch the route from the server.
          Once fetched, the location is published in a TRACK event tot the EventBroker.
         */
        @Override
        public void run() {

            // Construct the URL.
            String urlString = "";
            String body = "";

            try {
                if (trackRequest.getDynamic()) {
                    urlString = "http://" + Constants.Server.ADDRESS + "/route/return/";
                    body = constructDynamicBody();
                }else{
                    urlString = "http://" + Constants.Server.ADDRESS + "/route/generate/";
                    body = constructStaticBody();
                }

                
            }

            catch(
            UnsupportedEncodingException e)

            {
                Log.e("constructURL", e.getMessage(), e);
                 e.printStackTrace();
            }

            JSONObject response = Utils.PostRequest(body, urlString);






            if(response ==null)

            {
            try {
                //add bad request to event broker
                EventBroker.getInstance().addEvent(Constants.EventTypes.STATUS_CODE, 500, this.routeProvider);
            } catch (Exception e) {
                Log.e("InputStream", e.getLocalizedMessage(), e);
            }
            }else

            {
                publishEvent(response);
            }

        }


        /*
         * Construct the body for the POST-request of de static route
         */
        private String constructStaticBody() throws UnsupportedEncodingException {
            double[] bounds = calculateDistanceBounds();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int parkPreference = preferences.getInt(Constants.SettingTypes.PREF_KEY_ROUTING_PARK, Constants.DynamicRouting.DEFAULT_SLIDER);
            double scaledParkPreference = ((double) parkPreference) / Constants.DynamicRouting.SLIDER_HIGH;
            int waterPreference = preferences.getInt(Constants.SettingTypes.PREF_KEY_ROUTING_WATER, Constants.DynamicRouting.DEFAULT_SLIDER);
            double scaledWaterPreference = ((double) waterPreference) / Constants.DynamicRouting.SLIDER_HIGH;



            String body = URLEncoder.encode("lat", "UTF-8")
                    + "=" + URLEncoder.encode(trackRequest.getLocation().latitude + "", "UTF-8");
            body += "&" + URLEncoder.encode("lon", "UTF-8")
                    + "=" + URLEncoder.encode(trackRequest.getLocation().longitude + "", "UTF-8");
            body += "&" + URLEncoder.encode("min_length", "UTF-8")
                    + "=" + URLEncoder.encode(bounds[0] + "", "UTF-8");
            body += "&" + URLEncoder.encode("max_length", "UTF-8")
                    + "=" + URLEncoder.encode(bounds[1] + "", "UTF-8");
            body += "&" + URLEncoder.encode("type", "UTF-8")
                    + "=" + URLEncoder.encode("directions", "UTF-8");
            body += "&" + URLEncoder.encode("android_token", "UTF-8")
                    + "=" + URLEncoder.encode("1223", "UTF-8");

            /*
            tags for POI ----> ask gregory
             */
            for (String poiTag : GuiController.getInstance().getPoiTags()) {
                if(preferences.getBoolean(poiTag,false)){
                    body += "&" + URLEncoder.encode("tags", "UTF-8")
                            + "=" + URLEncoder.encode(poiTag, "UTF-8");
                }else{
                    body += "&" + URLEncoder.encode("neg_tags", "UTF-8")
                            + "=" + URLEncoder.encode(poiTag, "UTF-8");
                }

            }




            return body;
        }



        /*
         * Construct the body for the POST-request of de static route
         */
        private String constructDynamicBody() throws UnsupportedEncodingException {


            String body = URLEncoder.encode("lat", "UTF-8")
                    + "=" + URLEncoder.encode(trackRequest.getLocation().latitude + "", "UTF-8");
            body += "&" + URLEncoder.encode("lon", "UTF-8")
                    + "=" + URLEncoder.encode(trackRequest.getLocation().longitude + "", "UTF-8");
            body += "&" + URLEncoder.encode("distance", "UTF-8")
                    + "=" + URLEncoder.encode( (((double) trackRequest.getDistance().getDistance()) / 1000) + "", "UTF-8");
            body += "&" + URLEncoder.encode("visited_path", "UTF-8")
                    + "=" + URLEncoder.encode(trackRequest.getTag() + "", "UTF-8");
            body += "&" + URLEncoder.encode("type", "UTF-8")
                    + "=" + URLEncoder.encode("directions", "UTF-8");
            return body;

        }

        /**
         * Calculates the minimal and maximal bounds based on the user's preferred route length.
         * The bound is never smaller than MINIMAL_BOUND, never greater than MAXIMAL_BOUND
         * and rises linearly in between.
         *
         * @return array of length 2 containing bounds. First element is minimal length,
         * second element is maximal length
         */
        private double[] calculateDistanceBounds() {
            double preferredDistance = trackRequest.getDistance().getDistance()/1000.0;
            double[] bounds = new double[2];

            if (preferredDistance < Constants.RouteGenerator.FIRST_BORDER) {
                bounds[0] = preferredDistance - Constants.RouteGenerator.MINIMAL_BOUND;
                bounds[1] = preferredDistance + Constants.RouteGenerator.MINIMAL_BOUND;
            } else if (preferredDistance < Constants.RouteGenerator.SECOND_BORDER) {
                bounds[0] = preferredDistance - (Constants.RouteGenerator.FRACTION_BOUND * preferredDistance);
                bounds[1] = preferredDistance + (Constants.RouteGenerator.FRACTION_BOUND * preferredDistance);
            } else {
                bounds[0] = preferredDistance - Constants.RouteGenerator.MAXIMAL_BOUND;
                bounds[1] = preferredDistance + Constants.RouteGenerator.MAXIMAL_BOUND;
            }

            return bounds;
        }


    }
}


