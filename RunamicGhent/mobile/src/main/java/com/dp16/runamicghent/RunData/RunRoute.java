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

package com.dp16.runamicghent.RunData;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * This class contains a complete route for the user to run.
 * It is constructed from a JSON object, obtained from the server.
 * The route is an ArrayList of RunRoutePoints that contain a location and direction.
 * Created by hendrikdepauw on 31/03/2017.
 */

public class RunRoute {
    private ArrayList<RunRoutePoint> route;
    private RunDistance routeLength;
    private String tag;

    public RunRoute(Object JSONRoute) {
        RunRoute runRoute = convertJSONToRoute(JSONRoute);
        route = runRoute.getRoute();
        tag = runRoute.getTag();
        routeLength = runRoute.getRouteLength();
    }

    public RunRoute(ArrayList<RunRoutePoint> route, String tag) {
        this.route = route;
        this.tag = tag;
        routeLength = calculateRouteLength(route);
    }

    public ArrayList<RunRoutePoint> getRoute() {
        return route;
    }

    public void setRoute(ArrayList<RunRoutePoint> route){
        this.route = route;
    }

    public RunDistance getRouteLength(){
        return routeLength;
    }

    public String getTag(){
        return tag;
    }

    /**
     * This method generates an ArrayList only containing the RunRoutePoints containing a meaningful
     * direction (not none).
     *
     * @return ArrayList with RunRoutePoints where direction is not none.
     */
    public ArrayList<RunRoutePoint> getRunRouteInstructions() {
        ArrayList<RunRoutePoint> routeInstructions = new ArrayList<>();

        for (RunRoutePoint runRoutePoint : route) {
            if (runRoutePoint.getDirection().getDirection() != RunDirection.Direction.NONE) {
                routeInstructions.add(runRoutePoint);
            }
        }

        return routeInstructions;
    }

    /**
     * This method constructs an ArrayList of coordinates from the ArrayList of
     * RunRoutePoints. The main purpose of this list is to display them on a GoogleMap.
     *
     * @return an ArrayList<LatLng> of the route.
     */
    public ArrayList<LatLng> getRouteCoordinates() {
        ArrayList<LatLng> routeCoordinates = new ArrayList<>();

        for (RunRoutePoint runRoutePoint : route) {
            routeCoordinates.add(runRoutePoint.getLocation());
        }

        return routeCoordinates;
    }

    /**
     * This method converts a JSONObject to a RunRoute object.
     * The server does not return a completely closed route, that is why the first point
     * is added again at the end. Any changes in the JSON format of the server should have
     * to be adjusted in this method.
     *
     * @param JSONRoute JSONString obtained by the server.
     * @return a RunRoute object containing route obtained by server.
     */
    public static RunRoute convertJSONToRoute(Object JSONRoute) {
        JSONArray arr = null;
        RunRoute runRoute;
        String tag = "";
        ArrayList<RunRoutePoint> route = new ArrayList<>();

        try {
            // Retrieve the tag, is used for dynamic routing.
            tag = ((JSONObject) JSONRoute).getString("tag");

            // Extract the coordinates.
            arr = ((JSONObject) JSONRoute).getJSONArray("coordinates");

            for (int i = 0; i < arr.length(); i++) {
                JSONObject routePoint = arr.getJSONObject(i);
                double lat = Double.parseDouble(routePoint.getString("lat"));
                double lon = Double.parseDouble(routePoint.getString("lon"));
                String direction = routePoint.getString("c");
                route.add(new RunRoutePoint(lat, lon, direction));
            }

            //Add first point again to make route closed
            JSONObject routePoint = arr.getJSONObject(0);
            double lat = Double.parseDouble(routePoint.getString("lat"));
            double lon = Double.parseDouble(routePoint.getString("lon"));
            String direction = routePoint.getString("c");
            route.add(new RunRoutePoint(lat, lon, direction));
        } catch (JSONException | ClassCastException e) {
            Log.e("RunRoute", e.getMessage(), e);
        }

        return new RunRoute(route, tag);
    }

    /**
     * This static method calculated the total length of a route.
     *
     * @param route The route of which the length should be calculated.
     * @return RunDistance object containing the length of the route.
     */
    public static RunDistance calculateRouteLength(ArrayList<RunRoutePoint> route) {
        float totalDistance = 0.0f;
        float[] distance = {0.0f};

        for (int i = 0; i < route.size() - 1; i++) {
            Location.distanceBetween(route.get(i + 1).getLocation().latitude, route.get(i + 1).getLocation().longitude, route.get(i).getLocation().latitude, route.get(i).getLocation().longitude, distance);
            totalDistance += distance[0];
        }

        return new RunDistance(Math.round(totalDistance));
    }

}