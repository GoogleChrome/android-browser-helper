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

package com.google.androidbrowserhelper.trusted;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;

/**
 * This class handles the quality enforcement messages from the browser to enforce the quality bar
 * on the websites shown inside Trusted Web Activities.
 * Browser should send a message when violation occurs. for example if a link from a page on the
 * verified origin 404s or if a page cannot be displayed while offline.
 *
 * The purpose of this is to bring TWAs in line with native applications - if a native application
 * tries to start an Activity that doesn't exist, it will crash. We should hold web apps to the
 * same standard.
 */
public class QualityEnforcer extends CustomTabsCallback {
    private static final  String TAG = "TwaQualityEnforcement";
    static final String CRASH = "quality_enforcement.crash";
    static final String NOTIFY = "quality_enforcement.notify";
    static final String KEY_CRASH_REASON = "crash_reason";
    static final String KEY_SUCCESS = "success";

    private final Delegate mDelegate;

    interface Delegate {
        void crash(String message);
    }

    public QualityEnforcer() {
        mDelegate = (message) -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                throw new RuntimeException(message);
            });
        };
    }

    QualityEnforcer(Delegate delegate) { mDelegate = delegate; }

    @Nullable
    @Override
    public Bundle extraCallbackWithResult(
            @NonNull String callbackName, @Nullable Bundle args) {
        Bundle result = new Bundle();
        result.putBoolean(KEY_SUCCESS, false);
        if (callbackName.equals(NOTIFY)) {
            String message = (args != null) ? args.getString(KEY_CRASH_REASON) : "";
            Log.e(TAG, message);
            result.putBoolean(KEY_SUCCESS, true);
        } else if (callbackName.equals(CRASH)) {
            String message = (args != null) ? args.getString(KEY_CRASH_REASON) : "";
            result.putBoolean(KEY_SUCCESS, true);
            mDelegate.crash(message);
        }
        return result;
    }
};
