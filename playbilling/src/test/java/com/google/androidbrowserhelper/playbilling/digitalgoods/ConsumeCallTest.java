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

import com.android.billingclient.api.BillingClient;
import com.google.androidbrowserhelper.playbilling.provider.BillingWrapperFactory;
import com.google.androidbrowserhelper.playbilling.provider.MockBillingWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.google.androidbrowserhelper.playbilling.digitalgoods.ConsumeCall.RESPONSE_CONSUME;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.ConsumeCall.RESPONSE_CONSUME_RESPONSE_CODE;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.DigitalGoodsConverter.toChromiumResponseCode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests for {@link ConsumeCall} and {@link DigitalGoodsRequestHandler}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class ConsumeCallTest {
    private final MockBillingWrapper mBillingWrapper = new MockBillingWrapper();
    private DigitalGoodsRequestHandler mHandler;

    @Before
    public void setUp() {
        BillingWrapperFactory.setBillingWrapperForTesting(mBillingWrapper);
        mHandler = new DigitalGoodsRequestHandler(null);
    }

    @Test
    public void callsConsume() throws InterruptedException {
        Bundle args = ConsumeCall.createBundleForTesting("id1");
        CountDownLatch callbackTriggered = new CountDownLatch(1);
        int billingResponseCode = BillingClient.BillingResponseCode.ITEM_NOT_OWNED;
        int chromiumResponseCode = toChromiumResponseCode(billingResponseCode);

        DigitalGoodsCallback callback = (name, bundle) -> {
            assertEquals(RESPONSE_CONSUME, name);
            assertEquals(chromiumResponseCode, bundle.getInt(RESPONSE_CONSUME_RESPONSE_CODE));
            callbackTriggered.countDown();
        };

        assertTrue(mHandler.handle(ConsumeCall.COMMAND_NAME, args, callback));
        mBillingWrapper.triggerConnected();

        assertEquals("id1", mBillingWrapper.getConsumeToken());
        mBillingWrapper.triggerConsume(billingResponseCode, "?");

        assertTrue(callbackTriggered.await(5, TimeUnit.SECONDS));
    }
}
