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

import static androidx.browser.customtabs.CustomTabsService.TRUSTED_WEB_ACTIVITY_CATEGORY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.Token;
import androidx.browser.trusted.TokenStore;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the delegation {@link Token} lifecycle in {@link TwaLauncher}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class TwaLauncherTokenLifecycleTest {
    private static final Uri LAUNCH_URI = Uri.parse("https://www.example.com/");
    private static final String TWA_PROVIDER = "com.trustedweb.provider";
    private static final String CCT_PROVIDER = "com.customtabs.provider";
    private static final String BROWSER_PROVIDER = "com.browser.only";

    /**
     * Hand-written fake {@link TokenStore} that records every {@link #store(Token)} call in order,
     * distinguishing "never called" from "called with {@code null}".
     */
    private static final class RecordingTokenStore implements TokenStore {
        final List<Token> calls = new ArrayList<>();
        @Nullable
        Token currentToken;

        RecordingTokenStore(@Nullable Token initialToken) {
            this.currentToken = initialToken;
        }

        @Override
        public void store(@Nullable Token token) {
            calls.add(token);
            currentToken = token;
        }

        @Nullable
        @Override
        public Token load() {
            return currentToken;
        }
    }

    private Context mContext;
    private ShadowPackageManager mShadowPackageManager;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mShadowPackageManager = shadowOf(mContext.getPackageManager());
        mShadowPackageManager.setSystemFeature(ChromeOsSupport.ARC_FEATURE, false);
    }

    private void registerSignedPackage(String packageName) {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.signatures = new Signature[]{new Signature("0102030405060708")};
        mShadowPackageManager.addPackage(packageInfo);
    }

    private void installBrowserActivity(String packageName) {
        Intent intent = new Intent()
                .setData(Uri.fromParts("http", "", null))
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE);
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = packageName;
        mShadowPackageManager.addResolveInfoForIntent(intent, resolveInfo);
    }

    private void installCustomTabsProvider(String packageName) {
        registerSignedPackage(packageName);
        installBrowserActivity(packageName);

        Intent serviceIntent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;
        mShadowPackageManager.addResolveInfoForIntent(serviceIntent, resolveInfo);
    }

    private void installTrustedWebActivityProvider(String packageName) {
        registerSignedPackage(packageName);
        installBrowserActivity(packageName);

        Intent serviceIntent = new Intent()
                .setAction(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = packageName;
        resolveInfo.filter = mock(IntentFilter.class);
        when(resolveInfo.filter.hasCategory(eq(TRUSTED_WEB_ACTIVITY_CATEGORY))).thenReturn(true);
        mShadowPackageManager.addResolveInfoForIntent(serviceIntent, resolveInfo);
    }

    @Test
    public void launch_inCustomTabMode_clearsDelegationToken() {
        installCustomTabsProvider(CCT_PROVIDER);
        Token staleToken = Token.create(CCT_PROVIDER, mContext.getPackageManager());
        assertNotNull(staleToken);
        RecordingTokenStore tokenStore = new RecordingTokenStore(staleToken);

        TwaLauncher launcher = new TwaLauncher(mContext, null, 1, tokenStore);
        TwaLauncher.FallbackStrategy noOpFallback = (ctx, builder, pkg, cb) -> {};
        launcher.launch(new TrustedWebActivityIntentBuilder(LAUNCH_URI),
                new CustomTabsCallback(), null, null, noOpFallback);

        assertEquals(1, tokenStore.calls.size());
        assertNull(tokenStore.calls.get(0));
        assertNull(tokenStore.load());
    }

    @Test
    public void launch_inBrowserModeWithNullProvider_clearsDelegationToken() {
        registerSignedPackage(TWA_PROVIDER);
        Token staleToken = Token.create(TWA_PROVIDER, mContext.getPackageManager());
        assertNotNull(staleToken);
        RecordingTokenStore tokenStore = new RecordingTokenStore(staleToken);

        // No browsers installed -> LaunchMode.BROWSER with mProviderPackage == null.
        TwaLauncher launcher = new TwaLauncher(mContext, null, 1, tokenStore);
        assertNull(launcher.getProviderPackage());
        TwaLauncher.FallbackStrategy noOpFallback = (ctx, builder, pkg, cb) -> {};
        launcher.launch(new TrustedWebActivityIntentBuilder(LAUNCH_URI),
                new CustomTabsCallback(), null, null, noOpFallback);

        assertEquals(1, tokenStore.calls.size());
        assertNull(tokenStore.calls.get(0));
        assertNull(tokenStore.load());
    }

    @Test
    public void launch_onSynchronousBindFailure_clearsDelegationToken() {
        installTrustedWebActivityProvider(TWA_PROVIDER);
        Token staleToken = Token.create(TWA_PROVIDER, mContext.getPackageManager());
        RecordingTokenStore tokenStore = new RecordingTokenStore(staleToken);

        Context spiedContext = spy(mContext);
        doReturn(false).when(spiedContext)
                .bindService(any(Intent.class), any(ServiceConnection.class), anyInt());

        TwaLauncher launcher = new TwaLauncher(spiedContext, null, 1, tokenStore);
        boolean[] fallbackCalled = {false};
        TwaLauncher.FallbackStrategy recordingFallback =
                (ctx, builder, pkg, cb) -> fallbackCalled[0] = true;

        launcher.launch(new TrustedWebActivityIntentBuilder(LAUNCH_URI),
                new CustomTabsCallback(), null, null, recordingFallback);

        assertTrue(fallbackCalled[0]);
        assertEquals(1, tokenStore.calls.size());
        assertNull(tokenStore.calls.get(0));
        assertNull(tokenStore.load());
    }

    @Test
    public void launch_whenSessionEstablished_storesTokenAndPreservesOnDisconnect() {
        installTrustedWebActivityProvider(TWA_PROVIDER);
        RecordingTokenStore tokenStore = new RecordingTokenStore(null);

        final CustomTabsServiceConnection[] capturedConnection = new CustomTabsServiceConnection[1];
        Context spiedContext = spy(mContext);
        doAnswer(invocation -> {
            capturedConnection[0] = invocation.getArgument(1);
            return true;
        }).when(spiedContext)
                .bindService(any(Intent.class), any(ServiceConnection.class), anyInt());

        TwaLauncher launcher = new TwaLauncher(spiedContext, null, 42, tokenStore);
        launcher.launch(new TrustedWebActivityIntentBuilder(LAUNCH_URI),
                new CustomTabsCallback(), null, null, (ctx, builder, pkg, cb) -> {});

        // Before session establishment: token has NOT been stored yet.
        assertTrue(tokenStore.calls.isEmpty());
        assertNotNull(capturedConnection[0]);

        CustomTabsClient mockClient = mock(CustomTabsClient.class);
        ComponentName componentName = new ComponentName(TWA_PROVIDER, "CustomTabsService");
        CustomTabsSession mockSession =
                CustomTabsSession.createMockSessionForTesting(componentName);
        when(mockClient.newSession(any(), eq(42))).thenReturn(mockSession);

        capturedConnection[0].onCustomTabsServiceConnected(componentName, mockClient);

        // Session established: token stored once and matches TWA_PROVIDER.
        assertEquals(1, tokenStore.calls.size());
        assertNotNull(tokenStore.calls.get(0));
        assertTrue(tokenStore.calls.get(0).matches(TWA_PROVIDER, mContext.getPackageManager()));

        // Guard rail: onServiceDisconnected nulls mSession but must NOT clear the token.
        capturedConnection[0].onServiceDisconnected(componentName);
        assertEquals(1, tokenStore.calls.size());
        assertNotNull(tokenStore.load());
    }

    @Test
    public void launch_whenAsyncNewSessionReturnsNull_clearsDelegationToken() {
        installTrustedWebActivityProvider(TWA_PROVIDER);
        Token staleToken = Token.create(TWA_PROVIDER, mContext.getPackageManager());
        RecordingTokenStore tokenStore = new RecordingTokenStore(staleToken);

        final CustomTabsServiceConnection[] capturedConnection = new CustomTabsServiceConnection[1];
        Context spiedContext = spy(mContext);
        doAnswer(invocation -> {
            capturedConnection[0] = invocation.getArgument(1);
            return true;
        }).when(spiedContext)
                .bindService(any(Intent.class), any(ServiceConnection.class), anyInt());

        TwaLauncher launcher = new TwaLauncher(spiedContext, null, 42, tokenStore);
        boolean[] fallbackCalled = {false};
        launcher.launch(new TrustedWebActivityIntentBuilder(LAUNCH_URI),
                new CustomTabsCallback(), null, null,
                (ctx, builder, pkg, cb) -> fallbackCalled[0] = true);

        CustomTabsClient mockClient = mock(CustomTabsClient.class);
        when(mockClient.newSession(any(), eq(42))).thenReturn(null);
        capturedConnection[0].onCustomTabsServiceConnected(
                new ComponentName(TWA_PROVIDER, "CustomTabsService"), mockClient);

        assertTrue(fallbackCalled[0]);
        assertEquals(1, tokenStore.calls.size());
        assertNull(tokenStore.calls.get(0));
        assertNull(tokenStore.load());
    }

    @Test
    public void launch_onArc_neitherStoresNorClearsDelegationToken() {
        mShadowPackageManager.setSystemFeature(ChromeOsSupport.ARC_FEATURE, true);
        registerSignedPackage(TWA_PROVIDER);
        Token arcToken = Token.create(TWA_PROVIDER, mContext.getPackageManager());
        RecordingTokenStore tokenStore = new RecordingTokenStore(arcToken);

        // 1. Non-TWA launch under ARC must not clear the token.
        installCustomTabsProvider(CCT_PROVIDER);
        TwaLauncher cctLauncher = new TwaLauncher(mContext, null, 1, tokenStore);
        cctLauncher.launch(new TrustedWebActivityIntentBuilder(LAUNCH_URI),
                new CustomTabsCallback(), null, null, (ctx, builder, pkg, cb) -> {});
        assertTrue(tokenStore.calls.isEmpty());
        assertEquals(arcToken, tokenStore.load());

        // 2. Session-established TWA launch under ARC must not overwrite the token.
        final CustomTabsServiceConnection[] capturedConnection = new CustomTabsServiceConnection[1];
        Context spiedContext = spy(mContext);
        doAnswer(invocation -> {
            capturedConnection[0] = invocation.getArgument(1);
            return true;
        }).when(spiedContext)
                .bindService(any(Intent.class), any(ServiceConnection.class), anyInt());

        TwaLauncher twaLauncher = new TwaLauncher(spiedContext, TWA_PROVIDER, 2, tokenStore);
        twaLauncher.launch(new TrustedWebActivityIntentBuilder(LAUNCH_URI),
                new CustomTabsCallback(), null, null, (ctx, builder, pkg, cb) -> {});

        CustomTabsClient mockClient = mock(CustomTabsClient.class);
        ComponentName componentName = new ComponentName(TWA_PROVIDER, "CustomTabsService");
        when(mockClient.newSession(any(), eq(2)))
                .thenReturn(CustomTabsSession.createMockSessionForTesting(componentName));
        capturedConnection[0].onCustomTabsServiceConnected(componentName, mockClient);

        assertTrue(tokenStore.calls.isEmpty());
        assertEquals(arcToken, tokenStore.load());
    }

    @Test
    public void launch_nonTwaLaunchDuringLiveSession_revokesStoredToken() {
        installTrustedWebActivityProvider(TWA_PROVIDER);
        RecordingTokenStore tokenStore = new RecordingTokenStore(null);

        final CustomTabsServiceConnection[] capturedConnection = new CustomTabsServiceConnection[1];
        Context spiedContext = spy(mContext);
        doAnswer(invocation -> {
            capturedConnection[0] = invocation.getArgument(1);
            return true;
        }).when(spiedContext)
                .bindService(any(Intent.class), any(ServiceConnection.class), anyInt());

        TwaLauncher liveSessionLauncher = new TwaLauncher(spiedContext, TWA_PROVIDER, 1, tokenStore);
        liveSessionLauncher.launch(new TrustedWebActivityIntentBuilder(LAUNCH_URI),
                new CustomTabsCallback(), null, null, (ctx, builder, pkg, cb) -> {});

        CustomTabsClient mockClient = mock(CustomTabsClient.class);
        ComponentName componentName = new ComponentName(TWA_PROVIDER, "CustomTabsService");
        when(mockClient.newSession(any(), eq(1)))
                .thenReturn(CustomTabsSession.createMockSessionForTesting(componentName));
        capturedConnection[0].onCustomTabsServiceConnected(componentName, mockClient);
        assertNotNull(tokenStore.load());

        // Subsequent launch when only a Custom Tabs provider is visible clears the slot.
        installCustomTabsProvider(CCT_PROVIDER);
        TwaProviderPicker.restrictToPackageForTesting(CCT_PROVIDER);
        try {
            TwaLauncher secondLauncher = new TwaLauncher(mContext, null, 2, tokenStore);
            secondLauncher.launch(new TrustedWebActivityIntentBuilder(LAUNCH_URI),
                    new CustomTabsCallback(), null, null, (ctx, builder, pkg, cb) -> {});
        } finally {
            TwaProviderPicker.restrictToPackageForTesting(null);
        }

        assertEquals(2, tokenStore.calls.size());
        assertNotNull(tokenStore.calls.get(0));
        assertNull(tokenStore.calls.get(1));
        assertNull(tokenStore.load());
    }
}
