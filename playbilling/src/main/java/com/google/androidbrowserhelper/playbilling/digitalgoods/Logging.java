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

    static void logCommand(String commandName) {
        Log.d(TAG, "Got command: " + commandName);
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
        int responseCode = result.getResponseCode();
        Log.d(TAG, command + " returned code " + responseCode);
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, result.getDebugMessage());
        }
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
        int responseCode = result.getResponseCode();
        Log.d(TAG, "GetDetails returned code " + responseCode);
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, result.getDebugMessage());
        }
    }

    static void logConnected() {
        Log.d(TAG, "Connected to Play Billing library.");
    }

    static void logDisconnected() {
        Log.d(TAG, "Disconnected from Play Billing library.");
    }
}
