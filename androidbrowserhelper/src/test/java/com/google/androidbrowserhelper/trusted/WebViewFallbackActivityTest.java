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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
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

    public static class TestWebViewFallbackActivity extends WebViewFallbackActivity {
        int setupWebSettingsCallCount = 0;

        @Override
        public Resources getResources() {
            Resources spied = spy(super.getResources());
            doReturn(new String[]{EXTRA_ORIGIN}).when(spied).getStringArray(EXTRA_ORIGINS_RES_ID);
            return spied;
        }

        @Override
        protected void setupWebSettings(WebSettings webSettings) {
            super.setupWebSettings(webSettings);
            setupWebSettingsCallCount++;
        }
    }

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

        packageInfo.activities = new ActivityInfo[]{
                launcherActivityInfo, webViewActivityInfo
        };
        mShadowPackageManager.addPackage(packageInfo);
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
        assertEquals(uppercaseLaunchUri.toString(), shadowOf(webView).getLastLoadedUrl());
    }

    @Test
    public void onCreate_loadsLaunchUrlWithoutOriginRevalidation() {
        Uri customAllowedUri = Uri.parse("https://partner.example.net/callback");
        Intent intent = new Intent(mContext, TestWebViewFallbackActivity.class)
                .putExtra(KEY_LAUNCH_URI, customAllowedUri);

        ActivityController<TestWebViewFallbackActivity> controller =
                Robolectric.buildActivity(TestWebViewFallbackActivity.class, intent);
        controller.create();

        WebView webView = getLoadedWebView(controller.get());
        assertEquals(customAllowedUri.toString(), shadowOf(webView).getLastLoadedUrl());
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
    public void webViewFallback_ignoresSchemeLessExtraOrigin() {
        PackageInfo packageInfo = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        ActivityInfo bareHostActivityInfo = new ActivityInfo();
        bareHostActivityInfo.packageName = mContext.getPackageName();
        bareHostActivityInfo.name = BareHostExtraOriginWebViewFallbackActivity.class.getName();
        packageInfo.activities = new ActivityInfo[]{
                packageInfo.activities[0],
                packageInfo.activities[1],
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

        // Only full https origins are honoured; scheme-less entries are ignored, as before.
        assertTrue(client.shouldOverrideUrlLoading(
                webView, "https://bare.example.com/outside-twa"));
        assertFalse(client.shouldOverrideUrlLoading(
                webView, "https://bareport.example.com:8443/inside-twa"));
        assertTrue(client.shouldOverrideUrlLoading(
                webView, "https://www.evil.com/outside-twa"));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void setupWebSettings_disablesFileAndContentAccess() {
        Intent intent = new Intent(mContext, TestWebViewFallbackActivity.class)
                .putExtra(KEY_LAUNCH_URI, Uri.parse(DEFAULT_URL));

        ActivityController<TestWebViewFallbackActivity> controller =
                Robolectric.buildActivity(TestWebViewFallbackActivity.class, intent);
        controller.create();

        WebView webView = getLoadedWebView(controller.get());
        WebSettings settings = webView.getSettings();
        assertFalse(settings.getAllowFileAccess());
        assertFalse(settings.getAllowContentAccess());
        assertFalse(settings.getAllowFileAccessFromFileURLs());
        assertFalse(settings.getAllowUniversalAccessFromFileURLs());

        WebSettings mockSettings = mock(WebSettings.class);
        controller.get().setupWebSettings(mockSettings);
        verify(mockSettings).setAllowFileAccess(false);
        verify(mockSettings).setAllowContentAccess(false);
        verify(mockSettings).setAllowFileAccessFromFileURLs(false);
        verify(mockSettings).setAllowUniversalAccessFromFileURLs(false);
    }

    @Test
    public void onRenderProcessGone_reappliesHardenedWebSettingsAndSubclassOverride() {
        Intent intent = new Intent(mContext, TestWebViewFallbackActivity.class)
                .putExtra(KEY_LAUNCH_URI, Uri.parse(DEFAULT_URL));

        ActivityController<TestWebViewFallbackActivity> controller =
                Robolectric.buildActivity(TestWebViewFallbackActivity.class, intent);
        controller.create();

        assertEquals(1, controller.get().setupWebSettingsCallCount);
        WebView initialWebView = getLoadedWebView(controller.get());
        WebViewClient client = shadowOf(initialWebView).getWebViewClient();
        assertNotNull(client);

        assertTrue(client.onRenderProcessGone(
                initialWebView, mock(RenderProcessGoneDetail.class)));
        assertEquals(2, controller.get().setupWebSettingsCallCount);

        WebView replacementWebView = getLoadedWebView(controller.get());
        assertFalse(replacementWebView.getSettings().getAllowFileAccess());
        assertFalse(replacementWebView.getSettings().getAllowContentAccess());
    }
}
