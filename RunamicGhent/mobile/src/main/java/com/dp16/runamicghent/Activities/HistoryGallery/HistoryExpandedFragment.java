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
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dp16.runamicghent.Activities.Utils;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.StatTracker.RunningStatistics;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

import static com.dp16.runamicghent.Activities.Utils.preProcessRoute;

/**
 * This fragments shows the user information about a previous run.
 * It consists of a map and a pager, showing a {@link HistoryExpandedGeneralFragment} and a {@link HistoryExpandedDetailsFragment}.
 */
public class HistoryExpandedFragment extends Fragment implements OnMapReadyCallback {

    public static final String TAG = HistoryExpandedFragment.class.getSimpleName();

    //instance of Runningstatistics containing all data of this run
    private RunningStatistics runningStatistics;

    // ToolTip ID, used for checking that the user only gets to see this upon first use of the app
    private static final String TOOLTIP_ID = "history_expanded_tooltip";

    /**
     * UI elements for the map
     */
    private GoogleMap map;
    private MapView mapView;

    /**
     * Set to true as soon as the map is loaded.
     * This is necessary so we don't try to  place a marker when the map is not yet loaded.
     */
    private boolean mapReady = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //UI elements
        View view = inflater.inflate(R.layout.fragment_history_expanded, container, false);

        TabLayout tabs = (TabLayout) view.findViewById(R.id.tabs);
        final ViewPager pager = (ViewPager) view.findViewById(R.id.pager);
        final HistoryExpandedAdapter adapter = new HistoryExpandedAdapter(getActivity().getSupportFragmentManager(), this);

        pager.setAdapter(adapter);
        tabs.setupWithViewPager(pager);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    presentShowcaseSequence();
                } else if (tab.getPosition() == 1) {
                    ((HistoryExpandedDetailsFragment) adapter.getItem(tab.getPosition())).presentShowcaseSequence();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // nothing needs to be done
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // nothing needs to be done
            }
        });

        // Gets the MapView from the XML layout and creates it
        mapView = (MapView) view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(this);

        return view;
    }

    /**
     * Lifecycle methods necessary for MapView
     */
    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        presentShowcaseSequence();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
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
     * Called by HistoryFragment, to specify the runningStatistics the user clicked on.
     *
     * @param runningStatistics Statistics of that specific run.
     */
    public void addRunningStatistics(RunningStatistics runningStatistics) {
        this.runningStatistics = runningStatistics;
    }

    public RunningStatistics getRunningStatistics() {
        return runningStatistics;
    }

    public GoogleMap getGoogleMap() {
        return map;
    }

    public boolean getMapReady() {
        return mapReady;
    }

    /**
     * Called when the map is loaded. This method adds the route to the map
     * and pans the camera to the correct position.
     *
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        mapReady = true;

        //Get the route from the runningstatistics
        List<LatLng> polyLine = runningStatistics.getRoute();

        //Add the route to the map
        googleMap.addPolyline(new PolylineOptions()
                .addAll(preProcessRoute(polyLine, Constants.Smoothing.SMOOTHING_ENABLED))
                .geodesic(true)
                .color(ContextCompat.getColor(this.getActivity(), R.color.colorAccent)));

        //Construct LatLngBounds, this is necessary to pan camera to correct position
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (int i = 0; i < polyLine.size(); i++) {
            builder.include(polyLine.get(i));
        }

        //Update camera movements. Don't update if the route is empty
        if (!polyLine.isEmpty()) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), Utils.dpToPx(getContext(), Constants.MapSettings.ROUTE_PADDING)));
        }
    }

    /**
     * By making use of an external library, buttons in this fragment are highlighted with some explanation.
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
                        .setTarget(getActivity().findViewById(R.id.pager))
                        //.setDismissText(getString(R.string.tooltip_history_expanded_got_it))
                        .setDismissOnTouch(true)
                        .setContentText(getString(R.string.tooltip_history_expanded_pager_explanation))
                        .withRectangleShape()
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(getActivity().findViewById(R.id.tabs))
                        //.setDismissText(getString(R.string.tooltip_history_expanded_tabs_got_it))
                        .setDismissOnTouch(true)
                        .setContentText(getString(R.string.tooltip_history_expanded_tabs_explanation))
                        .withRectangleShape()
                        .build()
        );

        sequence.start();

    }

}
