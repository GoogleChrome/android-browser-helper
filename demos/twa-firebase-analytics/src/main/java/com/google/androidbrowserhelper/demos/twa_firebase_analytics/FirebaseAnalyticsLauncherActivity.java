// Copyright 2020 Google Inc. All Rights Reserved.
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

package com.google.androidbrowserhelper.demos.twa_firebase_analytics;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.androidbrowserhelper.trusted.LauncherActivity;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * This is a custom LauncherActivity that gets the Firebase Instance ID asynchronously before
 * launching the Trusted Web Activity and ensures the information is always available.
 */
public class FirebaseAnalyticsLauncherActivity extends LauncherActivity {
    private String mAppInstanceId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // `super.onCreate()` may have called `finish()`. In this case, we don't do any work.
        if (isFinishing()) {
            return;
        }

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Start the asynchronous task to get the Firebase application instance id.
        firebaseAnalytics.getAppInstanceId().addOnCompleteListener(task -> {
                // Once the task is complete, save the instance id so it can be used by
                // getLaunchingUrl().
                mAppInstanceId = task.getResult();
                launchTwa();
        });
    }

    @Override
    protected boolean shouldLaunchImmediately() {
        // launchImmediately() returns `false` so we can wait until Firebase Analytics is ready
        // and then launch the Trusted Web Activity with `launch()`.
        return false;
    }

    @Override
    protected Uri getLaunchingUrl() {
        Uri uri = super.getLaunchingUrl();
        // Attach the Firebase instance Id to the launchUrl. This example uses "appInstanceId" as
        // the parameter name.
        return uri.buildUpon()
                .appendQueryParameter("appInstanceId", mAppInstanceId)
                .build();
    }
}
