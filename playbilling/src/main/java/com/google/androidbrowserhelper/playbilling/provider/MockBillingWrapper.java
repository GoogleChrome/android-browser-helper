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

import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

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
    private SkuDetailsResponseListener mPendingQuerySkuDetailsCallback;

    private String mAcknowledgeToken;
    private AcknowledgePurchaseResponseListener mPendingAcknowledgeCallback;

    private String mConsumeToken;
    private ConsumeResponseListener mPendingConsumeCallback;

    private final CountDownLatch mConnectLatch = new CountDownLatch(1);
    private final CountDownLatch mQuerySkuDetailsLatch = new CountDownLatch(1);
    private final CountDownLatch mLaunchPaymentFlowLatch = new CountDownLatch(1);

    @Override
    public void connect(BillingClientStateListener callback) {
        mConnectionStateListener = callback;
        mConnectLatch.countDown();
    }

    @Override
    public void querySkuDetails(List<String> skus, SkuDetailsResponseListener callback) {
        mQueriedSkuDetails = skus;
        mQuerySkuDetailsLatch.countDown();
        mPendingQuerySkuDetailsCallback = callback;
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
    public boolean launchPaymentFlow(Activity activity, SkuDetails sku) {
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
        triggerOnGotSkuDetails(BillingClient.BillingResponseCode.OK, skuDetails);
    }

    public void triggerOnGotSkuDetails(int responseCode, List<SkuDetails> skuDetails) {
        mPendingQuerySkuDetailsCallback.onSkuDetailsResponse(toResult(responseCode), skuDetails);
    }

    public void triggerAcknowledge(int responseCode) {
        mPendingAcknowledgeCallback.onAcknowledgePurchaseResponse(toResult(responseCode));
    }

    public void triggerConsume(int responseCode, String token) {
        mPendingConsumeCallback.onConsumeResponse(toResult(responseCode), token);
    }

    public void triggerOnPurchasesUpdated() {
        mListener.onPurchaseFlowComplete(BillingClient.BillingResponseCode.OK);
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

    private static boolean wait(CountDownLatch latch) throws InterruptedException {
        return latch.await(5, TimeUnit.SECONDS);
    }

    private static BillingResult toResult(int responseCode) {
         return BillingResult.newBuilder().setResponseCode(responseCode).build();
    }
}
