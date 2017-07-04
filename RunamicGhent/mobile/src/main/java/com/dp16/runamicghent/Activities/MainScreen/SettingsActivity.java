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

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Activity that loads the preferences.xml on the screen
 * All complexity of the settings is contained in the xml file
 * Custom settings are located in the CustomSettings package
 * Created by hendrikdepauw on 17/03/2017.
 */

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);


            //set the build number
            String buildNr = "N/A";
            try {
                final PackageInfo pInfo = this.getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                buildNr = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(this.getTag(), e.getMessage(), e);
            }

            findPreference("pref_about_build_number").setSummary(buildNr);
        }
    }

    /**
     * Custom onBackPressed method makes sure that the profile fragment is loaded
     * on the introactivity by passing {@code <Fragment,"3">} in the extras.
     */
    @Override
    public void onBackPressed() {
        Map<String, Object> extras = new HashMap<>();
        extras.put("Fragment", 3);
        GuiController.getInstance().startActivity(this, Constants.ActivityTypes.MAINMENU, extras);
    }

}