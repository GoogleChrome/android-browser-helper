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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.Button;

public class OfflineFirstTWALauncherActivity extends LauncherActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // `super.onCreate()` may have called `finish()`. In this case, we don't do any work.
        if (isFinishing()) {
            return;
        }
        tryLaunchTwa();
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
            firstTimeLaunchTwa();
            return;
        }

        renderOfflineFallback();
    }

    private boolean hasTwaLaunchedSuccessfully() {
        // Return `true` if the preference "twa_launched_successfully" has already been set.
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
        setContentView(R.layout.activity_offline_first_twa);

        Button retryBtn = this.findViewById(R.id.retry_btn);
        retryBtn.setOnClickListener(v -> {
            // Check connection status. If online, launch the Trusted Web Activity for the first
            // time.
            if (isOnline()) {
                firstTimeLaunchTwa();
            }
        });
    }

    private void firstTimeLaunchTwa() {
        // Launch the TWA and set the preference "twa_launched_successfully" to true, to indicate
        // that it has launched successfully, at least, once.
        launchTwa();

        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.twa_offline_first_preferences_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.twa_launched_successfully), true);
        editor.apply();
    }

    private boolean isOnline() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
