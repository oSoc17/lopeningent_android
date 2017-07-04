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

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.dp16.runamicghent.Constants;
import com.dp16.runamicghent.GuiController.GuiController;
import com.dp16.runamicghent.R;
import com.facebook.CallbackManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;


/**
 * Activity where the user can change his account info, such as:
 * - name
 * - photo
 * - password
 * <p>
 * This activity is accessed from the {@link SettingsActivity}.
 */
public class ChangeprofileActivity extends AppCompatActivity {
    public static final String TAG = ChangeprofileActivity.class.getSimpleName();

    private static final int RESULT_LOAD_IMAGE = 128;

    // Variables that hold the user's token, name, link to picture and email address.
    private String name;
    private String photoURL;
    private String email;
    private String oldEmail;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private CallbackManager mCallbackManager;
    private boolean wrongOldPw = false;
    private Uri photoUri;

    ImageView profilePic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mCallbackManager = CallbackManager.Factory.create();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };


        setContentView(R.layout.activity_changeprofile);
        getUserInfo();

        profilePic = (ImageView) findViewById(R.id.profile_picture);
        Picasso.with(this)
                .load(photoURL) //extract as User instance method
                .transform(new CropCircleTransformation())
                .into(profilePic);

        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogChangeProfile();
            }
        });

        final EditText editName = (EditText) findViewById(R.id.editName);
        editName.setText(name);
        final EditText editEmail = (EditText) findViewById(R.id.editEmail);
        editEmail.setText(email);
        final EditText editOldPw = (EditText) findViewById(R.id.editOldPw);
        final EditText editPw1 = (EditText) findViewById(R.id.editPw1);
        final EditText editPw2 = (EditText) findViewById(R.id.editPw2);


        Button save = (Button) findViewById(R.id.saveButton);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleInput(editOldPw, editName.getText().toString(), editEmail.getText().toString(), editOldPw.getText().toString(), editPw1.getText().toString(), editPw2.getText().toString());

            }
        });

    }

    /**
     * This method checks if every important textbox is filled in
     * This method also does the appropriate actions if something goes wrong with the input values
     *
     * @param editOldPw
     * @param nameText New name.
     * @param emailText New email.
     * @param oldPw Current password
     * @param pw1Text New password
     * @param pw2Text Verification of new password
     */
    public void handleInput(EditText editOldPw, String nameText, String emailText, String oldPw, String pw1Text, String pw2Text) {

        if (pw1Text.equals(pw2Text) && (pw1Text.length() > 5 || pw1Text.length() == 0) && oldPw.length() > 0) {
            editProfileFirebase(nameText, emailText, pw1Text, oldPw);
            editPreferences(nameText, emailText);
        } else if (oldPw.length() == 0) {
            editOldPw.setHint("Required");
            editOldPw.setHintTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorRed));
        } else if (pw1Text.length() < 6) {
            dialog(R.string.pw_to_weak);
        } else {
            Log.d(TAG, pw1Text);
            Log.d(TAG, pw2Text);
            dialog(R.string.pw_not_correct);
        }
    }

    /**
     * Changes the name and email of the user.
     * @param name New name.
     * @param email New emaiL.
     */
    public void editPreferences(String name, String email) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = preferences.edit();
        if (name != null && !name.isEmpty()) {
            editor.putString("client name", name);
        }
        if (email != null && !email.isEmpty()) {
            editor.putString("client email", email);
        }
        editor.apply();
    }

    private void editPhotUrlPreferences(String photoURL){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("photo URL", photoURL);
        editor.apply();
    }

    /**
     * @param email This method updates the email in Firebase
     */
    public void setFirebaseEmail(String email) {
        if (email != null && !email.isEmpty()) {
            user.updateEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User email address updated.");
                    } else {
                        Log.d(TAG, "User email address NOT updated.");
                    }
                }
            });
        }
    }

    /**
     * @param pw This method updates the password in Firebase
     */
    public void setFirebasePassword(String pw) {
        if (pw != null && !pw.isEmpty()) {
            user.updatePassword(pw).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password updated.");
                    } else {
                        Log.d(TAG, "Password updated." + task.getException().getMessage());
                    }
                }
            });
        }
        Map<String, Object> extras = new HashMap<>();
        extras.put("Fragment", 3);
        GuiController.getInstance().startActivity(this, Constants.ActivityTypes.MAINMENU, extras);
    }

    /**
     * @param name This method updates the name in Firebase
     */
    public void setFirebaseName(String name) {
        if (!("").equals(name)) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();
            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User profile updated.");
                            } else {
                                Log.d(TAG, "User profile NOT updated.");
                            }
                        }
                    });
        }
    }

    /**
     * This method updates the picture URL in Firebase
     */
    private void setFirebasePicture(){
        if (!("").equals(name) && photoUri!=null) {
            FirebaseStorage storage =  FirebaseStorage.getInstance();

            try{
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                StorageReference storageRef = storage.getReference();
                StorageReference mountainsRef = storageRef.child(photoUri.getPath());
                bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
                byte[] data = baos.toByteArray();

                UploadTask uploadTask = mountainsRef.putBytes(data);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.d("","");
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    @SuppressWarnings("VisibleForTests")
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                        photoUri = taskSnapshot.getDownloadUrl();
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setPhotoUri(photoUri)
                                .build();
                        photoURL = photoUri.toString();
                        editPhotUrlPreferences(photoURL);
                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "User picture updated.");
                                        } else {
                                            Log.d(TAG, "User picture NOT updated.");
                                        }
                                    }
                                });
                    }
                });
            }catch(IOException e){
                Log.e(TAG, "Error setting Firebase picture", e);
            }

        }
    }


    /**
     * @param name  This is the updated value of the name of the user
     * @param email This is the updated value of the email address of the user
     * @param pw    This is the new password
     * @param pwOld This is the old password
     *              The password and email address are protected in Firebase, therefore reauthentication is mandatory.
     *              The reauthentication needs the old password and old email address to sign in.
     *              After a successful  reauthentication are the values updated
     */
    public void editProfileFirebase(final String name, final String email, final String pw, String pwOld) {

        AuthCredential credential = EmailAuthProvider
                .getCredential(oldEmail, pwOld);
        // Prompt the user to re-provide their sign-in credentials
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d(TAG, "User re-authenticated.");
                        if (task.getException() != null) {
                            wrongOldPw = true;
                            Log.d(TAG, "Wrong old password!!!  " + wrongOldPw);
                            dialog(R.string.wrong_old_pw);
                        } else {
                            setFirebaseName(name);
                            setFirebasePicture();
                            setFirebaseEmail(email);
                            setFirebasePassword(pw);
                        }
                    }
                });
    }

    /**
     * @param text is the value of the string stored in strings.xml
     *             This method is generally used if info needs to shown to the user
     */
    public void dialog(int text) {
        new AlertDialog.Builder(this)
                .setTitle(text)
                .setMessage(R.string.try_again)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        Log.d(TAG, "something went wrong");
                    }
                })
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .show();
    }

    /**
     * Updates the user info in class.
     */
    public void getUserInfo() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        name = preferences.getString("client name", "");
        photoURL = preferences.getString("photo URL", "");
        email = preferences.getString("client email", "");
        oldEmail = email;
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

    public void dialogChangeProfile() {
        // Intent to choose picture from gallery
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto , RESULT_LOAD_IMAGE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            // Result for the loaded image from the gallery
            case RESULT_LOAD_IMAGE:
                if (resultCode == RESULT_OK){
                    // Get URI from image
                    photoUri = data.getData();
                    editPhotUrlPreferences(photoUri.toString());
                    // Show image with Picasso
                    Picasso.with(getApplicationContext())
                                .load(photoUri) //extract as User instance method
                                .transform(new CropCircleTransformation())
                                .into(profilePic);
                    // Set photoURL such that it can be saved to sharedPreferences
                    photoURL = photoUri.toString();
                }
                break;
            default:
                break;
        }
    }
}
