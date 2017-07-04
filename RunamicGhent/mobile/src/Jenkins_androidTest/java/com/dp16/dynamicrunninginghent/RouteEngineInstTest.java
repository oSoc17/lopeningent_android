package com.dp16.runamicghent;

import android.app.Activity;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;

import com.dp16.runamicghent.Activities.RunningScreen.RunningActivity;
import com.dp16.runamicghent.Activities.SplashScreen;
import com.dp16.runamicghent.RunData.RunRoute;
import com.dp16.runamicghent.StatTracker.RouteEngine;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.dp16.eventbroker.EventPublisherClass;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Stiaan on 13/05/2017.
 */
public class RouteEngineInstTest {

    Activity activity;
    private EventBroker broker;
    private RunRoute runRoute;
    RouteEngine routeEngine;
    private static final long waitingTime = 100; // ms

    @Rule
    public ActivityTestRule<RunningActivity> mActivityRule = new ActivityTestRule<>(
            RunningActivity.class, false, false);//false false so activity isn't started automatically(not really necessary here)

    @Before
    public void Initialize(){
        //activity = Robolectric.buildActivity(RunningActivity.class).create().get();
        try {
            String json = new String("{\"coordinates\": [ { \"lat\": 51.0386722, \"c\": \"none\", \"lon\": 3.730139 }, { \"lat\": 51.0386317, \"c\": \"left\", \"lon\": 3.7301503 }, { \"lat\": 51.038596, \"c\": \"none\", \"lon\": 3.7301377 } ] }");
            runRoute = new RunRoute(new JSONObject(json));
        } catch (JSONException e) {
            Log.e("RunRouteTests", e.getMessage());
        }

        broker = EventBroker.getInstance();
        routeEngine = new RouteEngine((RunningActivity) activity);
    }

    @Test
    public void RouteEngine_StartStop(){
        routeEngine = new RouteEngine((RunningActivity) activity);
        routeEngine.start();
        routeEngine.stop();
    }

    @Test
    public void RouteEngine_handleEvent(){

        routeEngine.start();
        broker.start();

        routeEngine.startRunning();

        EventPublisherClass publisherClass = new EventPublisherClass();
        publisherClass.publishEvent(Constants.EventTypes.LOCATION, new LatLng(51.0386318, 3.7301504));

        publisherClass.publishEvent(Constants.EventTypes.ABNORMAL_HEART_RATE, "");
        publisherClass.publishEvent("randomString", "");

        routeEngine.stop();
        broker.stop();
    }

    @Test
    public void RouteEngine_RequestTrack() throws JSONException {

        final AtomicInteger messagesReceived = new AtomicInteger(0);
        // listener for TRACK_REQUEST events
        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                boolean boolMessage = (Boolean) message;
                if (boolMessage) {
                    messagesReceived.incrementAndGet();
                }
            }
        };

        broker.addEventListener(Constants.EventTypes.TRACK_REQUEST, listener);
        routeEngine.start();
        broker.start();

        String json = new String("{\"coordinates\": [ { \"lat\": 51.0386722, \"c\": \"none\", \"lon\": 3.730139 }, { \"lat\": 51.0386317, \"c\": \"left\", \"lon\": 3.7301503 }, { \"lat\": 51.038596, \"c\": \"none\", \"lon\": 3.7301377 } ] }");
        JSONObject track = new JSONObject(json);
        TrackResponse trackResponse = new TrackResponse(track, false, 1);

        routeEngine.handleEvent(Constants.EventTypes.TRACK, trackResponse);

        routeEngine.requestTrackDynamic(RouteEngine.DynamicRouteType.HOME);


        testUtils.waitUntilAtomicVariableReachesValue(25, 40, "RouteEngine did not publish a TRACK_REQUEST event within ", messagesReceived, 1);
        broker.stop();
        routeEngine.stop();
    }


    @Test
    public void routeEngine_publishEvent_locationInRadius(){
        final AtomicInteger received = new AtomicInteger(0);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                received.incrementAndGet();
            }
        };

        broker.addEventListener(Constants.EventTypes.NAVIGATION_DIRECTION, listener);
        routeEngine.start();
        broker.start();

        EventPublisherClass publisherClass = new EventPublisherClass();
        publisherClass.publishEvent(Constants.EventTypes.LOCATION, new LatLng(51.0386318, 3.7301504));

        //wait for publisher to publish... (thread of publisher is slower than this main thread)
        testUtils.waitOneSecUntilAtomicVariableReachesValue("RouteEngine did not publish Navigation Direction Event", received, 1);

        broker.stop();
        routeEngine.stop();
    }

    @Test
    public void routeEngine_publishEvent_locationInRadiusTwice(){
        final AtomicInteger received = new AtomicInteger(0);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                received.incrementAndGet();
            }
        };

        broker.addEventListener(Constants.EventTypes.NAVIGATION_DIRECTION, listener);
        routeEngine.start();
        broker.start();

        EventPublisherClass publisherClass = new EventPublisherClass();
        publisherClass.publishEvent(Constants.EventTypes.LOCATION, new LatLng(51.0386318, 3.7301504));

        //wait for publisher to publish... (thread of publisher is slower than this main thread)
        try {
            Thread.sleep(waitingTime);
        } catch (InterruptedException e) {
            // no actions needed
        }
        publisherClass.publishEvent(Constants.EventTypes.LOCATION, new LatLng(51.0386316, 3.7301502));

        //wait for publisher to publish... (thread of publisher is slower than this main thread)
        testUtils.waitOneSecUntilAtomicVariableReachesValue("RouteEngine published the same Navigation Direction event twice", received, 1);

        broker.stop();
        routeEngine.stop();
    }
}
