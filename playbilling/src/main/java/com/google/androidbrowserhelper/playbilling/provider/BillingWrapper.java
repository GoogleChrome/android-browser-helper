package com.google.androidbrowserhelper.playbilling.provider;

import android.app.Activity;

import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.List;

/**
 * Wraps communication with Play Billing to provide a simpler interface and allowing mocking in
 * tests.
 *
 * TODO(peconn): Move BillingWrapper and related classes into a different package.
 */
public interface BillingWrapper {
    /**
     * Callbacks for connection state and for purchase flow completion.
     */
    interface Listener {
        /** Will be called when connected to the Play Billing client. */
        void onConnected();

        /** Will be called when the Play Billing client disconnects. */
        void onDisconnected();

        /** Will be called after a call to {@link #launchPaymentFlow} that returns {@code true}. */
        void onPurchaseFlowComplete(int result);
    }

    /** Connect to the Play Billing client. */
    void connect();

    /**
     * Get {@link SkuDetails} objects for the provided SKUs.
     */
    void querySkuDetails(List<String> skus, SkuDetailsResponseListener callback);

    /**
     * Launches the Payment Flow. If it returns {@code true},
     * {@link Listener#onPurchaseFlowComplete} should be called.
     */
    boolean launchPaymentFlow(Activity activity, SkuDetails sku);
}
