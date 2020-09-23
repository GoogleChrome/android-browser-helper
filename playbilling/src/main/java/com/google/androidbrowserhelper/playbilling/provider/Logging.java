package com.google.androidbrowserhelper.playbilling.provider;

import android.util.Log;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;

/**
 * Consolidates all the logging for this package in one place.
 */
public class Logging {
    private static final String TAG = "TwaBilling";

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
