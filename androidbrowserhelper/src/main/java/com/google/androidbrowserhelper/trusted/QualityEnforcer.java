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
 * Browser should send a message when violation occurs. For example if a link from a page on the
 * verified origin 404s or if a page cannot be displayed while offline.
 *
 * The purpose of this is to bring TWAs in line with native applications - if a native application
 * tries to start an Activity that doesn't exist, it will crash. We should hold web apps to the
 * same standard.
 */
public class QualityEnforcer extends CustomTabsCallback {
    private static final  String TAG = "TwaQualityEnforcement";
    static final String CRASH = "quality_enforcement.crash";
    static final String KEY_CRASH_REASON = "crash_reason";
    static final String KEY_SUCCESS = "success";

    private final Delegate mDelegate;

    /**
     * A Delegate interface that provides implementations for handling quality enforcement messages.
     */
    interface Delegate {
        /* Handling the CRASH message from browser. */
        void crash(String message);
    }

    /* Constructor to be used in prod that throws a RuntimeException. */
    public QualityEnforcer() {
        mDelegate = (message) -> {
            // Put the exception in a looper so that the crash will not prevent returning the
            // execution result to browser.
            new Handler(Looper.getMainLooper()).post(() -> {
                throw new RuntimeException(message);
            });
        };
    }

    /* Constructor for use in tests. */
    QualityEnforcer(Delegate delegate) { mDelegate = delegate; }

    @Nullable
    @Override
    public Bundle extraCallbackWithResult(
            @NonNull String callbackName, @Nullable Bundle args) {
        String message = (args != null) ? args.getString(KEY_CRASH_REASON) : null;
        if (message == null) return Bundle.EMPTY;

        Bundle result = new Bundle();
        if (callbackName.equals(CRASH)) {
            result.putBoolean(KEY_SUCCESS, true);
            mDelegate.crash(message);
        }
        return result;
    }
};
