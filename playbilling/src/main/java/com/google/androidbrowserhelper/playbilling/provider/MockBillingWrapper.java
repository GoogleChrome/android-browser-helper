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
import android.content.Intent;

import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A {@link BillingWrapper} that can be be controlled for tests.
 */
public class MockBillingWrapper implements BillingWrapper {
    private Listener mListener;

    private BillingClientStateListener mConnectionStateListener;

    private boolean mPaymentFlowSuccessful;

    private InvocationTracker<String, AcknowledgePurchaseResponseListener>
            mAcknowledgeInvocation = new InvocationTracker<>();
    private InvocationTracker<String, ConsumeResponseListener>
            mConsumeInvocation = new InvocationTracker<>();

    private MultiProductTypeInvocationTracker<List<String>, ProductDetailsResponseListener>
            mQueryProductDetailsInvocation = new MultiProductTypeInvocationTracker<>();
    private MultiProductTypeInvocationTracker<Void, PurchasesResponseListener>
            mQueryPurchasesInvocation = new MultiProductTypeInvocationTracker<>();
    private MultiProductTypeInvocationTracker<Void, PurchaseHistoryResponseListener>
            mQueryPurchaseHistoryInvocation = new MultiProductTypeInvocationTracker<>();

    private Intent mPlayBillingFlowLaunchIntent;

    private final CountDownLatch mConnectLatch = new CountDownLatch(1);
    private final CountDownLatch mLaunchPaymentFlowLatch = new CountDownLatch(1);

    @Override
    public void connect(BillingClientStateListener callback) {
        mConnectionStateListener = callback;
        mConnectLatch.countDown();
    }

    @Override
    public void queryProductDetails(@BillingClient.ProductType String productType, List<String> productIds,
            ProductDetailsResponseListener callback) {
        mQueryProductDetailsInvocation.call(productType, productIds, callback);
    }

    @Override
    public void queryPurchases(@BillingClient.ProductType String productType, PurchasesResponseListener callback) {
        mQueryPurchasesInvocation.call(productType, null, callback);
    }

    @Override
    public void queryPurchaseHistory(@BillingClient.ProductType String productType, PurchaseHistoryResponseListener callback) {
        mQueryPurchaseHistoryInvocation.call(productType, null, callback);
    }

    @Override
    public void acknowledge(String token, AcknowledgePurchaseResponseListener callback) {
        mAcknowledgeInvocation.call(token, callback);
    }

    @Override
    public void consume(String token, ConsumeResponseListener callback) {
        mConsumeInvocation.call(token, callback);
    }

    @Override
    public boolean launchPaymentFlow(Activity activity, ProductDetails productDetails, MethodData data) {
        mPlayBillingFlowLaunchIntent = activity.getIntent();
        mLaunchPaymentFlowLatch.countDown();
        return mPaymentFlowSuccessful;
    }

    public void triggerConnected() {
        mConnectionStateListener.onBillingSetupFinished(
                toResult(BillingClient.BillingResponseCode.OK));
    }

    public void triggerDisconnected() {
        mConnectionStateListener.onBillingServiceDisconnected();
    }

    public void triggerOnGotProductDetails(List<ProductDetails> productDetails) {
        triggerOnGotInAppProductDetails(productDetails);
        triggerOnGotSubsProductDetails(Collections.emptyList());
    }

    public void triggerOnGotInAppProductDetails(List<ProductDetails> productDetails) {
        triggerOnGotInAppProductDetails(BillingClient.BillingResponseCode.OK, productDetails);
    }

    public void triggerOnGotInAppProductDetails(int responseCode, List<ProductDetails> productDetails) {
        mQueryProductDetailsInvocation.getCallback(BillingClient.ProductType.INAPP)
                .onProductDetailsResponse(toResult(responseCode), productDetails);
    }

    public void triggerOnGotSubsProductDetails(List<ProductDetails> productDetails) {
        triggerOnGotSubsProductDetails(BillingClient.BillingResponseCode.OK, productDetails);
    }

    public void triggerOnGotSubsProductDetails(int responseCode, List<ProductDetails> productDetails) {
        mQueryProductDetailsInvocation.getCallback(BillingClient.ProductType.SUBS)
                .onProductDetailsResponse(toResult(responseCode), productDetails);
    }

    public void triggerOnGotInAppPurchaseDetails(List<Purchase> details) {
        mQueryPurchasesInvocation.getCallback(BillingClient.ProductType.INAPP)
                .onQueryPurchasesResponse(toResult(BillingClient.BillingResponseCode.OK), details);
    }

    public void triggerOnGotSubsPurchaseDetails(List<Purchase> details) {
        mQueryPurchasesInvocation.getCallback(BillingClient.ProductType.SUBS)
                .onQueryPurchasesResponse(toResult(BillingClient.BillingResponseCode.OK), details);
    }

    public void triggerOnPurchaseHistoryResponse(String productType,
                                                 List<PurchaseHistoryRecord> records) {
        mQueryPurchaseHistoryInvocation.getCallback(productType)
                .onPurchaseHistoryResponse(toResult(BillingClient.BillingResponseCode.OK), records);
    }

    public void triggerAcknowledge(int responseCode) {
        mAcknowledgeInvocation.getCallback().onAcknowledgePurchaseResponse(toResult(responseCode));
    }

    public void triggerConsume(int responseCode, String token) {
        mConsumeInvocation.getCallback().onConsumeResponse(toResult(responseCode), token);
    }

    public void triggerOnPurchasesUpdated() {
        mListener.onPurchaseFlowComplete(toResult(BillingClient.BillingResponseCode.OK), "");
    }

    public boolean waitForConnect() throws InterruptedException {
        return wait(mConnectLatch);
    }

    public boolean waitForQueryProductDetails() throws InterruptedException {
        return mQueryProductDetailsInvocation.waitUntilCalled();
    }

    public boolean waitForLaunchPaymentFlow() throws InterruptedException {
        return wait(mLaunchPaymentFlowLatch);
    }

    public boolean waitForQueryPurchases() throws InterruptedException {
        return mQueryPurchasesInvocation.waitUntilCalled();
    }

    public boolean waitForQueryPurchaseHistory() throws InterruptedException {
        return mQueryPurchaseHistoryInvocation.waitUntilCalled();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setPaymentFlowWillBeSuccessful(boolean successful) {
        mPaymentFlowSuccessful = successful;
    }

    public List<String> getQueriedProductDetails() {
        List<String> productDetails = new ArrayList<>();

        List<String> inapp = mQueryProductDetailsInvocation.getArgument(BillingClient.ProductType.INAPP);
        List<String> subs = mQueryProductDetailsInvocation.getArgument(BillingClient.ProductType.SUBS);

        if (inapp == null && subs == null) return null;

        if (inapp != null) productDetails.addAll(inapp);
        if (subs != null) productDetails.addAll(subs);

        return productDetails;
    }

    public String getConsumeToken() {
        return mConsumeInvocation.getArgument();
    }

    public String getAcknowledgeToken() {
        return mAcknowledgeInvocation.getArgument();
    }

    public Intent getPlayBillingFlowLaunchIntent() {
        return mPlayBillingFlowLaunchIntent;
    }

    private static boolean wait(CountDownLatch latch) throws InterruptedException {
        return latch.await(5, TimeUnit.SECONDS);
    }

    private static BillingResult toResult(int responseCode) {
         return BillingResult.newBuilder().setResponseCode(responseCode).build();
    }
}
