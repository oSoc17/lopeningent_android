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


import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.dp16.runamicghent.Activities.HistoryGallery.HistoryExpandedFragment;
import com.dp16.runamicghent.Activities.HistoryGallery.HistoryGalleryAdapter;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.peekandpop.shalskar.peekandpop.PeekAndPop;

import java.util.ArrayList;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

/**
 * A simple {@link Fragment} subclass for the History tab.
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#LOAD_RUNNINGSTATISTICS}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> {@link com.dp16.runamicghent.Constants.EventTypes#LOADED_RUNNINGSTATISTICS}
 * </p>
 */
public class HistoryFragment extends Fragment implements EventListener, EventPublisher {

    public static final String TAG = HistoryFragment.class.getSimpleName();

    // ToolTip ID, used for checking that the user only gets to see this upon first use of the app
    private static final String TOOLTIP_ID = "history_tooltip";

    // Our own adapter
    private RecyclerView.Adapter mAdapter;

    // PeekAndPop object
    private PeekAndPop peekAndPop;

    // Handle for the recycler view
    RecyclerView mRecyclerView;

    private ProgressBar progressBar;

    View view;

    public HistoryFragment() {
        //empty constructor required
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_history, container, false);

        // Handle for the recycler view
        mRecyclerView = (RecyclerView) view.findViewById(R.id.rv);
        /*
        use this setting to improve performance if you know that changes
        in content do not change the layout size of the RecyclerView
         */
        mRecyclerView.setHasFixedSize(true);

        /*
          use a linear layout manager (cards will be stacked vertically under each other)
          LayoutManager used to position views inside the mRecyclerView
          */
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        peekAndPop = new PeekAndPop.Builder(getActivity())
                .blurBackground(true)
                .peekLayout(R.layout.history_peek)
                .parentViewGroupToDisallowTouchEvents(mRecyclerView)
                .animateFling(true)
                .build();

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        // Send message to load runningStatistics from local memory and subscribe to receive the result
        EventBroker.getInstance().addEventListener(Constants.EventTypes.LOADED_RUNNINGSTATISTICS, this);
        EventBroker.getInstance().addEvent(Constants.EventTypes.LOAD_RUNNINGSTATISTICS, null, this);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        presentShowcaseSequence();
    }

    @Override
    public void handleEvent(String eventType, Object message) {
        //We received the event and can now unsubscribe from the eventbroker again
        EventBroker.getInstance().removeEventListener(Constants.EventTypes.LOADED_RUNNINGSTATISTICS, this);

        //Cast the received message to an arraylist of runningStatistics
        ArrayList<RunningStatistics> runningStatisticsArrayList = (ArrayList<RunningStatistics>) message;

        // If RunningStatisticsArrayList is empty => no runs have been saved yet => show message to user
        if (runningStatisticsArrayList.isEmpty()) {
            Handler mainHandler = new Handler(getActivity().getMainLooper());

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (getView() != null) {
                        // set linearlayout containing text to visible
                        LinearLayout linearLayout = (LinearLayout) getView().findViewById(R.id.ll);
                        linearLayout.setVisibility(View.VISIBLE);
                    }
                }
            };
            mainHandler.post(runnable);
        }

        //Make a runnable to update the UI and post it on the UI thread
        Runnable task = new Worker(runningStatisticsArrayList);
        view.post(task);
    }

    /**
     * This Runnable takes an ArrayList of Runningstatistics and updates the UI accordingly.
     * This means setting the adapter correctly and removing the progressbar.
     */
    public class Worker implements Runnable {
        ArrayList<RunningStatistics> runningStatisticsArrayList;

        public Worker(ArrayList<RunningStatistics> runningStatisticsArrayList) {
            this.runningStatisticsArrayList = runningStatisticsArrayList;
        }

        @Override
        public void run() {
            // specify our own adapter
            mAdapter = new HistoryGalleryAdapter(runningStatisticsArrayList, peekAndPop, HistoryFragment.this);
            mRecyclerView.setAdapter(mAdapter);

            // Track Content View for Fabric Answers
            if (!Constants.DEVELOP) {
                Answers.getInstance().logContentView(new ContentViewEvent()
                        .putContentName("History Screen")
                        .putCustomAttribute("Number of runs", mAdapter.getItemCount()));
            }

            // Hide progressbar
            progressBar.setVisibility(View.GONE);
        }
    }

    public void switchToExpanded(RunningStatistics runningStatistics) {
        HistoryExpandedFragment historyExpandedFragment = new HistoryExpandedFragment();
        historyExpandedFragment.addRunningStatistics(runningStatistics);

        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, historyExpandedFragment, HistoryExpandedFragment.TAG);
        ft.addToBackStack("history");
        ft.commit();
    }

    /**
     * By making use of an external library, all buttons in this fragment are highlighted with some explanation.
     * These tips are only shown in case the app is started for the first time OR the preferences are reset.
     * If there are no previous runs, the "Go for a Run" text is used as element to showcase.
     * If there are runs, the tooltips covers the whole screen.
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

        View item = null;
        if (this.getActivity().findViewById(R.id.goForRun) != null) {
            item = this.getActivity().findViewById(R.id.noRuns);
        }
        if (item != null) {
            sequence.addSequenceItem(
                    new MaterialShowcaseView.Builder(this.getActivity())
                            .setTarget(item)
                            //.setDismissText(getString(R.string.tooltip_history_got_it))
                            .setContentText(getString(R.string.tooltip_history_explanation))
                            .setDismissOnTouch(true)
                            .build()
            );

            sequence.start();
        }
    }
}
