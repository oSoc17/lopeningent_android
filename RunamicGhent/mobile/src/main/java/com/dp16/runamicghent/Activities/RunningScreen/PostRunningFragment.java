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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.RunData.RunRoute;
import com.dp16.runamicghent.RunData.RunRating;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventPublisher;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

/**
 * This fragment is displayed when the user stops running.
 * It shows a CardView with the statistics of the run.
 * A user can choose to save the run or discard it.
 * After saving or discarding the run, the user is redirected to the MainActivity.
 *
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#RATING}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> None.
 * </p>
 *
 * Created by hendrikdepauw on 01/04/2017.
 */

public class PostRunningFragment extends Fragment implements EventPublisher {
    private RunningStatistics runningStatistics;

    private Button saveButton;
    private Button discardButton;

    private TextView speed;
    private TextView duration;
    private TextView heartrate;
    private TextView distance;
    private RatingBar ratingBar;

    // ToolTip ID, used for checking that the user only gets to see this upon first use of the app
    private static final String TOOLTIP_ID = "postrunning_tooltip";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the RunningStatistics from the StatTracker.
        runningStatistics = ((RunningActivity) getActivity()).getStatTracker().getRunningStatistics();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_running_post, container, false);

        // Get the route from the RunningStatistics and pass it to the MapRunningFragment to display.
        // TODO preprocess route.
        ((RunningActivity) getActivity()).getMapRunningFragment().setRoute(runningStatistics.getRoute());
        ((RunningActivity) getActivity()).getMapRunningFragment().displayRoute();
        ((RunningActivity) getActivity()).getMapRunningFragment().focusOnRoute();

        // Load all the TextViews to display the statistics of the run.
        speed = (TextView) view.findViewById(R.id.speed);
        speed.setText(runningStatistics.getAverageSpeed().toString(getContext()));

        duration = (TextView) view.findViewById(R.id.duration);
        duration.setText(runningStatistics.getRunDuration().toString());

        heartrate = (TextView) view.findViewById(R.id.heartrate);
        heartrate.setText(runningStatistics.getAverageHeartRate().toString());

        distance = (TextView) view.findViewById(R.id.distance);
        distance.setText(runningStatistics.getTotalDistance().toString());

        // Retrieve metric values of distance, duration, avgSpeed and avgHeartRate for Answers
        final double distance = (double) runningStatistics.getTotalDistance().getDistance() / 1000.0;
        final double duration = (double) runningStatistics.getRunDuration().getSecondsPassed() / 60.0;
        final double avgSpeed = runningStatistics.getAverageSpeed().getSpeed();
        final double avgHeartRate = (double) runningStatistics.getAverageHeartRate().getHeartRate();

        /*
        The user can add a rating to his run.
        The rating is added via the StatTracker upon pressing the save button.
        No rating is added to a run when the RunningStatistics are saved by
        pressing the back button.
         */
        ratingBar = (RatingBar) view.findViewById(R.id.ratingbar);

        // Save the RunningStatistics and switch to the MainActivity.
        saveButton = (Button) view.findViewById(R.id.save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Key Metric for Fabric Answers
                 * Run Saved Custom Event
                 * Run statistics as attributes to track values of runs that are SAVED
                 */
                if (!Constants.DEVELOP) {
                    Answers.getInstance().logCustom(new CustomEvent("Run Saved")
                            .putCustomAttribute(getString(R.string.distance_answers), distance)
                            .putCustomAttribute(getString(R.string.duration_answers), duration)
                            .putCustomAttribute(getString(R.string.avg_speed_answers), avgSpeed)
                            .putCustomAttribute(getString(R.string.avg_heart_rate_label), avgHeartRate)
                            .putCustomAttribute(getString(R.string.rating_answers), Float.toString(ratingBar.getRating())));
                }

                ((RunningActivity) getActivity()).getStatTracker().addRating(ratingBar.getRating());
                ((RunningActivity) getActivity()).switchToMainActivity(true);

                // Get tag of run and rating and create RunRating object, add Rating event to EventBroker
                // If runRoute is null, it was a free run. In this case, no rating has to be send to the server.
                RunRoute runRoute = ((RunningActivity) getActivity()).getRunRoute();
                if (runRoute != null) {
                    RunRating runRating = new RunRating(runRoute.getTag(), ratingBar.getRating());
                    EventBroker.getInstance().addEvent(Constants.EventTypes.RATING, runRating, PostRunningFragment.this);
                }
            }
        });

        // Discard the RunningStatistics and switch to the MainActivity.
        discardButton = (Button) view.findViewById(R.id.discard);
        discardButton.setOnClickListener(new View.OnClickListener() {

            /*
            Before discarding a run, the user is asked if he is sure about it.
            If the answer is yes, the run is discarded and the user is switched back to the MainActivity.
            In case it was an accidental click and the user presses no, the user is
            returned back to the PostRunningFragment.
             */

            @Override
            public void onClick(View v) {
                /*
                 * Key Metric for Fabric Answers
                 * Run Saved Custom Event
                 * Run statistics as attributes to track values of runs that are DELETED
                 */
                if (!Constants.DEVELOP) {
                    Answers.getInstance().logCustom(new CustomEvent("Run Discarded")
                            .putCustomAttribute(getString(R.string.distance_answers), distance)
                            .putCustomAttribute(getString(R.string.duration_answers), duration)
                            .putCustomAttribute(getString(R.string.avg_speed_answers), avgSpeed)
                            .putCustomAttribute(getString(R.string.avg_heart_rate_label), avgHeartRate));
                }

                // Click listener for the AlertDialog.
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                // Switch to MainActivity without saving.
                                ((RunningActivity) getActivity()).switchToMainActivity(false);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                // Do nothing
                                break;
                        }
                    }
                };

                // Build and display the AlertDialog.
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Are you sure you want to discard your run?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        });

        /*
        This adjusts the padding in the MapRunningFragment, so the location that is shown is shown
        in the center of the open space above the CardView and buttons.
        We have to post this in a Runnable because getHeight() will return 0 while the
        LinearLayout ll is not loaded yet. It is not super exactly the center, but more
        than close enough. This is due to padding in the LinearLayout and the CardView.
        It is run again in the postrunningfragment because ratingbar has been added in the cardview.
         */
        final LinearLayout ll = (LinearLayout) view.findViewById(R.id.running_ll);
        ll.post(new Runnable() {
            @Override
            public void run() {
                ((RunningActivity) getActivity()).getMapRunningFragment().setBottomPadding(ll.getHeight());
            }
        });

        // Disable the location Fab
        ((RunningActivity) getActivity()).getMapRunningFragment().disableLocationFab();

        presentShowcaseSequence();

        return view;
    }

    /**
     * By making use of an external library, all buttons in this fragment are highlighted with some explanation.
     * These tips are only shown in case the app is started for the first time OR the preferences are reset.
     */
    private void presentShowcaseSequence() {
        ShowcaseConfig cnf = new ShowcaseConfig();

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this.getActivity(), TOOLTIP_ID);
        sequence.setOnItemShownListener(new MaterialShowcaseSequence.OnSequenceItemShownListener() {
            @Override
            public void onShow(MaterialShowcaseView itemView, int position) {
                // Can be changed to so something extra on showing tooltip.
            }
        });

        sequence.setConfig(cnf);

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(ratingBar)
                        //.setDismissText(getString(R.string.tooltip_postrunning_ratingbar_got_it))
                        .setDismissOnTouch(true)
                        .setContentText(getString(R.string.tooltip_postrunning_raringbar_explanation))
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(saveButton)
                        //.setDismissText(getString(R.string.tooltip_postrunning_savebutton_got_it))
                        .setDismissOnTouch(true)
                        .setContentText(getString(R.string.tooltip_postrunning_savebutton_explanation))
                        .build()
        );

        sequence.start();

    }
}
