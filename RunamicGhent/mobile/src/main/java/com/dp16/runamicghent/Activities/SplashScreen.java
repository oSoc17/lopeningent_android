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

package com.dp16.runamicghent.Activities;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.dp16.runamicghent.Activities.HistoryGallery.HistoryExpandedFragment;
import com.dp16.runamicghent.Activities.LoginScreen.LoginActivity;
import com.dp16.runamicghent.Activities.MainScreen.ChangeprofileActivity;
import com.dp16.runamicghent.Activities.MainScreen.IntroActivity;
import com.dp16.runamicghent.Activities.MainScreen.LicenseActivity;
import com.dp16.runamicghent.Activities.MainScreen.SettingsActivity;
import com.dp16.runamicghent.Activities.RunningScreen.RunningActivity;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.DataProvider.RatingTransmitter;
import com.dp16.runamicghent.DataProvider.RouteProvider;
import com.dp16.runamicghent.Debug.DebugActivity;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.GuiController.GuiControllerException;
import com.dp16.runamicghent.Persistence.EventBasedPersistence;
import com.dp16.eventbroker.EventBroker;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.fabric.sdk.android.Fabric;


/**
 * This is the SplashScreen. It is seen as good practice to show a logo or something of the kind
 * when your app starts instead of a white screen. Some initial set-up can be done like register
 * activities to the GUIController and start the Eventbroker and RouteProvider.
 * <p>
 * important note!
 * 1. Layout of splashscreen should not be set with onContentView with a real layout but instead with
 * a drawable. This is done to waste as little time as possible.
 */
public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init Fabric with Crashlytics and Answers
        if (!Constants.DEVELOP) {
            Fabric.with(this, new Crashlytics(), new Answers());
        }

        // Register the activities with the GuiController
        GuiController controller = GuiController.getInstance();
        try {
            controller.register(Constants.ActivityTypes.MAINMENU, IntroActivity.class);
            controller.register(Constants.ActivityTypes.RUNNINGVIEW, RunningActivity.class);
            controller.register(Constants.ActivityTypes.DEBUG, DebugActivity.class);
            controller.register(Constants.ActivityTypes.HISTORYEXPANDED, HistoryExpandedFragment.class);
            controller.register(Constants.ActivityTypes.LOGIN, LoginActivity.class);
            controller.register(Constants.ActivityTypes.SETTINGS, SettingsActivity.class);
            controller.register(Constants.ActivityTypes.CHANGEPROFILE, ChangeprofileActivity.class);
            controller.register(Constants.ActivityTypes.LICENCES, LicenseActivity.class);
        } catch (GuiControllerException e) {
            Log.e(e.getClass().getName(), e.getMessage(), e);
        }

        // Start the EventBroker! very important (Singleton)
        EventBroker.getInstance().start();

        // Start the RouteProvider
        RouteProvider routeProvider = new RouteProvider(getApplicationContext());
        routeProvider.start();

        // Start the persistence component
        EventBasedPersistence persistence = new EventBasedPersistence(getApplicationContext());
        persistence.start();

        // Start the RatingTransmitter component
        RatingTransmitter ratingTransmitter = new RatingTransmitter(false);
        ratingTransmitter.start();

        // Tell the mongodb driver to shut up. (Disable logging for trivial matters.)
        if (Constants.Storage.TURNOFFLOGGING) {
            Logger.getLogger(Constants.Storage.LOGGERNAME).setLevel(Level.WARNING);
        }

        // Start LoginActivity and finish this activity
        controller.startActivity(this, Constants.ActivityTypes.LOGIN, null);
        controller.exitActivity(this);
    }
}
