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
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventPublisherClass;

/**
 * Fragment for the Tab General. General information of the run can be seen here.
 *
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#DELETE_RUNNINGSTATISTICS}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> None.
 * </p>
 */
public class HistoryExpandedGeneralFragment extends Fragment {

    public static final String TAG = HistoryExpandedGeneralFragment.class.getSimpleName();

    private RunningStatistics runningStatistics; //instance of Runningstatistics containing all data of this run

    private View view;
    private Button delete;

    private HistoryExpandedFragment parentFragment;

    public HistoryExpandedGeneralFragment() {
        // Required empty public constructor
    }

    public void setParentFragment(HistoryExpandedFragment parentFragment) {
        this.parentFragment = parentFragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Retrieve Parent Fragment and get running statistics
        //this.parentFragment = (HistoryExpandedFragment)getTargetFragment();
        this.runningStatistics = parentFragment.getRunningStatistics();

        // Inflate the layout for this fragment
        this.view = inflater.inflate(R.layout.fragment_history_expanded_general, container, false);

        initializeVariables();

        delete.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Delete file
                EventPublisherClass publisher = new EventPublisherClass();
                EventBroker.getInstance().addEvent(Constants.EventTypes.DELETE_RUNNINGSTATISTICS, runningStatistics, publisher);

                //Return to previous fragment
                getFragmentManager().popBackStackImmediate();
            }
        });

        return view;
    }

    /**
     * This method retrieves the UI elements from the view.
     * Also starts loading the map.
     */
    private void initializeVariables() {
        TextView duration = (TextView) view.findViewById(R.id.duration);
        TextView speed = (TextView) view.findViewById(R.id.speed);
        TextView distance = (TextView) view.findViewById(R.id.distance);
        TextView heartrate = (TextView) view.findViewById(R.id.heartrate);
        delete = (Button) view.findViewById(R.id.delete);
        RatingBar ratingBar = (RatingBar) view.findViewById(R.id.ratingbar);

        //Set initial values for the textfields
        duration.setText(String.valueOf(runningStatistics.getRunDuration()));
        speed.setText(runningStatistics.getAverageSpeed().toString(getContext()));
        distance.setText(String.valueOf(runningStatistics.getTotalDistance()));
        heartrate.setText(String.valueOf(runningStatistics.getAverageHeartRate()));
        ratingBar.setRating((float) runningStatistics.getRating());

    }
}
