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

package com.dp16.runamicghent.Activities.MainScreen.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.dp16.runamicghent.Activities.MainScreen.DistanceNumberPicker;
import com.dp16.runamicghent.Activities.MainScreen.IntroActivity;
import com.dp16.runamicghent.Activities.RunningMap;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.DataProvider.InCityChecker;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Map;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;


/**
 * Start Fragment contains the start screen for the app.
 * A map is shown with the user's location.
 * There is a generate route buttonGenerateRoute which leads to the running fragment.
 * This buttonGenerateRoute is disabled if the user is not in Ghent.
 * <p>
 *     <b>Messages Produced: </b> None.
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#LOCATION}, {@link com.dp16.runamicghent.Constants.EventTypes#IN_CITY}
 * </p>
 *
 */
public class StartFragment extends Fragment implements EventListener, OnMapReadyCallback {
    public static final String TAG = StartFragment.class.getSimpleName();

    // ToolTip ID, used for checking that the user only gets to see this upon first use of the app
    private static final String TOOLTIP_ID = "start_tooltip";
    private RunningMap runningMap;
    private MapView mapView;
    private CoordinatorLayout snackbarView;

    private Button buttonGenerateRoute;
    private Button buttonFreeRunning;
    private DistanceNumberPicker distanceNumberPicker;
    private FloatingActionButton myLocationButton;

    private InCityChecker inCityChecker;

    private Boolean requestRunningMap = true;

    public StartFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start the InCityChecker, to see whether the user is in a city.
        inCityChecker = new InCityChecker("Ghent");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_start, container, false);

        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        // Get coordinatorlayout where we can display a snackbar
        snackbarView = (CoordinatorLayout) view.findViewById(R.id.snackbarlocation);

        // Retrieve Google Map Asynchronously
        mapView.getMapAsync(this);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Get Generate route buttonGenerateRoute and set its onClick method.
        buttonGenerateRoute = (Button) getView().findViewById(R.id.generate_route);
        buttonGenerateRoute.setOnClickListener(new MyGenerateOnClickListener(true));

        buttonFreeRunning = (Button) getView().findViewById(R.id.free_running);
        buttonFreeRunning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRunningView(false);
            }
        });

        // Get DistanceNumberPicker
        distanceNumberPicker = (DistanceNumberPicker) getView().findViewById(R.id.route_length);

        // Get FloatingActionButton
        myLocationButton = (FloatingActionButton) getView().findViewById(R.id.myLocationButton);

        //Disable the buttonGenerateRoute if inghentchecker is used, otherwise enable
        if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("pref_key_debug_inghentchecker", false)) {
            buttonGenerateRoute.setEnabled(false);
        }
    }


    // Fragment Lifecycle methods
    @Override
    public void onStart() {
        super.onStart();
        // Start MapView
        mapView.onStart();

        // Add Event Listeners to the EventBroker
        EventBroker.getInstance().addEventListener(Constants.EventTypes.IN_CITY, this);

        requestRunningMap = true;

        // show the tooltips if the user logs in for the first time
        presentShowcaseSequence();

        // Check whether there is a connection to the internet, if not show snackbar
        if (!isNetworkAvailable()) {
            Snackbar.make(snackbarView, R.string.internet_connection, Snackbar.LENGTH_LONG)
                    .show();
        }

        // Start Location Updates
        ((IntroActivity) getActivity()).startLocation();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        inCityChecker.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();

        inCityChecker.pause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();

        inCityChecker.stop();

        // Remove StartFragment als listener to events from the EventBroker
        EventBroker.getInstance().removeEventListener(this);

        // Stop Location Updates
        ((IntroActivity) getActivity()).stopLocation();

        // Set requestRunningMap to false such that onMapReady doesn't create a RunningMap when it isn't needed
        requestRunningMap = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    /**
     * Handle messages from EventBroker to which this fragment is subscribed as listener.
     *
     * @param eventType Event type to be handled.
     * @param message   Message to (optionally) used while handling.
     */
    @Override
    public void handleEvent(String eventType, Object message) {
        switch (eventType) {
            case Constants.EventTypes.IN_CITY:
                if ((boolean) message) {
                    setButtonEnabledState(buttonGenerateRoute, true);
                    inCityChecker.changeListenerInterval(10000);
                } else if (PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("pref_key_debug_inghentchecker", true)) {
                    setButtonEnabledState(buttonGenerateRoute, false);
                }
                break;
            default:
                break;
        }
    }

    public void handleLocation(final LatLng currentLatLng) {
        Activity activity = getActivity();
        if (activity != null){
            Handler mainHandler = new Handler(activity.getMainLooper());

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (runningMap != null) {
                        runningMap.setMyLocationEnabled(true);
                        runningMap.animateCamera(currentLatLng);
                    }
                }
            };
            mainHandler.post(runnable);
        }
    }

    public void handleLocationAccurate(Boolean acc){
        buttonGenerateRoute.setOnClickListener(new MyGenerateOnClickListener(acc));
    }

    /**
     Set enabled/disabled state of a toggle buttonGenerateRoute
     E.g.: It is set to disabled if user is not in Ghent
     */
    private void setButtonEnabledState(final Button button, final boolean state) {
        Handler mainHandler = new Handler(getActivity().getMainLooper());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                button.setEnabled(state);
            }
        };
        mainHandler.post(runnable);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (requestRunningMap) {
            runningMap = new RunningMap(googleMap, this);
        }
    }

    /**
     * By making use of an external library, all buttons in this fragment are highlighted with some explanation.
     * These tips are only shown in case the app is started for the first time OR the preferences are reset.
     * Current flow is:
     * - Bottom navigation bar
     * ---- Action tab
     * ---- History tab
     * ---- Settings tab
     * - SnapTo Button
     * - DistanceLengthPicker
     * - Generate route button.
     */
    private void presentShowcaseSequence() {
        ShowcaseConfig cnf = new ShowcaseConfig();

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this.getActivity() ,TOOLTIP_ID);
        sequence.setOnItemShownListener(new MaterialShowcaseSequence.OnSequenceItemShownListener() {
            @Override
            public void onShow(MaterialShowcaseView itemView, int position) {
                // Can be changed to so something extra on showing tooltip.
            }
        });

        sequence.setConfig(cnf);

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(new View(getContext()))
                        //.setDismissText(getString(R.string.tooltip_bottombar_got_it))
                        .setContentText(getString(R.string.tooltip_welcome_explanation))
                        .setTitleText("\n\n\n\n"+getString(R.string.tooltip_welcome_tile))
                        .withRectangleShape()
                        .setDismissOnTouch(true)
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(getActivity().findViewById(R.id.bottom_navigation))
                        //.setDismissText(getString(R.string.tooltip_bottombar_got_it))
                        .setContentText(getString(R.string.tooltip_bottombar_explanation))
                        .withRectangleShape()
                        .setDismissOnTouch(true)
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(getActivity().findViewById(R.id.action_start))
                        //.setDismissText(getString(R.string.tooltip_start_bottombar_got_it))
                        .setContentText(getString(R.string.tooltip_start_bottombar_explanation))
                        .setDismissOnTouch(true)
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(getActivity().findViewById(R.id.action_history))
                        //.setDismissText(getString(R.string.tooltip_history_bottombar_got_it))
                        .setContentText(getString(R.string.tooltip_history_bottombar_explanation))
                        .setDismissOnTouch(true)
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(getActivity().findViewById(R.id.action_settings))
                        //.setDismissText(getString(R.string.tooltip_settings_bottombar_got_it))
                        .setDismissOnTouch(true)
                        .setContentText(getString(R.string.tooltip_settings_bottombar_explanation))
                        .build()
        );


        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(myLocationButton)
                        //.setDismissText(getString(R.string.tooltip_start_snapto_got_it))
                        .setDismissOnTouch(true)
                        .setContentText(getString(R.string.tooltip_start_snapto_explanation))
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(distanceNumberPicker)
                        //.setDismissText(getString(R.string.tooltip_start_distancepicker_got_it))
                        .setDismissOnTouch(true)
                        .setContentText(getString(R.string.tooltip_start_distancepicker_explanation))
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(buttonGenerateRoute)
                        .setDismissOnTouch(true)
                        .setContentText(getString(R.string.tooltip_start_generate_route_explanation))
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(buttonFreeRunning)
                        .setDismissOnTouch(true)
                        .setContentText(getString(R.string.tooltip_start_free_running_explanation))
                        .build()
        );

        sequence.start();

    }

    // Named class for the onClickListener for the Generate Route button
    private class MyGenerateOnClickListener implements View.OnClickListener {
        private final Boolean accurate;

        public MyGenerateOnClickListener(Boolean accurate) {
            this.accurate = accurate;
        }

        @Override
        public void onClick(View v) {
            // If the location is not accurate -> set extra dialog to appear
            if (!accurate) {
                // Click listener for the AlertDialog.
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                startRunningView(true);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                // Do nothing
                                break;
                        }
                    }
                };

                // Build and display the AlertDialog.
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(R.string.location_inaccurate_message).setPositiveButton(R.string.location_inaccurate_continue, dialogClickListener)
                        .setNegativeButton(R.string.location_inaccurate_wait, dialogClickListener).setTitle(R.string.location_inaccurate_title).show();
            } else {
                startRunningView(true);
            }
        }
    }

    // Pop to backstack and start running view.
    private void startRunningView(boolean runningWithRoute) {
        Map<String, Object> extras = new HashMap<>();
        extras.put("runningWithRoute", runningWithRoute);

        getActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        GuiController.getInstance().startActivity(getActivity(), Constants.ActivityTypes.RUNNINGVIEW, extras);
    }

    // Checks whether device is connected to internet via the ConnectivityManager
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
