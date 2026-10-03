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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
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
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        Utils.resetWarnedOriginsForTesting();
        mContext = RuntimeEnvironment.application;
        mContext.getApplicationInfo().flags &= ~ApplicationInfo.FLAG_DEBUGGABLE;
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
        String[] hosts = uri.getHost() != null ? new String[]{uri.getHost()} : new String[0];
        registerOwnBrowsableIntentFilter(activityClass.getName(), null, null, uri, hosts);
    }

    private void registerOwnBrowsableIntentFilter(
            Class<?> activityClass, Uri probeUri, String... hosts) {
        registerOwnBrowsableIntentFilter(activityClass.getName(), null, null, probeUri, hosts);
    }

    private void registerOwnBrowsableIntentFilter(
            String activityName,
            @Nullable String targetActivity,
            @Nullable Bundle metaData,
            Uri probeUri,
            String... hosts) {
        Intent probe = new Intent(Intent.ACTION_VIEW, probeUri)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setPackage(mContext.getPackageName());
        IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addCategory(Intent.CATEGORY_BROWSABLE);
        if (probeUri.getScheme() != null) {
            filter.addDataScheme(probeUri.getScheme());
        }
        for (String host : hosts) {
            filter.addDataAuthority(host, null);
        }
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = mContext.getPackageName();
        resolveInfo.activityInfo.name = activityName;
        resolveInfo.activityInfo.targetActivity = targetActivity;
        resolveInfo.activityInfo.metaData = metaData;
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
    public void isSameOrigin_rejectsNonAsciiHostsAndSchemesThatCaseFoldToAscii() {
        Uri asciiOrigin = Uri.parse("https://www.airhorner.com/");
        Uri dotlessI = Uri.parse("https://www.a\u0131rhorner.com/");
        Uri percentEncodedDotlessI = Uri.parse("https://www.a%C4%B1rhorner.com/");
        Uri dottedCapitalI = Uri.parse("https://www.a\u0130rhorner.com/");

        assertEquals("www.a\u0131rhorner.com", percentEncodedDotlessI.getHost());

        assertFalse(Utils.isSameOrigin(dotlessI, asciiOrigin));
        assertFalse(Utils.isSameOrigin(asciiOrigin, dotlessI));
        assertFalse(Utils.isSameOrigin(percentEncodedDotlessI, asciiOrigin));
        assertFalse(Utils.isSameOrigin(asciiOrigin, percentEncodedDotlessI));
        assertFalse(Utils.isSameOrigin(dottedCapitalI, asciiOrigin));
        assertFalse(Utils.isSameOrigin(asciiOrigin, dottedCapitalI));

        Uri longSScheme = Uri.parse("http\u017F://www.example.com/");
        Uri httpsExample = Uri.parse("https://www.example.com/");
        assertFalse(Utils.isSameOrigin(longSScheme, httpsExample));
        assertFalse(Utils.isSameOrigin(httpsExample, longSScheme));

        assertTrue(Utils.isSameOrigin(
                Uri.parse("https://WWW.AIRHORNER.COM/"), asciiOrigin));
    }

    @Test
    public void matchesOwnIntentFilter_rejectsNonAsciiHostThatCaseFoldsToFilterHost() {
        ComponentName component = new ComponentName(mContext, TestLauncherActivity.class);
        Uri nonAsciiUri = Uri.parse("https://l\u0131nks.example.com/a");
        Uri asciiUri = Uri.parse("https://links.example.com/a");
        registerOwnBrowsableIntentFilter(
                TestLauncherActivity.class, nonAsciiUri, "links.example.com");
        registerOwnBrowsableIntentFilter(
                TestLauncherActivity.class, asciiUri, "links.example.com");

        assertFalse(Utils.matchesOwnIntentFilter(mContext, component, nonAsciiUri));
        assertTrue(Utils.matchesOwnIntentFilter(mContext, component, asciiUri));
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
        registerOwnBrowsableIntentFilter(
                mContext.getPackageName() + ".LauncherAlias",
                TestLauncherActivity.class.getName(),
                null,
                aliasDeepLink,
                "alias.example.org");

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
        registerOwnBrowsableIntentFilter(TestLauncherActivity.class, untrustedUri, new String[0]);

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
    public void launchTwa_forwardsOriginalLaunchUrlForUnregisteredScheme() {
        Uri customSchemeUri = Uri.parse("myapp://open/x");
        Intent intent = new Intent(Intent.ACTION_VIEW, customSchemeUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertEquals(Uri.parse(DEFAULT_URL), controller.get().getLaunchingUrl());

        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(Uri.parse(DEFAULT_URL), launchedIntent.getData());
        assertEquals(
                customSchemeUri,
                launchedIntent.getParcelableExtra(
                        TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
    }

    @Test
    public void launchTwa_forwardsOriginalLaunchUrlForHttpDeepLink() {
        Uri httpDeepLink = Uri.parse("http://www.example.com/path");
        Intent intent = new Intent(Intent.ACTION_VIEW, httpDeepLink)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertEquals(Uri.parse(DEFAULT_URL), controller.get().getLaunchingUrl());

        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(Uri.parse(DEFAULT_URL), launchedIntent.getData());
        assertEquals(
                httpDeepLink,
                launchedIntent.getParcelableExtra(
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

    public static class SynthesizingLauncherActivity extends TestLauncherActivity {
        @Override
        protected Uri getUrlForIntent(Intent intent) {
            return Uri.parse("https://www.evil.com/synthesized?token=secret");
        }
    }

    @Test
    public void getUrlForIntent_overrideSynthesizingCrossOriginUrlFallsBackToDefaultUrl() {
        PackageInfo packageInfo = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        ActivityInfo synthesizingActivityInfo =
                createActivityInfo(SynthesizingLauncherActivity.class);
        packageInfo.activities = new ActivityInfo[]{
                packageInfo.activities[0], packageInfo.activities[1], synthesizingActivityInfo
        };

        Uri ignoredData = Uri.parse("https://www.example.com/ignored");
        Intent intent = new Intent(Intent.ACTION_VIEW, ignoredData)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityController<SynthesizingLauncherActivity> controller =
                Robolectric.buildActivity(SynthesizingLauncherActivity.class, intent);
        controller.create();

        assertEquals(Uri.parse(DEFAULT_URL), controller.get().getLaunchingUrl());

        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(Uri.parse(DEFAULT_URL), launchedIntent.getData());
        assertNull(launchedIntent.getParcelableExtra(
                TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
    }

    @Test
    public void isTrustedIntentUrl_trustsIntentFilterHostOnlyForHttps() {
        Uri httpsUri = Uri.parse("https://shop.example.com/deals");
        Uri httpUri = Uri.parse("http://shop.example.com/deals");
        registerOwnBrowsableIntentFilter(TestLauncherActivity.class, httpsUri);
        registerOwnBrowsableIntentFilter(TestLauncherActivity.class, httpUri);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEFAULT_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertTrue(controller.get().isTrustedIntentUrl(httpsUri));
        assertFalse(controller.get().isTrustedIntentUrl(httpUri));
    }

    public static class CountingLauncherActivity extends TestLauncherActivity {
        public int mTrustChecks;

        @Override
        protected boolean isTrustedIntentUrl(@Nullable Uri uri) {
            mTrustChecks++;
            return super.isTrustedIntentUrl(uri);
        }
    }

    @Test
    public void launch_checksInboundUrlTrustOnce() {
        PackageInfo packageInfo = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        ActivityInfo countingActivityInfo = createActivityInfo(CountingLauncherActivity.class);
        packageInfo.activities = new ActivityInfo[]{
                packageInfo.activities[0], packageInfo.activities[1], countingActivityInfo
        };

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.evil.com/phish"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityController<CountingLauncherActivity> controller =
                Robolectric.buildActivity(CountingLauncherActivity.class, intent);
        controller.create();

        assertNotNull(shadowOf(RuntimeEnvironment.application).getNextStartedActivity());
        assertEquals(1, controller.get().mTrustChecks);
    }

    @Test
    public void isTrustedOrigin_normalisesBareHostWithoutSchemeToHttps() {
        Resources spiedResources = spy(mContext.getResources());
        doReturn(new String[]{"barehost.example.com", "bareport.example.com:8443", EXTRA_ORIGIN})
                .when(spiedResources).getStringArray(EXTRA_ORIGINS_RES_ID);
        Context spiedContext = spy(mContext);
        doReturn(spiedResources).when(spiedContext).getResources();

        LauncherActivityMetadata metadata = LauncherActivityMetadata.parse(spiedContext);
        ShadowLog.clear();

        assertTrue(Utils.isTrustedOrigin(
                Uri.parse("https://barehost.example.com/page"), metadata));
        assertTrue(Utils.isTrustedOrigin(
                Uri.parse("https://bareport.example.com:8443/page"), metadata));
        assertFalse(Utils.isTrustedOrigin(
                Uri.parse("http://barehost.example.com/page"), metadata));
        assertTrue(Utils.isTrustedOrigin(
                Uri.parse("https://sub.example.com/page"), metadata));

        boolean foundWarning = false;
        for (ShadowLog.LogItem item : ShadowLog.getLogsForTag("TWAUtils")) {
            if (item.type == Log.WARN
                    && item.msg.contains("barehost.example.com")) {
                foundWarning = true;
                break;
            }
        }
        assertTrue(foundWarning);
    }

    @Test(expected = SecurityException.class)
    public void getLaunchingUrl_throwsSecurityExceptionInDebuggableBuildForUntrustedHttpsOrigin() {
        mContext.getApplicationInfo().flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        Uri untrustedUri = Uri.parse("https://www.evil.com/phish");
        Intent intent = new Intent(Intent.ACTION_VIEW, untrustedUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();
    }

    @Test
    public void getLaunchingUrl_rejectsHttpsWhenOwnFilterHostIsWildcard() {
        Uri untrustedUri = Uri.parse("https://www.evil.com/phish");
        registerOwnBrowsableIntentFilter(TestLauncherActivity.class, untrustedUri, "*");

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
    public void getLaunchingUrl_ignoresWildcardAuthorityInMixedFilter() {
        Uri allowedUri = Uri.parse("https://second.example/");
        registerOwnBrowsableIntentFilter(
                TestLauncherActivity.class, allowedUri, "*", "second.example");

        Uri evilUri = Uri.parse("https://evil.example/");
        registerOwnBrowsableIntentFilter(
                TestLauncherActivity.class, evilUri, "*", "second.example");

        Intent allowedIntent = new Intent(Intent.ACTION_VIEW, allowedUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityController<TestLauncherActivity> allowedController =
                Robolectric.buildActivity(TestLauncherActivity.class, allowedIntent);
        allowedController.create();
        assertEquals(allowedUri, allowedController.get().getLaunchingUrl());

        Intent evilIntent = new Intent(Intent.ACTION_VIEW, evilUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityController<TestLauncherActivity> evilController =
                Robolectric.buildActivity(TestLauncherActivity.class, evilIntent);
        evilController.create();
        assertEquals(Uri.parse(DEFAULT_URL), evilController.get().getLaunchingUrl());
    }

    @Test
    public void getLaunchingUrl_acceptsUppercaseSchemeForIntentFilterHost() {
        Uri normalizedFilterUri = Uri.parse("https://second.example/");
        registerOwnBrowsableIntentFilter(TestLauncherActivity.class, normalizedFilterUri);

        Uri uppercaseUri = Uri.parse("HTTPS://second.example/");
        Intent intent = new Intent(Intent.ACTION_VIEW, uppercaseUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertEquals(uppercaseUri, controller.get().getLaunchingUrl());
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(uppercaseUri, launchedIntent.getData());
    }

    @Test
    public void getLaunchingUrl_acceptsMixedCaseHostForIntentFilterHost() {
        Uri mixedCaseUri = Uri.parse("https://SECOND.Example/");
        registerOwnBrowsableIntentFilter(
                TestLauncherActivity.class, mixedCaseUri, "second.example");

        Intent intent = new Intent(Intent.ACTION_VIEW, mixedCaseUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertEquals(mixedCaseUri, controller.get().getLaunchingUrl());
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(mixedCaseUri, launchedIntent.getData());
    }

    public static class SuppressingRejectionLauncherActivity extends TestLauncherActivity {
        public final List<String> mReportedMessages = new ArrayList<>();
        public final List<RejectionOutcome> mReportedOutcomes = new ArrayList<>();

        @Override
        protected void reportRejection(
                @NonNull String message,
                @NonNull RejectionOutcome outcome) {
            mReportedMessages.add(message);
            mReportedOutcomes.add(outcome);
        }
    }

    @Test
    public void reportRejection_overrideSuppressesDebugExceptionButStillFallsBack() {
        mContext.getApplicationInfo().flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        PackageInfo packageInfo = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        ActivityInfo suppressingInfo =
                createActivityInfo(SuppressingRejectionLauncherActivity.class);
        packageInfo.activities = new ActivityInfo[]{
                packageInfo.activities[0], packageInfo.activities[1], suppressingInfo
        };

        Uri untrustedUri = Uri.parse("https://www.evil.com/phish");
        Intent intent = new Intent(Intent.ACTION_VIEW, untrustedUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<SuppressingRejectionLauncherActivity> controller =
                Robolectric.buildActivity(SuppressingRejectionLauncherActivity.class, intent);
        controller.create();

        assertEquals(
                Collections.singletonList(LauncherActivity.RejectionOutcome.LAUNCH_URL_SUBSTITUTED),
                controller.get().mReportedOutcomes);
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(Uri.parse(DEFAULT_URL), launchedIntent.getData());
        assertNull(launchedIntent.getParcelableExtra(
                TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
    }

    @Test
    public void reportRejection_releaseBuildMessageDoesNotContainPathOrQuery() {
        PackageInfo packageInfo = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        ActivityInfo suppressingInfo =
                createActivityInfo(SuppressingRejectionLauncherActivity.class);
        packageInfo.activities = new ActivityInfo[]{
                packageInfo.activities[0], packageInfo.activities[1], suppressingInfo
        };

        Uri untrustedUri = Uri.parse("https://evil.example/cb?code=SECRET");
        Intent intent = new Intent(Intent.ACTION_VIEW, untrustedUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<SuppressingRejectionLauncherActivity> controller =
                Robolectric.buildActivity(SuppressingRejectionLauncherActivity.class, intent);
        controller.create();

        assertEquals(1, controller.get().mReportedMessages.size());
        String message = controller.get().mReportedMessages.get(0);
        assertTrue(message.contains("https://evil.example"));
        assertFalse(message.contains("SECRET"));
        assertFalse(message.contains("/cb"));
    }

    @Test
    public void getMetadata_returnsParsedManifestMetadataAfterOnCreate() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEFAULT_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        assertNull(controller.get().getMetadata());

        controller.create();

        LauncherActivityMetadata metadata = controller.get().getMetadata();
        assertNotNull(metadata);
        assertEquals(DEFAULT_URL, metadata.defaultUrl);
        controller.destroy();
    }

    private void registerCustomTabsService(String packageName, boolean supportsTwa) {
        Intent serviceIntent = new Intent(
                androidx.browser.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
        IntentFilter filter = new IntentFilter(
                androidx.browser.customtabs.CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
        if (supportsTwa) {
            filter.addCategory(
                    androidx.browser.customtabs.CustomTabsService.TRUSTED_WEB_ACTIVITY_CATEGORY);
        }
        ResolveInfo serviceResolveInfo = new ResolveInfo();
        serviceResolveInfo.serviceInfo = new android.content.pm.ServiceInfo();
        serviceResolveInfo.serviceInfo.packageName = packageName;
        serviceResolveInfo.serviceInfo.name = packageName + ".CustomTabsService";
        serviceResolveInfo.filter = filter;
        mShadowPackageManager.addResolveInfoForIntent(serviceIntent, serviceResolveInfo);
    }

    @Test
    public void launchTwa_inCustomTabModeClearsLastLaunchedProviderAndDisablesManageDataActivity() {
        TwaSharedPreferencesManager prefs = new TwaSharedPreferencesManager(mContext);
        prefs.writeLastLaunchedProviderPackageName("com.android.chrome");
        registerCustomTabsService("com.android.chrome", false);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEFAULT_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertNull(prefs.readLastLaunchedProviderPackageName());
        assertEquals(
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                mContext.getPackageManager().getComponentEnabledSetting(
                        new ComponentName(mContext, ManageDataLauncherActivity.class)));
        controller.destroy();
    }

    @Test
    public void launchTwa_inTrustedWebActivityModeRecordsProviderPackage() {
        TwaSharedPreferencesManager prefs = new TwaSharedPreferencesManager(mContext);
        registerCustomTabsService("com.android.chrome", true);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEFAULT_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityController<TestLauncherActivity> controller =
                Robolectric.buildActivity(TestLauncherActivity.class, intent);
        controller.create();

        assertEquals("com.android.chrome", prefs.readLastLaunchedProviderPackageName());
        controller.destroy();
    }
}
