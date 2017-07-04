package com.dp16.runamicghent;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;

import com.dp16.runamicghent.Activities.SplashScreen;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.dp16.runamicghent.testUtils.loginWithTestAccount;
import static com.dp16.runamicghent.testUtils.setAllToolTipsAsDone;
import static com.dp16.runamicghent.testUtils.waitFor;
import static org.hamcrest.Matchers.is;

/**
 * Created by Stiaan on 12/04/2017.
 */
public class RouteUITest {
    UiDevice device;

    @Rule
    public ActivityTestRule<SplashScreen> mActivityRule = new ActivityTestRule<>(
            SplashScreen.class, false, false);//false false so activity isn't started automatically(not really necessary here)

    @Before
    public void initialize() throws Exception{

        mActivityRule.launchActivity(null);

        Activity activity = mActivityRule.getActivity();
        Context context = activity.getApplicationContext();

        setAllToolTipsAsDone(context);

        SharedPreferences.Editor preferencesEditor;
        preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        preferencesEditor.clear();
        preferencesEditor.commit();
        preferencesEditor.putBoolean("pref_key_debug_heartbeat_mock", true);
        preferencesEditor.putBoolean("pref_key_debug_fake_latlng_locationprovider", true);
        preferencesEditor.putBoolean("pref_key_debug_location_mock", true);
        preferencesEditor.commit();

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiObject signinpage = device.findObject(new UiSelector().text("SIGN IN"));
        if (signinpage.exists()) {
            loginWithTestAccount();
        }

    }

    @Test
    public void runningActivity_SimulateRunInApp() throws Exception {

        onView(withId(R.id.generate_route)).perform(click());
        UiObject continueRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(continueRequest.exists()){
            //throw new AssertionError("View with text <" + allowRequest.getText() + "> found!");
            continueRequest.click();
        }


        // wait during 5 seconds for generation
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(5)));

        onView(withId(R.id.start_button)).check(matches(isDisplayed()));

        onView(withId(R.id.start_button)).check(matches(isClickable()));

        onView(withId(R.id.start_button)).perform(click());

        // wait during 5 seconds while "running"
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(15)));

        try{
            onView(withId(R.id.stop_button)).perform(click());
        }catch(NoMatchingViewException e){
            assertThat("server response longer than 5 seconds", true, is(false));
        }


        UiObject sureRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(sureRequest.exists()){
            sureRequest.click();
        }

        onView(withId(R.id.discard)).perform(click());

    }

    @Test
    public void runningActivity_SimulateDynamicRequestRunInApp() throws Exception {

        onView(withId(R.id.generate_route)).perform(click());
        UiObject continueRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(continueRequest.exists()){
            //throw new AssertionError("View with text <" + allowRequest.getText() + "> found!");
            continueRequest.click();
        }


        // wait during 5 seconds for generation
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(5)));

        onView(withId(R.id.start_button)).check(matches(isDisplayed()));

        onView(withId(R.id.start_button)).check(matches(isClickable()));

        onView(withId(R.id.start_button)).perform(click());

        // wait during 5 seconds while "running"
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(5)));

        onView(withId(R.id.minusButton)).perform(click());
        // wait during 5 seconds while "running"
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(5)));

        onView(withId(R.id.plusButton)).perform(click());

        // wait during 5 seconds while "running"
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(5)));

        try{
            onView(withId(R.id.stop_button)).perform(click());
        }catch(NoMatchingViewException e){
            assertThat("server response longer than 5 seconds", true, is(false));
        }


        UiObject sureRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(sureRequest.exists()){
            sureRequest.click();
        }

        onView(withId(R.id.discard)).perform(click());

    }

}
