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

package com.dp16.runamicghent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.google.android.gms.wearable.PutDataMapRequest;

/**
 * Created by Stiaan on 5/03/2017.
 * This class provides an interface to the hardware heartbeat sensor in the wearable device and provides this data to other wear classes
 */

public class HeartRateSensor implements SensorEventListener, EventListener, EventPublisher{

    private static final String TAG = "HRS";
    private static final String ACCURACY = " Accuracy"; //the accuracy of the measurement of the sensor

    private SensorManager sensorManager;
    private PutDataMapRequest sensorData;
    
    private float heartRate = 0;

    HeartRateSensor(SensorManager sensMan){

        sensorManager = sensMan;
        //add eventlisteners
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.HEART_MEASURE_START, this);
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.HEART_MEASURE_STOP, this);
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.ON_STOP, this);

    }

    /**
     * start reading values with the sensor, new measurements are processed in onSensorChanged
     */
    public void startSensorListeners() {

        Sensor mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        if(mSensor == null){
            Log.e(TAG, "no heart rate sensor found");
        }
        else {
            Log.d(TAG, "startSensorListeners");
            sensorData = PutDataMapRequest.create("/sensordata");
            sensorData.getDataMap().putLong("Timestamp", System.currentTimeMillis());
            float[] empty = new float[0];

            sensorData.getDataMap().putFloatArray(mSensor.getName(), empty);
            sensorData.getDataMap().putInt(mSensor.getName() + ACCURACY, 0);
            sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override // SensorEventListener
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        //No accuracy changes are being taken into account
    }

    /**
     * this takes place when a new measurement is read
     * @param event
     */
    @Override // SensorEventListener
    public final void onSensorChanged(SensorEvent event) {

        String key = event.sensor.getName();
        float[] values = event.values;
        int accuracy = event.accuracy;

        //save the measurement
        sensorData.getDataMap().putFloatArray(key, values);
        for(float value: values){
            Log.e(TAG, value+" " + accuracy);
            //if the heartrate differs atleast 5bpm(to avoid to much messages) and is not a bad measurement(value = 0) or accuracy <=1) send it to the WearComm
            if(Math.abs(heartRate-value)>5 && value > 0 && accuracy > 1){
                heartRate = value;
                EventBroker.getInstance().addEvent(ConstantsWatch.EventTypes.HEART_RESPONSE, Math.round(heartRate), this);
            }

        }
        sensorData.getDataMap().putInt(key + ACCURACY, event.accuracy);

    }

    public void stopSensorListeners() {

        Log.d(TAG, "stopSensorListeners");
        sensorManager.unregisterListener(HeartRateSensor.this);
    }


    /**
     * handle the events published by the wearcomm and wearActivity
     * @param eventType
     * @param message
     */
    @Override
    public void handleEvent(String eventType, Object message) {
        switch (eventType) {
            case ConstantsWatch.EventTypes.HEART_MEASURE_START:
                //start the sensor
                startSensorListeners();
                break;
            case ConstantsWatch.EventTypes.HEART_MEASURE_STOP:
                //stop the sensor
                stopSensorListeners();
                break;
            case ConstantsWatch.EventTypes.ON_STOP:
                EventBroker.getInstance().removeEventListener(this);
                break;
            default:
                Log.e(TAG, "event not recognized");
                break;
        }
    }
}
