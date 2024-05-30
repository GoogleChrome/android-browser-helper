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
import com.android.billingclient.api.BillingClient.ProductType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.PriceChangeConfirmationListener;
import com.android.billingclient.api.PriceChangeFlowParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsParams.Product;
import com.android.billingclient.api.QueryPurchaseHistoryParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    public void queryProductDetails(@BillingClient.ProductType String productType, List<String> productsIds,
        ProductDetailsResponseListener callback) {
        QueryProductDetailsParams params = QueryProductDetailsParams
            .newBuilder()
            .setProductList(buildProductList(productType, productsIds))
            .build();

        mClient.queryProductDetailsAsync(params, callback);
    }

    @Override
    public void queryPurchases(@BillingClient.ProductType String productType,
        PurchasesResponseListener callback) {
        mClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(productType).build(), callback);
    }

    @Override
    public void queryPurchaseHistory(@BillingClient.ProductType String productType, PurchaseHistoryResponseListener callback) {
        mClient.queryPurchaseHistoryAsync(
            QueryPurchaseHistoryParams.newBuilder().setProductType(productType).build(), callback);
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
    public boolean launchPaymentFlow(Activity activity, ProductDetails productDetails, MethodData methodData) {
        BillingFlowParams.SubscriptionUpdateParams.Builder subUpdateParamsBuilder =
            BillingFlowParams.SubscriptionUpdateParams.newBuilder();
        BillingFlowParams.Builder builder = BillingFlowParams.newBuilder();
        List<ProductDetailsParams> productDetailsParamsList = Collections.singletonList(
            ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        );
        builder.setProductDetailsParamsList(productDetailsParamsList);

        if (methodData.purchaseToken != null) {
            subUpdateParamsBuilder.setOldPurchaseToken(methodData.purchaseToken);
        }

        if (methodData.prorationMode != null) {
            subUpdateParamsBuilder.setReplaceProrationMode(methodData.prorationMode);
        }

        if (methodData.purchaseToken != null || methodData.prorationMode != null) {
            builder.setSubscriptionUpdateParams(subUpdateParamsBuilder.build());
        }

        BillingResult result = mClient.launchBillingFlow(activity, builder.build());

        Logging.logLaunchPaymentFlow(result);

        return result.getResponseCode() == BillingClient.BillingResponseCode.OK;
    }

    @Override
    public void launchPriceChangeConfirmationFlow(Activity activity, ProductDetails productDetails,
        ProductDetailsResponseListener listener) {

        PriceChangeFlowParams params = PriceChangeFlowParams
            .newBuilder()
            .setSkuDetails(productDetails)
            .build();
        mClient.launchPriceChangeConfirmationFlow(activity, params, listener);
    }

    private List<Product> buildProductList(@ProductType String productType, List<String> ids){
        List<Product> products = new ArrayList<>();
        for (String id : ids) {
            products.add(Product.newBuilder()
                .setProductId(id)
                .setProductType(productType)
                .build());
        }
        return products;
    }
}
