package com.google.androidbrowserhelper.playbilling.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.android.billingclient.api.SkuDetails;
import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.browser.trusted.TokenStore;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link PaymentActivity}.
 */
@RunWith(AndroidJUnit4.class)
public class PaymentActivityTest {
    private static final String SKU = "some_sku";
    private static final String PAYMENT_METHOD = "some_method";

    private MockBillingWrapper mWrapper = new MockBillingWrapper();
    private Context mContext;
    private Context mTargetContext;
    private SharedPreferencesTokenStore mTokenStore;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mTargetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        mTokenStore = new SharedPreferencesTokenStore(mTargetContext);
        mTokenStore.setVerifiedProvider(mContext.getPackageName(), mContext.getPackageManager());

        BillingWrapperFactory.setBillingWrapperForTesting(mWrapper);
        WrapperActivity.reset();
    }

    @After
    public void tearDown() {
        mTokenStore.store(null);
        BillingWrapperFactory.setBillingWrapperForTesting(null);
    }

    @Test
    public void successfulFlow() throws InterruptedException, JSONException {
        mWrapper.setPaymentFlowWillBeSuccessful(true);

        launchActivity();

        assertTrue(mWrapper.waitForConnect());
        mWrapper.triggerConnected();

        assertTrue(mWrapper.waitForQuerySkuDetails());
        mWrapper.setSkuDetailsList(getSkuDetailsList());
        mWrapper.triggerOnGotSkuDetails();

        assertTrue(mWrapper.waitForLaunchPaymentFlow());
        mWrapper.triggerOnPurchasesUpdated();

        assertActivityResult(Activity.RESULT_OK);
    }

    @Test
    public void cancelledFlow() throws InterruptedException, JSONException {
        mWrapper.setPaymentFlowWillBeSuccessful(false);

        launchActivity();

        assertTrue(mWrapper.waitForConnect());
        mWrapper.triggerConnected();

        assertTrue(mWrapper.waitForQuerySkuDetails());
        mWrapper.setSkuDetailsList(getSkuDetailsList());
        mWrapper.triggerOnGotSkuDetails();

        assertTrue(mWrapper.waitForLaunchPaymentFlow());

        assertActivityResult(Activity.RESULT_CANCELED);
    }

    @Test
    public void billingDisconnected() throws InterruptedException {
        mWrapper.setPaymentFlowWillBeSuccessful(false);

        launchActivity();

        assertTrue(mWrapper.waitForConnect());
        mWrapper.triggerConnected();

        mWrapper.triggerDisconnected();

        assertActivityResult(Activity.RESULT_CANCELED);
    }

    @Test
    public void unverifiedCaller() throws InterruptedException {
        mTokenStore.store(null);

        launchActivity();
        assertActivityResult(Activity.RESULT_CANCELED);
    }

    @Test
    public void noSkuProvided() throws InterruptedException {
        launchActivity(null);

        assertActivityResult(Activity.RESULT_CANCELED);
    }

    @Test
    public void noSkuFound() throws InterruptedException {
        launchActivity();

        assertTrue(mWrapper.waitForConnect());
        mWrapper.triggerConnected();

        assertTrue(mWrapper.waitForQuerySkuDetails());
        mWrapper.setSkuDetailsList(Collections.emptyList());
        mWrapper.triggerOnGotSkuDetails();

        assertActivityResult(Activity.RESULT_CANCELED);
    }

    @Test
    public void skuPassedToPlayBilling() throws InterruptedException {
        launchActivity();

        assertTrue(mWrapper.waitForConnect());
        mWrapper.triggerConnected();

        assertTrue(mWrapper.waitForQuerySkuDetails());
        List<String> queriedSkuDetails = mWrapper.getQueriedSkuDetails();

        assertEquals(1, queriedSkuDetails.size());
        assertTrue(queriedSkuDetails.contains(SKU));
    }

    private List<SkuDetails> getSkuDetailsList() throws JSONException {
        return Arrays.asList(new SkuDetails("{}"));
    }

    private Intent getIntent(@Nullable String sku) {
        Intent innerIntent = new Intent();
        innerIntent.setAction("org.chromium.intent.action.PAY");
        innerIntent.setClass(mContext, PaymentActivity.class);

        if (sku != null) {
            ArrayList<String> paymentMethods =
                    new ArrayList<>(Collections.singletonList(PAYMENT_METHOD));
            innerIntent.putStringArrayListExtra("methodNames", paymentMethods);

            Bundle methodData = new Bundle();
            methodData.putString(PAYMENT_METHOD, "{ sku: \"" + sku + "\" }");

            innerIntent.putExtra("methodData", methodData);
        }

        Intent intent = new Intent();
        intent.putExtra(WrapperActivity.EXTRA_INNER_INTENT, innerIntent);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(mContext, WrapperActivity.class);
        return intent;
    }

    private void launchActivity() throws InterruptedException {
        launchActivity(SKU);
    }

    private void launchActivity(@Nullable String sku) throws InterruptedException {
        mContext.startActivity(getIntent(sku));
        assertTrue(WrapperActivity.waitForLaunch());
    }

    private void assertActivityResult(int result) throws InterruptedException {
        assertTrue(WrapperActivity.waitForFinish());
        assertEquals(WrapperActivity.getLastResultCode(), result);
    }
}
