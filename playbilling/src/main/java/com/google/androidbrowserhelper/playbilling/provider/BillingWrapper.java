package com.google.androidbrowserhelper.playbilling.provider;

import com.android.billingclient.api.SkuDetails;

import java.util.List;

import androidx.annotation.Nullable;

/**
 * Wraps communication with Play Billing to provide a simpler interface and allowing mocking in
 * tests.
 */
interface BillingWrapper {
    /**
     * Callbacks for various async calls.
     */
    interface Listener {
        /** Will be called when connected to the Play Billing client. */
        void onConnected();

        /** Will be called when the Play Billing client disconnects. */
        void onDisconnected();

        /** Will be called after a successful call to {@link #querySkuDetails}. */
        void onGotSkuDetails();

        /** Will be called after a call to {@link #launchPaymentFlow} that returns {@code true}. */
        void onPurchasesUpdated();
    }

    /** Connect to the Play Billing client. */
    void connect();

    /**
     * Get {@link SkuDetails} objects for the provided SKUs.
     * Can be accessed through {@link #getSkuDetailsList}.
     */
    void querySkuDetails(List<String> skus);

    /**
     * Returns the list of {@SkuDetails} fetched by a call to {@link #querySkuDetails}, may be null.
     */
    @Nullable
    List<SkuDetails> getSkuDetailsList();

    /**
     * Launches the Payment Flow. If it returns {@code true}, {@link Listener#onPurchasesUpdated()}
     * should be called.
     */
    boolean launchPaymentFlow(SkuDetails sku);
}
