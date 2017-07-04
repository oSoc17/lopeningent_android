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

package com.dp16.runamicghent.Activities.MainScreen;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.NumberPicker;

import com.dp16.runamicghent.Constants;

/**
 * Custom Number Picker.
 * Shows distance with km label combined.
 * New Value is directly saved in the Settings.
 * Created by lorenzvanherwaarden on 30/03/2017.
 */
public class DistanceNumberPicker extends NumberPicker implements NumberPicker.OnValueChangeListener {

    /**
     * Constructors
     */
    public DistanceNumberPicker(Context context) {
        super(context);

        // configure the Number Picker
        configureNumberPicker();
    }

    public DistanceNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);

        // configure the Number Picker
        configureNumberPicker();
    }

    public DistanceNumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // configure the Number Picker
        configureNumberPicker();
    }

    public DistanceNumberPicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        // configure the Number Picker
        configureNumberPicker();
    }

    /**
     * This configures the number picker. First min and max values are set.
     * Then the default setting is read from the settings and displayed.
     * The displayed values are strings with the distance and "km" combined.
     * If the user changes the wheel, the default setting is changed and written to the sharedpreferences.
     */
    private void configureNumberPicker() {
        // Create String Array with labels (e.g. "1 km")
        String[] numberKeys = new String[Constants.RouteGenerator.MAX_LENGTH - Constants.RouteGenerator.MIN_LENGTH + 1];
        for (int i = Constants.RouteGenerator.MIN_LENGTH; i <= Constants.RouteGenerator.MAX_LENGTH; i++) {
            numberKeys[i - 1] = Integer.toString(i).concat(" km");
        }

        this.setMinValue(Constants.RouteGenerator.MIN_LENGTH);
        this.setMaxValue(Constants.RouteGenerator.MAX_LENGTH);
        this.setDisplayedValues(numberKeys);
        this.setValue(PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("pref_key_routing_length", Constants.RouteGenerator.DEFAULT_LENGTH));

        // Set the onValueChanged method for this NumberPicker
        this.setOnValueChangedListener(this);
    }

    // Save new value in shared preferences
    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt("pref_key_routing_length", newVal).commit();
    }
}
