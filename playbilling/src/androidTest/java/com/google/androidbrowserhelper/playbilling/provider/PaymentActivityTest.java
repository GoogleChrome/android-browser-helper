package com.google.androidbrowserhelper.playbilling.provider;

import android.app.Activity;
import android.content.Intent;

import com.android.billingclient.api.SkuDetails;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link PaymentActivity}.
 */
@RunWith(AndroidJUnit4.class)
public class PaymentActivityTest {
    private MockBillingWrapper mWrapper = new MockBillingWrapper();

    private static final boolean INITIAL_TOUCH_MODE = false;
    private static final boolean LAUNCH_ACTIVITY = false;  // We launch the Activity manually.
    @Rule
    public ActivityTestRule<PaymentActivity> mActivityRule =
            new ActivityTestRule<>(PaymentActivity.class, INITIAL_TOUCH_MODE, LAUNCH_ACTIVITY);

    @Before
    public void setUp() {
        BillingWrapperFactory.setBillingWrapperForTesting(mWrapper);
    }

    @After
    public void tearDown() {
        BillingWrapperFactory.setBillingWrapperForTesting(null);
    }

    @Test
    public void successfulFlow() throws InterruptedException, JSONException {
        mActivityRule.launchActivity(getIntent());
        mWrapper.setPaymentFlowWillBeSuccessful(true);

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
        mActivityRule.launchActivity(getIntent());
        mWrapper.setPaymentFlowWillBeSuccessful(false);

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
        mActivityRule.launchActivity(getIntent());
        mWrapper.setPaymentFlowWillBeSuccessful(false);

        assertTrue(mWrapper.waitForConnect());
        mWrapper.triggerConnected();

        mWrapper.triggerDisconnected();

        assertActivityResult(Activity.RESULT_CANCELED);
    }

    private List<SkuDetails> getSkuDetailsList() throws JSONException {
        return Arrays.asList(new SkuDetails("{}"));
    }

    private Intent getIntent() {
        Intent intent = new Intent();
        intent.setAction("org.chromium.intent.action.PAY");
        return intent;
    }

    private void assertActivityResult(int result) {
        assertTrue(mActivityRule.getActivity().isFinishing());
        assertEquals(mActivityRule.getActivityResult().getResultCode(), result);
    }
}
