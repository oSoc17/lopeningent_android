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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.dp16.runamicghent.R;

import java.io.IOException;
import java.io.InputStream;

public class LicenseActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        ((TextView)findViewById(R.id.license_text_apache2)).setText(getTextFromFile(R.raw.license_apache2));
        ((TextView)findViewById(R.id.license_text_materialview)).setText(getTextFromFile(R.raw.license_materialshowcaseview));
        ((TextView)findViewById(R.id.license_text_peekandpop)).setText(getTextFromFile(R.raw.license_peekandpop));
        ((TextView)findViewById(R.id.license_text_picasso)).setText(getTextFromFile(R.raw.license_picasso));
        ((TextView)findViewById(R.id.license_text_mongo_java_driver)).setText(getTextFromFile(R.raw.license_mongo_java_driver));
    }

    /**
     * Auxiliary method that reads the content from an asset text file and returns it as a String.
     * @param file R. resource int
     * @return String containing file contents.
     */
    private String getTextFromFile(int file) {

        try {
            InputStream is = getResources().openRawResource(file);

            /* We guarantee that the available method returns the total
            * size of the asset...  of course, this does mean that a single
            * asset can't be more than 2 gigs.
            */
            int size = is.available();

            // Read the entire asset into a local byte buffer.
            byte[] buffer = new byte[size];
            int r = is.read(buffer);
            if (r == -1){
                Log.e("LicenceActivity", "Unable to load licence");
            }
            is.close();

            return new String(buffer);

        } catch (IOException e) {
            // Should never happen!
            Log.e("ERROR", e.getMessage(), e);
            return "Error loading file.";
        }
    }
}

