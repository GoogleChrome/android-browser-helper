// Copyright 2022 Google Inc. All Rights Reserved.
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

import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.PurchaseHistoryRecord;
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

import static com.google.androidbrowserhelper.playbilling.digitalgoods.DigitalGoodsConverter.toChromiumResponseCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests for {@link ListPurchaseHistoryCall} and {@link DigitalGoodsRequestHandler}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class ListPurchaseHistoryCallTest {
    private final static String PURCHASE_RECORD =
            "{ 'productId' = 'id', 'purchaseToken' = 'token' }"
                    .replace('\'', '"');
    private final static DigitalGoodsCallback EMPTY_CALLBACK = (name, args) -> {};

    private final MockBillingWrapper mBillingWrapper = new MockBillingWrapper();
    private DigitalGoodsRequestHandler mHandler;

    @Before
    public void setUp() {
        BillingWrapperFactory.setBillingWrapperForTesting(mBillingWrapper);
        mHandler = new DigitalGoodsRequestHandler(null);
    }

    @Test
    public void goodArgs() {
        assertTrue(mHandler.handle(
                ListPurchaseHistoryCall.COMMAND_NAME, new Bundle(), EMPTY_CALLBACK));
    }

    @Test
    public void parsesResult_inApp() throws JSONException, InterruptedException {
        PurchaseHistoryRecord record = new PurchaseHistoryRecord(PURCHASE_RECORD, "");

        call(Collections.singletonList(record), Collections.emptyList());
    }

    @Test
    public void parsesResult_subs() throws JSONException, InterruptedException {
        PurchaseHistoryRecord record = new PurchaseHistoryRecord(PURCHASE_RECORD, "");

        call(Collections.emptyList(), Collections.singletonList(record));
    }

    private void call(List<PurchaseHistoryRecord> inAppPurchaseHistory,
                      List<PurchaseHistoryRecord> subsPurchaseHistory) throws InterruptedException {
        CountDownLatch callbackTriggered = new CountDownLatch(1);

        DigitalGoodsCallback callback = (name, bundle) -> {
            assertEquals(ListPurchaseHistoryCall.RESPONSE_COMMAND, name);
            assertEquals(bundle.getInt(ListPurchaseHistoryCall.KEY_RESPONSE_CODE),
                    toChromiumResponseCode(BillingClient.BillingResponseCode.OK));

            Parcelable[] array = bundle
                    .getParcelableArray(ListPurchaseHistoryCall.KEY_PURCHASE_HISTORY_LIST);
            PurchaseDetails details = PurchaseDetails.create((Bundle) array[0]);

            assertEquals("id", details.id);
            assertEquals("token", details.purchaseToken);

            callbackTriggered.countDown();
        };

        assertTrue(mHandler.handle(ListPurchaseHistoryCall.COMMAND_NAME, new Bundle(), callback));
        mBillingWrapper.triggerConnected();

        assertTrue(mBillingWrapper.waitForQueryPurchaseHistory());
        mBillingWrapper.triggerOnPurchaseHistoryResponse(BillingClient.SkuType.INAPP,
                inAppPurchaseHistory);
        mBillingWrapper.triggerOnPurchaseHistoryResponse(BillingClient.SkuType.SUBS,
                subsPurchaseHistory);

        assertTrue(callbackTriggered.await(5, TimeUnit.SECONDS));
    }
}
