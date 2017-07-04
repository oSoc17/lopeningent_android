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
import android.util.AttributeSet;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.R;

/**
 * Preferences for priority of parks.
 * Created by Nick on 21-5-2017.
 */
public class ParkPreference extends SliderPreference {
    public ParkPreference(Context context) {
        super(context);
        setProtectedVars();
    }

    public ParkPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setProtectedVars();
    }

    public ParkPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setProtectedVars();
    }

    /**
     * This method configures the preference key to be used for the preference and the description to be shown to the user.
     * All the logic is handled by {@link SliderPreference}.
     */
    private void setProtectedVars() {
        this.preferenceKey = Constants.SettingTypes.PREF_KEY_ROUTING_PARK;
        this.titleString = R.string.pref_title_dynamic_routing_park;
        this.summaryString = R.string.pref_summary_dynamic_routing_park;
    }
}
