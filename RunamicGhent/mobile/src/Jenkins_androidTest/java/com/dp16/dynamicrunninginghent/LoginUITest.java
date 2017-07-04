package com.dp16.runamicghent;

import android.app.Activity;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;

import com.dp16.runamicghent.Activities.SplashScreen;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.dp16.runamicghent.testUtils.loginWithTestAccount;
import static com.dp16.runamicghent.testUtils.logoutUser;
import static com.dp16.runamicghent.testUtils.setAllToolTipsAsDone;
import static com.dp16.runamicghent.testUtils.waitFor;

/**
 * Created by Stiaan on 11/04/2017.
 */
public class LoginUITest {

    UiDevice device;

    @Rule
    public ActivityTestRule<SplashScreen> mActivityRule = new ActivityTestRule<>(
            SplashScreen.class);

    @Before
    public void initialize() throws Exception{

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        Activity activity = mActivityRule.getActivity();
        Context context = activity.getApplicationContext();

        setAllToolTipsAsDone(context);

    }

    @Test
    public void loginActivity_LoginWithTestAccount()throws Exception{

        //logout if already logged in
        UiObject signinpage = device.findObject(new UiSelector().text("SIGN IN"));
        if (!signinpage.exists()) {
            logoutUser();
        }

        onView(withId(R.id.sinin)).check(matches(isDisplayed()));

        onView(withId(R.id.email)).perform(typeText("test@test.test"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.pwd)).perform(typeText("testtest"));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.sinin)).perform(click());

        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(1)));

        UiObject allowRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(allowRequest.exists()){
            allowRequest.click();
        }

        onView(withId(R.id.generate_route)).check(matches(isDisplayed()));
    }

    @Test
    public void loginActivity_Logout() throws Exception{
        UiObject signinpage = device.findObject(new UiSelector().text("SIGN IN"));
        if (signinpage.exists()) {
            loginWithTestAccount();
        }

        logoutUser();

        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(1)));
        onView(withId(R.id.sinin)).check(matches(isDisplayed()));

    }

    @Test
    public void loginActivity_LoginWithNonExistentAccount()throws Exception{
        //logout if already logged in
        UiObject signinpage = device.findObject(new UiSelector().text("SIGN IN"));
        if (!signinpage.exists()) {
            logoutUser();
        }

        onView(withId(R.id.email)).perform(typeText("te@test.test"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.pwd)).perform(typeText("testtest"));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.sinin)).perform(click());

        onView(withId(R.id.sinin)).check(matches(isDisplayed()));
    }

    @Test
    public void loginActivity_CreateAccount()throws Exception{
        //logout if already logged in
        UiObject signinpage = device.findObject(new UiSelector().text("SIGN IN"));
        if (!signinpage.exists()) {
            logoutUser();
        }

        Random rand = new Random();
        int randomID = rand.nextInt();

        String randomUser = "test" + randomID + "@test.test";

        Espresso.closeSoftKeyboard();

        onView(withId(R.id.signUp)).perform(click());

        onView(withId(R.id.email)).perform(typeText(randomUser));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.pwd1)).perform(typeText("testtest"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.pwd2)).perform(typeText("testtest"));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.sinup)).perform(click());
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(1)));

        onView(withId(R.id.generate_route)).check(matches(isDisplayed()));

        logoutUser();

        //login with newly created account
        onView(withId(R.id.sinin)).check(matches(isDisplayed()));

        onView(withId(R.id.email)).perform(typeText(randomUser));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.pwd)).perform(typeText("testtest"));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.sinin)).perform(click());

        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(1)));

        onView(withId(R.id.generate_route)).check(matches(isDisplayed()));
        logoutUser();


    }
}
