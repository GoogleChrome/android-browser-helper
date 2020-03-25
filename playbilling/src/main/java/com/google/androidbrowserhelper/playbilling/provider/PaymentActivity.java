package com.google.androidbrowserhelper.playbilling.provider;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.billingclient.api.SkuDetails;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class PaymentActivity extends AppCompatActivity implements BillingWrapper.Listener {
    private static final String TAG = "PaymentActivity";

    private final static List<String> ALL_SKUS = Arrays.asList(
            "android.test.purchased", "android.test.canceled", "android.test.item_unavailable");

    private static final String METHOD_NAME = "https://beer.conn.dev";
    private static final String DETAILS = "{}";

    private BillingWrapper mWrapper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWrapper = BillingWrapperFactory.get(this, this);
        mWrapper.connect();
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "Billing Client disconnected.");
        failed();
    }

    @Override
    public void onConnected() {
        mWrapper.querySkuDetails(ALL_SKUS);
    }

    @Override
    public void onGotSkuDetails() {
        List<SkuDetails> details = mWrapper.getSkuDetailsList();

        if (details == null || details.isEmpty()) {
            Log.w(TAG, "No SKUs returned.");
            failed();
            return;
        }

        if (mWrapper.launchPaymentFlow(mWrapper.getSkuDetailsList().get(0))) return;

        Log.w(TAG, "Payment attempt failed (have you already bought it?).");
        failed();
    }

    @Override
    public void onPurchasesUpdated() {
        paid();
    }

    private void paid() {
        setResult(Activity.RESULT_OK, resultsIntent());
        finish();
    }

    private void failed() {
        setResult(Activity.RESULT_CANCELED, resultsIntent());
        finish();
    }

    private Intent resultsIntent() {
        Intent intent = new Intent();
        intent.putExtra("methodName", METHOD_NAME);
        intent.putExtra("details", DETAILS);
        return intent;
    }
}
