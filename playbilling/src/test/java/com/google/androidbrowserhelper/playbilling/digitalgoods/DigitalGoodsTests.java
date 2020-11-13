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

package com.google.androidbrowserhelper.playbilling.digitalgoods;

import android.os.Bundle;
import android.os.Parcelable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.google.androidbrowserhelper.playbilling.provider.BillingWrapperFactory;
import com.google.androidbrowserhelper.playbilling.provider.MockBillingWrapper;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.google.androidbrowserhelper.playbilling.digitalgoods.AcknowledgeCall.RESPONSE_ACKNOWLEDGE;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.AcknowledgeCall.RESPONSE_ACKNOWLEDGE_RESPONSE_CODE;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.DigitalGoodsConverter.toChromiumResponseCode;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.GetDetailsCall.RESPONSE_GET_DETAILS;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.GetDetailsCall.RESPONSE_GET_DETAILS_DETAILS_LIST;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.GetDetailsCall.RESPONSE_GET_DETAILS_RESPONSE_CODE;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.PurchaseDetails.CHROMIUM_PURCHASE_STATE_PENDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(manifest = Config.NONE)
public class DigitalGoodsTests {
    private final static DigitalGoodsCallback EMPTY_CALLBACK = (name, args) -> {};
    private final static String SKU_DETAILS = ItemDetailsTest.createSkuDetailsJson("id1",
            "My item", "Some description.", "GBP", 123450000, null, null, null, null);
    private final static String PURCHASE_DETAILS = PurchaseDetailsTest.createPurchaseJson(
            "id", "token", true, Purchase.PurchaseState.PENDING, 123_000, true);

    private final MockBillingWrapper mBillingWrapper = new MockBillingWrapper();
    private DigitalGoodsRequestHandler mHandler;

    @Before
    public void setUp() {
        BillingWrapperFactory.setBillingWrapperForTesting(mBillingWrapper);
        mHandler = new DigitalGoodsRequestHandler(null);
    }

    @Test
    public void getDetailsCall_unknownCommand() {
        assertFalse(mHandler.handle("unknown", new Bundle(), EMPTY_CALLBACK));
    }

    @Test
    public void getDetailsCall_wrongArgs() {
        assertFalse(mHandler.handle(GetDetailsCall.COMMAND_NAME, new Bundle(), EMPTY_CALLBACK));
    }

    @Test
    public void getDetailsCall_goodArgs() {
        Bundle args = GetDetailsCall.createBundleForTesting("id1");

        assertTrue(mHandler.handle(GetDetailsCall.COMMAND_NAME, args, EMPTY_CALLBACK));
    }

    @Test
    public void getDetailsCall_callsCallback() throws InterruptedException {
        Bundle args = GetDetailsCall.createBundleForTesting("id1");

        CountDownLatch callbackTriggered = new CountDownLatch(1);
        DigitalGoodsCallback callback = (name, bundle) -> callbackTriggered.countDown();

        assertTrue(mHandler.handle( GetDetailsCall.COMMAND_NAME, args, callback));
        mBillingWrapper.triggerConnected();

        assertTrue(mBillingWrapper.waitForQuerySkuDetails());
        mBillingWrapper.triggerOnGotInAppSkuDetails(Collections.emptyList());
        mBillingWrapper.triggerOnGotSubsSkuDetails(Collections.emptyList());

        assertTrue(callbackTriggered.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void getDetailsCall_parsesResult_inApp() throws InterruptedException, JSONException {
        checkParsesResult(
                Collections.singletonList(new SkuDetails(SKU_DETAILS)),
                Collections.emptyList());
    }

    @Test
    public void getDetailsCall_parsesResult_subs() throws InterruptedException, JSONException {
        checkParsesResult(
                Collections.emptyList(),
                Collections.singletonList(new SkuDetails(SKU_DETAILS)));
    }

    private void checkParsesResult(List<SkuDetails> inAppSkuDetails,
            List<SkuDetails> subsSkuDetails) throws InterruptedException {
        Bundle args = GetDetailsCall.createBundleForTesting("id1");
        CountDownLatch callbackTriggered = new CountDownLatch(1);

        DigitalGoodsCallback callback = (name, bundle) -> {
            assertEquals(RESPONSE_GET_DETAILS, name);
            assertEquals(bundle.getInt(RESPONSE_GET_DETAILS_RESPONSE_CODE),
                    BillingClient.BillingResponseCode.OK);

            Parcelable[] array = bundle.getParcelableArray(RESPONSE_GET_DETAILS_DETAILS_LIST);
            ItemDetails details = ItemDetails.create((Bundle) array[0]);

            assertEquals("id1", details.id);
            assertEquals("My item", details.title);
            assertEquals("Some description.", details.description);
            assertEquals("123.450000", details.value);
            assertEquals("GBP", details.currency);
            ItemDetailsTest.assertItemDetails(details, "id1", "My item", "Some description.",
                    "GBP", "123.450000", "", "", "", "GBP", "0.000000");

            callbackTriggered.countDown();
        };

        assertTrue(mHandler.handle(GetDetailsCall.COMMAND_NAME, args, callback));
        mBillingWrapper.triggerConnected();

        assertTrue(mBillingWrapper.waitForQuerySkuDetails());
        mBillingWrapper.triggerOnGotInAppSkuDetails(inAppSkuDetails);
        mBillingWrapper.triggerOnGotSubsSkuDetails(subsSkuDetails);

        assertTrue(callbackTriggered.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void acknowledgeCall_callsAcknowledge() throws InterruptedException {
        callAcknowledge(true);
    }

    @Test
    public void acknowledgeCall_callsConsume() throws InterruptedException {
        callAcknowledge(false);
    }

    private void callAcknowledge(boolean makeAvailableAgain) throws InterruptedException {
        Bundle args = AcknowledgeCall.createBundleForTesting("id1", makeAvailableAgain);
        CountDownLatch callbackTriggered = new CountDownLatch(1);
        int billingResponseCode = BillingClient.BillingResponseCode.ITEM_NOT_OWNED;
        int chromiumResponseCode = toChromiumResponseCode(billingResponseCode);

        DigitalGoodsCallback callback = (name, bundle) -> {
            assertEquals(RESPONSE_ACKNOWLEDGE, name);
            assertEquals(chromiumResponseCode, bundle.getInt(RESPONSE_ACKNOWLEDGE_RESPONSE_CODE));
            callbackTriggered.countDown();
        };

        assertTrue(mHandler.handle(AcknowledgeCall.COMMAND_NAME, args, callback));
        mBillingWrapper.triggerConnected();

        if (makeAvailableAgain) {
            assertEquals("id1", mBillingWrapper.getConsumeToken());
            mBillingWrapper.triggerConsume(billingResponseCode, "?");
        } else {
            assertEquals("id1", mBillingWrapper.getAcknowledgeToken());
            mBillingWrapper.triggerAcknowledge(billingResponseCode);
        }

        assertTrue(callbackTriggered.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void listPurchasesCall_goodArgs() {
        assertTrue(mHandler.handle(ListPurchasesCall.COMMAND_NAME, new Bundle(), EMPTY_CALLBACK));
    }

    @Test
    public void listPurchasesCall_parsesResult_inApp() throws JSONException, InterruptedException {
        checkListPurchasesCall(
                Collections.singletonList(new Purchase(PURCHASE_DETAILS, "")),
                Collections.emptyList());
    }

    @Test
    public void listPurchasesCall_parsesResult_subs() throws JSONException, InterruptedException {
        checkListPurchasesCall(
                Collections.emptyList(),
                Collections.singletonList(new Purchase(PURCHASE_DETAILS, "")));
    }

    public void checkListPurchasesCall(List<Purchase> inAppPurchaseDetails,
            List<Purchase> subsPurchaseDetails) throws InterruptedException {
        CountDownLatch callbackTriggered = new CountDownLatch(1);

        DigitalGoodsCallback callback = (name, bundle) -> {
            assertEquals(ListPurchasesCall.RESPONSE_COMMAND, name);
            assertEquals(bundle.getInt(ListPurchasesCall.KEY_RESPONSE_CODE),
                    toChromiumResponseCode(BillingClient.BillingResponseCode.OK));

            Parcelable[] array = bundle.getParcelableArray(ListPurchasesCall.KEY_PURCHASES_LIST);
            PurchaseDetails details = PurchaseDetails.create((Bundle) array[0]);

            PurchaseDetailsTest.assertPurchaseDetails(details,
                    "id",
                    "token",
                    true,
                    CHROMIUM_PURCHASE_STATE_PENDING,
                    123_000_000,
                    true);

            callbackTriggered.countDown();
        };

        assertTrue(mHandler.handle(ListPurchasesCall.COMMAND_NAME, new Bundle(), callback));
        mBillingWrapper.triggerConnected();

        assertTrue(mBillingWrapper.waitForQueryPurchases());
        mBillingWrapper.triggerOnGotInAppPurchaseDetails(inAppPurchaseDetails);
        mBillingWrapper.triggerOnGotSubsPurchaseDetails(subsPurchaseDetails);

        assertTrue(callbackTriggered.await(5, TimeUnit.SECONDS));
    }
}
