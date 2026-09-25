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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
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
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class WebViewFallbackActivityTest {
    private static final String DEFAULT_URL = "https://www.example.com/twa/home";
    private static final String EXTRA_ORIGIN = "https://sub.example.com";
    private static final int EXTRA_ORIGINS_RES_ID = 0x7f030002;
    private static final String KEY_PREFIX =
            "com.google.browser.examples.twawebviewfallback.WebViewFallbackActivity.";
    private static final String KEY_LAUNCH_URI = KEY_PREFIX + "LAUNCH_URL";
    private static final String KEY_EXTRA_ORIGINS = KEY_PREFIX + "KEY_EXTRA_ORIGINS";
    private static final String KEY_LAUNCHER_COMPONENT = KEY_PREFIX + "KEY_LAUNCHER_COMPONENT";

    public static class TestWebViewFallbackActivity extends WebViewFallbackActivity {
        @Override
        public Resources getResources() {
            Resources spied = spy(super.getResources());
            doReturn(new String[]{EXTRA_ORIGIN}).when(spied).getStringArray(EXTRA_ORIGINS_RES_ID);
            return spied;
        }
    }

    public static class CustomNonTwaActivity extends Activity {}

    private Context mContext;
    private ShadowPackageManager mShadowPackageManager;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mShadowPackageManager = shadowOf(mContext.getPackageManager());

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = mContext.getPackageName();

        ActivityInfo launcherActivityInfo = new ActivityInfo();
        launcherActivityInfo.packageName = mContext.getPackageName();
        launcherActivityInfo.name = LauncherActivity.class.getName();
        launcherActivityInfo.metaData = new Bundle();
        launcherActivityInfo.metaData.putString(
                "android.support.customtabs.trusted.DEFAULT_URL", DEFAULT_URL);
        launcherActivityInfo.metaData.putInt(
                "android.support.customtabs.trusted.ADDITIONAL_TRUSTED_ORIGINS",
                EXTRA_ORIGINS_RES_ID);

        ActivityInfo webViewActivityInfo = new ActivityInfo();
        webViewActivityInfo.packageName = mContext.getPackageName();
        webViewActivityInfo.name = TestWebViewFallbackActivity.class.getName();

        ActivityInfo customNonTwaActivityInfo = new ActivityInfo();
        customNonTwaActivityInfo.packageName = mContext.getPackageName();
        customNonTwaActivityInfo.name = CustomNonTwaActivity.class.getName();

        packageInfo.activities = new ActivityInfo[]{
                launcherActivityInfo, webViewActivityInfo, customNonTwaActivityInfo
        };
        mShadowPackageManager.addPackage(packageInfo);
    }

    private void registerBrowsableIntentFilter(Class<?> activityClass, Uri uri) {
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

    private static WebView getLoadedWebView(WebViewFallbackActivity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        assertNotNull(content);
        assertEquals(1, content.getChildCount());
        return (WebView) content.getChildAt(0);
    }

    @Test
    public void onCreate_acceptsTrustedLaunchUrl() {
        Uri trustedDeepLink = Uri.parse("https://www.example.com/articles/42");
        Intent intent = new Intent(mContext, TestWebViewFallbackActivity.class)
                .putExtra(KEY_LAUNCH_URI, trustedDeepLink);

        ActivityController<TestWebViewFallbackActivity> controller =
                Robolectric.buildActivity(TestWebViewFallbackActivity.class, intent);
        controller.create();

        WebView webView = getLoadedWebView(controller.get());
        assertEquals(trustedDeepLink.toString(), shadowOf(webView).getLastLoadedUrl());
    }

    @Test
    public void onCreate_acceptsUppercaseHttpsLaunchUrl() {
        Uri uppercaseLaunchUri = Uri.parse("HTTPS://www.example.com/articles/42");
        Intent intent = new Intent(mContext, TestWebViewFallbackActivity.class)
                .putExtra(KEY_LAUNCH_URI, uppercaseLaunchUri);

        ActivityController<TestWebViewFallbackActivity> controller =
                Robolectric.buildActivity(TestWebViewFallbackActivity.class, intent);
        controller.create();

        WebView webView = getLoadedWebView(controller.get());
        assertNotNull(shadowOf(webView).getLastLoadedUrl());
        assertEquals(uppercaseLaunchUri.toString(), shadowOf(webView).getLastLoadedUrl());
    }

    @Test
    public void onCreate_acceptsLaunchUrlMatchingOwnBrowsableIntentFilter() {
        Uri filterDeepLink = Uri.parse("https://airhorner.com/sound/1");
        registerBrowsableIntentFilter(LauncherActivity.class, filterDeepLink);

        Intent intent = new Intent(mContext, TestWebViewFallbackActivity.class)
                .putExtra(KEY_LAUNCH_URI, filterDeepLink);

        ActivityController<TestWebViewFallbackActivity> controller =
                Robolectric.buildActivity(TestWebViewFallbackActivity.class, intent);
        controller.create();

        WebView webView = getLoadedWebView(controller.get());
        assertEquals(filterDeepLink.toString(), shadowOf(webView).getLastLoadedUrl());
    }

    @Test
    public void onCreate_fallsBackToDefaultUrlWhenLaunchUrlIsUntrusted() {
        registerBrowsableIntentFilter(
                LauncherActivity.class, Uri.parse("https://airhorner.com/sound/1"));

        Uri untrustedLaunchUri = Uri.parse("https://www.evil.com/phish");
        Intent intent = new Intent(mContext, TestWebViewFallbackActivity.class)
                .putExtra(KEY_LAUNCH_URI, untrustedLaunchUri);

        ActivityController<TestWebViewFallbackActivity> controller =
                Robolectric.buildActivity(TestWebViewFallbackActivity.class, intent);
        controller.create();

        WebView webView = getLoadedWebView(controller.get());
        assertEquals(DEFAULT_URL, shadowOf(webView).getLastLoadedUrl());
    }

    @Test
    public void onCreate_ignoresLauncherComponentExtraOutsidePackage() {
        Uri customFilterUri = Uri.parse("https://custom.example.org/deep");
        registerBrowsableIntentFilter(CustomNonTwaActivity.class, customFilterUri);

        // ComponentName with an external package name must be ignored even if its class name
        // matches an activity filter.
        ComponentName externalComponent = new ComponentName(
                "com.attacker.external", CustomNonTwaActivity.class.getName());
        Intent spoiledIntent = new Intent(mContext, TestWebViewFallbackActivity.class)
                .putExtra(KEY_LAUNCH_URI, customFilterUri)
                .putExtra(KEY_LAUNCHER_COMPONENT, externalComponent);

        ActivityController<TestWebViewFallbackActivity> spoiledController =
                Robolectric.buildActivity(TestWebViewFallbackActivity.class, spoiledIntent);
        spoiledController.create();

        WebView spoiledWebView = getLoadedWebView(spoiledController.get());
        assertEquals(DEFAULT_URL, shadowOf(spoiledWebView).getLastLoadedUrl());

        // Same ComponentName within this app's package is honored.
        ComponentName internalComponent = new ComponentName(
                mContext.getPackageName(), CustomNonTwaActivity.class.getName());
        Intent validIntent = new Intent(mContext, TestWebViewFallbackActivity.class)
                .putExtra(KEY_LAUNCH_URI, customFilterUri)
                .putExtra(KEY_LAUNCHER_COMPONENT, internalComponent);

        ActivityController<TestWebViewFallbackActivity> validController =
                Robolectric.buildActivity(TestWebViewFallbackActivity.class, validIntent);
        validController.create();

        WebView validWebView = getLoadedWebView(validController.get());
        assertEquals(customFilterUri.toString(), shadowOf(validWebView).getLastLoadedUrl());
    }

    @Test
    public void onCreate_dropsExtraOriginsNotPresentInManifestMetadata() {
        Uri trustedLaunchUri = Uri.parse(DEFAULT_URL);
        ArrayList<String> injectedOrigins = new ArrayList<>(
                Arrays.asList(EXTRA_ORIGIN, "https://www.evil.com"));
        Intent intent = new Intent(mContext, TestWebViewFallbackActivity.class)
                .putExtra(KEY_LAUNCH_URI, trustedLaunchUri)
                .putStringArrayListExtra(KEY_EXTRA_ORIGINS, injectedOrigins);

        ActivityController<TestWebViewFallbackActivity> controller =
                Robolectric.buildActivity(TestWebViewFallbackActivity.class, intent);
        controller.create();

        WebView webView = getLoadedWebView(controller.get());
        WebViewClient client = shadowOf(webView).getWebViewClient();
        assertNotNull(client);

        // Configured extra origin remains trusted and stays inside WebView (returns false).
        assertFalse(client.shouldOverrideUrlLoading(
                webView, "https://sub.example.com/allowed-path"));

        // Unconfigured origin passed in KEY_EXTRA_ORIGINS was dropped and bounces out to CCT.
        assertTrue(client.shouldOverrideUrlLoading(
                webView, "https://www.evil.com/injected-path"));
    }

    public static class BareHostExtraOriginWebViewFallbackActivity extends WebViewFallbackActivity {
        @Override
        public Resources getResources() {
            Resources spied = spy(super.getResources());
            doReturn(new String[]{"bare.example.com", "bareport.example.com:8443"})
                    .when(spied).getStringArray(EXTRA_ORIGINS_RES_ID);
            return spied;
        }
    }

    @Test
    public void webViewFallback_acceptsBareHostExtraOrigin() {
        PackageInfo packageInfo = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        ActivityInfo bareHostActivityInfo = new ActivityInfo();
        bareHostActivityInfo.packageName = mContext.getPackageName();
        bareHostActivityInfo.name = BareHostExtraOriginWebViewFallbackActivity.class.getName();
        packageInfo.activities = new ActivityInfo[]{
                packageInfo.activities[0],
                packageInfo.activities[1],
                packageInfo.activities[2],
                bareHostActivityInfo
        };

        Uri trustedLaunchUri = Uri.parse(DEFAULT_URL);
        ArrayList<String> extraOrigins = new ArrayList<>(
                Arrays.asList("bare.example.com", "https://bareport.example.com:8443"));
        Intent intent = new Intent(mContext, BareHostExtraOriginWebViewFallbackActivity.class)
                .putExtra(KEY_LAUNCH_URI, trustedLaunchUri)
                .putStringArrayListExtra(KEY_EXTRA_ORIGINS, extraOrigins);

        ActivityController<BareHostExtraOriginWebViewFallbackActivity> controller =
                Robolectric.buildActivity(BareHostExtraOriginWebViewFallbackActivity.class, intent);
        controller.create();

        WebView webView = getLoadedWebView(controller.get());
        WebViewClient client = shadowOf(webView).getWebViewClient();
        assertNotNull(client);

        // Bare host configured in ADDITIONAL_TRUSTED_ORIGINS is normalised to https and kept in-WebView.
        assertFalse(client.shouldOverrideUrlLoading(
                webView, "https://bare.example.com/inside-twa"));
        assertFalse(client.shouldOverrideUrlLoading(
                webView, "https://bareport.example.com:8443/inside-twa"));
        assertTrue(client.shouldOverrideUrlLoading(
                webView, "https://www.evil.com/outside-twa"));
    }
}
