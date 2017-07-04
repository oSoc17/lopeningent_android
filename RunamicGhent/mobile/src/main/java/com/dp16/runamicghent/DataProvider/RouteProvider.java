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

import com.dp16.runamicghent.Constants;
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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
            URL url;
            if (trackRequest.getDynamic()) {
                url = constructURLDynamic();
            } else {
                url = constructURLStatic();
            }

            boolean goodRequest = false;
            int amountOfTries = 3;
            int status = 0;
            while (amountOfTries > 0 && !goodRequest) {
                if (url != null) {
                    try {
                        //open connection w/ URL
                        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                        status = httpURLConnection.getResponseCode();
                        InputStream inputStream = httpURLConnection.getInputStream();

                        //convert Input Stream to String
                        String result = convertInputStreamToString(inputStream);

                        //create JSON + publish event
                        JSONObject json = new JSONObject(result);
                        publishEvent(json);
                        goodRequest = true;
                    } catch (Exception e) {
                        Log.e("InputStream", e.getLocalizedMessage(), e);
                        amountOfTries--;
                    }
                }
            }
            if (amountOfTries == 0) {
                try {
                    //add bad request to event broker
                    EventBroker.getInstance().addEvent(Constants.EventTypes.STATUS_CODE, status, this.routeProvider);
                } catch (Exception e) {
                    Log.e("InputStream", e.getLocalizedMessage(), e);
                }
            }
        }

        /**
         * Construct the URL that will be used to fetch a static route from the server.
         *
         * @return {@link String} URL with the server request
         */
        private URL constructURLStatic() {
            double[] bounds = calculateDistanceBounds();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int parkPreference = preferences.getInt(Constants.SettingTypes.PREF_KEY_ROUTING_PARK, Constants.DynamicRouting.DEFAULT_SLIDER);
            double scaledParkPreference = ((double) parkPreference) / Constants.DynamicRouting.SLIDER_HIGH;
            int waterPreference = preferences.getInt(Constants.SettingTypes.PREF_KEY_ROUTING_WATER, Constants.DynamicRouting.DEFAULT_SLIDER);
            double scaledWaterPreference = ((double) waterPreference) / Constants.DynamicRouting.SLIDER_HIGH;

            StringBuilder urlString = new StringBuilder();

            if (Constants.DEVELOP) {
                urlString.append("https://groep16.cammaert.me/develop/route/generate?"); //Develop server
            } else {
                urlString.append("https://groep16.cammaert.me/app/route/generate?"); //Master server
            }

            urlString.append("lat=").append(trackRequest.getLocation().latitude);
            urlString.append("&&lon=").append(trackRequest.getLocation().longitude);
            urlString.append("&&min_length=").append(bounds[0]);
            urlString.append("&&max_length=").append(bounds[1]);
            urlString.append("&&type=directions");
            urlString.append("&&measure_park=").append(scaledParkPreference);
            urlString.append("&&measure_water=").append(scaledWaterPreference);

            URL url = null;
            try {
                url = new URL(urlString.toString());
            } catch (MalformedURLException e) {
                Log.e("constructURL", e.getMessage(), e);
            }

            return url;
        }

        /**
         * Construct the URL that will be used to fetch a dynamic route from the server.
         *
         * @return {@link String} URL with the server request
         */
        private URL constructURLDynamic() {
            String urlString;

            if (Constants.DEVELOP) {
                urlString = "https://groep16.cammaert.me/develop/route/return?"; //Develop server
            } else {
                urlString = "https://groep16.cammaert.me/app/route/return?"; //Master server
            }

            urlString = urlString.concat("lat=" + trackRequest.getLocation().latitude + "&&lon=" + trackRequest.getLocation().longitude);
            urlString = urlString.concat("&&distance=" + ((double) trackRequest.getDistance().getDistance()) / 1000);
            urlString = urlString.concat("&&tag=" + trackRequest.getTag());
            urlString = urlString.concat("&&type=directions");

            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                Log.e("constructURL", e.getMessage(), e);
            }

            return url;
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
            int preferredDistance = trackRequest.getDistance().getDistance();
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

        /**
         * Auxiliary method that outputs the content of an InputStream in the form of a string.
         */
        private String convertInputStreamToString(InputStream inputStream) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null)
                result.append(line);

            inputStream.close();
            return result.toString();
        }
    }
}
