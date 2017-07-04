package com.dp16.runamicghent;

import android.app.Activity;
import android.location.Location;
import android.support.test.rule.ActivityTestRule;

import com.dp16.runamicghent.Activities.SplashScreen;
import com.dp16.runamicghent.DataProvider.LocationProvider;
import com.dp16.eventbroker.EventBroker;
import com.dp16.eventbroker.EventListener;
import com.google.android.gms.location.LocationServices;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static com.dp16.runamicghent.testUtils.waitOneSecUntilAtomicVariableReachesValue;
import static org.junit.Assert.fail;

/**
 * Created by Stiaan on 9/05/2017.
 */

public class LocationProviderInstTest {

    private EventBroker broker;
    private LocationProvider provider;

    @Rule
    public ActivityTestRule<SplashScreen> mActivityRule = new ActivityTestRule<>(
            SplashScreen.class);

    @Before
    public void init(){
        broker = EventBroker.getInstance();
        Activity activity = mActivityRule.getActivity();
        provider = new LocationProvider(activity);
    }

    @Test
    public void LocationProvider_publishesToEventBroker(){
        final AtomicInteger messagesReceivedRaw = new AtomicInteger(0);
        final AtomicInteger messagesReceived = new AtomicInteger(0);

        EventListener listener = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messagesReceived.incrementAndGet();
            }
        };

        EventListener listenerRaw = new EventListener() {
            @Override
            public void handleEvent(String eventType, Object message) {
                messagesReceivedRaw.incrementAndGet();
            }
        };

        broker.addEventListener(Constants.EventTypes.RAW_LOCATION, listenerRaw);
        broker.addEventListener(Constants.EventTypes.LOCATION, listener);

        broker.start();
        provider.start();

        Location mockLocation = new Location("blabla");
        mockLocation.setLatitude(51);
        mockLocation.setLongitude(3);
        mockLocation.setTime(System.currentTimeMillis());

        // wait until the GoogleApiClient is connected
        int i = 0;
        while(!provider.getGoogleApiClient().isConnected()){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e){
                //
            }
            i++;
            if(i > 100){
                fail("GoogleApiClient not connected within 10 seconds");
            }
        }

        try {
            LocationServices.FusedLocationApi.setMockLocation(provider.getGoogleApiClient(), mockLocation);
        } catch (SecurityException e){
            e.printStackTrace();
        }

        waitOneSecUntilAtomicVariableReachesValue("LocationProvider did not publish a location message within ", messagesReceived, 1);
        waitOneSecUntilAtomicVariableReachesValue("LocationProvider did not publish a raw location message within ", messagesReceivedRaw, 1);

        provider.stop();
        broker.stop();
    }
}
