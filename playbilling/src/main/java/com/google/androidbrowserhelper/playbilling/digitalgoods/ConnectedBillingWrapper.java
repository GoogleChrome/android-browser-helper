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

import android.app.Activity;

import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.google.androidbrowserhelper.playbilling.provider.BillingWrapper;
import com.google.androidbrowserhelper.playbilling.provider.MethodData;

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

    private final List<Runnable> mPendingCallbacks = new ArrayList<>();

    public ConnectedBillingWrapper(BillingWrapper mInner) {
        this.mInner = mInner;
    }

    private void execute(Runnable callback) {
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
                Logging.logConnected();

                mState = CONNECTED;

                for (Runnable callback : mPendingCallbacks) {
                    callback.run();
                }

                mPendingCallbacks.clear();
            }

            @Override
            public void onBillingServiceDisconnected() {
                Logging.logDisconnected();

                mState = NOT_CONNECTED;
            }
        });
    }

    @Override
    public void connect(BillingClientStateListener callback) {
        execute(() -> {});
    }

    @Override
    public void querySkuDetails(@BillingClient.SkuType String skuType, List<String> skus,
            SkuDetailsResponseListener callback) {
        execute(() -> mInner.querySkuDetails(skuType, skus, callback));
    }

    @Override
    public void queryPurchases(String skuType, PurchasesResponseListener callback) {
        execute(() -> mInner.queryPurchases(skuType, callback));
    }

    @Override
    public void queryPurchaseHistory(String skuType, PurchaseHistoryResponseListener callback) {
        execute(() -> mInner.queryPurchaseHistory(skuType, callback));
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
    public boolean launchPaymentFlow(Activity activity, SkuDetails sku, MethodData data) {
        throw new IllegalStateException(
                "EnsuredConnectionBillingWrapper doesn't handle launch Payment flow");
    }
}
