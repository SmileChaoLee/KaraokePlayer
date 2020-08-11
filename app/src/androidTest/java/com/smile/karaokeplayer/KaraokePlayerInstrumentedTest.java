package com.smile.karaokeplayer;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;
import android.view.View;

import androidx.appcompat.widget.MenuPopupWindow;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class KaraokePlayerInstrumentedTest {

    private static final String TAG ="KaraokePlayerInstrumentedTest";
    private static Context appContext;
    private static Resources appResources;
    // private MainActivity mainActivity;
    private String appPackageName = "com.smile.karaokeplayer";

    @BeforeClass
    public static void test_Setup() {
        appContext = ApplicationProvider.getApplicationContext();
        appResources = appContext.getResources();
        System.out.println("Initializing before all test cases. One time running.");
    }

    @Before
    public void test_PreRun() {
        // appContext = myActivityTestRule.getActivity();
        // appResources = appContext.getResources();
        // mainActivity = mainActivityTestRule.getActivity();
        Espresso.closeSoftKeyboard();
        System.out.println("Setting up before each test case.");
    }

    @Test
    public void test_PackageName() {
        assertEquals(appPackageName, appContext.getPackageName());
    }

    @Test
    public void test_FileSubmenu() {
        // onView(withId(R.id.file)).perform(click());
        onView(withText(R.string.autoPlayString)).check(matches(isDisplayed()));
        onView(withText(R.string.openString)).check(matches(isDisplayed()));
        onView(withText(R.string.closeString)).check(matches(isDisplayed()));
        onView(withText(R.string.privacyPolicyString)).check(matches(isDisplayed()));
        onView(withText(R.string.exitString)).check(matches(isDisplayed()));
        onView(withText(R.string.privacyPolicyString)).check(matches(isDisplayed()));
        Espresso.pressBack();
    }

    @Test
    public void test_PrivacyPolicy() {
        // onView(withId(R.id.file)).perform(click());
        // test PrivacyPolicy MenuItem of File menu
        onData(CoreMatchers.anything())
                .inRoot(RootMatchers.isPlatformPopup()) // isPlatformPopup() == is in PopupWindow
                .inAdapterView(CoreMatchers.<View>instanceOf(MenuPopupWindow.MenuDropDownListView.class))
                .atPosition(3) // for the fourth submenu item, here: R.id.privacyPolicy
                .perform(click());

        SystemClock.sleep(5000);

        // R.id.privacyPolicy
        // onView(withText("Privacy Policy")).check(matches(isDisplayed()));  // succeeded
    }

    @Test
    public void test_AudioSubmenu() {
        onView(withId(R.id.audioTrack)).perform(click());
    }

    @Test
    public void test_ChannelSubmenu() {
        onView(withId(R.id.channel)).perform(click());
        onView(withText(R.string.leftChannelString)).check(matches(isDisplayed()));
        onView(withText(R.string.rightChannelString)).check(matches(isDisplayed()));
        onView(withText(R.string.stereoChannelString)).check(matches(isDisplayed()));
        Espresso.pressBack();
    }
}
