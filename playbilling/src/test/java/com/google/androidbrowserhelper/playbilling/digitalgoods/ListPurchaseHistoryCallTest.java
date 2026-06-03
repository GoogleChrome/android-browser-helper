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

import com.google.androidbrowserhelper.playbilling.provider.BillingWrapperFactory;
import com.google.androidbrowserhelper.playbilling.provider.MockBillingWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests for {@link ListPurchaseHistoryCall} and {@link DigitalGoodsRequestHandler}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class ListPurchaseHistoryCallTest {
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
    public void returnsErrorAndEmptyListImmediately() {
        boolean[] callbackTriggered = new boolean[1];

        DigitalGoodsCallback callback = (name, bundle) -> {
            assertEquals(ListPurchaseHistoryCall.RESPONSE_COMMAND, name);
            assertEquals(DigitalGoodsConverter.CHROMIUM_RESULT_ERROR,
                    bundle.getInt(ListPurchaseHistoryCall.KEY_RESPONSE_CODE));

            Parcelable[] array = bundle
                    .getParcelableArray(ListPurchaseHistoryCall.KEY_PURCHASE_HISTORY_LIST);
            assertEquals(0, array.length);

            callbackTriggered[0] = true;
        };

        assertTrue(mHandler.handle(ListPurchaseHistoryCall.COMMAND_NAME, new Bundle(), callback));
        assertTrue(callbackTriggered[0]);
    }
}

