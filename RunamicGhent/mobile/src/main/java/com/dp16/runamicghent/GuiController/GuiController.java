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

package com.dp16.runamicghent.GuiController;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.dp16.eventbroker.EventBroker;
import com.dp16.runamicghent.Constants;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.translate.Translate;
import com.google.api.services.translate.TranslateRequestInitializer;

import org.apache.commons.math3.analysis.function.Add;
import org.apache.commons.math3.analysis.function.Constant;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Singleton.
 * All communication between different Activities should pass through this class. This way a lose coupling between the Activities is achieved.
 * This in turn allows for hot-swapping activities of a certain type and modifiability in general.
 * <p>
 * <p>
 * <b>Normal use</b>
 * <br>
 * During the application initialization, the activity-types are linked to Activity classes by calling {@link #register(String, Class)}.
 * <br>
 * When a context (or activity) wishes to start another activity, it calls {@link #startActivity(Context, String, Map)}.
 * <br>
 * To close an activity {@link #exitActivity(Activity)} (with 'this' as parameter) should be used instead of {@link Activity#finish()}.
 * <br>
 * Manipulation of the registered activity-types is possible through {@link #changeActivity(String, Class)} and {@link #unregister(String)}.
 * </p>
 * Created by Nick on 5-3-2017.
 */

public class GuiController {
    private static  GuiController ourInstance ;
    private Map<String, Class> mapping = new HashMap<>();
    private Context mContext;
    private ArrayList<String> poiTags;

    private GuiController() {
    }

    private GuiController(ArrayList<String> array) {
        poiTags = array;
    }

    public static GuiController getInstance() {
        if (ourInstance==null){
            ArrayList<String> poiTags = new ArrayList<String>(Arrays.asList("tourism","Water","Park"));


            // Construct the URL.
            URL url = null;
            String urlString = "";
            try {

                urlString = "http://95.85.5.226/poi/types/";


                url = new URL(urlString.toString());
            }
            catch (MalformedURLException e) {
                urlString = "";
                Log.e("constructURL", e.getMessage(), e);
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
                        Log.d("Status", status +"");
                        InputStream inputStream = httpURLConnection.getInputStream();

                        //convert Input Stream to String
                        String result = convertInputStreamToString(inputStream);

                        Log.d("Json",result);

                        //create JSON + publish event
                        JSONObject json = new JSONObject(result);
                        goodRequest = true;
                        JSONArray jsonArray = (JSONArray)(new JSONObject(result)).get("types");
                        if (jsonArray != null) {
                            int len = jsonArray.length();
                            poiTags = new ArrayList<String>();
                            for (int i=0;i<len;i++){
                                poiTags.add(jsonArray.get(i).toString());
                            }
                        }
                    } catch (Exception e) {
                        Log.e("InputStream", e.getLocalizedMessage(), e);
                        amountOfTries--;
                    }
                }
            }


            ourInstance = new GuiController(poiTags);

        }
        return ourInstance;
    }

    

    /**
     * Registers an Activity class for a given type.
     *
     * @param type     Type for which to register an Activity
     * @param activity Activity Class to register
     * @throws TypeAlreadyExistsException If an Activity is already present for the given type. Use {@link #changeActivity(String, Class)} in this case.
     */
    public void register(String type, Class activity) throws TypeAlreadyExistsException {
        if (mapping.containsKey(type)) {
            throw new TypeAlreadyExistsException();
        }
        mapping.put(type, activity);
    }


    /**
     * Changes the activity that corresponds to a certain type.
     *
     * @param type     Type for which to change the Activity
     * @param activity New Activity Class to map to the type
     * @throws NoSuchTypeException If there is no existing mapping for the given type. Use {@link #register(String, Class)} in this case.
     */
    public void changeActivity(String type, Class activity) throws NoSuchTypeException {
        if (!mapping.containsKey(type)) {
            throw new NoSuchTypeException();
        }
        mapping.put(type, activity);
    }


    /**
     * Removes the mapping of a certain type.
     *
     * @param type Type for which to remove the mapping
     * @throws NoSuchTypeException If there is no existing mapping for the given type.
     */
    public void unregister(String type) throws NoSuchTypeException {
        if (!mapping.containsKey(type)) {
            throw new NoSuchTypeException();
        }
        mapping.remove(type);
    }


    /**
     * Starts a new Activity.
     *
     * @param origin Context (or Activity) that calls for the start of a new activity ('this' in most cases).
     * @param type   Type of activity to start. This method returns -1 if no activity is registered for the type.
     * @param extras A RunningMap for Object typed extras you would otherwise pass to an {@link Intent}. Use 'null' if no extras are needed.
     *               Currently supported Objects are String, Double, Integer, Boolean, Byte, Float, Long, Short and Serializable. Other Objects will throw a ClassCastException.
     * @return 0 if the activity was started, -1 is no activity of such type is found
     */
    public int startActivity(Context origin, String type, Map<String, Object> extras) {
        Class target = mapping.get(type);
        if (target == null) {
            return -1;
        }
        Intent intent = new Intent(origin, target);
        if (extras != null) {
            for (Map.Entry entry : extras.entrySet()) {
                Object object = entry.getValue();
                String key = entry.getKey().toString();

                if (object instanceof String) {
                    intent.putExtra(key, (String) object);
                    continue;
                }

                if (object instanceof Double) {
                    intent.putExtra(key, (Double) object);
                    continue;
                }

                if (object instanceof Integer) {
                    intent.putExtra(key, (Integer) object);
                    continue;
                }

                if (object instanceof Boolean) {
                    intent.putExtra(key, (Boolean) object);
                    continue;
                }

                if (object instanceof Byte) {
                    intent.putExtra(key, (Byte) object);
                    continue;
                }

                if (object instanceof Float) {
                    intent.putExtra(key, (Float) object);
                    continue;
                }

                if (object instanceof Long) {
                    intent.putExtra(key, (Long) object);
                    continue;
                }

                if (object instanceof Short) {
                    intent.putExtra(key, (Short) object);
                    continue;
                }

                if (object instanceof Serializable) {
                    intent.putExtra(key, (Serializable) object);
                    continue;
                }

                // we only get here if none of the if's were triggered
                throw new ClassCastException();
            }
        }
        origin.startActivity(intent);
        return 0;
    }


    /**
     * Exits an activity. Use this instead of {@link Activity#finish()}.
     *
     * @param activity Activity to exit ('this' in most cases).
     */
    public void exitActivity(Activity activity) {
        activity.finish();
    }


    /**
     * For testing purposes.
     * Removes all activities for the mapping.
     */
    public void emptyState() {
        mapping.clear();
    }

    /**
     * To easily acces strings from the resources a Context is needed.
     * This method gives out the last known context.
     *
     */

    public Context getContext(){
        return mContext;
    }
    public void setContext(Context context){
        this.mContext = context;
    }
    public ArrayList<String> getPoiTags(){
        return poiTags;
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
