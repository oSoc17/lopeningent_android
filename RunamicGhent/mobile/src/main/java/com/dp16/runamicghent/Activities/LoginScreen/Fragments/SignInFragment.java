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

package com.dp16.runamicghent.Activities.LoginScreen.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dp16.runamicghent.Activities.LoginScreen.LoginActivity;
import com.dp16.runamicghent.Activities.MainScreen.Fragments.StartFragment;
import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Fragment that handles the sign in of a user
 */
public class SignInFragment extends Fragment {
    private View view;
    public static final String TAG = StartFragment.class.getSimpleName();
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private CallbackManager mCallbackManager;

    public SignInFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // [START auth_state_listener]
        mAuth = FirebaseAuth.getInstance();
        FacebookSdk.sdkInitialize(getContext());
        mCallbackManager = CallbackManager.Factory.create();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    if (user.getPhotoUrl() != null) {
                        Log.d("Login activity", "onAuthStateChanged:signed_in:" + user.getPhotoUrl().toString());
                    }
                    ((LoginActivity) getActivity()).setUserInfoAfterLogin(user);

                    GuiController.getInstance().startActivity(getContext(), Constants.ActivityTypes.MAINMENU, null);

                    ((LoginActivity) getActivity()).userLoggedIn();
                } else {
                    // User is signed out
                    Log.d("Login activity", "onAuthStateChanged:signed_out");
                }
                // [START_EXCLUDE]
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_sign_in, container, false);

        final EditText emailEditText = (EditText) view.findViewById(R.id.email);
        final EditText passwordEditText = (EditText) view.findViewById(R.id.pwd);
        final Button singInButton = (Button) view.findViewById(R.id.sinin);
        singInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (("").equals(emailEditText.getText().toString()) && ("").equals(passwordEditText.getText().toString())) {
                    showResult(getString(R.string.empt_field));
                } else
                    loginEmail(emailEditText.getText().toString(), passwordEditText.getText().toString());
            }
        });

        final TextView noAccount = (TextView) view.findViewById(R.id.signUp);
        noAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SignUpFragment signUp = new SignUpFragment();
                ((LoginActivity) getActivity()).replaceFragment(signUp, TAG);
            }
        });


        LoginButton loginButton = (LoginButton) view.findViewById(R.id.button_facebook_login);
        loginButton.setReadPermissions("email", "public_profile");
        loginButton.setFragment(this);
        loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "facebook:onSuccess:" + loginResult);
                ((LoginActivity) getActivity()).handleFacebookAccessToken(mAuth, loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
                // ...
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
                ((LoginActivity) getActivity()).onError(error.getMessage());
            }
        });

        return view;
    }

    /**
     * @param email
     * @param password Firebase will be notified that a user wants to sign in with his given email and password
     */
    private void loginEmail(String email, String password) {
        String emailProcessed = email.replaceAll(" ","");
        mAuth.signInWithEmailAndPassword("" + emailProcessed, "" + password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            ((LoginActivity) getActivity()).onError(task.getException().getMessage());
                        }
                    }
                });
    }

    @SuppressWarnings("ConstantConditions")
    private void showResult(String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
