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

package com.dp16.runamicghent.Activities;

import android.content.Context;
import android.location.Location;
import android.util.DisplayMetrics;
import android.util.Log;

import com.dp16.runamicghent.Constants;
import com.google.android.gms.maps.model.LatLng;
import com.mongodb.util.JSON;

import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Class providing auxiliary methods. Mainly processing of {@link LatLng} objects.
 * Created by Simon on 29/03/17.
 */
public class Utils {

    private Utils() {
        // private constructor. We want methods to be public static
    }

    /**
     * Loess or Lowess interpolator that smoothes a route. If there are a very small amount of nodes, a bandwith of 1 is chosen.
     * if there are more nodes, a bandwith of 0.12 is chosen. A higher value results in too much smoothing on corners.
     *
     * @param route List of LatLng values, representing a route.
     * @return List of LatLng values, representing the smoothed route.
     */
    public static List<LatLng> smoothRoute(List<LatLng> route) {
        if (route.isEmpty()) {
            return route;
        }

        int arrayMultiplier = 1;
        double bandwidth;

        if (route.size() < 20) {
            bandwidth = 1;
        } else {
            bandwidth = (6.0 + 1.0) / route.size();
        }

        List<LatLng> result = new ArrayList<>();
        LoessInterpolator interpolator = new LoessInterpolator(bandwidth, 0);
        double[] lat = new double[route.size()];
        double[] lng = new double[route.size()];


        for (int i = 0; i < route.size(); i++) {
            lat[i] = route.get(i).latitude;
            lng[i] = route.get(i).longitude;
        }
        double[] index = new double[route.size()];
        for (int i = 0; i < index.length; i++) {
            index[i] = i;
        }

        lat = interpolator.smooth(index, lat);
        lng = interpolator.smooth(index, lng);


        for (int i = 0; i < route.size() * arrayMultiplier; i++) {
            LatLng value = new LatLng(lat[i], lng[i]);
            result.add(value);
        }

        return result;
    }

    /**
     * Auxiliary method called, executing the smoothing of routes if and only if a certain criterion is true.
     */
    public static List<LatLng> preProcessRoute(List<LatLng> route, boolean smoothRoute) {
        if (smoothRoute) {
            return smoothRoute(route);
        } else {
            return route;
        }
    }

    /**
     * Class for keeping a rolling average for integers
     */
    public static class RollingAvg {
        private int added = 0;
        private int size;
        private int total = 0;
        private int index = 0;
        private int[] samples;

        public RollingAvg(int size) {
            // Size represents the number of samples to average over
            this.size = size;
            samples = new int[size];

            // Initializing the samples to 0
            for (int i = 0; i < size; i++) {
                samples[i] = 0;
            }
        }

        public void add(int x) {
            // Subtract samples that will be overwritten, overwrite samples and add new samples to total
            total -= samples[index];
            samples[index] = x;
            total += x;

            if (++index == size) {
                index = 0; // cheaper than modulus
            }

            added += 1;
        }

        public double getAverage() {
            return total / (1.0 * size);
        }

        public Boolean isPopulated() {
            return added >= size;
        }
    }

    /**
     * This class holds the bearing for a run and can return a new bearing,
     * bearing in mind (pun intended) the old bearing, last location and current location.
     * Last Location is location that was last (at least) 8 meters away.
     */
    public static class BearingCalculator {
        private float bearing;
        private float DISCOUNT_FACTOR;
        private LatLng lastLocation;
        private Boolean first;
        float[] distance = {0.0f};


        public BearingCalculator(){
            bearing = 0.0f;
            DISCOUNT_FACTOR = Constants.MapSettings.DISCOUNT_FACTOR;
            first = true;
        }

        public float calculateBearing(LatLng currentLocation){
            if (first){
                this.lastLocation = currentLocation;
                first = false;
            }

            Location.distanceBetween(currentLocation.latitude, currentLocation.longitude, lastLocation.latitude, lastLocation.longitude, distance);
            if (distance[0] > 8.0f){
                float newBearing = (float) Utils.calculateBearing(lastLocation, currentLocation);
                if (Math.abs(bearing - newBearing) > 180.0f) newBearing += 360.0f;
                bearing = DISCOUNT_FACTOR * bearing + (1.0f - DISCOUNT_FACTOR) * newBearing;

                lastLocation = currentLocation;
            }
            return bearing;
        }
    }

    /**
     * Calculate bearing (rotation) between currentLocation and rolling average
     * Formula found here: http://www.movable-type.co.uk/scripts/latlong.html
     *
     * @return bearing
     */
    public static double calculateBearing(LatLng previousLocation, LatLng currentLocation) {
        //LatLng rolling = rollingLatLon.getAverage();
        double dLon = currentLocation.longitude - previousLocation.longitude;
        double y = Math.sin(dLon) * Math.cos(currentLocation.latitude);
        double x = Math.cos(previousLocation.latitude) * Math.sin(currentLocation.latitude) - Math.sin(previousLocation.latitude) * Math.cos(currentLocation.latitude) * Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));
        return -(360 - ((brng + 360) % 360));
    }

    /**
     * Calculate pixels from dp (dependent on screen density of smartphone)
     */
    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    /**
     * Calculate dp from pixels (dependent on screen density of smartphone)
     */
    public static int pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static JSONObject PostRequest(String body,String urlString){
        JSONObject resultJSON = null;
        URL url = null;
        try {


            url = new URL(urlString.toString());
        }
        catch (MalformedURLException e) {
            urlString = "";
            Log.e("constructURL", e.getMessage(), e);
        }
        boolean goodRequest = false;
        int amountOfTries = 3;
        while (amountOfTries > 0 && !goodRequest) {
            if (url != null) {
                try {
                    //open connection w/ URL
                    InputStream stream = null;
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setDoOutput(true);



                    httpURLConnection.connect();

                    OutputStreamWriter wr = new OutputStreamWriter(httpURLConnection.getOutputStream());
                    wr.write(body);
                    wr.flush();

                    stream = httpURLConnection.getInputStream();
                    String result = convertInputStreamToString(stream);
                    Log.d("Json",result);

                    //create JSON + publish event
                    resultJSON = new JSONObject(result);
                    goodRequest = true;
                } catch (Exception e) {
                    Log.e("InputStream", e.getLocalizedMessage(), e);
                    amountOfTries--;
                }
            }
        }

        return resultJSON;
    }
    /**
     * Auxiliary method that outputs the content of an InputStream in the form of a string.
     */
    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder result = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null)
            result.append(line);

        inputStream.close();
        return result.toString();
    }
}
