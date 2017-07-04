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

package com.dp16.runamicghent.Debug;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.dp16.runamicghent.Activities.MainScreen.Fragments.StartFragment;
import com.dp16.runamicghent.Activities.RunningScreen.RunningActivity;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.GuiController.NoSuchTypeException;
import com.dp16.runamicghent.R;

/**
 * Created by Nick
 */

public class DebugActivity extends AppCompatActivity {
    public static final String TAG = StartFragment.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

    }

    public void onSwapRunningViewDummy(View view) {
        try {
            GuiController.getInstance().changeActivity(Constants.ActivityTypes.RUNNINGVIEW, DummyRunningActivity.class);
        } catch (NoSuchTypeException e) {
            Log.d("DebugActivity", "Unable to swap running view to dummy view", e);
        }
    }

    public void onSwapRunningViewReal(View view) {
        try {
            GuiController.getInstance().changeActivity(Constants.ActivityTypes.RUNNINGVIEW, RunningActivity.class);
        } catch (NoSuchTypeException e) {
            Log.d("DebugActivity", "Unable to swap running view to real view", e);
        }
    }

    public void onStartCrudTest() {
        GuiController.getInstance().startActivity(this, Constants.ActivityTypes.RESTTEST, null);
    }

}
