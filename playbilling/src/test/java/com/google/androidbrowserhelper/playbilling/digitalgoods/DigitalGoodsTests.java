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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.google.androidbrowserhelper.playbilling.digitalgoods.AcknowledgeCall.RESPONSE_ACKNOWLEDGE;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.GetDetailsCall.RESPONSE_GET_DETAILS;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.GetDetailsCall.RESPONSE_GET_DETAILS_DETAILS_LIST;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.GetDetailsCall.RESPONSE_GET_DETAILS_RESPONSE_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(manifest = Config.NONE)
public class DigitalGoodsTests {
    private final static DigitalGoodsCallback EMPTY_CALLBACK = (name, args) -> {};

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

        assertTrue(mBillingWrapper.waitForQuerySkuDetails());
        mBillingWrapper.triggerOnGotSkuDetails(Collections.emptyList());

        assertTrue(callbackTriggered.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void getDetailsCall_parsesResult() throws InterruptedException, JSONException {
        Bundle args = GetDetailsCall.createBundleForTesting("id1");
        CountDownLatch callbackTriggered = new CountDownLatch(1);

        String skuJson = "{"
                + "\"productId\" = \"id1\","
                + "\"title\" = \"My item\","
                + "\"description\" = \"Some description.\","
                + "\"price\" = \"123.45\","
                + "\"price_currency_code\" = \"GBP\""
                + "}";

        DigitalGoodsCallback callback = (name, bundle) -> {
            assertEquals(RESPONSE_GET_DETAILS, name);
            assertEquals(bundle.getInt(RESPONSE_GET_DETAILS_RESPONSE_CODE),
                    BillingClient.BillingResponseCode.OK);

            Parcelable[] array = bundle.getParcelableArray(RESPONSE_GET_DETAILS_DETAILS_LIST);
            ItemDetails details = ItemDetails.create((Bundle) array[0]);

            assertEquals("id1", details.id);
            assertEquals("My item", details.title);
            assertEquals("Some description.", details.description);
            assertEquals("123.45", details.value);
            assertEquals("GBP", details.currency);

            callbackTriggered.countDown();
        };

        assertTrue(mHandler.handle(GetDetailsCall.COMMAND_NAME, args, callback));

        assertTrue(mBillingWrapper.waitForQuerySkuDetails());
        mBillingWrapper.triggerOnGotSkuDetails(Collections.singletonList(new SkuDetails(skuJson)));

        assertTrue(callbackTriggered.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void acknowledgeCall_callsAcknowledge() {
        boolean makeAvailableAgain = true;
        Bundle args = AcknowledgeCall.createBundleForTesting("id1", makeAvailableAgain);
        CountDownLatch callbackTriggered = new CountDownLatch(1);

        DigitalGoodsCallback callback = (name, bundle) -> {
            assertEquals(RESPONSE_ACKNOWLEDGE, name);
            callbackTriggered.countDown();
        };

        assertTrue(mHandler.handle(AcknowledgeCall.COMMAND_NAME, args, callback));

        // TODO check things work...
    }

    @Test
    public void acknowledgeCall_callsConsume() {

    }
}
