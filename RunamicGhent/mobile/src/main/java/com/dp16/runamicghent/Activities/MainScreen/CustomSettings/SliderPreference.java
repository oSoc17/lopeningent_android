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

package com.dp16.runamicghent.Activities.MainScreen.CustomSettings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.R;

/**
 * Abstract base class for a SliderPreference. Inherits from {@link Preference}.
 * It retrieves the previously set value from {@link SharedPreferences} to display them and
 * saves newly set value in {@link SharedPreferences}.
 * It gets the preference key, titleString and summaryString from this child classes.
 * Created by Nick on 20-5-2017.
 */
public abstract class SliderPreference extends Preference {
    private SeekBar seekBar;
    protected String preferenceKey;
    protected int titleString;
    protected int summaryString;

    public SliderPreference(Context context) {
        super(context);
    }

    public SliderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SliderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);

        // Inflate layout and get seekBar view
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = li.inflate(R.layout.slider_preference, parent, false);
        seekBar = (SeekBar) view.findViewById(R.id.seekBar);

        // set the description of the preference
        TextView title = (TextView) view.findViewById(R.id.seekBarTitle);
        title.setText(titleString);
        TextView summary = (TextView) view.findViewById(R.id.seekBarSummary);
        summary.setText(summaryString);

        return view;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        // Get current and max value from SharedPreferences and set it on the SeekBar
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        int value = sharedPreferences.getInt(preferenceKey, Constants.DynamicRouting.DEFAULT_SLIDER);
        seekBar.setMax(Constants.DynamicRouting.SLIDER_HIGH);
        seekBar.setProgress(value);

        // register a change listener for the SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (fromUser) {
                    editor.putInt(preferenceKey, value);
                    editor.apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // This is triggered when the user starts a touch gesture.
                // Not interesting for us.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // User has finished a touch gesture.
                // We don't care about that.
            }
        });
    }
}
