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

import android.app.Activity;
import android.content.Context;

import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.PriceChangeConfirmationListener;
import com.android.billingclient.api.PriceChangeFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.List;

/**
 * A {@link BillingWrapper} that communicates with the Play Billing libraries.
 */
public class PlayBillingWrapper implements BillingWrapper {
    private final Listener mListener;
    private final BillingClient mClient;

    private final PurchasesUpdatedListener mPurchaseUpdateListener =
            new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {
            Logging.logPurchasesUpdate(billingResult, list);

            if (list == null || list.size() == 0) {
                mListener.onPurchaseFlowComplete(billingResult, "");
            } else {
                mListener.onPurchaseFlowComplete(billingResult, list.get(0).getPurchaseToken());
            }
        }
    };

    public PlayBillingWrapper(Context context, Listener listener) {
        mListener = listener;
        mClient = BillingClient
                .newBuilder(context)
                .setListener(mPurchaseUpdateListener)
                .enablePendingPurchases()
                .build();
    }

    @Override
    public void connect(BillingClientStateListener callback) {
        mClient.startConnection(callback);
    }

    @Override
    public void querySkuDetails(@BillingClient.SkuType String skuType, List<String> skus,
            SkuDetailsResponseListener callback) {
        SkuDetailsParams params = SkuDetailsParams
                .newBuilder()
                .setSkusList(skus)
                .setType(skuType)
                .build();

        mClient.querySkuDetailsAsync(params, callback);
    }

    @Override
    public void queryPurchases(@BillingClient.SkuType  String skuType,
                               PurchasesResponseListener callback) {
        mClient.queryPurchasesAsync(skuType, callback);
    }

    @Override
    public void queryPurchaseHistory(String skuType, PurchaseHistoryResponseListener callback) {
        mClient.queryPurchaseHistoryAsync(skuType, callback);
    }

    @Override
    public void acknowledge(String token, AcknowledgePurchaseResponseListener callback) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams
                .newBuilder()
                .setPurchaseToken(token)
                .build();

        mClient.acknowledgePurchase(params, callback);
    }

    @Override
    public void consume(String token, ConsumeResponseListener callback) {
        ConsumeParams params = ConsumeParams
                .newBuilder()
                .setPurchaseToken(token)
                .build();

        mClient.consumeAsync(params, callback);
    }

    @Override
    public boolean launchPaymentFlow(Activity activity, SkuDetails sku, MethodData methodData) {
        BillingFlowParams.SubscriptionUpdateParams.Builder subUpdateParamsBuilder =
            BillingFlowParams.SubscriptionUpdateParams.newBuilder();
        BillingFlowParams.Builder builder = BillingFlowParams.newBuilder();
        builder.setSkuDetails(sku);

        if (methodData.prorationMode != null) {
            subUpdateParamsBuilder.setReplaceSkusProrationMode(methodData.prorationMode);
        }

        if (methodData.purchaseToken != null) {
            subUpdateParamsBuilder.setOldSkuPurchaseToken(methodData.purchaseToken);
            builder.setSubscriptionUpdateParams(subUpdateParamsBuilder.build());
        }

        BillingResult result = mClient.launchBillingFlow(activity, builder.build());

        Logging.logLaunchPaymentFlow(result);

        return result.getResponseCode() == BillingClient.BillingResponseCode.OK;
    }

    @Override
    public void launchPriceChangeConfirmationFlow(Activity activity, SkuDetails sku,
            PriceChangeConfirmationListener listener) {
        PriceChangeFlowParams params = PriceChangeFlowParams
                .newBuilder()
                .setSkuDetails(sku)
                .build();
        mClient.launchPriceChangeConfirmationFlow(activity, params, listener);
    }
}
