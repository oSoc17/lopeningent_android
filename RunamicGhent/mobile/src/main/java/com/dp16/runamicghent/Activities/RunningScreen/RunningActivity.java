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

package com.dp16.runamicghent.Activities.RunningScreen;


import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.dp16.runamicghent.AudioPlayer;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.DataProvider.LocationProvider;
import com.dp16.runamicghent.DataProvider.MockUp.LocationProviderMock;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.RunData.RunRoute;
import com.dp16.runamicghent.StatTracker.RouteEngine;
import com.dp16.runamicghent.StatTracker.StatTracker;
import com.dp16.eventbroker.EventPublisherClass;

import java.util.HashMap;
import java.util.Map;


/**
 * Running Activity incorporates the:
 * - {@link MapRunningFragment}
 * - {@link PreRunningFragment}
 * - {@link WhileRunningFragment}
 * - {@link PostRunningFragment}
 * <p>
 * Objects that are needed by both, such as {@link StatTracker}, are kept here.
 * <p>
 * The Activity has an {@link } as attribute, which can be implemented by Fragments contained by the activity.
 * If a Fragment implements this interface, the OnBackPressed behaviour of the activity will be changed for this activity.
 * See {@link RunningActivity#onBackPressed()}}.
 */
public class RunningActivity extends FragmentActivity {
    protected static final String TAG = RunningActivity.class.getSimpleName();

    private boolean runningWithRoute;

    // Possible fragments that can be displayed.
    private MapRunningFragment mapRunningFragment;
    private PreRunningFragment preRunningFragment;
    private WhileRunningFragment whileRunningFragment;
    private PostRunningFragment postRunningFragment;

    // The location providers.
    private LocationProvider locationProvider;
    private LocationProviderMock locationProviderMock;

    // Stattracker and route that is being ran. Be careful, these values can be null.
    private StatTracker statTracker;
    private RunRoute runRoute;
    private RouteEngine routeEngine;
    private AudioPlayer audioPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);

        // Make UI display under the status bar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        runningWithRoute = getIntent().getBooleanExtra("runningWithRoute", false);

        //Start the location provider
        locationProvider = new LocationProvider(this);
        locationProvider.start();

        mapRunningFragment = new MapRunningFragment();
        preRunningFragment = new PreRunningFragment();

        if(runningWithRoute){
            routeEngine = new RouteEngine(this);
            routeEngine.start();
        }

        //Load the map as background.
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, mapRunningFragment, mapRunningFragment.getTag())
                .commit();

        if(runningWithRoute){
            //Load the preRunningFragment on top of it. Notice 'add' instead of 'replace'.
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.content_frame, preRunningFragment, preRunningFragment.getTag())
                    .commit();
        } else {
            switchToWhileRunningFragment();
        }
    }

    /**
     * The activity is always destroyed when leaving it.
     * This method makes sure the location providers are stopped.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (locationProvider != null) {
            locationProvider.pause();
            locationProvider.stop();
        }
        if (locationProviderMock != null) {
            locationProviderMock.stop();
        }
    }

    /**
     * When the backButton is pressed, the user is always switched back to the main activity.
     * The boolean true indicates that the statistics should be saved in memory.
     * The user can always delete them later on.
     */
    @Override
    public void onBackPressed() {
        switchToMainActivity(true);
    }

    public StatTracker getStatTracker() {
        return statTracker;
    }

    public RouteEngine getRouteEngine(){
        return routeEngine;
    }

    public RunRoute getRunRoute() {
        return runRoute;
    }

    public boolean getRunningWithRoute(){
        return runningWithRoute;
    }

    public MapRunningFragment getMapRunningFragment() {
        return mapRunningFragment;
    }

    public WhileRunningFragment getWhileRunningFragment() {
        return whileRunningFragment;
    }

    public void setRunRoute(RunRoute runRoute) {
        this.runRoute = runRoute;
    }

    /**
     * This method is supposed to be called from the PreRunningFragment.
     * It switches from the PreRunningFragment to the WhileRunningFragment.
     * If the setting for the mock location provider is enabled, it is started here
     * and the regular location provider is stopped.
     * The stattracker and routeEngine are also created and started.
     * Before adding the new fragment, the PreRunningFragment is removed. Again notice the use of
     * 'add' instead of 'replace'.
     */
    public void switchToWhileRunningFragment(){
        if(runningWithRoute){
            routeEngine.startRunning();

            if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.SettingTypes.PREF_KEY_DEBUG_LOCATION_MOCK, false)) {
                locationProvider.pause();
                locationProvider.stop();
                locationProviderMock = new LocationProviderMock(runRoute.getRouteCoordinates(), 3.0, 13.0); //12.0 speed
                locationProviderMock.start();
            }
        }

        /*
        Create the audioplayer. Used for directions but also for feedback.
        Thus should also be constructed if user runs without a route.
         */
        audioPlayer = new AudioPlayer(this);
        audioPlayer.start();

        statTracker = new StatTracker(this);
        statTracker.startStatTracker();

        EventPublisherClass publisher = new EventPublisherClass();
        publisher.publishEvent(Constants.EventTypes.START_WEAR, "");

        whileRunningFragment = new WhileRunningFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .remove(preRunningFragment)
                .add(R.id.content_frame, whileRunningFragment, whileRunningFragment.getTag())
                .commit();
    }

    /**
     * This method is supposed to be called from the WhileRunningFragment.
     * It switches from the WhileRunningFragment to the PostRunningFragment.
     * The (mock) location provider is stopped here, as it is no longer needed in this activity.
     * The stattracker is stopped as well as it should no longer add data to the
     * runningStatistics (except for the rating).
     * The routeEngine is stopped as well because running directions are no longer needed.
     * Before adding the new fragment, the WhileRunningFragment is removed. Again notice the use of
     * 'add' instead of 'replace'.
     */
    public void switchToPostRunningFragment() {
        if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(Constants.SettingTypes.PREF_KEY_DEBUG_LOCATION_MOCK, false) && runningWithRoute) {
            locationProviderMock.stop();
        } else {
            locationProvider.pause();
            locationProvider.stop();
        }

        audioPlayer.stop();
        statTracker.stopStatTracker();

        if(runningWithRoute){
            routeEngine.stop();
        }

        postRunningFragment = new PostRunningFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .remove(whileRunningFragment)
                .add(R.id.content_frame, postRunningFragment, postRunningFragment.getTag())
                .commit();
    }

    /**
     * This method switches back from the RunningActivity to the MainActivity.
     * The stattracker is stopped if it exists and the runningStatistics are saved.
     * The routeEngine is stopped if it exists.
     * Then the GuiController is called to switch to the MainActivity, and the frist fragment should be loaded.
     * To finish, exitActivity(this) is called to make sure this activity is destroyed.
     *
     * @param saveRunningStatistics whether the runningStatistics should be saved or not.
     */
    public void switchToMainActivity(boolean saveRunningStatistics) {
        if (statTracker != null) {
            statTracker.stopStatTracker();
            if (saveRunningStatistics) {
                statTracker.saveRunningStatistics();
            }
        }

        if (routeEngine != null) {
            routeEngine.stop();
        }

        Map<String, Object> extras = new HashMap<>();
        extras.put("Fragment", 1);

        GuiController.getInstance().startActivity(this, Constants.ActivityTypes.MAINMENU, extras);
        GuiController.getInstance().exitActivity(this);
    }
}