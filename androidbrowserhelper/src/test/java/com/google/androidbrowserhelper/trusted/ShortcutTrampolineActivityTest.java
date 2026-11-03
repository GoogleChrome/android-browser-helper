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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.app.ActivityManager;
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
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.browser.customtabs.TrustedWebUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowActivityManager;
import org.robolectric.shadows.ShadowAppTask;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class ShortcutTrampolineActivityTest {
    private Context mContext;
    private ShadowPackageManager mShadowPackageManager;
    private ShadowActivityManager mShadowActivityManager;

    private static final String DEFAULT_URL = "https://www.example.com/twa/home";
    private static final int ADDITIONAL_ORIGINS_RES_ID = 0x7f030003;

    public static class SubclassLauncherActivity extends LauncherActivity {}

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mShadowPackageManager = shadowOf(mContext.getPackageManager());
        mShadowActivityManager = shadowOf((ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE));

        // Set up the package info with metadata on a dummy LauncherActivity
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = mContext.getPackageName();

        ActivityInfo dummyLauncherActivity = new ActivityInfo();
        dummyLauncherActivity.packageName = mContext.getPackageName();
        dummyLauncherActivity.name = LauncherActivity.class.getName();
        dummyLauncherActivity.metaData = new Bundle();
        dummyLauncherActivity.metaData.putString("android.support.customtabs.trusted.DEFAULT_URL", DEFAULT_URL);

        ActivityInfo trampolineActivity = new ActivityInfo();
        trampolineActivity.packageName = mContext.getPackageName();
        trampolineActivity.name = ShortcutTrampolineActivity.class.getName();

        ActivityInfo coldShortcutActivity = new ActivityInfo();
        coldShortcutActivity.packageName = mContext.getPackageName();
        coldShortcutActivity.name = ColdShortcutActivity.class.getName();

        // Register a fake browser that can handle HTTP/HTTPS intents
        // so that resolveActivity() succeeds in the fallback strategy.
        registerBrowserForUri(Uri.parse("https://www.example.com/twa/shortcut"));

        packageInfo.activities = new ActivityInfo[]{dummyLauncherActivity, trampolineActivity, coldShortcutActivity};
        mShadowPackageManager.addPackage(packageInfo);
    }

    private void registerBrowserForUri(Uri uri) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = "com.android.chrome";
        resolveInfo.activityInfo.name = "com.android.chrome.ChromeTabbedActivity";
        mShadowPackageManager.addResolveInfoForIntent(browserIntent, resolveInfo);
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
        ResolveInfo ownFilterInfo = new ResolveInfo();
        ownFilterInfo.activityInfo = new ActivityInfo();
        ownFilterInfo.activityInfo.packageName = mContext.getPackageName();
        ownFilterInfo.activityInfo.name = activityName;
        ownFilterInfo.activityInfo.targetActivity = targetActivity;
        ownFilterInfo.activityInfo.metaData = metaData;
        ownFilterInfo.filter = filter;
        mShadowPackageManager.addResolveInfoForIntent(probe, ownFilterInfo);
    }

    @Test
    public void activityFinishesSynchronously() {
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("https://www.example.com/twa/shortcut"));

        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();

        assertTrue(controller.get().isFinishing());
    }

    @Test
    public void launchesColdShortcutActivity_whenNoTwaTaskRunningOnDesktop() {
        mShadowPackageManager.setSystemFeature(ChromeOsSupport.ARC_FEATURE, true);

        Uri trustedUri = Uri.parse("https://www.example.com/twa/shortcut");
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(trustedUri);

        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();

        // The trampoline activity finishes synchronously.
        assertTrue(controller.get().isFinishing());

        // Cold launch on desktop starts ColdShortcutActivity directly in a new task.
        Intent launchedIntent = shadowOf(controller.get()).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(new ComponentName(mContext, ColdShortcutActivity.class), launchedIntent.getComponent());
        assertEquals(trustedUri, launchedIntent.getData());
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, launchedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void launchesTwaViaLauncher_whenColdLaunchOnMobile() {
        // Desktop features absent (standard mobile device environment).
        Uri trustedUri = Uri.parse("https://www.example.com/twa/shortcut");
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(trustedUri);

        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();
        shadowOf(Looper.getMainLooper()).idle();

        assertTrue(controller.get().isFinishing());

        // Mobile cold launch routes via TwaLauncher without creating an opaque ColdShortcutActivity.
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertNotEquals(new ComponentName(mContext, ColdShortcutActivity.class), launchedIntent.getComponent());
        assertEquals(Intent.ACTION_VIEW, launchedIntent.getAction());
        assertEquals(trustedUri, launchedIntent.getData());
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, launchedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void launchesCustomColdShortcutActivity_whenConfiguredInMetadata() {
        mShadowPackageManager.setSystemFeature(ChromeOsSupport.ARC_FEATURE, true);

        String customColdActivity = ".CustomColdShortcutActivity";
        mShadowPackageManager.addOrUpdateActivity(
                new ActivityInfo() {{
                    packageName = mContext.getPackageName();
                    name = LauncherActivity.class.getName();
                    metaData = new Bundle();
                    metaData.putString("android.support.customtabs.trusted.DEFAULT_URL", DEFAULT_URL);
                    metaData.putString("android.support.customtabs.trusted.COLD_SHORTCUT_ACTIVITY", customColdActivity);
                }});

        Uri trustedUri = Uri.parse("https://www.example.com/twa/shortcut");
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(trustedUri);

        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();

        assertTrue(controller.get().isFinishing());

        Intent launchedIntent = shadowOf(controller.get()).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(new ComponentName(mContext, mContext.getPackageName() + customColdActivity),
                launchedIntent.getComponent());
        assertEquals(trustedUri, launchedIntent.getData());
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, launchedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void launchesTwaViaLauncher_whenTwaTaskAlreadyRunning() {
        // Simulate an already-running TWA task.
        ActivityManager.AppTask task = ShadowAppTask.newInstance();
        ShadowAppTask shadowAppTask = shadowOf(task);
        ActivityManager.RecentTaskInfo taskInfo = new ActivityManager.RecentTaskInfo();
        taskInfo.id = 123;
        taskInfo.baseIntent = new Intent().setComponent(new ComponentName(mContext, LauncherActivity.class));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            taskInfo.taskId = 123;
            taskInfo.isRunning = true;
        }
        shadowAppTask.setTaskInfo(taskInfo);
        mShadowActivityManager.setAppTasks(Collections.singletonList(task));

        Uri trustedUri = Uri.parse("https://www.example.com/twa/shortcut");
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(trustedUri);

        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();
        shadowOf(Looper.getMainLooper()).idle();

        assertTrue(controller.get().isFinishing());

        // Warm launch routes through TwaLauncher fallback, NOT ColdShortcutActivity.
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertNotEquals(new ComponentName(mContext, ColdShortcutActivity.class), launchedIntent.getComponent());
        assertEquals(Intent.ACTION_VIEW, launchedIntent.getAction());
        assertEquals(trustedUri, launchedIntent.getData());
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, launchedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void launchesTwaViaLauncher_whenCustomColdShortcutTaskRunningOnDesktop() {
        mShadowPackageManager.setSystemFeature(ChromeOsSupport.ARC_FEATURE, true);

        String customColdActivity = ".CustomColdShortcutActivity";
        mShadowPackageManager.addOrUpdateActivity(
                new ActivityInfo() {{
                    packageName = mContext.getPackageName();
                    name = LauncherActivity.class.getName();
                    metaData = new Bundle();
                    metaData.putString("android.support.customtabs.trusted.DEFAULT_URL", DEFAULT_URL);
                    metaData.putString("android.support.customtabs.trusted.COLD_SHORTCUT_ACTIVITY", customColdActivity);
                }});

        // Simulate a TWA task that was started by the custom cold shortcut activity.
        ComponentName customColdComponent =
                new ComponentName(mContext, mContext.getPackageName() + customColdActivity);
        ActivityManager.AppTask task = ShadowAppTask.newInstance();
        ShadowAppTask shadowAppTask = shadowOf(task);
        ActivityManager.RecentTaskInfo taskInfo = new ActivityManager.RecentTaskInfo();
        taskInfo.id = 123;
        taskInfo.baseIntent = new Intent().setComponent(customColdComponent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            taskInfo.taskId = 123;
            taskInfo.isRunning = true;
        }
        shadowAppTask.setTaskInfo(taskInfo);
        mShadowActivityManager.setAppTasks(Collections.singletonList(task));

        Uri trustedUri = Uri.parse("https://www.example.com/twa/shortcut");
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(trustedUri);

        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();
        shadowOf(Looper.getMainLooper()).idle();

        assertTrue(controller.get().isFinishing());

        // The running custom cold shortcut task is recognized, so the shortcut takes the warm
        // TwaLauncher path instead of starting another cold shortcut activity.
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertNotEquals(customColdComponent, launchedIntent.getComponent());
        assertEquals(Intent.ACTION_VIEW, launchedIntent.getAction());
        assertEquals(trustedUri, launchedIntent.getData());
    }

    @Test
    public void launchesColdShortcutActivity_whenRunningTaskIsNotTwa() {
        mShadowPackageManager.setSystemFeature(ChromeOsSupport.ARC_FEATURE, true);

        // Simulate a running non-TWA task (e.g. WebViewFallbackActivity).
        ActivityManager.AppTask task = ShadowAppTask.newInstance();
        ShadowAppTask shadowAppTask = shadowOf(task);
        ActivityManager.RecentTaskInfo taskInfo = new ActivityManager.RecentTaskInfo();
        taskInfo.id = 456;
        taskInfo.baseIntent = new Intent().setComponent(new ComponentName(mContext, WebViewFallbackActivity.class));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            taskInfo.taskId = 456;
            taskInfo.isRunning = true;
        }
        shadowAppTask.setTaskInfo(taskInfo);
        mShadowActivityManager.setAppTasks(Collections.singletonList(task));

        Uri trustedUri = Uri.parse("https://www.example.com/twa/shortcut");
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(trustedUri);

        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();

        assertTrue(controller.get().isFinishing());

        // Non-TWA task is ignored; cold launch starts ColdShortcutActivity.
        Intent launchedIntent = shadowOf(controller.get()).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(new ComponentName(mContext, ColdShortcutActivity.class), launchedIntent.getComponent());
    }

    @Test
    public void dropsUntrustedUri() {
        Uri untrustedUri = Uri.parse("https://www.evil.com/twa/shortcut");
        Intent intent = new Intent(Intent.ACTION_VIEW).setData(untrustedUri);

        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();

        assertTrue(controller.get().isFinishing());

        // Neither ColdShortcutActivity nor TwaLauncher should be started.
        assertNull(shadowOf(controller.get()).getNextStartedActivity());
        assertNull(shadowOf(RuntimeEnvironment.application).getNextStartedActivity());
    }

    @Test
    public void launchesTwaForHostDeclaredInOwnIntentFilter() {
        Uri secondDomainUri = Uri.parse("https://shop.example.com/deals");
        registerOwnBrowsableIntentFilter(
                LauncherActivity.class.getName(),
                null,
                null,
                secondDomainUri,
                "shop.example.com");
        registerBrowserForUri(secondDomainUri);

        Intent intent = new Intent(Intent.ACTION_VIEW).setData(secondDomainUri);
        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();
        shadowOf(Looper.getMainLooper()).idle();

        assertTrue(controller.get().isFinishing());
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(secondDomainUri, launchedIntent.getData());
    }

    @Test
    public void coldShortcutActivityOnDesktop_acceptsHostDeclaredInLauncherIntentFilter() {
        mShadowPackageManager.setSystemFeature(ChromeOsSupport.ARC_FEATURE, true);

        Uri secondDomainUri = Uri.parse("https://shop.example.com/deals");
        registerOwnBrowsableIntentFilter(
                LauncherActivity.class.getName(),
                null,
                null,
                secondDomainUri,
                "shop.example.com");
        registerBrowserForUri(secondDomainUri);
        Intent httpBrowserIntent = new Intent()
                .setData(Uri.fromParts("http", "", null))
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE);
        ResolveInfo browserResolveInfo = new ResolveInfo();
        browserResolveInfo.activityInfo = new ActivityInfo();
        browserResolveInfo.activityInfo.packageName = "com.android.chrome";
        browserResolveInfo.activityInfo.name = "com.android.chrome.ChromeTabbedActivity";
        mShadowPackageManager.addResolveInfoForIntent(httpBrowserIntent, browserResolveInfo);

        Intent shortcutIntent = new Intent(Intent.ACTION_VIEW).setData(secondDomainUri);
        ActivityController<ShortcutTrampolineActivity> trampolineController =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, shortcutIntent);
        trampolineController.create();

        Intent coldLaunchIntent = shadowOf(trampolineController.get()).getNextStartedActivity();
        assertNotNull(coldLaunchIntent);
        assertEquals(new ComponentName(mContext, ColdShortcutActivity.class),
                coldLaunchIntent.getComponent());
        assertEquals(secondDomainUri, coldLaunchIntent.getData());

        // ColdShortcutActivity itself has no <intent-filter>, so verify it accepts the URI
        // declared on the app's launcher activity when started by ShortcutTrampolineActivity.
        ActivityController<ColdShortcutActivity> coldController =
                Robolectric.buildActivity(ColdShortcutActivity.class, coldLaunchIntent);
        coldController.create();
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(secondDomainUri, coldController.get().getLaunchingUrl());
        Intent browserLaunched = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(browserLaunched);
        assertEquals(secondDomainUri, browserLaunched.getData());
        coldController.destroy();
    }

    @Test
    public void launchesTwaForHostDeclaredOnAliasWithDefaultUrlMetadata() {
        PackageInfo packageInfo = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        ActivityInfo subclassInfo = new ActivityInfo();
        subclassInfo.packageName = mContext.getPackageName();
        subclassInfo.name = SubclassLauncherActivity.class.getName();
        packageInfo.activities = new ActivityInfo[]{
                packageInfo.activities[0], packageInfo.activities[1], packageInfo.activities[2], subclassInfo
        };

        Bundle aliasMetaData = new Bundle();
        aliasMetaData.putString("android.support.customtabs.trusted.DEFAULT_URL", DEFAULT_URL);

        Uri aliasDomainUri = Uri.parse("https://alias.example.com/deals");
        registerOwnBrowsableIntentFilter(
                mContext.getPackageName() + ".LauncherAlias",
                SubclassLauncherActivity.class.getName(),
                aliasMetaData,
                aliasDomainUri,
                "alias.example.com");
        registerBrowserForUri(aliasDomainUri);

        Intent intent = new Intent(Intent.ACTION_VIEW).setData(aliasDomainUri);
        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();
        shadowOf(Looper.getMainLooper()).idle();

        assertTrue(controller.get().isFinishing());
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNotNull(launchedIntent);
        assertEquals(aliasDomainUri, launchedIntent.getData());
    }

    @Test
    public void dropsUriForHostDeclaredOnAliasWithoutDefaultUrlMetadata() {
        PackageInfo packageInfo = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        ActivityInfo subclassInfo = new ActivityInfo();
        subclassInfo.packageName = mContext.getPackageName();
        subclassInfo.name = SubclassLauncherActivity.class.getName();
        packageInfo.activities = new ActivityInfo[]{
                packageInfo.activities[0], packageInfo.activities[1], packageInfo.activities[2], subclassInfo
        };

        Uri aliasDomainUri = Uri.parse("https://alias.example.com/deals");
        registerOwnBrowsableIntentFilter(
                mContext.getPackageName() + ".LauncherAlias",
                SubclassLauncherActivity.class.getName(),
                new Bundle(),
                aliasDomainUri,
                "alias.example.com");
        registerBrowserForUri(aliasDomainUri);

        Intent intent = new Intent(Intent.ACTION_VIEW).setData(aliasDomainUri);
        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();
        shadowOf(Looper.getMainLooper()).idle();

        assertTrue(controller.get().isFinishing());
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNull(launchedIntent);
    }

    @Test
    public void shortcut_dropsNonHttpsUriEvenWhenItMatchesOwnIntentFilter() {
        Uri customSchemeUri = Uri.parse("myapp://home/dashboard");
        registerOwnBrowsableIntentFilter(
                LauncherActivity.class.getName(),
                null,
                null,
                customSchemeUri,
                "home");

        Intent intent = new Intent(Intent.ACTION_VIEW).setData(customSchemeUri);
        ActivityController<ShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(ShortcutTrampolineActivity.class, intent);

        controller.create();
        shadowOf(Looper.getMainLooper()).idle();

        assertTrue(controller.get().isFinishing());
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNull(launchedIntent);
    }

    public static class HttpOriginShortcutTrampolineActivity extends ShortcutTrampolineActivity {
        @Override
        public Resources getResources() {
            Resources spied = spy(super.getResources());
            doReturn(new String[]{"http://legacy.example.com"})
                    .when(spied).getStringArray(ADDITIONAL_ORIGINS_RES_ID);
            return spied;
        }
    }

    @Test
    public void dropsHttpUriEvenWhenMatchingHttpAdditionalTrustedOrigin() {
        PackageInfo packageInfo = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        packageInfo.activities[0].metaData.putInt(
                "android.support.customtabs.trusted.ADDITIONAL_TRUSTED_ORIGINS",
                ADDITIONAL_ORIGINS_RES_ID);
        ActivityInfo trampolineInfo = new ActivityInfo();
        trampolineInfo.packageName = mContext.getPackageName();
        trampolineInfo.name = HttpOriginShortcutTrampolineActivity.class.getName();
        packageInfo.activities = new ActivityInfo[]{
                packageInfo.activities[0], packageInfo.activities[1], packageInfo.activities[2], trampolineInfo
        };

        Uri httpUri = Uri.parse("http://legacy.example.com/page");
        registerBrowserForUri(httpUri);

        Intent intent = new Intent(Intent.ACTION_VIEW).setData(httpUri);
        ActivityController<HttpOriginShortcutTrampolineActivity> controller =
                Robolectric.buildActivity(HttpOriginShortcutTrampolineActivity.class, intent);

        controller.create();
        shadowOf(Looper.getMainLooper()).idle();

        assertTrue(controller.get().isFinishing());
        Intent launchedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedActivity();
        assertNull(launchedIntent);
    }
}
