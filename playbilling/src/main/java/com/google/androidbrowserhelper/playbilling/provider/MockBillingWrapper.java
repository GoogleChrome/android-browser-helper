package com.google.androidbrowserhelper.playbilling.provider;

import com.android.billingclient.api.SkuDetails;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A {@link BillingWrapper} that can be be controlled for tests.
 */
public class MockBillingWrapper implements BillingWrapper {
    private Listener mListener;

    private List<SkuDetails> mSkuDetailsList;
    private boolean mPaymentFlowSuccessful;

    private final CountDownLatch mConnectLatch = new CountDownLatch(1);
    private final CountDownLatch mQuerySkuDetailsLatch = new CountDownLatch(1);
    private final CountDownLatch mLaunchPaymentFlowLatch = new CountDownLatch(1);

    @Override
    public void connect() {
        mConnectLatch.countDown();
    }

    @Override
    public void querySkuDetails(List<String> skus) {
        mQuerySkuDetailsLatch.countDown();
    }

    @Override
    public List<SkuDetails> getSkuDetailsList() {
        return mSkuDetailsList;
    }

    @Override
    public boolean launchPaymentFlow(SkuDetails sku) {
        mLaunchPaymentFlowLatch.countDown();
        return mPaymentFlowSuccessful;
    }

    public void triggerConnected() {
        mListener.onConnected();
    }

    public void triggerDisconnected() {
        mListener.onDisconnected();
    }

    public void triggerOnGotSkuDetails() {
        mListener.onGotSkuDetails();
    }

    public void triggerOnPurchasesUpdated() {
        mListener.onPurchasesUpdated();
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

    public void setSkuDetailsList(List<SkuDetails> skuDetailsList) {
        mSkuDetailsList = skuDetailsList;
    }

    public void setPaymentFlowWillBeSuccessful(boolean successful) {
        mPaymentFlowSuccessful = successful;
    }

    private static boolean wait(CountDownLatch latch) throws InterruptedException {
        return latch.await(5, TimeUnit.SECONDS);
    }
}
