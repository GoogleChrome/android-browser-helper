package com.google.androidbrowserhelper.playbilling.digitalgoods;

import android.app.Activity;

import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.androidbrowserhelper.playbilling.provider.BillingWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper around {@link BillingWrapper} that ensures it is connected before calling
 * {@link #querySkuDetails}, {@link #acknowledge} or {@link #consume}.
 */
public class ConnectedBillingWrapper implements BillingWrapper {
    private final BillingWrapper mInner;

    private static final int NOT_CONNECTED = 0;
    private static final int CONNECTING = 1;
    private static final int CONNECTED = 2;
    private int mState = NOT_CONNECTED;

    private interface Callback {
        void run();
    }

    private final List<Callback> mPendingCallbacks = new ArrayList<>();

    public ConnectedBillingWrapper(BillingWrapper mInner) {
        this.mInner = mInner;
    }

    private void execute(Callback callback) {
        if (mState == CONNECTED) {
            callback.run();
            return;
        }

        mPendingCallbacks.add(callback);

        if (mState == CONNECTING) return;

        mState = CONNECTING;
        mInner.connect(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                mState = CONNECTED;

                for (Callback callback : mPendingCallbacks) {
                    callback.run();
                }

                mPendingCallbacks.clear();
            }

            @Override
            public void onBillingServiceDisconnected() {
                mState = NOT_CONNECTED;
            }
        });
    }

    @Override
    public void connect(BillingClientStateListener callback) {
        execute(() -> {});
    }

    @Override
    public void querySkuDetails(List<String> skus, SkuDetailsResponseListener callback) {
        execute(() -> mInner.querySkuDetails(skus, callback));
    }

    @Override
    public void acknowledge(String token, AcknowledgePurchaseResponseListener callback) {
        execute(() -> mInner.acknowledge(token, callback));
    }

    @Override
    public void consume(String token, ConsumeResponseListener callback) {
        execute(() -> mInner.consume(token, callback));
    }

    @Override
    public boolean launchPaymentFlow(Activity activity, SkuDetails sku) {
        throw new IllegalStateException(
                "EnsuredConnectionBillingWrapper doesn't handle launch Payment flow");
    }
}
