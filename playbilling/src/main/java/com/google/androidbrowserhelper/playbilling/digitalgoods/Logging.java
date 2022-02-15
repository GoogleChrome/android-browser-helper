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

package com.google.androidbrowserhelper.playbilling.digitalgoods;

import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;

import java.util.List;

/**
 * Consolidates all the logging for this package in one place.
 */
public class Logging {
    private static final String TAG = "TwaBilling.DG";

    static void logUnknownCommand(String commandName) {
        Log.d(TAG, "Got unknown command: " + commandName);
    }

    static void logAckCall(String token, boolean makeAvailableAgain) {
        if (makeAvailableAgain) {
            Log.d(TAG, "Calling acknowledge " + token);
        } else {
            Log.d(TAG, "Calling consume " + token);
        }
    }

    static void logAckResponse(BillingResult result, boolean makeAvailableAgain) {
        String command = makeAvailableAgain ? "Acknowledge" : "Consume";
        logResult(result, command + " returned:");
    }

    static void logConsumeCall(String token) {
        Log.d(TAG, "Calling consume (v2.1) " + token);
    }

    static void logConsumeResponse(BillingResult result) {
        logResult(result, "Consume (v2.1) returned:");
    }

    static void logGetDetailsCall(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String id : ids) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(id);
            first = false;
        }

        Log.d(TAG, "Calling getDetails for " + sb.toString());
    }

    static void logGetDetailsResponse(BillingResult result) {
        logResult(result, "GetDetails returned:");
    }

    static void logListPurchasesCall() {
        Log.d(TAG, "Calling listPurchases");
    }

    static void logListPurchasesResult(BillingResult result) {
        logResult(result, "ListPurchases returned:");
    }

    static void logListPurchaseHistoryCall() {
        Log.d(TAG, "Calling listPurchaseHistory");
    }

    static void logListPurchaseHistoryResult(BillingResult result) {
        logResult(result, "ListPurchaseHistory returned:");
    }

    private static void logResult(BillingResult result, String message) {
        int responseCode = result.getResponseCode();

        Log.d(TAG, message + " " + responseCode);
        String debugMessage = result.getDebugMessage();
        if (debugMessage != null && !debugMessage.isEmpty()) {
            Log.d(TAG, debugMessage);
        }
    }

    static void logConnected() {
        Log.d(TAG, "Connected to Play Billing library.");
    }

    static void logDisconnected() {
        Log.d(TAG, "Disconnected from Play Billing library.");
    }

    static void logUnknownResultCode(int resultCode) {
        Log.w(TAG, "Cannot convert result code: " + resultCode);
    }
}
