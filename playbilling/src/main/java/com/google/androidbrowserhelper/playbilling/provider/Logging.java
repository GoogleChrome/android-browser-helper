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

package com.google.androidbrowserhelper.playbilling.provider;

import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;

import java.util.List;

import androidx.annotation.Nullable;

/**
 * Consolidates all the logging for this package in one place.
 */
public class Logging {
    private static final String TAG = "TwaBilling.P";

    static void logLaunchPaymentFlow(BillingResult result) {
        logResult(result, "Payment flow launch:");
    }

    static void logPurchasesUpdate(BillingResult result, @Nullable List<Purchase> list) {
        logResult(result, "Purchases updated:");

        if (list == null) {
            Log.d(TAG, "No items updated.");
        } else {
            Log.d(TAG, list.size() + " item(s) updated.");
        }
    }

    private static void logResult(BillingResult result, String message) {
        int responseCode = result.getResponseCode();

        Log.d(TAG, message + " " + responseCode);
        String debugMessage = result.getDebugMessage();
        if (debugMessage != null && !debugMessage.isEmpty()) {
            Log.d(TAG, debugMessage);
        }
    }
}
