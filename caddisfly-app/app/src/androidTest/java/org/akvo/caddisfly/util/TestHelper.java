/*
 * Copyright (C) Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Caddisfly.
 *
 * Akvo Caddisfly is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Caddisfly is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Caddisfly. If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.caddisfly.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.StringRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.util.DisplayMetrics;

import org.akvo.caddisfly.BuildConfig;
import org.akvo.caddisfly.R;
import org.akvo.caddisfly.app.CaddisflyApp;
import org.akvo.caddisfly.helper.FileHelper;
import org.hamcrest.Matchers;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.akvo.caddisfly.util.TestUtil.clickListViewItem;
import static org.akvo.caddisfly.util.TestUtil.findButtonInScrollable;
import static org.akvo.caddisfly.util.TestUtil.nextSurveyPage;
import static org.akvo.caddisfly.util.TestUtil.sleep;

public final class TestHelper {

    private static final Map<String, String> STRING_HASH_MAP_EN = new HashMap<>();
    private static final Map<String, String> STRING_HASH_MAP_FR = new HashMap<>();
    private static final Map<String, String> STRING_HASH_MAP_IN = new HashMap<>();
    private static final Map<String, String> CALIBRATION_HASH_MAP = new HashMap<>();
    private static final boolean TAKE_SCREENSHOTS = false;
    public static Map<String, String> currentHashMap;
    public static UiDevice mDevice;
    public static String mCurrentLanguage = "en";
    @SuppressWarnings("FieldCanBeLocal")
    private static int mCounter;

    private TestHelper() {
    }

    private static void addString(String key, String... values) {
        STRING_HASH_MAP_EN.put(key, values[0]);
        STRING_HASH_MAP_FR.put(key, values[1]);
        if (values.length > 2) {
            STRING_HASH_MAP_IN.put(key, values[2]);
        } else {
            STRING_HASH_MAP_IN.put(key, values[0]);
        }
    }

    private static void addCalibration(String key, String colors) {
        CALIBRATION_HASH_MAP.put(key, colors);
    }

    public static String getString(Activity activity, @StringRes int resourceId) {
        Resources currentResources = activity.getResources();
        AssetManager assets = currentResources.getAssets();
        DisplayMetrics metrics = currentResources.getDisplayMetrics();
        Configuration config = new Configuration(currentResources.getConfiguration());
        config.locale = new Locale(mCurrentLanguage);
        Resources res = new Resources(assets, metrics, config);

        return res.getString(resourceId);

    }

    @SuppressWarnings("deprecation")
    public static void loadData(Activity activity, String languageCode) {
        mCurrentLanguage = languageCode;

        STRING_HASH_MAP_EN.clear();
        STRING_HASH_MAP_FR.clear();
        STRING_HASH_MAP_IN.clear();
        CALIBRATION_HASH_MAP.clear();

        Resources currentResources = activity.getResources();
        AssetManager assets = currentResources.getAssets();
        DisplayMetrics metrics = currentResources.getDisplayMetrics();
        Configuration config = new Configuration(currentResources.getConfiguration());
        config.locale = new Locale(languageCode);
        Resources res = new Resources(assets, metrics, config);

        addString(TestConstant.LANGUAGE, "English", "Français", "Bahasa Indonesia");
        addString("otherLanguage", "Français", "English");
        addString(TestConstant.FLUORIDE, "Water - Fluoride", res.getString(R.string.testName));
        addString("chlorine", "Water - Free Chlorine", res.getString(R.string.freeChlorine));
        addString("survey", "Survey", res.getString(R.string.survey));
        addString("sensors", "Sensors", res.getString(R.string.sensors));
        addString("electricalConductivity", "Water - Electrical Conductivity", res.getString(R.string.electricalConductivity));
        addString("next", "Next", res.getString(R.string.next));
        addString(TestConstant.GO_TO_TEST, "Launch", res.getString(R.string.launch));
        // Restore device-specific locale
        new Resources(assets, metrics, currentResources.getConfiguration());

        addCalibration("TestValid", "0.0=255  38  186\n"
                + "0.5=255  51  129\n"
                + "1.0=255  59  89\n"
                + "1.5=255  62  55\n"
                + "2.0=255  81  34\n");

        addCalibration("TestInvalid", "0.0=255  88  177\n"
                + "0.5=255  110  15\n"
                + "1.0=255  138  137\n"
                + "1.5=253  174  74\n"
                + "2.0=253  174  76\n"
                + "2.5=236  172  81\n"
                + "3.0=254  169  61\n");

        addCalibration("OutOfSequence", "0.0=255  38  186\n"
                + "0.5=255  51  129\n"
                + "1.0=255  62  55\n"
                + "1.5=255  59  89\n"
                + "2.0=255  81  34\n");

        addCalibration("HighLevelTest", "0.0=255  38  180\n"
                + "0.5=255  51  129\n"
                + "1.0=255  53  110\n"
                + "1.5=255  55  100\n"
                + "2.0=255  59  89\n");

        addCalibration("TestInvalid2", "0.0=255  88  47\n"
                + "0.5=255  60  37\n"
                + "1.0=255  35  27\n"
                + "1.5=253  17  17\n"
                + "2.0=254  0  0\n");

        addCalibration("LowLevelTest", "0.0=255  60  37\n"
                + "0.5=255  35  27\n"
                + "1.0=253  17  17\n"
                + "1.5=254  0  0\n"
                + "2.0=224  0  0\n");

        addCalibration("TestValidChlorine", "0.0=255  38  186\n"
                + "0.5=255  51  129\n"
                + "1.0=255  59  89\n"
                + "1.5=255  62  55\n"
                + "2.0=255  81  34\n"
                + "2.5=255  101  24\n"
                + "3.0=255  121  14\n");

        if ("en".equals(languageCode)) {
            currentHashMap = STRING_HASH_MAP_EN;
        } else if ("in".equals(languageCode)) {
            currentHashMap = STRING_HASH_MAP_IN;
        } else {
            currentHashMap = STRING_HASH_MAP_FR;
        }
    }

    public static void takeScreenshot() {
        if (TAKE_SCREENSHOTS) {
            File path = new File(Environment.getExternalStorageDirectory().getPath()
                    + "/" + BuildConfig.APPLICATION_ID + "/screenshots/"
                    + "screen-" + mCounter++ + "-" + mCurrentLanguage + ".png");
            mDevice.takeScreenshot(path, 0.5f, 60);
        }
    }

    public static void takeScreenshot(String name, int page) {
        if (TAKE_SCREENSHOTS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            File path;
            if (page < 0) {
                path = new File(Environment.getExternalStorageDirectory().getPath()
                        + "/" + BuildConfig.APPLICATION_ID + "/screenshots/"
                        + name + "-" + mCurrentLanguage + ".png");
            } else {
                path = new File(Environment.getExternalStorageDirectory().getPath()
                        + "/" + BuildConfig.APPLICATION_ID + "/screenshots/"
                        + name + "-" + page + "-" + mCurrentLanguage + ".png");
            }
            mDevice.takeScreenshot(path, 0.2f, 40);
        }
    }


    public static void goToMainScreen() {

        boolean found = false;
        while (!found) {
            try {
                onView(withId(R.id.actionSettings)).check(matches(isDisplayed()));
                found = true;
            } catch (NoMatchingViewException e) {
                Espresso.pressBack();
            }
        }
    }

    public static void activateTestMode(Activity activity) {
        onView(withId(R.id.actionSettings)).perform(click());

        onView(withText(R.string.about)).check(matches(isDisplayed())).perform(click());

        String version = CaddisflyApp.getAppVersion(false);

        onView(withText(version)).check(matches(isDisplayed()));

        enterDiagnosticMode();

        goToMainScreen();

        onView(withId(R.id.actionSettings)).perform(click());

        clickListViewItem(getString(activity, R.string.testModeOn));
    }

    public static void clickExternalSourceButton(String id) {
        switch (id) {
            case TestConstant.WATER_FLUORIDE_ID:
                nextSurveyPage(3, "Water Tests 1");
                clickExternalSourceButton(2);
                break;
            case TestConstant.SOIL_IRON_ID:
                nextSurveyPage(3, "Soil Tests 2");
                clickExternalSourceButton(2);
                break;
        }
    }

    public static void clickExternalSourceButton(int index) {
        clickExternalSourceButton(index, TestConstant.GO_TO_TEST);
    }

    public static void clickExternalSourceButton(int index, String text) {

        String buttonText = currentHashMap.get(text);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            buttonText = buttonText.toUpperCase();
        }

        findButtonInScrollable(buttonText);

        List<UiObject2> buttons = mDevice.findObjects(By.text(buttonText));
        buttons.get(index).click();

        mDevice.waitForWindowUpdate("", 2000);

        sleep(4000);
    }

    public static void clickExternalSourceButton(Context context, String text) {
        try {

            String buttonText = currentHashMap.get(text);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                buttonText = buttonText.toUpperCase();
            }

            findButtonInScrollable(buttonText);

            mDevice.findObject(new UiSelector().text(buttonText)).click();

            // New Android OS seems to popup a button for external app
            if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.M
                    && (text.equals(TestConstant.USE_EXTERNAL_SOURCE)
                    || text.equals(TestConstant.GO_TO_TEST))) {
                sleep(1000);
                mDevice.findObject(By.text(context.getString(R.string.appName))).click();
                sleep(1000);
            }

            mDevice.waitForWindowUpdate("", 2000);

        } catch (UiObjectNotFoundException e) {
            Timber.e(e);
        }
    }

    public static void saveCalibration(String name, String id) {
        File path = FileHelper.getFilesDir(FileHelper.FileType.CALIBRATION, id);

        FileUtil.saveToFile(path, name, CALIBRATION_HASH_MAP.get(name));
    }

    public static void gotoSurveyForm() {

        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(TestConstant.EXTERNAL_SURVEY_PACKAGE_NAME);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        context.startActivity(intent);

        mDevice.waitForIdle();

        sleep(1000);

        UiObject addButton = mDevice.findObject(new UiSelector()
                .resourceId(TestConstant.EXTERNAL_SURVEY_PACKAGE_NAME + ":id/enter_data"));

        try {
            if (addButton.exists() && addButton.isEnabled()) {
                addButton.click();
            }
        } catch (UiObjectNotFoundException e) {
            Timber.e(e);
        }

        mDevice.waitForIdle();

        clickListViewItem("Automated Testing");

        mDevice.waitForIdle();

        UiObject goToStartButton = mDevice.findObject(new UiSelector()
                .resourceId(TestConstant.EXTERNAL_SURVEY_PACKAGE_NAME + ":id/jumpBeginningButton"));

        try {
            if (goToStartButton.exists() && goToStartButton.isEnabled()) {
                goToStartButton.click();
            }
        } catch (UiObjectNotFoundException e) {
            Timber.e(e);
        }

        mDevice.waitForIdle();

    }

    public static void enterDiagnosticMode() {
        for (int i = 0; i < 10; i++) {
            onView(withId(R.id.textVersion)).perform(click());
        }
    }

    public static void leaveDiagnosticMode() {
        goToMainScreen();
        onView(withId(R.id.fabDisableDiagnostics)).perform(click());
    }

    public static void resetLanguage() {

        goToMainScreen();

        onView(withId(R.id.actionSettings)).perform(click());

        onView(withText(R.string.language)).perform(click());

        onData(Matchers.hasToString(Matchers.startsWith(currentHashMap.get("language")))).perform(click());

        mDevice.waitForIdle();

        goToMainScreen();

        onView(withId(R.id.actionSettings)).perform(click());

        onView(withText(R.string.language)).perform(click());

        onData(Matchers.hasToString(Matchers.startsWith(currentHashMap.get("language")))).perform(click());

        mDevice.waitForIdle();

        goToMainScreen();

    }

    public static void clearPreferences(ActivityTestRule activityTestRule) {
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(activityTestRule.getActivity());
        prefs.edit().clear().apply();
    }
}
