/*
 * Copyright (c) 2017 Hendrik Depauw
 * Copyright (c) 2017 Lorenz van Herwaarden
 * Copyright (c) 2017 Nick Aelterman
 * Copyright (c) 2017 Olivier Cammaert
 * Copyright (c) 2017 Maxim Deweirdt
 * Copyright (c) 2017 Gerwin Dox
 * Copyright (c) 2017 Simon Neuville
 * Copyright (c) 2017 Stiaan Uyttersprot
 * Copyright (c) 2017 Redouane Arroubai
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */

package com.dp16.runamicghent.Activities.MainScreen;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dp16.runamicghent.Activities.MainScreen.Fragments.HistoryFragment;
import com.dp16.runamicghent.Activities.MainScreen.Fragments.ProfileFragment;
import com.dp16.runamicghent.Activities.MainScreen.Fragments.RouteSettingsFragment;
import com.dp16.runamicghent.Activities.MainScreen.Fragments.StartFragment;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.DataProvider.LocationProvider;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisherClass;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;


/**
 * Activity that will contain:
 * - {@link StartFragment}
 * - {@link HistoryFragment}
 * - {@link ProfileFragment}
 * <p>
 * It contains the {@link #bottomNavigationView} and is the first Activity that is loaded after the {@link com.dp16.runamicghent.Activities.SplashScreen}.
 * <p>
 *     <b>Messages Produced: </b> {@link com.dp16.runamicghent.Constants.EventTypes#SYNC_WITH_DATABASE}
 * </p>
 * <p>
 *     <b>Messages Consumed: </b> None.
 * </p>
 */
public class IntroActivity extends AppCompatActivity implements EventListener{

    protected static final String TAG = "IntroActivity";

    protected boolean locationPermission;

    // App-defined int constant to link request with callback
    protected static final int MY_PERMISSIONS_REQUEST = 64;

    protected int currentItem = -1;

    private LinearLayout linearLayout;

    //Fragments
    private StartFragment startFragment = null;
    private HistoryFragment historyFragment = null;
    private ProfileFragment profileFragment = null;
    private RouteSettingsFragment routeSettingsFragment = null;

    BottomNavigationView bottomNavigationView = null;

    private LocationProvider locationProvider;

    private Boolean locationRequests = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        GuiController.getInstance().setContext(this);

        // Initialize the preferences with default settings if
        // this is the first time the application is ever opened
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Ask the persistence to perform a sync with the database
        EventPublisherClass eventPublisherClass = new EventPublisherClass();
        EventBroker.getInstance().addEvent(Constants.EventTypes.SYNC_WITH_DATABASE, null, eventPublisherClass);

        /*
          You can choose which fragment should be loaded upon starting the activity
          Put in the extras with key 'Fragment' and a number.
          1: StartFragment (default)
          2: HistoryFragment
          3: ProfileFragment
          4: RouteSettingsFragment
         */
        int fragmentToStart = getIntent().getIntExtra("Fragment", 1);
        final Fragment fragment;

        switch (fragmentToStart) {
            case 1:
                if (this.startFragment == null) {
                    this.startFragment = new StartFragment();
                }
                fragment = this.startFragment;
                break;
            case 2:
                if (this.historyFragment == null) {
                    this.historyFragment = new HistoryFragment();
                }
                fragment = this.historyFragment;
                break;
            case 3:
                if (this.profileFragment == null) {
                    this.profileFragment = new ProfileFragment();
                }
                fragment = this.profileFragment;
                break;
            case 4:
                if (this.routeSettingsFragment == null) {
                    this.routeSettingsFragment = new RouteSettingsFragment();
                }
                fragment = this.routeSettingsFragment;
                break;
            default:
                fragment = null;
                Log.d(TAG, "default case was taken. Should not happen.");
        }

        // Retrieve root linear layout to display snackbar later
        linearLayout = (LinearLayout) findViewById(R.id.ll);

        // Show start fragment in IntroActivity
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.content_frame, fragment, StartFragment.TAG)
                .commit();

        bottomNavigationView = (BottomNavigationView)
                findViewById(R.id.bottom_navigation);
        bottomNavigationView.getMenu().getItem(fragmentToStart - 1).setChecked(true);


        // Set Listener for the bottom navigation bar
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        return bottomNavigationSwitch(item);
                    }
                });

        requestPermissions();

        // Start the LocationProvider for location updates
        locationProvider = new LocationProvider(this, true);
    }

    // Switch between fragments if they are pressed in the bottom navigation bar
    private boolean bottomNavigationSwitch(@NonNull MenuItem item) {
        if (item.getItemId() != currentItem) {
            switch (item.getItemId()) {
                case R.id.action_start:
                    if (this.startFragment == null) {
                        this.startFragment = new StartFragment();
                    }
                    replaceFragment(this.startFragment, StartFragment.TAG);
                    break;

                case R.id.action_history:
                    if (this.historyFragment == null) {
                        this.historyFragment = new HistoryFragment();
                    }
                    replaceFragment(this.historyFragment, HistoryFragment.TAG);
                    break;

                case R.id.action_settings:
                    if (this.profileFragment == null) {
                        this.profileFragment = new ProfileFragment();
                    }
                    replaceFragment(this.profileFragment, ProfileFragment.TAG);
                    break;
                case R.id.action_routesettings:
                    if (this.routeSettingsFragment == null) {
                        this.routeSettingsFragment = new RouteSettingsFragment();
                    }
                    replaceFragment(this.routeSettingsFragment, RouteSettingsFragment.TAG);
                    break;

                default:
                    break;
            }
            currentItem = item.getItemId();
        }
        return true;
    }


    /**
     * Replace fragment with @param fragment
     *
     * @param fragment
     * @param tag
     */
    private void replaceFragment(Fragment fragment, String tag) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment, tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    /**
     * Request necessary permissions for the app
     */
    protected void requestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            locationPermission = false;
        } else {
            locationPermission = true;
        }
        if (!permissionsNeeded.isEmpty())
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[permissionsNeeded.size()]), MY_PERMISSIONS_REQUEST);
    }

    /**
     * Invoked when user responds to request permission dialog.
     * It passes the user's response.
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            handleLocationPermission(grantResults);
        }
    }

    // Handle specific Location Permission
    private void handleLocationPermission(int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locationPermission = true;
        } else {
            // Location permission denied :(
            Snackbar snackbar = Snackbar
                    .make(linearLayout, "App does not work without location permissions!", Snackbar.LENGTH_LONG);

            // Changing action button text color
            View sbView = snackbar.getView();
            TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(Color.YELLOW);
            snackbar.show();

            locationPermission = false;
        }
    }

    /**
     * When in the {@link IntroActivity}, the backbutton should only work when the user is in an expanded
     * fragment. The backbutton can not be used to switch between fragments from the main navigation
     * bar. When the backbutton is pressed in the main activity, it returns to the home screen.
     */
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            /*
             * If the user is on the main screen and presses the back button, the Android guidelines
             * tell us that the user should be redirected to the home screen. As I do not know
             * how I can do this using our GUI controller, I did it this way. Code found on
             * http://stackoverflow.com/a/10112753.
             */
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    public BottomNavigationView getBottomNavigationView() {
        return bottomNavigationView;
    }

    public void startLocation(){
        locationRequests = true;

        // Start Location Provider
        locationProvider.start();

        // Resume location updates. Have been stopped to preserve battery
        locationProvider.resume();
    }

    public void stopLocation(){
        locationRequests = false;

        // Pause LocationProvider: Stop location updates to save battery
        locationProvider.pause();

        // Stop LocationProvider.
        locationProvider.stop();
    }

    @Override
    public void handleEvent(String eventType, Object message) {
        switch (eventType) {
            case Constants.EventTypes.RAW_LOCATION:
                handleLocation((Location) message);
                break;
            case Constants.EventTypes.LOCATION_ACCURATE:
                if (this.startFragment != null && locationRequests){
                    this.startFragment.handleLocationAccurate((Boolean) message);
                }
                break;
            default:
                break;
        }
    }

    // Handle Raw Location Event from the EventBroker
    private void handleLocation(final Location location) {
        if (this.startFragment != null && locationRequests) {
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            this.startFragment.handleLocation(currentLatLng);
        }
    }

    // Fragment Lifecycle methods
    @Override
    public void onStart() {
        super.onStart();

        // Add Event Listeners to the EventBroker
        EventBroker.getInstance().addEventListener(Constants.EventTypes.RAW_LOCATION, this);
        EventBroker.getInstance().addEventListener(Constants.EventTypes.LOCATION_ACCURATE, this);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Remove StartFragment als listener to events from the EventBroker
        EventBroker.getInstance().removeEventListener(this);
    }

    /**
     * Check result of user input on dialog for location setting.
     * Request for location setting is started in the Runningmap is this setting is disabled.
     * Result of request is handled here.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.MapSettings.REQUEST_CHECK_SETTINGS) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            if (resultCode == Activity.RESULT_OK) {
                Log.i(TAG, "User agreed to make required location settings changes.");

                // Start Location Updates user agreed to make required location setting changes
                locationProvider.startLocationUpdates();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.i(TAG, "User chose not to make required location settings changes.");
            } else {
                Log.i(TAG, "unknown activity result code received: " + resultCode);
            }
        } else {
            Log.i(TAG, "unknown activity request received: " + requestCode);
        }
    }
}
