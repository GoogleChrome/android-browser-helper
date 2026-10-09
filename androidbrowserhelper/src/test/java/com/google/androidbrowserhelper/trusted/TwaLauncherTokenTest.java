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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;

import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.Token;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowPackageManager;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Tests for verifying the browser against an expected {@link Token}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class TwaLauncherTokenTest {
    private static final String BROWSER = "com.test.browser";
    private static final String OTHER_BROWSER = "com.other.browser";
    private static final String BROWSER_SIGNATURE = "abcdef1234567890";
    private static final String OTHER_SIGNATURE = "1234567890abcdef";
    private static final Uri URL = Uri.parse("https://www.example.com/");
    private static final int SESSION_ID = 1;

    private Application mContext;
    private ShadowPackageManager mShadowPackageManager;
    private SharedPreferencesTokenStore mTokenStore;

    @Before
    public void setUp() throws Exception {
        mContext = RuntimeEnvironment.application;
        mShadowPackageManager = shadowOf(mContext.getPackageManager());
        mTokenStore = new SharedPreferencesTokenStore(mContext);
        installPackage(BROWSER, BROWSER_SIGNATURE);
        installPackage(OTHER_BROWSER, OTHER_SIGNATURE);

        Field activitiesAlive = LauncherActivity.class.getDeclaredField("sLauncherActivitiesAlive");
        activitiesAlive.setAccessible(true);
        activitiesAlive.setInt(null, 0);
    }

    @Test
    public void withMatchingToken_launchesAndStoresToken() {
        Token token = createToken(BROWSER);
        TwaLauncher launcher = new TwaLauncher(mContext, BROWSER, SESSION_ID, mTokenStore, token);
        assertEquals(BROWSER, launcher.getProviderPackage());

        launcher.launch(new TrustedWebActivityIntentBuilder(URL), new QualityEnforcer(), null,
                null, (ctx, builder, pkg, cb) -> {});
        establishSession(BROWSER);

        assertNotNull(mTokenStore.load());
        assertArrayEquals(token.serialize(), mTokenStore.load().serialize());
    }

    @Test
    public void withMismatchedOrMalformedToken_blocksLaunchAndClearsStoredToken() {
        // Different package token.
        assertBlocked(BROWSER, createToken(OTHER_BROWSER));
        // Token set without a provider package.
        assertBlocked(null, createToken(BROWSER));
        // Malformed / empty token bytes.
        assertBlocked(BROWSER, Token.deserialize(new byte[0]));
        assertBlocked(BROWSER, Token.deserialize(new byte[]{1, 2, 3}));

        // Different signing certificate on the same package name.
        Token browserToken = createToken(BROWSER);
        installPackage(BROWSER, OTHER_SIGNATURE);
        assertBlocked(BROWSER, browserToken);

        // Uninstalled package.
        mShadowPackageManager.removePackage(BROWSER);
        assertBlocked(BROWSER, browserToken);
    }

    @Test
    public void withMismatchedToken_andCustomTabsFallback_showsDialogInstead() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        TwaLauncher launcher = new TwaLauncher(activity, BROWSER, SESSION_ID, mTokenStore,
                createToken(OTHER_BROWSER));

        launcher.launch(new TrustedWebActivityIntentBuilder(URL), new QualityEnforcer(), null,
                null);

        assertNull(shadowOf(activity).getNextStartedActivity());
        assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    @Test
    public void launcherActivity_verifiesLaunchingBrowserTokenMetadata() throws Exception {
        String validEncoded = Base64.encodeToString(createToken(BROWSER).serialize(),
                Base64.NO_WRAP);

        // Valid token launches in browser.
        assertEquals(1, launchActivity(BROWSER, validEncoded, null));
        assertNull(ShadowAlertDialog.getLatestAlertDialog());

        // Mismatched or invalid token blocks with dialog, even with webview fallback configured.
        assertEquals(0, launchActivity(BROWSER,
                Base64.encodeToString(createToken(OTHER_BROWSER).serialize(), Base64.NO_WRAP),
                "webview"));
        assertNotNull(ShadowAlertDialog.getLatestAlertDialog());

        ShadowAlertDialog.reset();
        assertEquals(0, launchActivity(BROWSER, "not-valid-token", null));
        assertNotNull(ShadowAlertDialog.getLatestAlertDialog());
    }

    private void assertBlocked(@Nullable String providerPackage, Token expectedToken) {
        mTokenStore.store(createToken(OTHER_BROWSER));
        TwaLauncher launcher = new TwaLauncher(mContext, providerPackage, SESSION_ID, mTokenStore,
                expectedToken);
        assertNull(launcher.getProviderPackage());

        String[] fallbackPkg = {"unset"};
        launcher.launch(new TrustedWebActivityIntentBuilder(URL), new QualityEnforcer(), null,
                null, (ctx, builder, pkg, cb) -> fallbackPkg[0] = pkg);

        assertNull(fallbackPkg[0]);
        assertTrue(shadowOf(mContext).getBoundServiceConnections().isEmpty());
        assertNull(mTokenStore.load());
    }

    private void establishSession(String packageName) {
        List<ServiceConnection> connections = shadowOf(mContext).getBoundServiceConnections();
        assertEquals(1, connections.size());
        ComponentName componentName = new ComponentName(packageName, "CustomTabsService");
        CustomTabsClient client = mock(CustomTabsClient.class);
        when(client.newSession(any(), eq(SESSION_ID)))
                .thenReturn(CustomTabsSession.createMockSessionForTesting(componentName));
        ((CustomTabsServiceConnection) connections.get(0))
                .onCustomTabsServiceConnected(componentName, client);
    }

    private int launchActivity(@Nullable String browser, @Nullable String encodedToken,
            @Nullable String fallbackStrategy) {
        Bundle metaData = new Bundle();
        metaData.putString("android.support.customtabs.trusted.DEFAULT_URL", URL.toString());
        if (browser != null) {
            metaData.putString("android.support.customtabs.trusted.LAUNCHING_BROWSER", browser);
        }
        if (encodedToken != null) {
            metaData.putString("android.support.customtabs.trusted.LAUNCHING_BROWSER_TOKEN",
                    encodedToken);
        }
        if (fallbackStrategy != null) {
            metaData.putString("android.support.customtabs.trusted.FALLBACK_STRATEGY",
                    fallbackStrategy);
        }
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = mContext.getPackageName();
        activityInfo.name = LauncherActivity.class.getName();
        activityInfo.metaData = metaData;
        mShadowPackageManager.addOrUpdateActivity(activityInfo);

        Intent intent = new Intent(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityController<LauncherActivity> controller =
                Robolectric.buildActivity(LauncherActivity.class, intent).create();
        int bound = shadowOf(mContext).getBoundServiceConnections().size();
        controller.destroy();
        return bound;
    }

    private void installPackage(String packageName, String signature) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.signatures = new Signature[]{new Signature(signature)};
        mShadowPackageManager.addPackage(packageInfo);
    }

    private Token createToken(String packageName) {
        Token token = Token.create(packageName, mContext.getPackageManager());
        assertNotNull(token);
        return token;
    }
}
