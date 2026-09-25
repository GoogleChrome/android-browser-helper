// Copyright 2026 Google Inc. All Rights Reserved.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.TrustedWebActivityIntent;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

import com.google.androidbrowserhelper.trusted.splashscreens.SplashScreenStrategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Collections;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class LauncherActivityOriginValidationTest {
    private static final String DEFAULT_URL = "https://www.example.com/twa/home";
    private static final String EXTRA_ORIGIN = "https://sub.example.com";
    private static final int EXTRA_ORIGINS_RES_ID = 0x7f030001;

    public static class TestLauncherActivity extends LauncherActivity {
        public TrustedWebActivityIntent mCapturedTwaIntent;

        @Override
        public Resources getResources() {
            Resources spied = spy(super.getResources());
            doReturn(new String[]{EXTRA_ORIGIN}).when(spied).getStringArray(EXTRA_ORIGINS_RES_ID);
            return spied;
        }

        @Override
        protected Map<String, Uri> getProtocolHandlers() {
            return Collections.singletonMap(
                    "web+coffee", Uri.parse("https://www.example.com/coffee?type=%s"));
        }

        @Override
        protected TwaLauncher createTwaLauncher() {
            return new TwaLauncher(this) {
                @Override
                public void launch(
                        TrustedWebActivityIntentBuilder twaBuilder,
                        CustomTabsCallback customTabsCallback,
                        @Nullable SplashScreenStrategy splashScreenStrategy,
                        @Nullable Runnable completionCallback,
                        FallbackStrategy fallbackStrategy) {
                    CustomTabsSession session = CustomTabsSession.createMockSessionForTesting(
                            new ComponentName("com.android.chrome", "CustomTabsService"));
                    mCapturedTwaIntent = twaBuilder.build(session);
                    mCapturedTwaIntent.launchTrustedWebActivity(TestLauncherActivity.this);
                }
            };
        }
    }

    public static class RewritingLauncherActivity extends TestLauncherActivity {
        @Override
        protected Uri getLaunchingUrl() {
            return super.getLaunchingUrl()
                    .buildUpon()
                    .appendQueryParameter("appInstanceId", "123")
                    .build();
        }
    }

    private Context mContext;
    private ShadowPackageManager mShadowPackageManager;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mContext.getApplicationInfo().flags &= ~android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;
        mShadowPackageManager = shadowOf(mContext.getPackageManager());

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = mContext.getPackageName();

        ActivityInfo launcherActivityInfo = createActivityInfo(TestLauncherActivity.class);
        ActivityInfo rewritingActivityInfo = createActivityInfo(RewritingLauncherActivity.class);

        packageInfo.activities = new ActivityInfo[]{launcherActivityInfo, rewritingActivityInfo};
        mShadowPackageManager.addPackage(packageInfo);

        Intent browserIntent = new Intent()
                .setData(Uri.fromParts("http", "", null))
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE);
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = "com.android.chrome";
        resolveInfo.activityInfo.name = "com.android.chrome.ChromeTabbedActivity";
        mShadowPackageManager.addResolveInfoForIntent(browserIntent, resolveInfo);
    }

    private ActivityInfo createActivityInfo(Class<?> activityClass) {
        ActivityInfo info = new ActivityInfo();
        info.packageName = mContext.getPackageName();
        info.name = activityClass.getName();
        info.metaData = new Bundle();
        info.metaData.putString(
                "android.support.customtabs.trusted.DEFAULT_URL", DEFAULT_URL);
        info.metaData.putInt(
                "android.support.customtabs.trusted.ADDITIONAL_TRUSTED_ORIGINS",
                EXTRA_ORIGINS_RES_ID);
        return info;
    }

    private void registerOwnBrowsableIntentFilter(Class<?> activityClass, Uri uri) {
        Intent probe = new Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setPackage(mContext.getPackageName());
        IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addCategory(Intent.CATEGORY_BROWSABLE);
        if (uri.getScheme() != null) {
            filter.addDataScheme(uri.getScheme());
        }
        if (uri.getHost() != null) {
            filter.addDataAuthority(uri.getHost(), null);
        }
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = mContext.getPackageName();
        resolveInfo.activityInfo.name = activityClass.getName();
        resolveInfo.filter = filter;
        mShadowPackageManager.addResolveInfoForIntent(probe, resolveInfo);
    }

    @Test
    public void isSameOrigin_normalizesDefaultPortsAndComparesCaseInsensitively() {
        assertTrue(Utils.isSameOrigin(
                Uri.parse("https://WWW.EXAMPLE.COM/path"),
                Uri.parse("https://www.example.com:443/other")));
        assertTrue(Utils.isSameOrigin(
                Uri.parse("http://example.com:80/a"),
                Uri.parse("http://example.com/b")));
        assertFalse(Utils.isSameOrigin(
                Uri.parse("https://www.example.com/path"),
                Uri.parse("https://www.example.com:8443/path")));
        assertFalse(Utils.isSameOrigin(
                Uri.parse("https://www.example.com/path"),
                Uri.parse("http://www.example.com/path")));
        assertFalse(Utils.isSameOrigin(
                Uri.parse("https://www.example.com/path"),
                Uri.parse("https://www.evil.com/path")));
        assertFalse(Utils.isSameOrigin(null, Uri.parse("https://www.example.com")));
    }

    @Test
    public void getLaunchingUrl_allowsSameOriginAsDefaultUrl() {
        Uri deepLink = Uri.parse("https://www.example.com/deep/link?foo=bar");
        Intent intent = new Intent(Intent.ACTION_VIEW, deepLink)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertEquals(deepLink, controller.get().getLaunchingUrl());
    }

    @Test
    public void getLaunchingUrl_allowsAdditionalTrustedOrigin() {
        Uri extraOriginLink = Uri.parse("https://sub.example.com/feature/page");
        Intent intent = new Intent(Intent.ACTION_VIEW, extraOriginLink)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertEquals(extraOriginLink, controller.get().getLaunchingUrl());
    }

    @Test
    public void getLaunchingUrl_allowsSecondHostDeclaredInOwnIntentFilter() {
        Uri apexDeepLink = Uri.parse("https://example.com/products/42");
        registerOwnBrowsableIntentFilter(TestLauncherActivity.class, apexDeepLink);

        Intent intent = new Intent(Intent.ACTION_VIEW, apexDeepLink)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertEquals(apexDeepLink, controller.get().getLaunchingUrl());
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(apexDeepLink, launchedIntent.getData());
    }

    @Test
    public void getLaunchingUrl_allowsHostDeclaredOnActivityAlias() {
        Uri aliasDeepLink = Uri.parse("https://alias.example.org/welcome");
        Intent probe = new Intent(Intent.ACTION_VIEW, aliasDeepLink)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setPackage(mContext.getPackageName());
        IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addCategory(Intent.CATEGORY_BROWSABLE);
        filter.addDataScheme("https");
        filter.addDataAuthority("alias.example.org", null);
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = mContext.getPackageName();
        resolveInfo.activityInfo.name = mContext.getPackageName() + ".LauncherAlias";
        resolveInfo.activityInfo.targetActivity = TestLauncherActivity.class.getName();
        resolveInfo.filter = filter;
        mShadowPackageManager.addResolveInfoForIntent(probe, resolveInfo);

        Intent intent = new Intent(Intent.ACTION_VIEW, aliasDeepLink)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertEquals(aliasDeepLink, controller.get().getLaunchingUrl());
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(aliasDeepLink, launchedIntent.getData());
    }

    @Test
    public void getLaunchingUrl_rejectsUntrustedHttpsWhenOwnFilterIsSchemeOnly() {
        Uri untrustedUri = Uri.parse("https://www.evil.com/phish");
        Intent probe = new Intent(Intent.ACTION_VIEW, untrustedUri)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setPackage(mContext.getPackageName());
        IntentFilter schemeOnlyFilter = new IntentFilter(Intent.ACTION_VIEW);
        schemeOnlyFilter.addCategory(Intent.CATEGORY_DEFAULT);
        schemeOnlyFilter.addCategory(Intent.CATEGORY_BROWSABLE);
        schemeOnlyFilter.addDataScheme("https");
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = mContext.getPackageName();
        resolveInfo.activityInfo.name = TestLauncherActivity.class.getName();
        resolveInfo.filter = schemeOnlyFilter;
        mShadowPackageManager.addResolveInfoForIntent(probe, resolveInfo);

        Intent intent = new Intent(Intent.ACTION_VIEW, untrustedUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertEquals(Uri.parse(DEFAULT_URL), controller.get().getLaunchingUrl());

        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(Uri.parse(DEFAULT_URL), launchedIntent.getData());
        assertNull(launchedIntent.getParcelableExtra(
                TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
    }

    @Test
    public void getLaunchingUrl_rejectsUntrustedHttpsOriginAndFallsBackToDefaultUrl() {
        registerOwnBrowsableIntentFilter(
                TestLauncherActivity.class, Uri.parse("https://example.com/products/42"));

        Uri untrustedUri = Uri.parse("https://www.evil.com/phish");
        Intent intent = new Intent(Intent.ACTION_VIEW, untrustedUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertEquals(Uri.parse(DEFAULT_URL), controller.get().getLaunchingUrl());

        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(Uri.parse(DEFAULT_URL), launchedIntent.getData());
        assertNull(launchedIntent.getParcelableExtra(
                TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
    }

    @Test
    public void getLaunchingUrl_rejectsUnregisteredSchemeAndDoesNotLeakOriginalLaunchUrl() {
        Uri untrustedHttpUri = Uri.parse("http://www.evil.com/phish");
        Intent intent = new Intent(Intent.ACTION_VIEW, untrustedHttpUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertEquals(Uri.parse(DEFAULT_URL), controller.get().getLaunchingUrl());

        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(Uri.parse(DEFAULT_URL), launchedIntent.getData());
        assertNull(launchedIntent.getParcelableExtra(
                TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
    }

    @Test
    public void launchTwa_setsOriginalLaunchUrlForTrustedHttpsWhenLaunchingUrlIsRewritten() {
        Uri trustedDeepLink = Uri.parse("https://www.example.com/deep/link");
        Intent intent = new Intent(Intent.ACTION_VIEW, trustedDeepLink)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<RewritingLauncherActivity> controller =
                Robolectric.buildActivity(RewritingLauncherActivity.class, intent);
        controller.create();

        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(
                Uri.parse("https://www.example.com/deep/link?appInstanceId=123"),
                launchedIntent.getData());
        assertEquals(
                trustedDeepLink,
                launchedIntent.getParcelableExtra(
                        TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
    }

    @Test
    public void launchTwa_setsOriginalLaunchUrlForRegisteredProtocolHandlerScheme() {
        Uri protocolUri = Uri.parse("web+coffee://latte");
        Intent intent = new Intent(Intent.ACTION_VIEW, protocolUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(
                Uri.parse("https://www.example.com/coffee?type=web%2Bcoffee%3A%2F%2Flatte"),
                launchedIntent.getData());
        assertEquals(
                protocolUri,
                launchedIntent.getParcelableExtra(
                        TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
    }

    public static class CustomHookLauncherActivity extends RewritingLauncherActivity {
        @Override
        protected boolean isTrustedIntentUrl(@Nullable Uri uri) {
            return uri != null && "partner.example.net".equalsIgnoreCase(uri.getHost());
        }
    }

    @Test
    public void isTrustedIntentUrl_overrideAllowsCustomPartnerOriginInGetLaunchingUrlAndLaunchTwa() {
        PackageInfo packageInfo = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        ActivityInfo customActivityInfo = createActivityInfo(CustomHookLauncherActivity.class);
        packageInfo.activities = new ActivityInfo[]{
                packageInfo.activities[0], packageInfo.activities[1], customActivityInfo
        };

        Uri partnerCallback = Uri.parse("https://partner.example.net/oauth/callback");
        Intent intent = new Intent(Intent.ACTION_VIEW, partnerCallback)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<CustomHookLauncherActivity> controller =
                Robolectric.buildActivity(CustomHookLauncherActivity.class, intent);
        controller.create();

        Uri expectedRewritten = Uri.parse(
                "https://partner.example.net/oauth/callback?appInstanceId=123");
        assertEquals(expectedRewritten, controller.get().getLaunchingUrl());

        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(expectedRewritten, launchedIntent.getData());
        assertEquals(
                partnerCallback,
                launchedIntent.getParcelableExtra(
                        TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
    }

    @Test
    public void isTrustedOrigin_normalisesBareHostWithoutSchemeToHttps() {
        Resources spiedResources = spy(mContext.getResources());
        doReturn(new String[]{"barehost.example.com", "bareport.example.com:8443", EXTRA_ORIGIN})
                .when(spiedResources).getStringArray(EXTRA_ORIGINS_RES_ID);
        Context spiedContext = spy(mContext);
        doReturn(spiedResources).when(spiedContext).getResources();

        LauncherActivityMetadata metadata = LauncherActivityMetadata.parse(spiedContext);
        org.robolectric.shadows.ShadowLog.clear();

        assertTrue(Utils.isTrustedOrigin(
                Uri.parse("https://barehost.example.com/page"), metadata));
        assertTrue(Utils.isTrustedOrigin(
                Uri.parse("https://bareport.example.com:8443/page"), metadata));
        assertFalse(Utils.isTrustedOrigin(
                Uri.parse("http://barehost.example.com/page"), metadata));
        assertTrue(Utils.isTrustedOrigin(
                Uri.parse("https://sub.example.com/page"), metadata));

        boolean foundWarning = false;
        for (org.robolectric.shadows.ShadowLog.LogItem item :
                org.robolectric.shadows.ShadowLog.getLogsForTag("TWAUtils")) {
            if (item.type == android.util.Log.WARN
                    && item.msg.contains("barehost.example.com")) {
                foundWarning = true;
                break;
            }
        }
        assertTrue(foundWarning);
    }

    @Test(expected = SecurityException.class)
    public void getLaunchingUrl_throwsSecurityExceptionInDebuggableBuildForUntrustedHttpsOrigin() {
        mContext.getApplicationInfo().flags |= android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;
        Uri untrustedUri = Uri.parse("https://www.evil.com/phish");
        Intent intent = new Intent(Intent.ACTION_VIEW, untrustedUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();
    }
}
