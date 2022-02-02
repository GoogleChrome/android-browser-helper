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
import com.android.billingclient.api.PriceChangeConfirmationListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

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

    private List<String> mQueriedSkuDetails;
    private boolean mPaymentFlowSuccessful;
    private SkuDetailsResponseListener mPendingQueryInAppSkuDetailsCallback;
    private SkuDetailsResponseListener mPendingQuerySubsSkuDetailsCallback;
    private PurchasesResponseListener mPendingQueryInAppPurchaseDetailsCallback;
    private PurchasesResponseListener mPendingQuerySubsPurchaseDetailsCallback;
    private PurchaseHistoryResponseListener mPendingInAppListPurchaseHistoryCallback;
    private PurchaseHistoryResponseListener mPendingSubsListPurchaseHistoryCallback;
    private PriceChangeConfirmationListener mPendingPriceChangeConfirmationFlowCallback;

    private String mAcknowledgeToken;
    private AcknowledgePurchaseResponseListener mPendingAcknowledgeCallback;

    private String mConsumeToken;
    private ConsumeResponseListener mPendingConsumeCallback;

    private Intent mPlayBillingFlowLaunchIntent;

    private final CountDownLatch mConnectLatch = new CountDownLatch(1);
    private final CountDownLatch mLaunchPaymentFlowLatch = new CountDownLatch(1);
    private final CountDownLatch mLaunchPriceChangeConfirmationFlowLatch = new CountDownLatch(1);

    // These CountDownLatches are initialized to 2 because they should be called for both in app
    // and subscription SKU types.
    private final CountDownLatch mQuerySkuDetailsLatch = new CountDownLatch(2);
    private final CountDownLatch mQueryPurchasesLatch = new CountDownLatch(2);
    private final CountDownLatch mQueryPurchaseHistoryLatch = new CountDownLatch(2);

    @Override
    public void connect(BillingClientStateListener callback) {
        mConnectionStateListener = callback;
        mConnectLatch.countDown();
    }

    @Override
    public void querySkuDetails(@BillingClient.SkuType String skuType, List<String> skus,
            SkuDetailsResponseListener callback) {
        mQueriedSkuDetails = skus;
        mQuerySkuDetailsLatch.countDown();
        if (BillingClient.SkuType.INAPP.equals(skuType)) {
            mPendingQueryInAppSkuDetailsCallback = callback;
        } else {
            mPendingQuerySubsSkuDetailsCallback = callback;
        }
    }

    @Override
    public void queryPurchases(String skuType, PurchasesResponseListener callback) {
        mQueryPurchasesLatch.countDown();
        if (BillingClient.SkuType.INAPP.equals(skuType)) {
            mPendingQueryInAppPurchaseDetailsCallback = callback;
        } else {
            mPendingQuerySubsPurchaseDetailsCallback = callback;
        }
    }

    @Override
    public void queryPurchaseHistory(String skuType, PurchaseHistoryResponseListener callback) {
        mQueryPurchaseHistoryLatch.countDown();
        if (BillingClient.SkuType.INAPP.equals(skuType)) {
            mPendingInAppListPurchaseHistoryCallback = callback;
        } else {
            mPendingSubsListPurchaseHistoryCallback = callback;
        }
    }

    @Override
    public void acknowledge(String token, AcknowledgePurchaseResponseListener callback) {
        mAcknowledgeToken = token;
        mPendingAcknowledgeCallback = callback;
    }

    @Override
    public void consume(String token, ConsumeResponseListener callback) {
        mConsumeToken = token;
        mPendingConsumeCallback = callback;
    }

    @Override
    public boolean launchPaymentFlow(Activity activity, SkuDetails sku, MethodData data) {
        mPlayBillingFlowLaunchIntent = activity.getIntent();
        mLaunchPaymentFlowLatch.countDown();
        return mPaymentFlowSuccessful;
    }

    @Override
    public void launchPriceChangeConfirmationFlow(Activity activity, SkuDetails sku,
            PriceChangeConfirmationListener listener) {
        mLaunchPriceChangeConfirmationFlowLatch.countDown();
        mPendingPriceChangeConfirmationFlowCallback = listener;
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
        mPendingQueryInAppSkuDetailsCallback.onSkuDetailsResponse(toResult(responseCode), skuDetails);
    }

    public void triggerOnGotSubsSkuDetails(List<SkuDetails> skuDetails) {
        triggerOnGotSubsSkuDetails(BillingClient.BillingResponseCode.OK, skuDetails);
    }

    public void triggerOnGotSubsSkuDetails(int responseCode, List<SkuDetails> skuDetails) {
        mPendingQuerySubsSkuDetailsCallback.onSkuDetailsResponse(toResult(responseCode), skuDetails);
    }

    public void triggerOnGotInAppPurchaseDetails(List<Purchase> details) {
        mPendingQueryInAppPurchaseDetailsCallback.onQueryPurchasesResponse(
                toResult(BillingClient.BillingResponseCode.OK), details);
    }

    public void triggerOnGotSubsPurchaseDetails(List<Purchase> details) {
        mPendingQuerySubsPurchaseDetailsCallback.onQueryPurchasesResponse(
                toResult(BillingClient.BillingResponseCode.OK), details);
    }

    public void triggerOnPurchaseHistoryResponse(String skuType,
                                                 List<PurchaseHistoryRecord> records) {
        if (BillingClient.SkuType.INAPP.equals(skuType)) {
            mPendingInAppListPurchaseHistoryCallback.onPurchaseHistoryResponse(
                    toResult(BillingClient.BillingResponseCode.OK), records);
        } else {
            mPendingSubsListPurchaseHistoryCallback.onPurchaseHistoryResponse(
                    toResult(BillingClient.BillingResponseCode.OK), records);
        }
    }

    public void triggerAcknowledge(int responseCode) {
        mPendingAcknowledgeCallback.onAcknowledgePurchaseResponse(toResult(responseCode));
    }

    public void triggerConsume(int responseCode, String token) {
        mPendingConsumeCallback.onConsumeResponse(toResult(responseCode), token);
    }

    public void triggerOnPurchasesUpdated() {
        mListener.onPurchaseFlowComplete(toResult(BillingClient.BillingResponseCode.OK), "");
    }

    public void triggerOnPriceChangeConfirmationResult() {
        mPendingPriceChangeConfirmationFlowCallback.onPriceChangeConfirmationResult(
                toResult(BillingClient.BillingResponseCode.OK));
    }

    public boolean waitForConnect() throws InterruptedException {
        return wait(mConnectLatch);
    }

    public boolean waitForQuerySkuDetails() throws InterruptedException {
        return wait(mQuerySkuDetailsLatch);
    }

    public boolean waitForLaunchPaymentFlow() throws InterruptedException {
        return wait(mLaunchPaymentFlowLatch);
    }

    public boolean waitForLaunchPriceChangeConfirmationFlow() throws InterruptedException {
        return wait(mLaunchPriceChangeConfirmationFlowLatch);
    }

    public boolean waitForQueryPurchases() throws InterruptedException {
        return wait(mQueryPurchasesLatch);
    }

    public boolean waitForQueryPurchaseHistory() throws InterruptedException {
        return wait(mQueryPurchaseHistoryLatch);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setPaymentFlowWillBeSuccessful(boolean successful) {
        mPaymentFlowSuccessful = successful;
    }

    public List<String> getQueriedSkuDetails() {
        return mQueriedSkuDetails;
    }

    public String getConsumeToken() {
        return mConsumeToken;
    }

    public String getAcknowledgeToken() {
        return mAcknowledgeToken;
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
