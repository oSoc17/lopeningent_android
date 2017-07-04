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

package com.dp16.runamicghent.DataProvider;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;


/**
 * This class is used to smooth the location updates that are received from the Google FusionLocationProvider.
 * It uses the Apache implementation of a Kalman Filter, see {@link org.apache.commons.math3.filter.KalmanFilter}.
 * Created by Simon on 28/03/17.
 */
public class Kalman {

    private boolean isInitialised;

    private RealMatrix a;
    private RealVector x;
    private RealMatrix r;
    private KalmanFilter filter;


    public Kalman() {
        this.isInitialised = false;
        double dt = 1.0d;

        // State transition matrix
        a = new Array2DRowRealMatrix(new double[][]{
                {1d, 0d, dt, 0d},
                {0d, 1d, 0d, dt},
                {0d, 0d, 1d, 0d},
                {0d, 0d, 0d, 1d}
        });

        // control input matrix
        RealMatrix b = new Array2DRowRealMatrix(new double[][]{
                {Math.pow(dt, 2d) / 2},
                {Math.pow(dt, 2d) / 2},
                {dt},
                {dt}
        });

        // Measurement matrix
        RealMatrix h = new Array2DRowRealMatrix(new double[][]{
                {1d, 0d, 0d, 0d},
                {0d, 1d, 0d, 0d}
        });

        // Noise covariance matrix
        RealMatrix q = new Array2DRowRealMatrix(new double[][]{
                {Math.pow(dt, 4d) / 4d, 0d, Math.pow(dt, 3d) / 2d, 0d},
                {0d, Math.pow(dt, 4d) / 4d, 0d, Math.pow(dt, 3d) / 2d},
                {Math.pow(dt, 3d) / 2d, 0d, Math.pow(dt, 2d), 0d},
                {0d, Math.pow(dt, 3d) / 2d, 0d, Math.pow(dt, 2d)}
        });

        double measurementNoise = 10d;
        r = new Array2DRowRealMatrix(new double[][]{
                {Math.pow(measurementNoise, 2d), 0d},
                {0d, Math.pow(measurementNoise, 2d)}
        });

        //initialize to reading position and speeds?
        x = new ArrayRealVector(new double[]{51.0127, 3.708612, 0.0, 0.0});

        ProcessModel pm = new DefaultProcessModel(a, b, q, x, null);
        MeasurementModel mm = new DefaultMeasurementModel(h, r);
        filter = new KalmanFilter(pm, mm);

    }

    /**
     * Estimate the position by:
     * 1. predicting, based on previous events
     * 2. improving the state of the filter by looking at the error we made in step 1.
     *
     * @param position {@link android.location.Location} which contains location information.
     * @return {@link LatLng} containing the estimation we made.
     */
    public LatLng estimatePosition(Location position) {
        if (!this.isInitialised) {
            x.setEntry(0, position.getLatitude());
            x.setEntry(1, position.getLongitude());
            this.isInitialised = true;
        }

        filter.predict();

        // set noise
        r.setEntry(0, 0, Math.pow(position.getAccuracy(), 2d));
        r.setEntry(1, 1, Math.pow(position.getAccuracy(), 2d));

        // x = a*x (state prediction)
        x = new ArrayRealVector(new double[]{position.getLatitude(), position.getLatitude(), position.getSpeed(), position.getSpeed()});
        x = a.operate(x);

        // The measurement we got as an argument. (position X and Y)
        RealVector z = new ArrayRealVector(new double[]{position.getLatitude(), position.getLongitude()});

        //correct the state estimate
        filter.correct(z);

        // get the correct state - the position
        double pX = filter.getStateEstimation()[0];
        double pY = filter.getStateEstimation()[1];

        return new LatLng(pX, pY);
    }

}
