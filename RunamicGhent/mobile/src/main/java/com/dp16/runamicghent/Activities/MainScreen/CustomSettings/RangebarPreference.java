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

import com.appyvet.rangebar.RangeBar;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.R;

/**
 * RangebarPreference inherits from {@link Preference}.
 * It retrieves the previously set upper and lower values from {@link SharedPreferences} to display them and
 * saves newly set upper and lower values in {@link SharedPreferences}.
 * Created by lorenzvanherwaarden on 12/04/2017.
 */
public class RangebarPreference extends Preference {
    private RangeBar rangeBar;

    public RangebarPreference(Context context) {
        super(context);
    }

    public RangebarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RangebarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);

        // Inflate layout and get rangeBar view
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = li.inflate(R.layout.rangebar_preference, parent, false);
        rangeBar = (RangeBar) view.findViewById(R.id.rangebar);
        return view;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        // Get upper and lower limit for heart range from SharedPreferences
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        int lower = sharedPreferences.getInt("pref_key_dynamic_heart_rate_lower", Constants.DynamicRouting.HEART_RATE_LOWER);
        int upper = sharedPreferences.getInt("pref_key_dynamic_heart_rate_upper", Constants.DynamicRouting.HEART_RATE_UPPER);

        // Set pins of RangeBar depending on previously set/standard upper and lower limits
        rangeBar.setRangePinsByValue(lower, upper);

        // ChangeListener for the RangeBar
        rangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public void onRangeChangeListener(RangeBar rangeBar, int leftPinIndex,
                                              int rightPinIndex,
                                              String leftPinValue, String rightPinValue) {
                // Put new values in SharedPreferences for upper and lower limit
                editor.putInt("pref_key_dynamic_heart_rate_lower", Math.max(Constants.DynamicRouting.RANGEBAR_LOW, Integer.valueOf(leftPinValue)));
                editor.putInt("pref_key_dynamic_heart_rate_upper", Math.min(Constants.DynamicRouting.RANGEBAR_HIGH, Integer.valueOf(rightPinValue)));
                editor.commit();
            }
        });
    }
}
