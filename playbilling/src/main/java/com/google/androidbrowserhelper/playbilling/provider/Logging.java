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

/**
 * Consolidates all the logging for this package in one place.
 */
public class Logging {
    private static final String TAG = "TwaBilling.P";

    static void logLaunchPaymentFlow(BillingResult result) {
        int responseCode = result.getResponseCode();

        if (responseCode != BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Payment flow failed to launch " + responseCode);
            Log.d(TAG, result.getDebugMessage());
        } else {
            Log.d(TAG, "Payment Flow launched " + responseCode);
        }
    }

    static void logPaymentFlowComplete(BillingResult result) {
        int responseCode = result.getResponseCode();

        if (responseCode != BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "Payment flow failed " + responseCode);
            Log.d(TAG, result.getDebugMessage());
        } else {
            Log.d(TAG, "Payment Flow succeeded " + responseCode);
        }
    }
}
