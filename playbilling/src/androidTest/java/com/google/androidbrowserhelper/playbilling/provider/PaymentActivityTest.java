package com.google.androidbrowserhelper.playbilling.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.android.billingclient.api.SkuDetails;
import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import androidx.browser.trusted.TokenStore;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link PaymentActivity}.
 */
@RunWith(AndroidJUnit4.class)
public class PaymentActivityTest {
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

    private List<SkuDetails> getSkuDetailsList() throws JSONException {
        return Arrays.asList(new SkuDetails("{}"));
    }

    private Intent getIntent() {
        Intent innerIntent = new Intent();
        innerIntent.setAction("org.chromium.intent.action.PAY");
        innerIntent.setClass(mContext, PaymentActivity.class);

        Intent intent = new Intent();
        intent.putExtra(WrapperActivity.EXTRA_INNER_INTENT, innerIntent);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(mContext, WrapperActivity.class);
        return intent;
    }

    private void launchActivity() throws InterruptedException {
        mContext.startActivity(getIntent());
        assertTrue(WrapperActivity.waitForLaunch());
    }

    private void assertActivityResult(int result) throws InterruptedException {
        assertTrue(WrapperActivity.waitForFinish());
        assertEquals(WrapperActivity.getLastResultCode(), result);
    }
}
