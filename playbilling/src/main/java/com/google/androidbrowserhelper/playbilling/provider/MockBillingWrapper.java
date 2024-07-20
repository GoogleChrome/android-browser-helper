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
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

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

    private MultiSkuTypeInvocationTracker<List<String>, SkuDetailsResponseListener>
            mQuerySkuDetailsInvocation = new MultiSkuTypeInvocationTracker<>();
    private MultiSkuTypeInvocationTracker<Void, PurchasesResponseListener>
            mQueryPurchasesInvocation = new MultiSkuTypeInvocationTracker<>();
    private MultiSkuTypeInvocationTracker<Void, PurchaseHistoryResponseListener>
            mQueryPurchaseHistoryInvocation = new MultiSkuTypeInvocationTracker<>();

    private Intent mPlayBillingFlowLaunchIntent;

    private final CountDownLatch mConnectLatch = new CountDownLatch(1);
    private final CountDownLatch mLaunchPaymentFlowLatch = new CountDownLatch(1);

    @Override
    public void connect(BillingClientStateListener callback) {
        mConnectionStateListener = callback;
        mConnectLatch.countDown();
    }

    @Override
    public void querySkuDetails(@BillingClient.SkuType String skuType, List<String> skus,
            SkuDetailsResponseListener callback) {
        mQuerySkuDetailsInvocation.call(skuType, skus, callback);
    }

    @Override
    public void queryPurchases(String skuType, PurchasesResponseListener callback) {
        mQueryPurchasesInvocation.call(skuType, null, callback);
    }

    @Override
    public void queryPurchaseHistory(String skuType, PurchaseHistoryResponseListener callback) {
        mQueryPurchaseHistoryInvocation.call(skuType, null, callback);
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
    public boolean launchPaymentFlow(Activity activity, SkuDetails sku, MethodData data) {
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

    public void triggerOnGotSkuDetails(List<SkuDetails> skuDetails) {
        triggerOnGotInAppSkuDetails(skuDetails);
        triggerOnGotSubsSkuDetails(Collections.emptyList());
    }

    public void triggerOnGotInAppSkuDetails(List<SkuDetails> skuDetails) {
        triggerOnGotInAppSkuDetails(BillingClient.BillingResponseCode.OK, skuDetails);
    }

    public void triggerOnGotInAppSkuDetails(int responseCode, List<SkuDetails> skuDetails) {
        mQuerySkuDetailsInvocation.getCallback(BillingClient.SkuType.INAPP)
                .onSkuDetailsResponse(toResult(responseCode), skuDetails);
    }

    public void triggerOnGotSubsSkuDetails(List<SkuDetails> skuDetails) {
        triggerOnGotSubsSkuDetails(BillingClient.BillingResponseCode.OK, skuDetails);
    }

    public void triggerOnGotSubsSkuDetails(int responseCode, List<SkuDetails> skuDetails) {
        mQuerySkuDetailsInvocation.getCallback(BillingClient.SkuType.SUBS)
                .onSkuDetailsResponse(toResult(responseCode), skuDetails);
    }

    public void triggerOnGotInAppPurchaseDetails(List<Purchase> details) {
        mQueryPurchasesInvocation.getCallback(BillingClient.SkuType.INAPP)
                .onQueryPurchasesResponse(toResult(BillingClient.BillingResponseCode.OK), details);
    }

    public void triggerOnGotSubsPurchaseDetails(List<Purchase> details) {
        mQueryPurchasesInvocation.getCallback(BillingClient.SkuType.SUBS)
                .onQueryPurchasesResponse(toResult(BillingClient.BillingResponseCode.OK), details);
    }

    public void triggerOnPurchaseHistoryResponse(String skuType,
                                                 List<PurchaseHistoryRecord> records) {
        mQueryPurchaseHistoryInvocation.getCallback(skuType)
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

    public boolean waitForQuerySkuDetails() throws InterruptedException {
        return mQuerySkuDetailsInvocation.waitUntilCalled();
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

    public List<String> getQueriedSkuDetails() {
        List<String> skuDetails = new ArrayList<>();

        List<String> inapp = mQuerySkuDetailsInvocation.getArgument(BillingClient.SkuType.INAPP);
        List<String> subs = mQuerySkuDetailsInvocation.getArgument(BillingClient.SkuType.SUBS);

        if (inapp == null && subs == null) return null;

        if (inapp != null) skuDetails.addAll(inapp);
        if (subs != null) skuDetails.addAll(subs);

        return skuDetails;
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
