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

package com.dp16.runamicghent.Activities.RunningScreen;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dp16.runamicghent.Activities.Utils;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.RunData.RunDistance;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.data.Layer;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.mongodb.util.JSON;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * This Fragment is meant to be loaded on top of the MapRunningFragment.
 * It displays a start button, which switches the user to the WhileRunningActivity.
 * The button is only enables when the route is loaded. For this a TRACK_LOADED event is used.
 * <p>
 *     <b>Messages Produced: </b> None.
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#LOCATION}, {@link com.dp16.runamicghent.Constants.EventTypes#STATUS_CODE}, {@link com.dp16.runamicghent.Constants.EventTypes#TRACK_LOADED}
 * </p>
 *
 * Created by hendrikdepauw on 31/03/2017.
 */

public class PreRunningFragment extends Fragment implements EventPublisher, EventListener {
    private Button startButton;
    private Button alternativeButton;
    private LatLng location;
    private TextView lengthText;
    private Activity mActivity;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActivity = getActivity();
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_running_pre, container, false);

        // Get the start button and disable it by default. Only enable it when a track is loaded.
        startButton = (Button) view.findViewById(R.id.start_button);
        startButton.setEnabled(false);

        // Switch to the WhileRunningFragment when the user presses Start Running.
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((RunningActivity) getActivity()).switchToWhileRunningFragment();
            }
        });

        // Get the generate alternative button.
        alternativeButton = (Button) view.findViewById(R.id.generate_alternative);
        alternativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateRoute();
            }
        });

        lengthText = (TextView) view.findViewById(R.id.length);

        /*
        This adjusts the padding in the MapRunningFragment, so the location that is shown is shown
        in the center of the open space above the generating route/start run button
        We have to post this in a Runnable because getHeight() will return 0 while the
        StartButton is not loaded yet. Take into account height and bottommargin of button.
         */
        startButton.post(new Runnable() {
            @Override
            public void run() {
                LinearLayout button_ll = (LinearLayout) getView().findViewById(R.id.button_ll);
                ((RunningActivity) getActivity()).getMapRunningFragment().setBottomPadding(button_ll.getHeight());
            }
        });


        return view;
    }
    //Set markers
    public void setPoiMarkers(){
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        if (preferences.getBoolean("pref_mapmarkers",false)){
            new Thread(new Runnable() {
                public void run() {
                    String urlString = "http://"+ Constants.Server.ADDRESS+"/poi/coords/";
                    String body = "";
                    try {
                        for (String poiTag : GuiController.getInstance().getPoiTags()) {
                            if (preferences.getBoolean(poiTag, false)) {

                                body += "&" + URLEncoder.encode("tags", "UTF-8")
                                        + "=" + URLEncoder.encode(poiTag, "UTF-8");

                            }
                        }
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    JSONObject result = Utils.PostRequest(body, urlString);
                    if (result!=null){

                        try {
                            JSONArray arrayPOI = (JSONArray) result.get("coords");
                            ((RunningActivity) getActivity()).getMapRunningFragment().addPoiMarker(arrayPOI);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }

            }).start();
        }

    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Add Event Listeners to the EventBroker
        EventBroker.getInstance().addEventListener(Constants.EventTypes.LOCATION, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.TRACK_LOADED, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.STATUS_CODE, this);
    }

    @Override
    public void handleEvent(String eventType, Object message) {
        Handler mainHandler = new Handler(mActivity.getMainLooper());
        Runnable runnable = null;
        switch (eventType) {
            case Constants.EventTypes.LOCATION:
                // Only listen for 1 location update and send a TRACK_REQUEST
                EventBroker.getInstance().removeEventListener(Constants.EventTypes.LOCATION, this);
                location = (LatLng) message;
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        generateRoute();
                    }
                };
                break;
            case Constants.EventTypes.TRACK_LOADED:
                // The track is loaded in the MapRunningFragment. The start button can be enabled.
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        // Set the text on the Start button and enable it.
                        startButton.setText(getString(R.string.start_running));
                        startButton.setEnabled(true);

                        // Enabled the generate alternative button
                        alternativeButton.setEnabled(true);

                        // Tell the MapRunningFragment to put the focus on the route.
                        ((RunningActivity) getActivity()).getMapRunningFragment().focusOnRoute();

                        // Draw arrow to show direction runner has to start running in, delete previous arrow if there is one
                        ((RunningActivity) getActivity()).getMapRunningFragment().addStartArrow();

                        //add poi markers
                        setPoiMarkers();

                        // Calculate route length and display it
                        RunDistance totalRouteLength = ((RunningActivity) getActivity()).getRunRoute().getRouteLength();
                        lengthText.setText(totalRouteLength.toString());
                        lengthText.setVisibility(View.VISIBLE);
                    }
                };
                break;
            case Constants.EventTypes.STATUS_CODE:
                int status = (Integer) message;
                handleError(status);
                break;
            default:
                break;
        }
        mainHandler.post(runnable);
    }

    public void handleError(int status) {
        Handler mainHandler = new Handler(mActivity.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                showDialog();
            }
        });
    }


    public void showDialog() {
        new AlertDialog.Builder(mActivity)
                .setTitle(R.string.wrong_message)
                .setMessage(R.string.try_again)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        GuiController.getInstance().startActivity(mActivity, Constants.ActivityTypes.MAINMENU, null);
                        GuiController.getInstance().exitActivity(mActivity);
                    }
                })
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .show();
    }


    /**
     * Generate new route.
     */
    private void generateRoute() {
        /*
         Disable alternativeButton, hide distance of route,
         set text of startButton to "generating route..." and disable startButton
          */
        alternativeButton.setEnabled(false);
        lengthText.setVisibility(View.GONE);
        startButton.setText(getString(R.string.generating_route));
        startButton.setEnabled(false);

        ((RunningActivity) getActivity()).getRouteEngine().requestTrackStatic(location);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unsubscribe from the eventBroker.
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.LOCATION, this);
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.TRACK_LOADED, this);
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.STATUS_CODE, this);

    }
}
