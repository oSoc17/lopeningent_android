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

package com.dp16.runamicghent.Activities.HistoryGallery;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dp16.runamicghent.R;
import com.dp16.runamicghent.RunData.RunDuration;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

/**
 * A fragment for the Tab Details. More detailed information like statistics at specific
 * times in your run can be seen here.
 */
public class HistoryExpandedDetailsFragment extends Fragment {

    public static final String TAG = HistoryExpandedDetailsFragment.class.getSimpleName();

    //instance of Runningstatistics containing all data of this run
    private RunningStatistics runningStatistics;

    private static final String TOOLTIP_ID = "history_expanded_detailed_tooltip";

    /**
     * UI elements for the map
     */
    private GoogleMap map;
    private Marker marker;

    /**
     * UI elements
     */
    private View view;
    private SeekBar seekBar;
    private TextView seekbarText;
    private TextView speed;
    private TextView distance;
    private TextView heartrate;

    private HistoryExpandedFragment parentFragment;

    public HistoryExpandedDetailsFragment() {
        // Required empty public constructor
    }

    public void setParentFragment(HistoryExpandedFragment parentFragment) {
        this.parentFragment = parentFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setUserVisibleHint(false);

        // Retrieve Parent Fragment and get running statistics
        this.runningStatistics = parentFragment.getRunningStatistics();

        // Inflate the layout for this fragment
        this.view = inflater.inflate(R.layout.fragment_history_expanded_details, container, false);

        initializeVariables();

        //Listen to changes from the seekbar to change the values in the textfields
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            /**
             * This method is called while the user is changing the SeekBar.
             * While sliding, the values in the textfields change, as well as the marker on the map.
             * Markers are only added when the map is already loaded.
             * @param seekBar
             * @param progress
             * @param fromUser
             */
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                this.progress = progress;

                //Update textfields
                seekbarText.setText(RunDuration.toString(this.progress));
                speed.setText(runningStatistics.getSpeed(this.progress * (long) 1000).toString(getContext()));
                distance.setText(String.valueOf(runningStatistics.getDistance(this.progress * (long) 1000)));
                heartrate.setText(String.valueOf(runningStatistics.getHeartrate(this.progress * (long) 1000)));

                if (parentFragment.getMapReady()) {
                    map = parentFragment.getGoogleMap();
                    if (marker != null) {
                        marker.remove();
                    }
                    marker = map.addMarker(new MarkerOptions().position(runningStatistics.getLocation(this.progress * (long) 1000)));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // not implemented
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Not implemented
            }
        });

        return this.view;
    }

    /**
     * This method retrieves the UI elements from the view.
     * Also starts loading the map.
     */
    private void initializeVariables() {
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        seekbarText = (TextView) view.findViewById(R.id.seekBar_text);
        speed = (TextView) view.findViewById(R.id.speed);
        distance = (TextView) view.findViewById(R.id.distance);
        heartrate = (TextView) view.findViewById(R.id.heartrate);

        //Set initial values for the textfields
        seekBar.setMax(runningStatistics.getRunDuration().getSecondsPassed());
        seekbarText.setText(RunDuration.toString(seekBar.getProgress()));
        speed.setText(runningStatistics.getSpeed(seekBar.getProgress() * (long) 1000).toString(getContext()));
        distance.setText(String.valueOf(runningStatistics.getDistance(seekBar.getProgress() * (long) 1000)));
        heartrate.setText(String.valueOf(runningStatistics.getHeartrate(seekBar.getProgress() * (long) 1000)));
    }


    /**
     * By making use of an external library, all buttons in this fragment are highlighted with some explanation.
     * These tips are only shown in case the app is started for the first time OR the preferences are reset.
     */
    protected void presentShowcaseSequence() {
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
                        .setTarget(this.parentFragment.getActivity().findViewById(R.id.pager))
                        //.setDismissText(getString(R.string.tooltip_history_expanded_got_it))
                        .setDismissOnTouch(true)
                        .setContentText(getString(R.string.tooltip_history_expanded_detail_pager_explanation))
                        .withRectangleShape()
                        .build()
        );

        sequence.start();

    }


}
