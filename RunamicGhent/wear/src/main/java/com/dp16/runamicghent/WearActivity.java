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

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisher;
import com.dp16.eventbroker.EventPublisherClass;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by Stiaan on 7/03/2017.
 * This is the main activity of the wearable device. It shows statistics(speed, duration, bpm) during a run
 * The wearable app has no interactive elements.
 */
public class WearActivity extends WearableActivity implements EventListener, EventPublisher {

    private TextView timerTextView;
    private TextView heartRateTextView;
    private ImageView navigationArrowView;
    private GoogleApiClient mGoogleApiClient;

    //state of the user, running or not
    private boolean running = false;

    private static final String TAG = "WearActivity";

    //current heart rate (from most recent heart rate update)
    private static volatile int currentHeart = 0;
    //current speed (from most recent speed update)
    private static volatile String currentSpeed = "0";
    private static volatile String currentDistance= "0";

    //look if the activity is running already, if it is set this to active
    private static boolean active = false;

    //variables for requesting permission for body sensors
    private String[] permissions = {"android.permission.BODY_SENSORS"};
    private int[] grantResult = {0};

    private Vibrator vibrator;

    private ImageView runnerImageView;

    long startTime = 0;
    long lastTime = 0;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            if(millis < 0){
                millis = 0;
                startTime = System.currentTimeMillis();
            }
            lastTime = System.currentTimeMillis();
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            timerTextView = (TextView) findViewById(R.id.timer);
            timerTextView.setText(String.format("%d:%02d", minutes, seconds));

            timerHandler.postDelayed(this, 500);
        }
    };

    //handler for removing the shown navigation arrow after some time
    Handler navigationHandler = new Handler();
    Runnable navigationRunnable = new Runnable() {

        @Override
        public void run() {

            navigationArrowView = (ImageView) findViewById(R.id.navigationview);
            navigationArrowView.setVisibility(View.INVISIBLE);
        }
    };

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean active) {
        WearActivity.active = active;
    }


    /**
     * create the main wearable activity
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //ask for permission
        while (checkSelfPermission(permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, 1);

            onRequestPermissionsResult(1, permissions, grantResult);

        }
        Log.i(TAG, "We have permission");

        WearActivity.setActive(true);

        //start event broker and add listeners
        EventBroker.getInstance().start();

        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.START_MOBILE, this);
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.STOP_MOBILE, this);
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.PAUSE_MOBILE, this);
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.NAVIGATE, this);
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.HEART_RESPONSE, this);

        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.TIME_MOBILE, this);
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.RUN_STATE_START_MOBILE, this);
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.RUN_STATE_PAUSED_MOBILE, this);
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.SPEED_MOBILE, this);
        EventBroker.getInstance().addEventListener(ConstantsWatch.EventTypes.DISTANCE_MOBILE, this);

        timerTextView = (TextView) findViewById(R.id.timer);
        heartRateTextView = (TextView) findViewById(R.id.heartrate);
        navigationArrowView = (ImageView) findViewById(R.id.navigationview);
        runnerImageView = (ImageView) findViewById(R.id.runner);

        setContentView(R.layout.activity_wear);
        setAmbientEnabled();

        //initialize other classes
        createModules();
    }

    /**
     * used for initializing the wearcomm, vibrator, google APi and heartratesensor
     */
    private void createModules() {

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        WearComm mComm = new WearComm();

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(mComm)
                    .addOnConnectionFailedListener(mComm)
                    .build();

            mGoogleApiClient.connect();
        }

        WearComm.setGoogleApiClient(mGoogleApiClient);
        //initialize the latch
        WearComm.setLatch();
        mComm.setEventListeners();

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        new HeartRateSensor(sensorManager);

    }

    /**
     * when an event to start the run has been received, start the heart measure, initialize the time
     * start the timer and set the running state to true + show runner icon
     */
    public void startRun() {
        EventPublisherClass publisher = new EventPublisherClass();
        publisher.publishEvent(ConstantsWatch.EventTypes.HEART_MEASURE_START, "");

        Handler mainHandler = new Handler(this.getMainLooper());
        Runnable runnable;

        //if run had not yet started, start a new timer, else start a timer from the current duration( received from mobile)
        if (startTime == 0)
            startTime = System.currentTimeMillis();
        else
            startTime += System.currentTimeMillis() - lastTime;
        //start timer
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, 0);
        running = true;

        runnable = new Runnable() {
            @Override
            public void run() {
                showRunnerIcon();
            }
        };
        mainHandler.post(runnable);
    }


    /**
     * set running state to false and stop timer + hide runner icon, keep measuring heart rate
     */
    public void pauseRun() {
        EventPublisherClass publisher = new EventPublisherClass();
        publisher.publishEvent(ConstantsWatch.EventTypes.HEART_MEASURE_STOP, "");

        Handler mainHandler = new Handler(this.getMainLooper());
        Runnable runnable;

        //stop timer
        timerHandler.removeCallbacks(timerRunnable);
        running = false;

        runnable = new Runnable() {
            @Override
            public void run() {
                hideRunnerIcon();
            }
        };
        mainHandler.post(runnable);
    }
    /**
     * stop heart measurement, set running state false, stop timer, hide icon
     */
    public void stopRun() {
        EventPublisherClass publisher = new EventPublisherClass();
        publisher.publishEvent(ConstantsWatch.EventTypes.HEART_MEASURE_STOP, "");

        Handler mainHandler = new Handler(this.getMainLooper());
        Runnable runnable;

        //stop timer
        timerHandler.removeCallbacks(timerRunnable);
        running = false;
        startTime = 0;
        lastTime = 0;

        runnable = new Runnable() {
            @Override
            public void run() {
                hideRunnerIcon();
            }
        };
        mainHandler.post(runnable);
    }

    private void showRunnerIcon() {
        runnerImageView = (ImageView) findViewById(R.id.runner);
        runnerImageView.setVisibility(View.VISIBLE);
    }

    private void hideRunnerIcon() {
        runnerImageView = (ImageView) findViewById(R.id.runner);
        runnerImageView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }


    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    /**
     * when paused set the buttons to unclickable
     */
    @Override
    public void onPause() {
        Log.i(TAG, "PAUSE");
        super.onPause();

    }

    /**
     * set buttons to clickable again, start the timer where it left off
     */
    @Override
    public void onResume() {
        Log.i(TAG, "RESUME");
        super.onResume();
        //ask mobile device for current state
        EventBroker.getInstance().addEvent(ConstantsWatch.EventTypes.REQUEST_STATE_WEAR, "", this);
        if (running) {
            if (startTime == 0)
                startTime = System.currentTimeMillis();
            else
                startTime += System.currentTimeMillis() - lastTime;
            timerHandler.postDelayed(timerRunnable, 0);
        }

    }

    @Override
    public void onStop() {
        Log.i(TAG, "STOP");
        super.onStop();
    }

    /**
     * stop all components and remove listeners
     */
    @Override
    protected void onDestroy() {
        Log.i(TAG, "DESTROY");
        WearActivity.setActive(false);
        running = false;
        startTime = 0;
        EventPublisherClass publisher = new EventPublisherClass();
        publisher.publishEvent(ConstantsWatch.EventTypes.HEART_MEASURE_STOP, "");
        publisher.publishEvent(ConstantsWatch.EventTypes.ON_STOP,"");
        EventBroker.getInstance().removeEventListener(this);
        super.onDestroy();

    }

    private void updateDisplay() {
        //not implemented
    }

    /**
     * show the appropriate arrow
     * @param i indicating the direction (-1 = left, 0 = forward, 1 = right, 2 = uturn)
     */
    public void showNavigation(int i) {
        switch (i) {
            case -1:
                runLeftArrow();
                break;
            case 0:
                runStraightArrow();
                break;
            case 1:
                runRightArrow();
                break;
            case 2:
                runUTurnArrow();
                break;
            default:
                break;
        }
    }

    private void runUTurnArrow() {
        Handler mainHandler = new Handler(this.getMainLooper());
        Runnable runnable;

        runnable = new Runnable() {
            @Override
            public void run() {
                setUTurnArrow();
            }
        };
        mainHandler.post(runnable);
    }

    /**
     * put a runnable for the main handler to show the forward arrow
     */
    private void runStraightArrow() {
        Handler mainHandler = new Handler(this.getMainLooper());
        Runnable runnable;

        runnable = new Runnable() {
            @Override
            public void run() {
                setStraightArrow();
            }
        };
        mainHandler.post(runnable);
    }
    /**
     * put a runnable for the main handler to show the right arrow
     */
    private void runRightArrow() {
        Handler mainHandler = new Handler(this.getMainLooper());
        Runnable runnable;

        runnable = new Runnable() {
            @Override
            public void run() {
                setRightArrow();
            }
        };
        mainHandler.post(runnable);
    }
    /**
     * put a runnable for the main handler to show the left arrow
     */
    private void runLeftArrow() {
        Handler mainHandler = new Handler(this.getMainLooper());
        Runnable runnable;

        runnable = new Runnable() {
            @Override
            public void run() {
                setLeftArrow();
            }
        };
        mainHandler.post(runnable);
    }

    /**
     * set the correct arrow and vibrate shortly, also post a handler removing the arrow after several seconds
     * if any callback for removing was already posted for the runnable remove these first,
     * to avoid removing arrows to fast if navigation instructions follow in quick succession
     */
    private void setRightArrow() {
        vibrator.vibrate(300);
        navigationArrowView = (ImageView) findViewById(R.id.navigationview);
        navigationArrowView.setImageResource(R.drawable.arrowright);
        navigationArrowView.setVisibility(View.VISIBLE);
        navigationHandler.removeCallbacks(navigationRunnable);
        navigationHandler.postDelayed(navigationRunnable, 5000);
    }

    private void setStraightArrow() {
        vibrator.vibrate(300);
        navigationArrowView = (ImageView) findViewById(R.id.navigationview);
        navigationArrowView.setImageResource(R.drawable.arrowstraight);
        navigationArrowView.setVisibility(View.VISIBLE);
        navigationHandler.removeCallbacks(navigationRunnable);
        navigationHandler.postDelayed(navigationRunnable, 5000);
    }

    private void setLeftArrow() {
        vibrator.vibrate(300);
        navigationArrowView = (ImageView) findViewById(R.id.navigationview);
        navigationArrowView.setImageResource(R.drawable.arrowleft);
        navigationArrowView.setVisibility(View.VISIBLE);
        navigationHandler.removeCallbacks(navigationRunnable);
        navigationHandler.postDelayed(navigationRunnable, 5000);

    }


    private void setUTurnArrow() {
        vibrator.vibrate(300);
        navigationArrowView = (ImageView) findViewById(R.id.navigationview);
        navigationArrowView.setImageResource(R.drawable.uturnarrow);
        navigationArrowView.setVisibility(View.VISIBLE);
        navigationHandler.removeCallbacks(navigationRunnable);
        navigationHandler.postDelayed(navigationRunnable, 5000);
    }

    public static void setCurrentHeart(int currentHeart) {
        WearActivity.currentHeart = currentHeart;
    }

    public void setHeartRateText(String heartRateText) {
        heartRateTextView = (TextView) findViewById(R.id.heartrate);
        heartRateTextView.setText(heartRateText);
    }

    public void setHeartRateText() {
        heartRateTextView = (TextView) findViewById(R.id.heartrate);
        heartRateTextView.setText(String.valueOf(currentHeart));
    }

    private static void setCurrentSpeed(String curSp) {
        currentSpeed = curSp;
    }
    private static void setCurrentDistance(String curDist) {
        currentDistance = curDist;
    }

    private void setSpeedText() {
        TextView speedTextView = (TextView) findViewById(R.id.speed);
        speedTextView.setText(currentSpeed);
    }

    private void setDistanceText() {
        TextView speedTextView = (TextView) findViewById(R.id.distance);
        speedTextView.setText(currentDistance);
    }

    /**
     * handle the UI part of received events
     * @param eventType
     * @param message
     */
    @Override
    public void handleEvent(String eventType, Object message) {

        switch (eventType) {
            case ConstantsWatch.EventTypes.HEART_RESPONSE:
                setCurrentHeart((int) message);
                updateHeartRateText();
                break;
            case ConstantsWatch.EventTypes.START_MOBILE:
            case ConstantsWatch.EventTypes.RUN_STATE_START_MOBILE:
                startRun();
                break;
            case ConstantsWatch.EventTypes.STOP_MOBILE:
                stopRun();
                setInactiveHeartRateText();
                break;
            case ConstantsWatch.EventTypes.NAVIGATE:
                showNavigation((int) message);
                break;
            case ConstantsWatch.EventTypes.PAUSE_MOBILE:
            case ConstantsWatch.EventTypes.RUN_STATE_PAUSED_MOBILE:
                pauseRun();
                break;
            case ConstantsWatch.EventTypes.TIME_MOBILE:
                startTime = System.currentTimeMillis()- (int)message;
                break;
            case ConstantsWatch.EventTypes.SPEED_MOBILE:
                setCurrentSpeed(message.toString());
                updateCurrentSpeed();
                break;
            case ConstantsWatch.EventTypes.DISTANCE_MOBILE:
                setCurrentDistance(message.toString());
                updateCurrentDistance();
                break;
            default:
                Log.e(TAG, "event not recognized");
        }
    }



    /**
     * update the UI with the current speed
     */
    private void updateCurrentSpeed() {
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
        Runnable runnable;
        runnable = new Runnable() {
            @Override
            public void run() {
                setSpeedText();
            }
        };
        mainHandler.post(runnable);

    }

    /**
     * update the UI with the current distance
     */
    private void updateCurrentDistance() {
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
        Runnable runnable;
        runnable = new Runnable() {
            @Override
            public void run() {
                setDistanceText();
            }
        };
        mainHandler.post(runnable);
    }

    /**
     * update the UI with the current heart rate
     */
    private void updateHeartRateText() {
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
        Runnable runnable;

        runnable = new Runnable() {
            @Override
            public void run() {
                setHeartRateText();
            }
        };
        mainHandler.post(runnable);
    }

    /**
     * when no measurement is taken, show "--" as heart rate value
     */
    private void setInactiveHeartRateText() {
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
        Runnable runnable;

        runnable = new Runnable() {
            @Override
            public void run() {
                setHeartRateText("--");
            }
        };
        mainHandler.post(runnable);
    }

}
