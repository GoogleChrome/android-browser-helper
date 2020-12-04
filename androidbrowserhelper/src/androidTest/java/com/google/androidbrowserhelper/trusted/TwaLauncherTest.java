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

package com.google.androidbrowserhelper.trusted;

import static com.google.androidbrowserhelper.trusted.testutils.TestUtil.getBrowserActivityWhenLaunched;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import static androidx.browser.customtabs.TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.androidbrowserhelper.trusted.splashscreens.SplashScreenStrategy;
import com.google.androidbrowserhelper.trusted.testutils.EnableComponentsTestRule;
import com.google.androidbrowserhelper.trusted.testcomponents.TestActivity;
import com.google.androidbrowserhelper.trusted.testcomponents.TestBrowser;
import com.google.androidbrowserhelper.trusted.testcomponents.TestCustomTabsService;
import com.google.androidbrowserhelper.trusted.testcomponents.TestCustomTabsServiceSupportsTwas;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsSessionToken;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Instrumentation tests for {@link TwaLauncher}
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class TwaLauncherTest {
    private static final Uri URL = Uri.parse("https://www.test.com/default_url/");

    private Context mContext = InstrumentationRegistry.getContext();
    private static final CustomTabsCallback mCustomTabsCallback = new QualityEnforcer();

    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            TestActivity.class,
            TestBrowser.class,
            TestCustomTabsServiceSupportsTwas.class,
            TestCustomTabsService.class
    );
    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class, false, true);

    private TestActivity mActivity;

    private TwaLauncher mTwaLauncher;

    @Before
    public void setUp() {
        TwaProviderPicker.restrictToPackageForTesting(mContext.getPackageName());

        // TODO(peconn): Maybe make this a test rule?
        TestCustomTabsService.setCanCreateSessions(true);
        mActivity = mActivityTestRule.getActivity();
        mTwaLauncher = new TwaLauncher(mActivity);
    }

    @After
    public void tearDown() {
        TwaProviderPicker.restrictToPackageForTesting(null);
        mTwaLauncher.destroy();
    }

    @Test
    public void launchesTwaWithJustUrl() {
        Runnable launchRunnable = () -> mTwaLauncher.launch(URL);
        TestBrowser browser = getBrowserActivityWhenLaunched(launchRunnable);
        assertTrue(browser.getIntent().getBooleanExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY,
                false));
        assertEquals(URL, browser.getIntent().getData());
    }

    @Test
    public void transfersTwaBuilderParams() {
        // Checking just one parameters. TrustedWebActivityBuilderTest tests the rest. Here we just
        // check that TwaLauncher doesn't ignore the passed builder.
        int color = 0x0000ff;
        int expected = color | 0xff000000;

        TrustedWebActivityIntentBuilder builder = makeBuilder().setToolbarColor(color);
        Runnable launchRunnable = () -> mTwaLauncher.launch(builder, mCustomTabsCallback,
                null, null);
        Intent intent = getBrowserActivityWhenLaunched(launchRunnable).getIntent();

        assertEquals(expected, intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
    }

    @Test
    public void fallsBackToCustomTab() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        TwaLauncher launcher = new TwaLauncher(mActivity);

        Runnable launchRunnable = () -> launcher.launch(URL);
        Intent intent = getBrowserActivityWhenLaunched(launchRunnable).getIntent();

        launcher.destroy();
        assertFalse(intent.hasExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY));
    }

    @Test
    public void fallsBackToCustomTab_whenSessionCreationFails() {
        TestCustomTabsService.setCanCreateSessions(false);

        Runnable launchRunnable = () -> mTwaLauncher.launch(URL);
        TestBrowser browser = getBrowserActivityWhenLaunched(launchRunnable);
        assertFalse(browser.getIntent().getBooleanExtra(EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY,
                false));
    }

    @Test
    public void customTabFallbackUsesStatusBarColor() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        TwaLauncher launcher = new TwaLauncher(mActivity);

        int color = 0x0000ff;
        int expected = color | 0xff000000;

        TrustedWebActivityIntentBuilder builder = makeBuilder().setToolbarColor(color);
        Runnable launchRunnable = () -> launcher.launch(builder, mCustomTabsCallback,
                null, null);
        Intent intent = getBrowserActivityWhenLaunched(launchRunnable).getIntent();

        launcher.destroy();
        assertEquals(expected, intent.getIntExtra(CustomTabsIntent.EXTRA_TOOLBAR_COLOR, 0));
    }

    @Test
    public void reusesSessionForSubsequentLaunches() {
        TwaLauncher launcher1 = new TwaLauncher(mActivity);
        CustomTabsSessionToken token1 =
                getSessionTokenFromLaunchedBrowser(() -> launcher1.launch(URL));
        launcher1.destroy();

        // New activity is created (e.g. by an external VIEW intent).
        TwaLauncher launcher2 = new TwaLauncher(mActivity);
        CustomTabsSessionToken token2 =
                getSessionTokenFromLaunchedBrowser(() -> launcher2.launch(URL));
        launcher2.destroy();

        assertEquals(token1, token2);
    }

    @Test
    public void createsDifferentSessions_IfDifferentIdsSpecified() {
        int sessionId1 = 1;
        int sessionId2 = 2;

        TwaLauncher launcher1 = new TwaLauncher(mActivity, null, sessionId1,
                new SharedPreferencesTokenStore(mActivity));
        CustomTabsSessionToken token1 =
                getSessionTokenFromLaunchedBrowser(() -> launcher1.launch(URL));
        launcher1.destroy();

        // New activity is created (e.g. by an external VIEW intent).
        TwaLauncher launcher2 = new TwaLauncher(mActivity, null, sessionId2,
                new SharedPreferencesTokenStore(mActivity));
        CustomTabsSessionToken token2 =
                getSessionTokenFromLaunchedBrowser(() -> launcher2.launch(URL));
        launcher2.destroy();

        assertNotEquals(token1, token2);
    }

    @Test
    public void completionCallbackCalled() {
        Runnable callback = mock(Runnable.class);
        Runnable launchRunnable = () -> mTwaLauncher.launch(makeBuilder(), mCustomTabsCallback,
                null, callback);
        getBrowserActivityWhenLaunched(launchRunnable);
        verify(callback).run();
    }

    @Test
    public void completionCallbackCalled_WhenFallingBackToCct() {
        mEnableComponents.manuallyDisable(TestCustomTabsServiceSupportsTwas.class);
        TwaLauncher twaLauncher = new TwaLauncher(mActivity);

        Runnable callback = mock(Runnable.class);
        Runnable launchRunnable = () -> twaLauncher.launch(makeBuilder(), mCustomTabsCallback,
                null, callback);
        getBrowserActivityWhenLaunched(launchRunnable);
        verify(callback).run();
        twaLauncher.destroy();
    }

    @Test
    public void notifiesSplashScreenStrategyOfLaunchInitiation() {
        SplashScreenStrategy strategy = mock(SplashScreenStrategy.class);
        TrustedWebActivityIntentBuilder builder = makeBuilder();
        mTwaLauncher.launch(builder, mCustomTabsCallback, strategy, null);
        verify(strategy).onTwaLaunchInitiated(
                eq(InstrumentationRegistry.getContext().getPackageName()),
                eq(builder));
    }

    @Test
    public void doesntLaunch_UntilSplashScreenStrategyFinishesConfiguring() {
        SplashScreenStrategy strategy = mock(SplashScreenStrategy.class);

        // Using spy to verify intent is never built to avoid testing directly that activity is
        // not launched.
        TrustedWebActivityIntentBuilder builder = spy(makeBuilder());
        mTwaLauncher.launch(builder, mCustomTabsCallback, strategy, null);
        verify(builder, never()).build(any());
    }

    @Test
    public void launches_WhenSplashScreenStrategyFinishesConfiguring() {
        SplashScreenStrategy strategy = mock(SplashScreenStrategy.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(strategy).configureTwaBuilder(any(), any(), any());

        Runnable launchRunnable = () -> mTwaLauncher.launch(makeBuilder(), mCustomTabsCallback,
                strategy, null);
        assertNotNull(getBrowserActivityWhenLaunched(launchRunnable));
    }

    @Test
    public void cancelsLaunch_IfSplashScreenStrategyFinishes_AfterDestroy() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        SplashScreenStrategy strategy = mock(SplashScreenStrategy.class);
        doAnswer(invocation -> {
            mTwaLauncher.destroy();
            ((Runnable) invocation.getArgument(2)).run();
            latch.countDown();
            return null;
        }).when(strategy).configureTwaBuilder(any(), any(), any());

        TrustedWebActivityIntentBuilder builder = spy(makeBuilder());
        mTwaLauncher.launch(builder, mCustomTabsCallback, strategy, null);
        assertTrue(latch.await(3, TimeUnit.SECONDS));
        verify(builder, never()).build(any());
    }

    private TrustedWebActivityIntentBuilder makeBuilder() {
        return new TrustedWebActivityIntentBuilder(URL);
    }


    private CustomTabsSessionToken getSessionTokenFromLaunchedBrowser(Runnable launchRunnable) {
        return CustomTabsSessionToken.getSessionTokenFromIntent(
                getBrowserActivityWhenLaunched(launchRunnable).getIntent());
    }
}
