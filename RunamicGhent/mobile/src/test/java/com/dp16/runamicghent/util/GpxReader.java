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

package com.dp16.runamicghent.util;

import android.location.Location;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that is responsible for reading in a GPX file.
 * To be used in simulation testing.
 * Please note that this class is not robust. Giving it a file that does not follow the assumptions WILL throw uncatched exceptions
 * <p>
 * Created by Nick on 21-3-2017.
 */

public class GpxReader {
    private String gpxFilename;
    private final static String lengthIndicator = "<gpsies:trackLengthMeter>";

    /**
     * Makes a new GpxReader associated with a filename.
     *
     * @param filename Name of the file this class will read.
     */
    public GpxReader(String filename) {
        gpxFilename = filename;
    }

    /**
     * @return The length of the track as found in the gpx file
     */
    public double getTrackLength() {
        double result = 0.0;
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(gpxFilename);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            String line = reader.readLine();

            while (line != null) {
                int indexLengthIndicator = line.indexOf(lengthIndicator);
                if (indexLengthIndicator != -1) {
                    int indexClosingTag = line.indexOf("</", indexLengthIndicator);
                    String lengthAsString = line.substring(indexLengthIndicator + lengthIndicator.length(), indexClosingTag);
                    result = Double.parseDouble(lengthAsString);
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * @return The gps points in the gpx file in {@link Location} format with an accuracy of 0.01 and a speed of 12 km/h.
     */
    public List<Location> getLocations() {
        List<Location> result = new ArrayList<>();

        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(gpxFilename);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            String line = reader.readLine();

            while (line != null) {
                String trackPointPattern = ".*<trkpt lat=\"(\\d+\\.\\d+).*(\\d+\\.\\d+).*";
                Pattern pattern = Pattern.compile(trackPointPattern);
                Matcher matcher = pattern.matcher(line);

                // if we have found the first line of a trackpoint -> read it
                if (matcher.find()) {
                    Location newLocation = new Location("GpxReader");
                    newLocation.setLatitude(Double.parseDouble(matcher.group(1)));
                    newLocation.setLongitude(Double.parseDouble(matcher.group(2)));

                    // read the next line which contains the elevation (not used)
                    reader.readLine();

                    // read the next line which contains the time
                    line = reader.readLine();
                    String timePattern = ".*<time>(\\d+)-(\\d+)-(\\d+)T(\\d+):(\\d+):(\\d+)Z</time>.*";
                    pattern = Pattern.compile(timePattern);
                    matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.YEAR, 1900 + Integer.parseInt(matcher.group(1)));
                        calendar.set(Calendar.MONTH, Integer.parseInt(matcher.group(2)));
                        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(matcher.group(3)));
                        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(matcher.group(4)));
                        calendar.set(Calendar.MINUTE, Integer.parseInt(matcher.group(5)));
                        calendar.set(Calendar.SECOND, Integer.parseInt(matcher.group(6)));
                        newLocation.setTime(calendar.getTimeInMillis());
                        newLocation.setAccuracy(0.01f);

                        // calculate the speed (given as 12 km/h)
                        newLocation.setSpeed(12.0f / 3.6f);

                        result.add(newLocation);
                    } else {
                        // this means we have a corrupted file
                        System.err.println("The file " + gpxFilename + " does not have the expected layout.");
                        System.exit(-1);
                    }
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
