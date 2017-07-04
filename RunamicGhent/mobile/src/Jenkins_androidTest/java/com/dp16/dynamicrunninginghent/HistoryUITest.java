package com.dp16.runamicghent;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.dp16.runamicghent.Activities.SplashScreen;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

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
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class HistoryUITest {

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
    public void runningActivity_CheckAfterRunSavedOneRunAdded() throws Exception {

        onView(withId(R.id.action_history)).perform(click());
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(1)));

        RecyclerViewItemCountAssertion rvCount = new RecyclerViewItemCountAssertion();
        onView(withId(R.id.rv)).check(rvCount);

        onView(withId(R.id.action_start)).perform(click());

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

        try{
            onView(withId(R.id.stop_button)).perform(click());
        }catch(NoMatchingViewException e){
            assertThat("server response longer then 5 seconds", false, is(true));
        }


        UiObject sureRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(sureRequest.exists()){
            sureRequest.click();
        }

        onView(withId(R.id.save)).perform(click());
        onView(withId(R.id.action_history)).perform(click());
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(1)));

        rvCount.setTestType(1);
        onView(withId(R.id.rv)).check(rvCount);

    }

    @Test
    public void runningActivity_CheckAfterRunDiscardNoRunAdded() throws Exception {

        onView(withId(R.id.action_history)).perform(click());
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(1)));

        RecyclerViewItemCountAssertion rvCount = new RecyclerViewItemCountAssertion();
        onView(withId(R.id.rv)).check(rvCount);

        onView(withId(R.id.action_start)).perform(click());

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
        Log.d("histest", "start button clicked");

        // wait during 5 seconds while "running"
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(5)));
        Log.d("histest", "waited 5 seconds");


        try{
            onView(withId(R.id.stop_button)).perform(click());
        }catch(NoMatchingViewException e){
            assertThat("server response longer then 5 seconds", false, is(true));
        }


        UiObject sureRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(sureRequest.exists()){
            sureRequest.click();
        }

        onView(withId(R.id.discard)).perform(click());

        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(5)));

        sureRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(sureRequest.exists()){
            sureRequest.click();
        }

        onView(withId(R.id.action_history)).perform(click());
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(1)));

        rvCount.setTestType(2);
        onView(withId(R.id.rv)).check(rvCount);

    }

    public class RecyclerViewItemCountAssertion implements ViewAssertion {
        private int previousCount = -1;
        private int testType;

        public RecyclerViewItemCountAssertion() {
            this.testType = 0;
        }

        public void setTestType(int testType){
            this.testType = testType;
        }

        /**
         * testType is used to check which test should be performed
         * testType == 0: initialize previous count
         * testType == 1: check that one entry was added
         * testType == 2: check that no entry was added
         * @param view
         * @param noViewFoundException
         */
        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }
            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.Adapter adapter = recyclerView.getAdapter();
            if(testType == 0){
                previousCount = adapter.getItemCount();
                assertThat(true, is(true));
            }
            else if(testType == 1) assertThat(adapter.getItemCount(), is(previousCount+1));
            else if(testType == 2) assertThat(adapter.getItemCount(), is(previousCount));
        }
    }


}
