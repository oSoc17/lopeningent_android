/**
 * Copyright (c) 2017 Redouane Arroubai
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */
package com.dp16.runamicghent.Activities.MainScreen.Fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.dp16.runamicghent.R;

public class RouteSettingsFragment extends Fragment {


    public static final String TAG = RouteSettingsFragment.class.getSimpleName();
    private View view;

    /*
    *
    *   Variables for the parameters of the route: to be added
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getCurrentSettings();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_routesettings, container, false);


        return view;
    }


    /**
     * Retrieves the route settings that have been defined
     */
    public void getCurrentSettings() {

    }




}
