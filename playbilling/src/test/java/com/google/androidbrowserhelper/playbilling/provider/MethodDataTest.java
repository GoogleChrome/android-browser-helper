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

import android.os.Build;

import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingFlowParams.ProrationMode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import androidx.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link MethodData}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class MethodDataTest {
    @Test
    public void fromJson_basic() {
        String json = "{ 'sku' = 'mySku' }".replace('\'', '\"');
        MethodData data = MethodData.fromJson(json);

        assertMethodData(data, "mySku", null, null, null);
    }

    @Test
    public void fromJson_malformed() {
        assertNull(MethodData.fromJson("abc"));
    }

    @Test
    public void fromJson_noSku() {
        assertNull(MethodData.fromJson("{}"));
    }

    @Test
    public void fromJson_oldSku() {
        String json = "{ 'sku' = 'mySku', 'oldSku' = 'myOldSku' }"
                .replace('\'', '\"');
        MethodData data = MethodData.fromJson(json);

        assertMethodData(data, "mySku", "myOldSku", null, null);
    }

    @Test
    public void fromJson_priceChangeInformation() {
        String json = "{ 'sku' = 'mySku', 'priceChangeConfirmation' = 'true' }"
                .replace('\'', '\"');
        MethodData data = MethodData.fromJson(json);

        assertTrue(data.isPriceChangeConfirmation);
    }

    @Test
    public void fromJson_priceChangeInformation_default() {
        String json = "{ 'sku' = 'mySku' }"
                .replace('\'', '\"');
        MethodData data = MethodData.fromJson(json);

        assertFalse(data.isPriceChangeConfirmation);
    }

    @Test
    public void fromJson_purchaseToken() {
        String json = "{ 'sku' = 'mySku', 'purchaseToken' = 'abc123' }"
                .replace('\'', '\"');
        MethodData data = MethodData.fromJson(json);

        assertMethodData(data, "mySku", null, "abc123", null);
    }

    @Test
    public void fromJson_prorationMode() {
        prorationMode("deferred", ProrationMode.DEFERRED);
        prorationMode("immediateAndChargeProratedPrice",
                ProrationMode.IMMEDIATE_AND_CHARGE_PRORATED_PRICE);
        prorationMode("immediateWithoutProration", ProrationMode.IMMEDIATE_WITHOUT_PRORATION);
        prorationMode("immediateWithTimeProration", ProrationMode.IMMEDIATE_WITH_TIME_PRORATION);
        prorationMode("unknownSubscriptionUpgradeDowngradePolicy",
                ProrationMode.UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY);
        prorationMode("immediateAndChargeFullPrice",
                ProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE);
        prorationMode("invalid", null);
    }

    private static void assertMethodData(MethodData data, String sku, @Nullable String oldSku,
            @Nullable String purchaseToken, @Nullable Integer prorationMode) {
        assertEquals(sku, data.sku);
        assertEquals(oldSku, data.oldSku);
        assertEquals(purchaseToken, data.purchaseToken);
        assertEquals(prorationMode, data.prorationMode);
    }

    private static void prorationMode(String mode, Integer expected) {
        String json = ("{ 'sku' = 'mySku', 'prorationMode' = '" + mode + "' }")
                .replace('\'', '\"');
        MethodData data = MethodData.fromJson(json);

        assertMethodData(data, "mySku", null, null, expected);
    }
}
