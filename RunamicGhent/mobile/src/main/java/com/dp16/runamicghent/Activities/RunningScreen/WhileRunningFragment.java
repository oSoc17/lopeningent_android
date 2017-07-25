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

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.DynamicCheckers.HeartRateChecker;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.RunData.RunDistance;
import com.dp16.runamicghent.RunData.RunDuration;
import com.dp16.runamicghent.RunData.RunHeartRate;
import com.dp16.runamicghent.RunData.RunSpeed;
import com.dp16.runamicghent.StatTracker.RouteEngine;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisherClass;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.ui.IconGenerator;

import java.util.Timer;
import java.util.TimerTask;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

/**
 * This fragment is displayed while the user is running. It should be loaded on top of
 * a MapRunningFragment. This class listens to SPEED, DURATION, DISTANCE and HEARTRATE events
 * to keep the statistics on the screen up to date. These statistics are shown in a CardView.
 * This fragment also contains a pause button which pauses and resumes the statTracker.
 * When the stop button is pressed, it switches from this fragment to the PostRunningFragment.
 *
 * <p>
 *     <b>Messages Produced: </b> None.
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#DURATION}, {@link com.dp16.runamicghent.Constants.EventTypes#SPEED}
 * </p>
 *
 * Created by hendrikdepauw on 31/03/2017.
 */

public class WhileRunningFragment extends Fragment implements EventListener {
    private static final String TOOLTIP_ID = "whilerunning_tooltip";
    private static final String TOOLTIP_ID_WITH_ROUTE = "whilerunning_tooltip_with_route";
    private Button buttonStop = null;
    private ToggleButton togglePauseResume = null;
    private FloatingActionButton minusButton;
    private FloatingActionButton plusButton;

    private TextView speed;
    private TextView duration;
    private TextView heartrate;
    private TextView distance;
    private TextView routeTotal;
    private ImageView heartImageView;
    private Animation pulse;
    private Boolean heartAnimation = false;
    private Marker startMarker;
    private LinearLayout dynamicButtons;

    private boolean runningWithRoute;

    private boolean dynamicHeartRateOn;

    private HeartRateChecker heartRateChecker = null;

    //Dynamic run time
    private RunningStatistics runningStatistics;
    private double currentSpeed;
    Timer timer = new Timer();
    TimerTask timerTask;
    boolean dynamicAvgSpeedRouting;
    //marker test
    private Marker poiMarker;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_running_while, container, false);

        // Check if the user is running with a route or without.
        runningWithRoute = ((RunningActivity) getActivity()).getRunningWithRoute();

        /*
         * All the TextViews that display data in the CardView are loaded.
         * They are set to the default String, which is replaced as soon as
         * data arrives via the eventBroker.
         */
        speed = (TextView) view.findViewById(R.id.speed);
        speed.setText(RunSpeed.getDefaultString(getContext()));

        duration = (TextView) view.findViewById(R.id.duration);
        duration.setText(RunDuration.getDefaultString());

        heartrate = (TextView) view.findViewById(R.id.heartrate);
        heartrate.setText(RunHeartRate.getDefaultString());

        heartImageView = (ImageView) view.findViewById(R.id.heart);
        pulse = AnimationUtils.loadAnimation(getContext(), R.anim.heart_pulse);

        distance = (TextView) view.findViewById(R.id.distance);
        distance.setText(RunDistance.getDefaultString());

        routeTotal = (TextView) view.findViewById(R.id.routeTotal);
        if (runningWithRoute) {
            routeTotal.setText(getString(R.string.total_route_length_label).concat(" ").concat(((RunningActivity) getActivity()).getRunRoute().getRouteLength().toString()));
        } else {
            routeTotal.setText("");
        }

        buttonStop = (Button) view.findViewById(R.id.stop_button);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*
                When the user presses the stop button, a dialog is shown where the user is
                asked if he/she really wants to stop running. If so, the user is switched to
                the PostRunningFragment. If this was an accidental click and the user pressed
                no, nothing happens.
                 */

                // Click listener for the AlertDialog.
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                // Notify wear
                                EventPublisherClass publisher = new EventPublisherClass();
                                publisher.publishEvent(Constants.EventTypes.STOP_WEAR, "");

                                // Proceed to PostRunningFragment.
                                ((RunningActivity) getActivity()).switchToPostRunningFragment();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                // Do nothing
                            default:
                                break;
                        }
                    }
                };

                // Build and display the AlertDialog.
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(getResources().getString(R.string.confirm_stop)).setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();

            }
        });

        /*
         The pause ToggleButton pauses and resumes the statTracker. All updates except
         for location updates are paused. The location updates are not stored in the RunningStatistics.
          */
        togglePauseResume = (ToggleButton) view.findViewById(R.id.pause_toggle);
        togglePauseResume.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    ((RunningActivity) getActivity()).getStatTracker().pauseStatTracker();
                } else {
                    ((RunningActivity) getActivity()).getStatTracker().resumeStatTracker();
                }
            }
        });

        /*
         Above the CardView a plus and a minus button are displayed. These will shorten or lengthen the
         route on the fly. It is the responsibility of the RouteEngine to handle these new tracks correctly.
         A 'home' button can be added, that will return the fastest route to the start location.
         These buttons should not be displayed when the user is running without a route.
         */
        minusButton = (FloatingActionButton) view.findViewById(R.id.minusButton);
        plusButton = (FloatingActionButton) view.findViewById(R.id.plusButton);
        //dynamic routing avgspeed
        dynamicAvgSpeedRouting = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext()).getBoolean("pref_key_avgspeed_routing_on_off", false);

        // If user chose to run with a route
        if (runningWithRoute) {
            minusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((RunningActivity) getActivity()).getRouteEngine().requestTrackDynamic(RouteEngine.DynamicRouteType.SHORTER);
                    Toast.makeText(getActivity().getBaseContext(), R.string.shorter_route, Toast.LENGTH_SHORT).show();
                }
            });

            plusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((RunningActivity) getActivity()).getRouteEngine().requestTrackDynamic(RouteEngine.DynamicRouteType.LONGER);
                    Toast.makeText(getActivity().getBaseContext(), R.string.longer_route, Toast.LENGTH_SHORT).show();
                }
            });
            //ADDED
            if (PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext()).getBoolean("Time", false) && dynamicAvgSpeedRouting){

                setDynamicRoutingTimerTask();

            }
        } else {
            // Not running with a route -> no plus and minus button
            minusButton.setVisibility(View.GONE);
            plusButton.setVisibility(View.GONE);
        }

        /*
        This adjusts the padding in the MapRunningFragment, so the location that is shown is shown
        in the center of the open space above the CardView and buttons.
        We have to post this in a Runnable because getHeight() will return 0 while the
        LinearLayout ll is not loaded yet. It is not super exactly the center, but more
        than close enough. This is due to padding in the LinearLayout and the CardView.
         */
        final LinearLayout ll = (LinearLayout) view.findViewById(R.id.running_ll);
        ll.post(new Runnable() {
            @Override
            public void run() {
                ((RunningActivity) getActivity()).getMapRunningFragment().setBottomPadding(ll.getHeight());
            }
        });

        // Make sure the map is focused on the location of the user.
        ((RunningActivity) getActivity()).getMapRunningFragment().focusOnLocation();

        // Remove start arrow from map (this is delayed in mapfragment)
        if (runningWithRoute) {
            ((RunningActivity) getActivity()).getMapRunningFragment().removeStartArrow();
        }

        /*
         Create Map Util IconGenerator to create start marker.
         Add marker to Google Map via MapFragment.
         Only do so in case the user is running with a route.
          */
        if (runningWithRoute) {
            IconGenerator iconFactory = new IconGenerator(getContext());
            iconFactory.setRotation(180);
            iconFactory.setContentRotation(180);
            startMarker = ((RunningActivity) getActivity()).getMapRunningFragment().addStartMarker(iconFactory, "Start");
            poiMarker = ((RunningActivity) getActivity()).getMapRunningFragment().addPoiMarker("gent", new LatLng(51.0535,3.7304));

        }

        /*
         Add Event Listeners to the EventBroker.
         Moves from onCreate to here because events could start coming in before
         the TextViews are actually created and loaded.
          */
        EventBroker.getInstance().addEventListener(Constants.EventTypes.SPEED, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.HEART_RESPONSE, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.DISTANCE, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.DURATION, this);

        this.dynamicButtons = (LinearLayout) view.findViewById(R.id.dynamic_buttons);

        // Has dynamic heart rate routing been switched on?
        dynamicHeartRateOn = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext()).getBoolean("pref_key_dynamic_heart_routing_on_off", false);


        presentShowcaseSequence();


        return view;
    }

    /*
TimerTask
*/
    public void setDynamicRoutingTimerTask(){
            runningStatistics = ((RunningActivity) getActivity()).getStatTracker().getRunningStatistics();
            int difficulty = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext()).getInt("difficulty", 0);
            //Get avg speed
            switch (difficulty){
                case 0: currentSpeed = Constants.RouteGenerator.BEGINNER_SPEED;
                    break;
                case 1: currentSpeed = Constants.RouteGenerator.AVERAGE_SPEED;
                    break;
                case 2: currentSpeed = Constants.RouteGenerator.EXPERT_SPEED;
                    break;
                default: currentSpeed = Constants.RouteGenerator.AVERAGE_SPEED;
            }

            timerTask = new TimerTask () {
                @Override
                public void run () {

                    checkAverageSpeed();
                }
            };
            timer.schedule(timerTask,1000*60*2, 1000*60*2);

    }
    public void checkAverageSpeed(){
        double avgSpeed = runningStatistics.getAverageSpeed().getSpeed();
        double difference = avgSpeed - currentSpeed;
        if (difference >= - Constants.RouteGenerator.AVERAGE_SPEED_DIFFERENCE && difference <= Constants.RouteGenerator.AVERAGE_SPEED_DIFFERENCE ){
            ((RunningActivity) getActivity()).getRouteEngine().requestTrackDynamicTime(avgSpeed);
        }
       // Toast.makeText(getActivity().getBaseContext(), R.string.shorter_route, Toast.LENGTH_SHORT).show();
        currentSpeed = avgSpeed;
    }


    public TextView getRouteTotalText() {
        return routeTotal;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //stop timer
        timer.cancel();
        // stop heart rate checker if it exists
        if (heartRateChecker != null) {
            heartRateChecker.stop();
        }

        // Unsubscribe from the eventBroker
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.SPEED, this);
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.HEART_RESPONSE, this);
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.DISTANCE, this);
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.DURATION, this);
    }

    @Override
    public void handleEvent(String eventType, Object message) {
        // Decide what TextView should be updated and call setTextUI on the appropriate element.
        switch (eventType) {
            case Constants.EventTypes.DISTANCE:
                setTextUI(distance, message.toString());
                break;
            case Constants.EventTypes.SPEED:
                setTextUI(speed, ((RunSpeed) message).toString(getContext()));
                break;
            case Constants.EventTypes.HEART_RESPONSE:
                setTextUI(heartrate, message.toString());
                // Start Heart Animation if the animation isn't active yet
                if (!heartAnimation) {
                    Handler mainHandler = new Handler(getActivity().getMainLooper());

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            heartImageView.startAnimation(pulse);
                        }
                    };
                    mainHandler.post(runnable);
                    heartAnimation = true;
                }

                // Only when a heart rate has been registered, do we start the HeartRateChecker
                if (heartRateChecker == null && dynamicHeartRateOn) {
                    heartRateChecker = new HeartRateChecker(getActivity().getBaseContext());
                    heartRateChecker.start();
                }
                break;
            case Constants.EventTypes.DURATION:
                setTextUI(duration, message.toString());
                break;
            default:
                break;
        }
    }



    /**
     * Update the UI on the main thread.
     *
     * @param textView TextView that should be updated.
     * @param text     The new value for the TextView.
     */
    private void setTextUI(final TextView textView, final String text) {
        Handler mainHandler = new Handler(getActivity().getMainLooper());

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        };
        mainHandler.post(runnable);
    }

    /**
     * By making use of an external library, all buttons in this fragment are highlighted with some explanation.
     * These tips are only shown in case the app is started for the first time OR the preferences are reset.
     */
    private void presentShowcaseSequence() {

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this.getActivity() ,TOOLTIP_ID);
        sequence.setOnItemShownListener(new MaterialShowcaseSequence.OnSequenceItemShownListener() {
            @Override
            public void onShow(MaterialShowcaseView itemView, int position) {
                // Can be changed to so something extra on showing tooltip.
            }
        });


        sequence.addSequenceItem(
        new MaterialShowcaseView.Builder(this.getActivity())
                .setDismissOnTouch(true)
                .withRectangleShape()
                .setTarget(new View(getContext()))
                .setTitleText("\n\n\n\n" + getString(R.string.tooltip_whilerunning_title))
                .setContentText(getString(R.string.tooltip_whilerunning_explanation))
                .singleUse(TOOLTIP_ID)
                .build()
        );


        if (runningWithRoute) {
            sequence.addSequenceItem(
            new MaterialShowcaseView.Builder(this.getActivity())
                    .setDismissOnTouch(true)
                    .withCircleShape()
                    .setTarget(dynamicButtons)
                    .setContentText(getString(R.string.tooltip_whilerunning_dynamic_buttons_explanation))
                    .singleUse(TOOLTIP_ID_WITH_ROUTE)
                    .build()
            );
        }


        sequence.start();

    }
}