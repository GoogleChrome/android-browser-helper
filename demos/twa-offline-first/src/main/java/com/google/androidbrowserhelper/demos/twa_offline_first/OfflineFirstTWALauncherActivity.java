// Copyright 2021 Google Inc. All Rights Reserved.
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

package com.google.androidbrowserhelper.demos.twa_offline_first;

import com.google.androidbrowserhelper.trusted.LauncherActivity;
import com.google.androidbrowserhelper.trusted.QualityEnforcer;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;

import java.lang.ref.WeakReference;

public class OfflineFirstTWALauncherActivity extends LauncherActivity {
    // The instance showing the offline fallback screen, if any.
    private static WeakReference<OfflineFirstTWALauncherActivity> sOfflineFallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // `super.onCreate()` may have called `finish()`. In this case, we don't do any work.
        if (isFinishing()) {
            return;
        }

        // A share or link that arrives while the offline fallback screen is shown starts a new
        // instance of this Activity. Finish the previous one, so the fallback screens don't stack.
        OfflineFirstTWALauncherActivity offlineFallback =
                sOfflineFallback == null ? null : sOfflineFallback.get();
        if (offlineFallback != null) {
            offlineFallback.finish();
        }

        tryLaunchTwa();
    }

    @Override
    protected void onDestroy() {
        if (sOfflineFallback != null && sOfflineFallback.get() == this) {
            sOfflineFallback = null;
        }
        super.onDestroy();
    }

    @Override
    protected boolean shouldLaunchImmediately() {
        // launchImmediately() returns `false` so we can check connection
        // and then render a fallback page or launch the Trusted Web Activity with `launchTwa()`.
        return false;
    }

    private void tryLaunchTwa() {
        // If TWA has already launched successfully, launch TWA immediately.
        // Otherwise, check connection status. If online, launch the Trusted Web Activity with
        // `launchTwa()`. Otherwise, if offline, render the offline fallback screen.
        if (hasTwaLaunchedSuccessfully()) {
            launchTwa();
            return;
        }

        if (isOnline()) {
            launchTwa();
            return;
        }

        renderOfflineFallback();
    }

    private boolean hasTwaLaunchedSuccessfully() {
        // Return `true` if the preference "twa_launched_successfully" has already been set, which
        // happens once the browser finished loading a page (see `getCustomTabsCallback()`).
        // The code to access shared preferences is surrounded by additional `ThreadPolicy` code to
        // avoid the app breaking the first time it runs (as it requires a disk read, which might be
        // slower the first time).
        StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
        try {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
            SharedPreferences sharedPref = getSharedPreferences(
                    getString(R.string.twa_offline_first_preferences_file_key),
                    Context.MODE_PRIVATE);
            return sharedPref.getBoolean(getString(R.string.twa_launched_successfully), false);
        } finally {
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }

    private void renderOfflineFallback() {
        sOfflineFallback = new WeakReference<>(this);
        setContentView(R.layout.activity_offline_first_twa);

        Button retryBtn = this.findViewById(R.id.retry_btn);
        retryBtn.setOnClickListener(v -> {
            // Check connection status. If online, launch the Trusted Web Activity for the first
            // time.
            if (isOnline()) {
                launchTwa();
            }
        });
    }

    @Override
    protected CustomTabsCallback getCustomTabsCallback() {
        if (hasTwaLaunchedSuccessfully()) {
            return super.getCustomTabsCallback();
        }
        return new LaunchRecorder(getApplicationContext().getSharedPreferences(
                getString(R.string.twa_offline_first_preferences_file_key), Context.MODE_PRIVATE),
                getString(R.string.twa_launched_successfully));
    }

    // Sets the preference "twa_launched_successfully" only once the browser reports a finished
    // page load. Setting it when the launch starts would skip the offline fallback screen on later
    // launches, although an interrupted first launch never loaded the page that registers the
    // service worker. A failed navigation still finishes loading the browser's error page, so a
    // failure since the last navigation start keeps the preference unset. The class is static so
    // the browser's binder does not keep the Activity alive, and it extends `QualityEnforcer` to
    // keep the default quality enforcement.
    private static class LaunchRecorder extends QualityEnforcer {
        // Matches the package-private `QualityEnforcer.CRASH`.
        private static final String CRASH = "quality_enforcement.crash";

        private final SharedPreferences mSharedPref;
        private final String mKey;
        private boolean mFailed;

        LaunchRecorder(SharedPreferences sharedPref, String key) {
            mSharedPref = sharedPref;
            mKey = key;
        }

        @Override
        public void onNavigationEvent(int navigationEvent, @Nullable Bundle extras) {
            super.onNavigationEvent(navigationEvent, extras);
            switch (navigationEvent) {
                case NAVIGATION_STARTED:
                    mFailed = false;
                    break;
                case NAVIGATION_FAILED:
                case NAVIGATION_ABORTED:
                    mFailed = true;
                    break;
                case NAVIGATION_FINISHED:
                    if (!mFailed) {
                        mSharedPref.edit().putBoolean(mKey, true).apply();
                    }
                    break;
            }
        }

        @Nullable
        @Override
        public Bundle extraCallbackWithResult(@NonNull String callbackName,
                @Nullable Bundle args) {
            if (CRASH.equals(callbackName)) {
                mFailed = true;
            }
            return super.extraCallbackWithResult(callbackName, args);
        }
    }

    // An always-on VPN reports internet access even when no network carries it, so the active
    // network must also have been validated, which Android checks through the VPN tunnel.
    private boolean isOnline() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(
                connectivityManager.getActiveNetwork());
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
