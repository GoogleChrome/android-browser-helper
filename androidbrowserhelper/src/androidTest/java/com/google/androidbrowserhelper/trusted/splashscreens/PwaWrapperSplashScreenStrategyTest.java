// Copyright 2019 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.androidbrowserhelper.trusted.splashscreens;

import static com.google.androidbrowserhelper.trusted.testutils.TestUtil.runOnUiThreadBlocking;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import static androidx.browser.trusted.TrustedWebActivityIntentBuilder.EXTRA_SPLASH_SCREEN_PARAMS;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.ImageView;

import com.google.androidbrowserhelper.test.R;
import com.google.androidbrowserhelper.trusted.testcomponents.TestActivity;
import com.google.androidbrowserhelper.trusted.testcomponents.TestBrowser;
import com.google.androidbrowserhelper.trusted.testcomponents.TestCustomTabsService;
import com.google.androidbrowserhelper.trusted.testcomponents.TestCustomTabsServiceNoSplashScreens;
import com.google.androidbrowserhelper.trusted.testcomponents.TestCustomTabsServiceSupportsTwas;
import com.google.androidbrowserhelper.trusted.testutils.CustomTabConnectionRule;
import com.google.androidbrowserhelper.trusted.testutils.EnableComponentsTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.TrustedWebActivityIntent;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;
import androidx.browser.trusted.splashscreens.SplashScreenParamKey;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

/**
 * Tests for {@link PwaWrapperSplashScreenStrategy}.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class PwaWrapperSplashScreenStrategyTest {

    // For clean-up. See SplashImageTransferTask.java.
    private static final String FOLDER_NAME = "twa_splash";
    private static final String FILE_PROVIDER_AUTHORITY =
            "com.google.androidbrowserhelper.test_fileprovider";

    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            TestActivity.class,
            TestBrowser.class,
            TestCustomTabsServiceSupportsTwas.class
    );

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class, false, true);

    @Rule
    public final CustomTabConnectionRule mConnectionRule = new CustomTabConnectionRule();

    private TestActivity mActivity;
    private CustomTabsSession mSession;
    private PwaWrapperSplashScreenStrategy mStrategy;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mSession = mConnectionRule.establishSessionBlocking(mActivity);
        mStrategy = new PwaWrapperSplashScreenStrategy(mActivity, R.drawable.splash, 0,
                ImageView.ScaleType.FIT_XY, null, 0, FILE_PROVIDER_AUTHORITY, true);
    }

    @After
    public void tearDown() {
        new File(mActivity.getFilesDir(), FOLDER_NAME).delete(); // Clean up save splash image file.
    }

    @Test
    public void showsSplashScreenInClient_WhenTwaLaunchInitiated() {
        initiateLaunch(mStrategy);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // The splash screen should be full-screen blue. Check just one pixel.
        assertEquals(mActivity.getColor(R.color.splash_screen_color), getColorOfAPixel());
    }

    @Test
    public void doesntShowsSplashScreenInClient_IfSplashScreensNotSupported() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        mEnableComponents.manuallyEnable(TestCustomTabsServiceNoSplashScreens.class);

        initiateLaunch(mStrategy);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertNotEquals(mActivity.getColor(R.color.splash_screen_color), getColorOfAPixel());
    }

    @Test
    public void imageFileIsSuccessfullyTransferredToService() {
        initiateLaunch(mStrategy);
        mStrategy.configureTwaBuilder(new TrustedWebActivityIntentBuilder(Uri.EMPTY),
                mSession, null);
        assertTrue(TestCustomTabsService.getInstance().waitForSplashImageFile(3000));
    }

    @Test
    public void setsParametersOnTwaBuilder() throws InterruptedException {
        int bgColor = 0x115599;
        ImageView.ScaleType scaleType = ImageView.ScaleType.MATRIX;
        Matrix matrix = new Matrix();
        matrix.setSkew(1, 2, 3, 4);
        int fadeOutDuration = 300;

        PwaWrapperSplashScreenStrategy strategy = new PwaWrapperSplashScreenStrategy(mActivity,
                R.drawable.splash, bgColor, scaleType, matrix, fadeOutDuration,
                FILE_PROVIDER_AUTHORITY, true);
        strategy.onActivityEnterAnimationComplete();
        initiateLaunch(strategy);

        TrustedWebActivityIntentBuilder builder = new TrustedWebActivityIntentBuilder(
                Uri.parse("https://test.com"));

        CountDownLatch latch = new CountDownLatch(1);
        strategy.configureTwaBuilder(builder, mSession, latch::countDown);
        assertTrue(latch.await(3, TimeUnit.SECONDS));

        TrustedWebActivityIntent intent = builder.build(mSession);
        Bundle bundle = intent.getIntent().getBundleExtra(EXTRA_SPLASH_SCREEN_PARAMS);

        assertEquals(bgColor, bundle.getInt(SplashScreenParamKey.KEY_BACKGROUND_COLOR));
        assertEquals(scaleType.ordinal(), bundle.getInt(SplashScreenParamKey.KEY_SCALE_TYPE));
        assertEquals(fadeOutDuration, bundle.getInt(SplashScreenParamKey.KEY_FADE_OUT_DURATION_MS));

        float[] matrixValues = new float[9];
        matrix.getValues(matrixValues);
        assertArrayEquals(matrixValues, bundle.getFloatArray(
                SplashScreenParamKey.KEY_IMAGE_TRANSFORMATION_MATRIX), 1e-3f);
    }

    private void initiateLaunch(PwaWrapperSplashScreenStrategy strategy) {
        runOnUiThreadBlocking(() ->  strategy.onTwaLaunchInitiated(mActivity.getPackageName(),
                new TrustedWebActivityIntentBuilder(Uri.EMPTY)));
    }

    private int getColorOfAPixel() {
        PowerManager powerManager =
                (PowerManager) mActivity.getSystemService(Context.POWER_SERVICE);
        assertTrue("This test doesn't work with screen turned off", powerManager.isInteractive());

        View view = runOnUiThreadBlocking(() -> mActivity.findViewById(android.R.id.content));
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap.getPixel(0, 0);
    }

}
