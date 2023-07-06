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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import static com.google.androidbrowserhelper.playbilling.provider.PaymentActivity.PROXY_PACKAGE_KEY;
import static org.junit.Assert.assertEquals;
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
        mWrapper.triggerOnGotSkuDetails(getSkuDetailsList());

        assertTrue(mWrapper.waitForLaunchPaymentFlow());
        mWrapper.triggerOnPurchasesUpdated();

        assertActivityResult(Activity.RESULT_OK);
    }

    @Test
    public void setsProxy() throws InterruptedException, JSONException {
        mWrapper.setPaymentFlowWillBeSuccessful(true);

        launchActivity();

        assertTrue(mWrapper.waitForConnect());
        mWrapper.triggerConnected();

        assertTrue(mWrapper.waitForQuerySkuDetails());
        mWrapper.triggerOnGotSkuDetails(getSkuDetailsList());

        assertTrue(mWrapper.waitForLaunchPaymentFlow());
        mWrapper.triggerOnPurchasesUpdated();

        assertActivityResult(Activity.RESULT_OK);

        String proxy = mWrapper.getPlayBillingFlowLaunchIntent().getStringExtra(PROXY_PACKAGE_KEY);
        assertEquals(proxy, mContext.getPackageName());
    }

    @Test
    public void cancelledFlow() throws InterruptedException, JSONException {
        mWrapper.setPaymentFlowWillBeSuccessful(false);

        launchActivity();

        assertTrue(mWrapper.waitForConnect());
        mWrapper.triggerConnected();

        assertTrue(mWrapper.waitForQuerySkuDetails());
        mWrapper.triggerOnGotSkuDetails(getSkuDetailsList());

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
        mWrapper.triggerOnGotSkuDetails(Collections.emptyList());

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
        String json = "{ 'productId' = 'mySku', 'type' = 'inapp' }".replace('\'', '\"');
        return Arrays.asList(new SkuDetails(json));
    }

    private Intent getIntent(@Nullable String sku) {
        return getIntent(sku, false);
    }

    private Intent getIntent(@Nullable String sku, boolean priceChangeConfirmation) {
        Intent innerIntent = new Intent();
        innerIntent.setAction("org.chromium.intent.action.PAY");
        innerIntent.setClass(mContext, PaymentActivity.class);

        if (sku != null) {
            ArrayList<String> paymentMethods =
                    new ArrayList<>(Collections.singletonList(PAYMENT_METHOD));
            innerIntent.putStringArrayListExtra("methodNames", paymentMethods);

            Bundle methodData = new Bundle();
            methodData.putString(PAYMENT_METHOD, "{" +
                    "sku: \"" + sku + "\"," +
                    "priceChangeConfirmation: \"" + priceChangeConfirmation + "\"}");

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
