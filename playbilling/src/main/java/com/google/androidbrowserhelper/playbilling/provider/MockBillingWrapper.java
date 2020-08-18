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

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
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

    private List<String> mQueriedSkuDetails;
    private boolean mPaymentFlowSuccessful;
    private SkuDetailsResponseListener mPendingQuerySkuDetailsCallback;

    private final CountDownLatch mConnectLatch = new CountDownLatch(1);
    private final CountDownLatch mQuerySkuDetailsLatch = new CountDownLatch(1);
    private final CountDownLatch mLaunchPaymentFlowLatch = new CountDownLatch(1);

    @Override
    public void connect() {
        mConnectLatch.countDown();
    }

    @Override
    public void querySkuDetails(List<String> skus, SkuDetailsResponseListener callback) {
        mQueriedSkuDetails = skus;
        mQuerySkuDetailsLatch.countDown();
        mPendingQuerySkuDetailsCallback = callback;
    }

    @Override
    public boolean launchPaymentFlow(Activity activity, SkuDetails sku) {
        mLaunchPaymentFlowLatch.countDown();
        return mPaymentFlowSuccessful;
    }

    public void triggerConnected() {
        mListener.onConnected();
    }

    public void triggerDisconnected() {
        mListener.onDisconnected();
    }

    public void triggerOnGotSkuDetails(List<SkuDetails> skuDetails) {
        triggerOnGotSkuDetails(BillingClient.BillingResponseCode.OK, skuDetails);
    }

    public void triggerOnGotSkuDetails(int responseCode, List<SkuDetails> skuDetails) {
        BillingResult result = BillingResult.newBuilder().setResponseCode(responseCode).build();
        mPendingQuerySkuDetailsCallback.onSkuDetailsResponse(result, skuDetails);
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

    private static boolean wait(CountDownLatch latch) throws InterruptedException {
        return latch.await(5, TimeUnit.SECONDS);
    }
}
