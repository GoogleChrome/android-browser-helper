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

import android.os.Build;

import com.android.billingclient.api.Purchase;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import static com.google.androidbrowserhelper.playbilling.digitalgoods.JsonUtils.addField;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.JsonUtils.addFieldWithoutLeadingComma;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.LegacyPurchaseDetails.CHROMIUM_PURCHASE_STATE_PENDING;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class LegacyPurchaseDetailsTest {
    @Test
    public void create() throws JSONException {
        Purchase purchase = new Purchase(createPurchaseJson("id", "token", true,
                Purchase.PurchaseState.PENDING, 123_000, true), "");

        LegacyPurchaseDetails details = LegacyPurchaseDetails.create(purchase);

        assertPurchaseDetails(details,
                "id",
                "token",
                true,
                CHROMIUM_PURCHASE_STATE_PENDING,  // Converted to the Chromium type.
                123_000_000,  // Converted to microseconds.
                true);
    }

    @Test
    public void bundleConversion() throws JSONException {
        Purchase purchase = new Purchase(createPurchaseJson("id", "token", true,
                Purchase.PurchaseState.PENDING, 123_000, true), "");

        LegacyPurchaseDetails details =
                LegacyPurchaseDetails.create(LegacyPurchaseDetails.create(purchase).toBundle());

        assertPurchaseDetails(details,
                "id",
                "token",
                true,
                CHROMIUM_PURCHASE_STATE_PENDING,  // Converted to the Chromium type.
                123_000_000,  // Converted to microseconds.
                true);
    }

    static void assertPurchaseDetails(LegacyPurchaseDetails details, String id, String token,
                                      boolean acknowledged, int state, long purchaseTimeMicrosecondsPastUnixEpoch,
                                      boolean willAutoRenew) {
        assertEquals(details.id, id);
        assertEquals(details.purchaseToken, token);
        assertEquals(details.acknowledged, acknowledged);
        assertEquals(details.purchaseState, state);
        assertEquals(details.purchaseTimeMicrosecondsPastUnixEpoch,
                purchaseTimeMicrosecondsPastUnixEpoch);
        assertEquals(details.willAutoRenew, willAutoRenew);
    }

    static String createPurchaseJson(String id, String purchaseToken, boolean acknowledged,
            int purchaseState, long purchaseTime, boolean willAutoRenew) {
        // In the input JSON to a Play Billing Purchase, a PurchaseState of 4 corresponds to
        // PENDING and a PurchaseState of anything else corresponds to PURCHASED.
        assert purchaseState == Purchase.PurchaseState.PENDING ||
                purchaseState == Purchase.PurchaseState.PURCHASED;
        int fixedPurchaseState = purchaseState == Purchase.PurchaseState.PENDING ? 4 : 0;

        StringBuilder b = new StringBuilder();

        b.append("{");

        addFieldWithoutLeadingComma(b, "productId", id);
        addField(b, "token", purchaseToken);
        addField(b, "acknowledged", acknowledged);
        addField(b, "purchaseState", fixedPurchaseState);
        addField(b, "purchaseTime", purchaseTime);
        addField(b, "autoRenewing", willAutoRenew);

        b.append("}");
        return b.toString();
    }
}
