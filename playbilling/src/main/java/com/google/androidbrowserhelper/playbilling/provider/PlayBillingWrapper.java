package com.google.androidbrowserhelper.playbilling.provider;

import android.app.Activity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.List;

import androidx.annotation.Nullable;

/**
 * A {@link BillingWrapper} that communicates with the Play Billing libraries.
 */
public class PlayBillingWrapper implements BillingWrapper {
    private final Activity mActivity;
    private final Listener mListener;
    private final BillingClient mClient;

    @Nullable
    private List<SkuDetails> mSkuDetailsList;

    private final PurchasesUpdatedListener mPurchaseUpdateListener =
            new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {
            mListener.onPurchasesUpdated();
        }
    };

    public PlayBillingWrapper(Activity activity, Listener listener) {
        mActivity = activity;
        mListener = listener;
        mClient = BillingClient
                .newBuilder(activity)
                .setListener(mPurchaseUpdateListener)
                .enablePendingPurchases()
                .build();
    }

    @Override
    public void connect() {
        mClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                mListener.onConnected();
            }

            @Override
            public void onBillingServiceDisconnected() {
                mListener.onDisconnected();
            }
        });
    }

    @Override
    public void querySkuDetails(List<String> skus) {
        SkuDetailsParams params = SkuDetailsParams
                .newBuilder()
                .setSkusList(skus)
                .setType(BillingClient.SkuType.INAPP)
                .build();

        mClient.querySkuDetailsAsync(params, (billingResult, list) -> {
            // TODO: Check result
            mSkuDetailsList = list;
            mListener.onGotSkuDetails();
        });
    }

    @Override
    public List<SkuDetails> getSkuDetailsList() {
        return mSkuDetailsList;
    }

    @Override
    public boolean launchPaymentFlow(SkuDetails sku) {
        BillingFlowParams params = BillingFlowParams
                .newBuilder()
                .setSkuDetails(sku)
                .build();

        BillingResult result = mClient.launchBillingFlow(mActivity, params);

        return result.getResponseCode() == BillingClient.BillingResponseCode.OK;
    }
}
