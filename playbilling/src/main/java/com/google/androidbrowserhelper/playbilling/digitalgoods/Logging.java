package com.google.androidbrowserhelper.playbilling.digitalgoods;

import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;

import java.util.List;

/**
 * Consolidates all the logging for this package in one place.
 */
public class Logging {
    private static final String TAG = "TwaBilling";

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

    }

    static void logDisconnected() {

    }
}
