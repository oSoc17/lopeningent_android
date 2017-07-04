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
import android.preference.DialogPreference;
import android.util.AttributeSet;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

/**
 * This preference is used to reset the tooltips that are shown to the user on first-use.
 * It makes use of the {@link MaterialShowcaseView#resetAll(Context)} method. This method makes
 * use of {@link android.content.SharedPreferences} internally.
 * These preferences are stored with key {@code "status_"+ TOOLTIP_ID}, where {@code TOOLTIP} represents
 * a developer-specified string. To edit these preferences manually, the following code can be used:
 * <pre>
 * {@code SharedPreferences internal = this.context.getSharedPreferences("material_showcaseview_prefs", 0);
 * int isFinished = uk.co.deanwild.materialshowcaseview.PrefsManager.SEQUENCE_FINISHED;
 * int isNotFinished = uk.co.deanwild.materialshowcaseview.PrefsManager.SEQUENCE_NEVER_STARTED;
 * <p>
 * internal.edit().putInt("status_" + "settings_tooltip", isFinished).apply(); //ProfileFragment
 * internal.edit().putInt("status_" + "history_tooltip", isFinished).apply(); //HistoryFragment
 * internal.edit().putInt("status_" + "start_tooltip", isFinished).apply(); // StartFragment
 * internal.edit().putInt("status_" + "history_expanded_tooltip", isFinished).apply(); // HistoryExpandedFragment
 *} </pre>
 * @see uk.co.deanwild.materialshowcaseview.PrefsManager
 * <p>
 * Created by Simon on 20/04/17.
 */

public class ResetTooltipDialogPreference extends DialogPreference {
    public ResetTooltipDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ResetTooltipDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ResetTooltipDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResetTooltipDialogPreference(Context context) {
        super(context);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            MaterialShowcaseView.resetAll(this.getContext());
        }

    }
}
