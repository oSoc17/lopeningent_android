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

package com.dp16.runamicghent.Activities.MainScreen.Fragments;


import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatistics;
import com.dp16.runamicghent.StatTracker.AggregateRunningStatisticsHandler;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

import static uk.co.deanwild.materialshowcaseview.PrefsManager.SEQUENCE_NEVER_STARTED;

/**
 * Fragment that represents your Profile. Settings can also be accessed here.
 */
public class ProfileFragment extends Fragment {

    public static final String TAG = StartFragment.class.getSimpleName();
    // ToolTip ID, used for checking that the user only gets to see this upon first use of the app
    private static final String TOOLTIP_ID = "settings_tooltip";
    // Variables that hold the user's token, name, link to picture and e-mailadress.
    private String name;
    private String photoURL;
    private String email;
    private boolean facebook = false;
    private View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getUserInfo();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_profile, container, false);

        setStatistics();

        if (facebook) {
            ImageButton edit = (ImageButton) view.findViewById(R.id.button_edit);
            edit.setVisibility(View.INVISIBLE);
        }
        TextView nameText = (TextView) view.findViewById(R.id.name);
        nameText.setText(name);

        TextView emailText = (TextView) view.findViewById(R.id.emailProfile);
        emailText.setText(email);

        ImageView profilePic = (ImageView) view.findViewById(R.id.profile_picture);
        Picasso.with(getContext())//These following lines will make the photo round
                .load(photoURL) //extract as User instance method
                .transform(new CropCircleTransformation())
                .into(profilePic);

        Button logOut = (Button) view.findViewById(R.id.logout);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                signOut();
                                break;
                            //case DialogInterface.BUTTON_NEGATIVE:
                                // Do nothing
                                //break;
                            default:
                                break;
                        }
                    }
                };

                // Build and display the AlertDialog for logging out
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Are you sure you want to log out?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        });

        Button resetStatistics = (Button) view.findViewById(R.id.reset_statistics);
        resetStatistics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Click listener for the AlertDialog.
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                resetStatistics();
                                break;

                            //case DialogInterface.BUTTON_NEGATIVE:
                                // Do nothing
                                //break;
                            default:
                                break;
                        }
                    }
                };

                // Build and display the AlertDialog for resetting statistics
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Are you sure you want to reset your statistics?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        });

        // Greet user by using his name
        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) view.findViewById(R.id.toolbar_layout);
        collapsingToolbarLayout.setTitle(name);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Set action of settingsbutton
        final ImageButton settingsButton = (ImageButton) getView().findViewById(R.id.button_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                GuiController.getInstance().startActivity(getActivity(), Constants.ActivityTypes.SETTINGS, null);
            }
        });

        final ImageButton changeProfileButton = (ImageButton) getView().findViewById(R.id.button_edit);
        changeProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                GuiController.getInstance().startActivity(getActivity(), Constants.ActivityTypes.CHANGEPROFILE, null);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getSequenceStatus() == SEQUENCE_NEVER_STARTED){
            presentShowcaseSequence();
        }
    }

    /**
     * Retrieve info of the user from the SharedPreferences that have been previously inserted by the LoginActivity.
     */
    public void getUserInfo() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        name = preferences.getString("client name", "");
        photoURL = preferences.getString("photo URL", "");
        email = preferences.getString("client email", "");
        facebook = preferences.getBoolean("facebook", false);
    }

    /**
     * {@link FirebaseAuth} automatically detects which user is online. Also facebook logout is required if facebook login is used.
     * {@link LoginManager#getInstance()} is necessary for Firebase to initialize firebase in this fragment
     * Facebook also needs initialization in the fragment.
     */
    private void signOut() {
        // Firebase sign out
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();
        if (facebook) {
            FacebookSdk.sdkInitialize(getContext());
            LoginManager.getInstance().logOut();
        }
        GuiController.getInstance().startActivity(getContext(), Constants.ActivityTypes.LOGIN, null);
    }

    /**
     * Resets the aggregate statistics and updates the UI.
     */
    private void resetStatistics() {
        AggregateRunningStatisticsHandler.deleteAggregateRunningStatistics();
        setStatistics();
    }

    /**
     * This method sets the values of the aggregated statistics in the fragment.
     */
    private void setStatistics() {
        Handler mainHandler = new Handler(getActivity().getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                AggregateRunningStatisticsHandler aHandler = new AggregateRunningStatisticsHandler();
                AggregateRunningStatistics statistics = aHandler.getAggregateRunningStatistics();

                TextView totalDistance = (TextView) view.findViewById(R.id.totalDistance);
                totalDistance.setText(statistics.getTotalDistance().toString());

                TextView averageDistance = (TextView) view.findViewById(R.id.averageDistance);
                averageDistance.setText(statistics.getAverageDistance().toString());

                TextView totalDuration = (TextView) view.findViewById(R.id.totalDuration);
                totalDuration.setText(statistics.getTotalDuration().toString());

                TextView averageDuration = (TextView) view.findViewById(R.id.averageDuration);
                averageDuration.setText(statistics.getAverageDuration().toString());

                TextView averageHeartRate = (TextView) view.findViewById(R.id.averageHeartRate);
                averageHeartRate.setText(statistics.getAverageHeartRate().toString());

                TextView averageRunSpeed = (TextView) view.findViewById(R.id.averageRunSpeed);
                averageRunSpeed.setText(statistics.getAverageRunSpeed().toString(getContext()));

                TextView numberOfRuns = (TextView) view.findViewById(R.id.numberOfRuns);
                numberOfRuns.setText(Integer.toString(statistics.getNumberOfRuns()));

            }
        });

    }

    /**
     * By making use of an external library, all buttons in this fragment are highlighted with some explanation.
     * These tips are only shown in case the app is started for the first time OR the preferences are reset.
     * Current flow is:
     * - Profile Picture + name
     * - Info + statistics
     * - Edit button
     * - Settings cogwheel.
     */
    private void presentShowcaseSequence() {
        ShowcaseConfig cnf = new ShowcaseConfig();

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this.getActivity(), TOOLTIP_ID);
        sequence.setOnItemShownListener(new MaterialShowcaseSequence.OnSequenceItemShownListener() {
            @Override
            public void onShow(MaterialShowcaseView itemView, int position) {
                // Can be changed to so something extra on showing tooltip.
            }
        });

        sequence.setConfig(cnf);

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(getActivity().findViewById(R.id.toolbar_layout))
                        .setDismissOnTouch(true)
                        .setContentText(getString(R.string.tooltip_settings_profile_picture_explanation))
                        .withRectangleShape()
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this.getActivity())
                        .setTarget(getActivity().findViewById(R.id.profile_linlayout))
                        //.setDismissText(getString(R.string.tooltip_settings_statistics_got_it))
                        .setDismissOnTouch(true)
                        .setContentText(getString(R.string.tooltip_settings_statistics_explanation))
                        .withRectangleShape()
                        .build()
        );

        sequence.start();

    }

    int getSequenceStatus() {
        return this.getContext().getSharedPreferences("material_showcaseview_prefs", 0).getInt("status_" + TOOLTIP_ID, SEQUENCE_NEVER_STARTED);
    }
}
