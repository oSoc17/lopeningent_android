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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

/**
 * Adapter for the TabLayout to switch between ViewPage. In our case: switch between general and details fragment.
 * Names of the different pages can be retrieved here as well as which fragment should be set when a specific tab is pressed.
 * Created by lorenzvanherwaarden on 20/03/2017.
 */
class HistoryExpandedAdapter extends android.support.v4.app.FragmentStatePagerAdapter {
    private HistoryExpandedFragment parentFragment;

    private HistoryExpandedGeneralFragment expandedGeneralFragment;
    private HistoryExpandedDetailsFragment expandedDetailsFragment;

    // Parent fragment is used to set the parent of the tab fragment
    HistoryExpandedAdapter(FragmentManager fm, HistoryExpandedFragment parentFragment) {
        super(fm);
        this.parentFragment = parentFragment;
        this.expandedDetailsFragment = new HistoryExpandedDetailsFragment();
        this.expandedGeneralFragment = new HistoryExpandedGeneralFragment();

    }

    // Get fragment corresponding to the position
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            HistoryExpandedGeneralFragment fragment = expandedGeneralFragment;
            fragment.setParentFragment(parentFragment);
            return fragment;
        } else {
            HistoryExpandedDetailsFragment fragment = expandedDetailsFragment;
            fragment.setParentFragment(parentFragment);
            return fragment;
        }
    }

    // Total number for tabs
    @Override
    public int getCount() {
        return 2;
    }

    // Get title of tab
    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0) {
            return "General";
        } else {
            return "Details";
        }
    }
}
