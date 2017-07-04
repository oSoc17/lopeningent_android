package com.dp16.runamicghent;

import android.app.Activity;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import com.dp16.runamicghent.Activities.SplashScreen;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.dp16.runamicghent.testUtils.loginWithTestAccount;
import static com.dp16.runamicghent.testUtils.logoutUser;
import static com.dp16.runamicghent.testUtils.setAllToolTipsAsDone;
import static com.dp16.runamicghent.testUtils.waitFor;

/**
 * Created by Stiaan on 27/04/2017.
 */

public class ProfileUITest {

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

        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiObject signinpage = device.findObject(new UiSelector().text("SIGN IN"));
        if (signinpage.exists()) {
            loginWithTestAccount();
        }
        resetChangesAccount();
    }

    @Test
    public void checkAndChangeProfile() throws Exception{
        onView(withId(R.id.action_settings)).perform(click());

        String oldName = "test";
        String oldPass = "testtest";
        String oldMail = "test@test.test";

        String newName = "OtherName";
        String newMail = "othermail@mail.mail";
        String newPass = "newnewnew";

        onView(withId(R.id.name)).check(matches(withText(oldName)));
        onView(withId(R.id.emailProfile)).check(matches(withText(oldMail)));
        //change account details
        onView(withId(R.id.button_edit)).perform(click());
        onView(withId(R.id.editName)).perform(clearText());
        onView(withId(R.id.editName)).perform(typeText(newName));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.editEmail)).perform(clearText());
        onView(withId(R.id.editEmail)).perform(typeText(newMail));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.editOldPw)).perform((typeText(oldPass)));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.editPw1)).perform((typeText(newPass)));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.editPw2)).perform((typeText(newPass)));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.saveButton)).perform(click());
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(5)));
        //check that they changed
        onView(withId(R.id.name)).check(matches(withText(newName)));
        onView(withId(R.id.emailProfile)).check(matches(withText(newMail)));

        onView(withId(R.id.action_start)).perform(click());
        //logout
        logoutUser();

        //try to log in with old account details
        loginWithTestAccount();
        onView(withId(R.id.sinin)).check(matches(isDisplayed()));

        //log in with new account details
        onView(withId(R.id.email)).perform(clearText());
        onView(withId(R.id.email)).perform(typeText(newMail));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.pwd)).perform(clearText());
        onView(withId(R.id.pwd)).perform(typeText(newPass));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.sinin)).perform(click());

        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(1)));

        UiObject allowRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(allowRequest.exists()){
            allowRequest.click();
        }
        //check login screen passed
        onView(withId(R.id.generate_route)).check(matches(isDisplayed()));
        onView(withId(R.id.action_settings)).perform(click());

        //change back to old credentials
        onView(withId(R.id.name)).check(matches(withText(newName)));
        onView(withId(R.id.emailProfile)).check(matches(withText(newMail)));

        //change account details
        onView(withId(R.id.button_edit)).perform(click());
        onView(withId(R.id.editName)).perform(clearText());
        onView(withId(R.id.editName)).perform(typeText(oldName));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.editEmail)).perform(clearText());
        onView(withId(R.id.editEmail)).perform(typeText(oldMail));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.editOldPw)).perform((typeText(newPass)));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.editPw1)).perform((typeText(oldPass)));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.editPw2)).perform((typeText(oldPass)));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.saveButton)).perform(click());
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(5)));
        //check that they changed
        onView(withId(R.id.name)).check(matches(withText(oldName)));
        onView(withId(R.id.emailProfile)).check(matches(withText(oldMail)));

        //check that restoring login is successful, UI bug log out button appears here
        /*onView(withId(R.id.action_settings)).perform(click());

        UiScrollable listView = new UiScrollable(new UiSelector().className("android.widget.LinearLayout"));

        listView.scrollToEnd(10);

        onView(withId(R.id.logout)).perform(click());

        UiObject continueRequest = device.findObject(new UiSelector().clickable(true).checkable(false).index(1));
        if(continueRequest.exists()){
            //throw new AssertionError("View with text <" + allowRequest.getText() + "> found!");
            continueRequest.click();
        }
        loginWithTestAccount();
        onView(withId(R.id.generate_route)).check(matches(isDisplayed()));*/

    }

    @Test
    public void tryChangeWithWrongPassword() throws UiObjectNotFoundException {
        onView(withId(R.id.action_settings)).perform(click());

        String oldName = "test";
        String oldMail = "test@test.test";

        String newName = "OtherName";
        String newMail = "othermail@mail.mail";
        String newPass = "newnewnew";

        onView(withId(R.id.name)).check(matches(withText(oldName)));
        onView(withId(R.id.emailProfile)).check(matches(withText(oldMail)));
        //change account details
        onView(withId(R.id.button_edit)).perform(click());
        onView(withId(R.id.editName)).perform(clearText());
        onView(withId(R.id.editName)).perform(typeText(newName));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.editEmail)).perform(clearText());
        onView(withId(R.id.editEmail)).perform(typeText(newMail));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.editOldPw)).perform((typeText(newPass)));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.editPw1)).perform((typeText(newPass)));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.editPw2)).perform((typeText(newPass)));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.saveButton)).perform(click());
        onView(isRoot()).perform(waitFor( TimeUnit.SECONDS.toMillis(1)));

        UiObject failed = device.findObject(new UiSelector().text("OK"));
        if (failed.exists()) failed.click();

        onView(withId(R.id.saveButton)).check(matches(isDisplayed()));
    }

    public void resetChangesAccount(){
        onView(withId(R.id.action_settings)).perform(click());

        UiObject testUserName = device.findObject(new UiSelector().text("test"));
        if (!testUserName.exists()) {

            onView(withId(R.id.button_edit)).perform(click());
            onView(withId(R.id.editName)).perform(clearText());
            onView(withId(R.id.editName)).perform(typeText("test"));
            Espresso.closeSoftKeyboard();
            onView(withId(R.id.editEmail)).perform(clearText());
            onView(withId(R.id.editEmail)).perform(typeText("test@test.test"));
            Espresso.closeSoftKeyboard();
            onView(withId(R.id.editOldPw)).perform((typeText("newnewnew")));
            Espresso.closeSoftKeyboard();
            onView(withId(R.id.editPw1)).perform((typeText("testtest")));
            Espresso.closeSoftKeyboard();
            onView(withId(R.id.editPw2)).perform((typeText("testtest")));
            Espresso.closeSoftKeyboard();

            onView(withId(R.id.saveButton)).perform(click());
            onView(isRoot()).perform(waitFor(TimeUnit.SECONDS.toMillis(5)));
        }

        onView(withId(R.id.action_start)).perform(click());
    }


}
