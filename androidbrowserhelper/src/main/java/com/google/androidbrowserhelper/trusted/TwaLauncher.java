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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.androidbrowserhelper.trusted.splashscreens.SplashScreenStrategy;

import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.customtabs.TrustedWebUtils;
import androidx.browser.trusted.Token;
import androidx.browser.trusted.TokenStore;
import androidx.browser.trusted.TrustedWebActivityIntent;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

import com.google.androidbrowserhelper.BuildConfig;

/**
 * Encapsulates the steps necessary to launch a Trusted Web Activity, such as establishing a
 * connection with {@link CustomTabsService}.
 */
public class TwaLauncher {
    private static final String TAG = "TwaLauncher";

    private static final int DEFAULT_SESSION_ID = 96375;

    private static final String EXTRA_STARTUP_UPTIME_MILLIS =
            "org.chromium.chrome.browser.customtabs.trusted.STARTUP_UPTIME_MILLIS";

    private static final String EXTRA_ANDROID_BROWSER_HELPER_VERSION =
            "org.chromium.chrome.browser.ANDROID_BROWSER_HELPER_VERSION";

    public static final FallbackStrategy CCT_FALLBACK_STRATEGY =
            (context, twaBuilder, providerPackage, completionCallback) -> {
        // CustomTabsIntent will fall back to launching the Browser if there are no Custom Tabs
        // providers installed.
        CustomTabsIntent intent = twaBuilder.buildCustomTabsIntent();
        if (providerPackage != null) {
            intent.intent.setPackage(providerPackage);
        }
        if (ChromeOsSupport.isRunningOnArc(context.getPackageManager())) {
            // Work around as ARC++ does not support native TWAs at the moment.
            intent.intent.putExtra(TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, true);
        }
        intent.launchUrl(context, twaBuilder.getUri());
        if (completionCallback != null) {
            completionCallback.run();
        }
    };

    public static final FallbackStrategy WEBVIEW_FALLBACK_STRATEGY =
            (context, twaBuilder, providerPackage, completionCallback) -> {
        Intent intent = WebViewFallbackActivity.createLaunchIntent(context,
                twaBuilder.getUri(), LauncherActivityMetadata.parse(context));
        context.startActivity(intent);
        if (completionCallback != null) {
            completionCallback.run();
        }
    };

    private Context mContext;

    @Nullable
    private final String mProviderPackage;

    @TwaProviderPicker.LaunchMode
    private final int mLaunchMode;

    private final int mSessionId;

    @Nullable
    private TwaCustomTabsServiceConnection mServiceConnection;

    @Nullable
    private CustomTabsSession mSession;

    private TokenStore mTokenStore;

    private boolean mDestroyed;

    private long mStartupUptimeMillis;

    public interface FallbackStrategy {
        void launch(Context context,
                    TrustedWebActivityIntentBuilder twaBuilder,
                    @Nullable String providerPackage,
                    @Nullable Runnable completionCallback);
    }

    /**
     * Creates an instance that will automatically choose the browser to launch a TWA in.
     * If no browser supports TWA, will launch a usual Custom Tab (see {@link TwaProviderPicker}.
     */
    public TwaLauncher(Context context) {
        this(context, (String) null);
    }

    /**
     * Same as above, but also allows to specify a browser to launch. If specified, it is assumed to
     * support TWAs.
     */
    public TwaLauncher(Context context, @Nullable String providerPackage) {
        this(context, providerPackage, DEFAULT_SESSION_ID,
                new SharedPreferencesTokenStore(context));
    }

    /**
     * @deprecated This method is no longer recommended for use since TwaLauncher is rolling back to
     * sessionId instead of taskId.
     */
    @Deprecated(forRemoval = true)
    public TwaLauncher(Context context, @Nullable Integer taskId) {
        this(context, null, taskId);
    }

    /**
     * @deprecated This method is no longer recommended for use since TwaLauncher is rolling back to
     * sessionId instead of taskId.
     */
    @Deprecated(forRemoval = true)
    public TwaLauncher(Context context, @Nullable String providerPackage, @Nullable Integer taskId) {
        this(context, providerPackage, SessionStore.makeSessionId(taskId),
                new SharedPreferencesTokenStore(context));
    }

    /**
     * Same as above, but also accepts a session id. This allows to launch multiple TWAs in the same
     * task.
     */
    public TwaLauncher(Context context, @Nullable String providerPackage, @Nullable Integer sessionId,
                       TokenStore tokenStore) {
        mContext = context;
        mSessionId = sessionId;
        mTokenStore = tokenStore;
        if (providerPackage == null) {
            TwaProviderPicker.Action action =
                    TwaProviderPicker.pickProvider(context.getPackageManager());
            mProviderPackage = action.provider;
            mLaunchMode = action.launchMode;
        } else {
            mProviderPackage = providerPackage;
            mLaunchMode = TwaProviderPicker.LaunchMode.TRUSTED_WEB_ACTIVITY;
        }
    }

    /**
     * Opens the specified url in a TWA.
     * When TWA is already running in the current task, the url will be opened in existing TWA,
     * if the same instance TwaLauncher is used. If another instance of TwaLauncher is used,
     * the TWA will be reused only if the session ids match (see constructors).
     *
     * @param url Url to open.
     */
    public void launch(Uri url) {
        launch(new TrustedWebActivityIntentBuilder(url), new QualityEnforcer(), null, null, null);
    }


    /**
     * Similar to {@link #launch(Uri)}, but allows more customization.
     *
     * @param twaBuilder {@link TrustedWebActivityIntentBuilder} containing the url to open, along with
     * optional parameters: status bar color, additional trusted origins, etc.
     * @param customTabsCallback {@link CustomTabsCallback} to get messages from the browser, use
     * for quality enforcement.
     * @param splashScreenStrategy {@link SplashScreenStrategy} to use for showing splash screens,
     * null if splash screen not needed.
     * @param completionCallback Callback triggered when the url has been opened.
     * @param fallbackStrategy Called when there is no TWA provider available or when launching
     * the Trusted Web Activity fails.
     */
    public void launch(TrustedWebActivityIntentBuilder twaBuilder,
                       CustomTabsCallback customTabsCallback,
                       @Nullable SplashScreenStrategy splashScreenStrategy,
                       @Nullable Runnable completionCallback,
                       FallbackStrategy fallbackStrategy) {
        if (mDestroyed) {
            throw new IllegalStateException("TwaLauncher already destroyed");
        }

        if (mLaunchMode == TwaProviderPicker.LaunchMode.TRUSTED_WEB_ACTIVITY) {
            launchTwa(twaBuilder, customTabsCallback, splashScreenStrategy, completionCallback,
                    fallbackStrategy);
        } else {
            fallbackStrategy.launch(mContext, twaBuilder, mProviderPackage, completionCallback);
        }

        // Remember who we connect to as the package that is allowed to delegate notifications
        // to us.
        if (!ChromeOsSupport.isRunningOnArc(mContext.getPackageManager())) {
            // Since ChromeOS may not follow this path when launching a TWA, we set the verified
            // provider in DelegationService instead.
            mTokenStore.store(Token.create(mProviderPackage, mContext.getPackageManager()));
        }
    }

    /**
     * Similar to {@link #launch(Uri)}, but allows more customization. Uses a Custom Tabs fallback
     * when a TWA provider is not available or when launching a TWA fails.
     *
     * @param twaBuilder {@link TrustedWebActivityIntentBuilder} containing the url to open, along with
     * optional parameters: status bar color, additional trusted origins, etc.
     * @param customTabsCallback {@link CustomTabsCallback} to get messages from the browser, use
     * for quality enforcement.
     * @param splashScreenStrategy {@link SplashScreenStrategy} to use for showing splash screens,
     * null if splash screen not needed.
     * @param completionCallback Callback triggered when the url has been opened.
     */
    public void launch(TrustedWebActivityIntentBuilder twaBuilder,
            CustomTabsCallback customTabsCallback,
            @Nullable SplashScreenStrategy splashScreenStrategy,
            @Nullable Runnable completionCallback) {
        launch(twaBuilder, customTabsCallback, splashScreenStrategy, completionCallback,
                CCT_FALLBACK_STRATEGY);
    }

    private void launchTwa(TrustedWebActivityIntentBuilder twaBuilder,
            CustomTabsCallback customTabsCallback,
            @Nullable SplashScreenStrategy splashScreenStrategy,
            @Nullable Runnable completionCallback,
            FallbackStrategy fallbackStrategy) {
        if (splashScreenStrategy != null) {
            splashScreenStrategy.onTwaLaunchInitiated(mProviderPackage, twaBuilder);
        }

        Runnable onSessionCreatedRunnable = () ->
                launchWhenSessionEstablished(twaBuilder, splashScreenStrategy, completionCallback);

        if (mSession != null) {
            onSessionCreatedRunnable.run();
            return;
        }

        Runnable onSessionCreationFailedRunnable = () -> {
            // The provider has been unable to create a session for us, we can't launch a
            // Trusted Web Activity. We launch a fallback specially designed to provide the
            // best user experience.
            fallbackStrategy.launch(mContext, twaBuilder, mProviderPackage, completionCallback);
        };

        if (mServiceConnection == null) {
            mServiceConnection = new TwaCustomTabsServiceConnection(customTabsCallback);
        }

        mServiceConnection.setSessionCreationRunnables(
                onSessionCreatedRunnable, onSessionCreationFailedRunnable);
        CustomTabsClient.bindCustomTabsServicePreservePriority(
                mContext, mProviderPackage, mServiceConnection);
    }

    private void launchWhenSessionEstablished(TrustedWebActivityIntentBuilder twaBuilder,
            @Nullable SplashScreenStrategy splashScreenStrategy,
            @Nullable Runnable completionCallback) {
        if (mSession == null) {
            throw new IllegalStateException("mSession is null in launchWhenSessionEstablished");
        }

        if (splashScreenStrategy != null) {
            splashScreenStrategy.configureTwaBuilder(twaBuilder, mSession,
                    () -> launchWhenSplashScreenReady(twaBuilder, completionCallback));
        } else {
            launchWhenSplashScreenReady(twaBuilder, completionCallback);
        }
    }

    private void launchWhenSplashScreenReady(TrustedWebActivityIntentBuilder builder,
            @Nullable Runnable completionCallback) {
        if (mDestroyed || mSession == null) {
            return;  // Service was disconnected and / or TwaLauncher was destroyed while preparing
                     // the splash screen (e.g. user closed the app). See https://crbug.com/1052367
                     // for further details.
        }
        Log.d(TAG, "Launching Trusted Web Activity.");
        TrustedWebActivityIntent intent = builder.build(mSession);
        if (mStartupUptimeMillis != 0) {
            intent.getIntent().putExtra(EXTRA_STARTUP_UPTIME_MILLIS, mStartupUptimeMillis);
        }
        intent.getIntent().putExtra(
                EXTRA_ANDROID_BROWSER_HELPER_VERSION, BuildConfig.LIBRARY_VERSION);
        FocusActivity.addToIntent(intent.getIntent(), mContext);
        intent.launchTrustedWebActivity(mContext);

        if (completionCallback != null) {
            completionCallback.run();
        }
    }

    /**
     * Performs clean-up.
     */
    public void destroy() {
        if (mDestroyed) {
            return;
        }
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
        }
        mContext = null;
        mDestroyed = true;
    }

    /**
     * Returns package name of the browser this TwaLauncher is launching.
     */
    @Nullable
    public String getProviderPackage() {
        return mProviderPackage;
    }

    /**
     * Sets the timestamp (in SystemClock.uptimeMillis()) when the TWA launcher
     * activity was created. This timestamp is used to report the full startup
     * duration to the browser.
     */
    public void setStartupUptimeMillis(long startupUptimeMillis) {
        mStartupUptimeMillis = startupUptimeMillis;
    }

    private class TwaCustomTabsServiceConnection extends CustomTabsServiceConnection {
        private Runnable mOnSessionCreatedRunnable;
        private Runnable mOnSessionCreationFailedRunnable;
        private CustomTabsCallback mCustomTabsCallback;

        TwaCustomTabsServiceConnection(CustomTabsCallback callback) {
            mCustomTabsCallback = callback;
        }

        private void setSessionCreationRunnables(@Nullable Runnable onSuccess,
                @Nullable Runnable onFailure) {
            mOnSessionCreatedRunnable = onSuccess;
            mOnSessionCreationFailedRunnable = onFailure;
        }

        @Override
        public void onCustomTabsServiceConnected(ComponentName componentName,
                CustomTabsClient client) {
            if (!ChromeLegacyUtils
                    .supportsLaunchWithoutWarmup(mContext.getPackageManager(), mProviderPackage)) {
                client.warmup(0);
            }

            try {
                mSession = client.newSession(mCustomTabsCallback, mSessionId);

                if (mSession != null && mOnSessionCreatedRunnable != null) {
                    mOnSessionCreatedRunnable.run();
                } else if (mSession == null && mOnSessionCreationFailedRunnable != null) {
                    mOnSessionCreationFailedRunnable.run();
                }
            } catch (RuntimeException e) {
                Log.w(TAG, e);
                mOnSessionCreationFailedRunnable.run();
            }

            mOnSessionCreatedRunnable = null;
            mOnSessionCreationFailedRunnable = null;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSession = null;
        }
    }
}
