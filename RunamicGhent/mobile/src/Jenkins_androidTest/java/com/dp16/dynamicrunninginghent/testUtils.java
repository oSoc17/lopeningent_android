package com.dp16.runamicghent;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.view.View;

import org.hamcrest.Matcher;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.fail;

/**
 * Created by Stiaan on 20/04/2017.
 */

public class testUtils {

    //this method crashes test but can be used to clear app data before testing
    public void clearAppData() {
        try {
            // clearing app data
            Runtime runtime = Runtime.getRuntime();
            runtime.exec("pm clear com.dp16.dynamicrunninginghent");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Perform action of waiting for a specific time.
     */
    public static ViewAction waitFor(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Wait for " + millis + " milliseconds.";
            }

            @Override
            public void perform(UiController uiController, final View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }

    public static void loginWithTestAccount() throws UiObjectNotFoundException{
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        //login
        onView(withId(R.id.email)).perform(typeText("test@test.test"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.pwd)).perform(typeText("testtest"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.sinin)).perform(click());
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(3)));

        UiObject allowRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(allowRequest.exists()){
            //throw new AssertionError("View with text <" + allowRequest.getText() + "> found!");
            allowRequest.click();
        }
    }

    public static void logoutUser() throws UiObjectNotFoundException {

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiObject allowRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(allowRequest.exists()){
            allowRequest.click();
        }


        onView(withId(R.id.action_settings)).perform(click());

        UiScrollable listView = new UiScrollable(new UiSelector().className("android.widget.LinearLayout"));

        listView.scrollToEnd(10);

        onView(withId(R.id.logout))
                .perform(click());

        UiObject continueRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(continueRequest.exists()){
            //throw new AssertionError("View with text <" + allowRequest.getText() + "> found!");
            continueRequest.click();
        }
    }

    public static void setAllToolTipsAsDone(Context context){

        SharedPreferences internal = context.getSharedPreferences("material_showcaseview_prefs", 0);
        int isFinished = uk.co.deanwild.materialshowcaseview.PrefsManager.SEQUENCE_FINISHED;
        int isNotFinished = uk.co.deanwild.materialshowcaseview.PrefsManager.SEQUENCE_NEVER_STARTED;

        internal.edit().putInt("status_" + "settings_tooltip", isFinished).apply(); //ProfileFragment
        internal.edit().putInt("status_" + "history_tooltip", isFinished).apply(); //HistoryFragment
        internal.edit().putInt("status_" + "start_tooltip", isFinished).apply(); // StartFragment
        internal.edit().putInt("status_" + "history_expanded_tooltip", isFinished).apply(); // HistoryExpandedFragment
        internal.edit().putInt("status_" + "history_expanded_detailed_tooltip", isFinished).apply(); // HistoryExpandedFragment
        internal.edit().putInt("status_" + "postrunning_tooltip", isFinished).apply(); // PostRunningFragment
    }

    /**
     * Waits one second until an atomic variable has a certain value or fails with the message "-failMessage- 1000 ms"  after a timeout.
     * <p>
     * Uses {@link #waitUntilAtomicVariableReachesValue(int, int, String, AtomicInteger, int)} internally with the first two arguments to 25 and 40.
     *
     * @param failMessage   Message to pass to junit.fail() upon timeout
     * @param atomic        Variable that must be waited upon
     * @param expectedValue Value above variable should be.
     */
    public static void waitOneSecUntilAtomicVariableReachesValue(String failMessage, AtomicInteger atomic, int expectedValue) {
        waitUntilAtomicVariableReachesValue(25, 40, failMessage, atomic, expectedValue);
    }

    /**
     * Waits until an atomic variable has a certain value or fails with the message "-failMessage- -timeout- ms"  after a timeout.
     *
     * @param waitTimePerIteration How long the thread should sleep each waiting iteration (in ms)
     * @param maxIterations        How many waiting iterations there should be
     * @param failMessage          Message to pass to junit.fail() upon timeout
     * @param atomic               Variable that must be waited upon
     * @param expectedValue        Value above variable should be.
     */
    public static void waitUntilAtomicVariableReachesValue(int waitTimePerIteration, int maxIterations, String failMessage, AtomicInteger atomic, int expectedValue) {
        int i = 0;
        while (true) {
            if (i > maxIterations) {
                fail(failMessage + waitTimePerIteration * maxIterations + " ms");
            }
            if (atomic.get() == expectedValue) {
                return;
            }
            try {
                Thread.sleep(waitTimePerIteration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            i++;
        }
    }
}
