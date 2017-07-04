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

package com.dp16.runamicghent.Activities.LoginScreen;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.LoginEvent;
import com.crashlytics.android.answers.SignUpEvent;
import com.dp16.runamicghent.Activities.LoginScreen.Fragments.SignInFragment;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.R;
import com.facebook.AccessToken;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;

/**
 * Activity that handles the sign in and sign up.
 */
public class LoginActivity extends AppCompatActivity {

    private String digits = "Digits";
    public static final String TAG = LoginActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.content_frame, new SignInFragment(), SignInFragment.TAG)
                .commit();
    }

    /**
     * @param user has every info about the user available in Firebase
     *             The info from the Firebase user will be used to store info in the shared preferences so we do not need to contact Firebase
     *             every time to retrieve user info.
     */
    public void setUserInfoAfterLogin(FirebaseUser user) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString("client name", user.getDisplayName());
        editor.putString("client email", user.getEmail());
        String facebookUserId = "";
        String photoUrl = "";
        if (user.getPhotoUrl() != null) {
            photoUrl = user.getPhotoUrl().toString();
        }
        for (UserInfo profile : user.getProviderData()) {
            // check if the provider id matches "facebook.com"
            if (("facebook.com").equals(profile.getProviderId())) {
                facebookUserId = profile.getUid();
                photoUrl = "https://graph.facebook.com/" + facebookUserId + "/picture?height=480&type=large";
            }
        }

        if (!("").equals(facebookUserId)) {
            editor.putBoolean("facebook", true);
        } else {
            editor.putBoolean("facebook", false);
        }

        if ("".equals(photoUrl)) {
            photoUrl = "https://scontent-ams3-1.xx.fbcdn.net/v/t31.0-1/c282.0.960.960/p960x960/10506738_10150004552801856_220367501106153455_o.jpg?oh=4e508b87dc864d6dfca85375cc026e2e&oe=594E9012";
        }
        editor.putString("photo URL", photoUrl);
        editor.putString("client token", user.getUid());
        editor.putBoolean("loged in", true);
        editor.apply();

        // Use user information for Fabric CrashLytics
        if (!Constants.DEVELOP) {
            Crashlytics.setUserIdentifier(user.getUid());
            Crashlytics.setUserEmail(user.getEmail());
            Crashlytics.setUserName(user.getDisplayName());
        }
    }

    public boolean checkAlreadyLoggedIn() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getBoolean("logged in", false);
    }

    public void userSignedUp() {
        // SignUpEvent for Fabric Answers
        if (!Constants.DEVELOP) {
            Answers.getInstance().logSignUp(new SignUpEvent()
                    .putMethod(digits)
                    .putSuccess(true));
        }
    }

    public void userLoggedIn() {
        // LoginEvent for Fabric Answers
        if (!Constants.DEVELOP) {
            Answers.getInstance().logLogin(new LoginEvent()
                    .putMethod(digits)
                    .putSuccess(true));
        }
    }

    public void onError(String error) {
        // LoginEvent for Fabric Answers
        if (!Constants.DEVELOP) {
            Answers.getInstance().logLogin(new LoginEvent()
                    .putMethod(digits)
                    .putSuccess(false));
        }

        //Exception occurred
        showResult(error);
    }

    private void showResult(String message) {
        Snackbar.make(getCurrentFocus(), message, Snackbar.LENGTH_LONG).show();
    }

    public void replaceFragment(Fragment fragment, String tag) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment, tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    /**
     * @param mAuth
     * @param token Method used to sign in / sign up with facebook
     *              Signing up and signing in is the same in Firebase.
     */
    public void handleFacebookAccessToken(final FirebaseAuth mAuth, AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        final AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            onError(task.getException().getMessage());
                        }
                    }
                });
    }

}