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

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.SkuDetails;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import androidx.annotation.Nullable;

import static com.google.androidbrowserhelper.playbilling.digitalgoods.JsonUtils.addField;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.JsonUtils.addFieldWithoutLeadingComma;
import static com.google.androidbrowserhelper.playbilling.digitalgoods.JsonUtils.addOptionalField;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ItemDetails}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(manifest = Config.NONE)
public class ItemDetailsTest {

    @Test
    public void create() throws JSONException {
        String json = createSkuDetailsJson("id", "title", "desc", "GBP", 123_000_000, "month", "week",
                "day", 45_000_000L);

        ItemDetails item = ItemDetails.create(new SkuDetails(json));
        assertItemDetails(item, "id", "title", "desc", "GBP", "123.000000", "month", "week", "day",
                "GBP", "45.000000");
    }

    @Test
    public void create_optionalOnly() throws JSONException {
        String json = createSkuDetailsJson("id", "title", "desc", "GBP", 123_000_000, null, null,
                null, null);

        ItemDetails item = ItemDetails.create(new SkuDetails(json));
        assertItemDetails(item, "id", "title", "desc", "GBP", "123.000000", "", "", "", "GBP",
                "0.000000");
    }

    @Test
    public void bundleConversion() throws JSONException {
        String json = createSkuDetailsJson("id", "title", "desc", "GBP", 123_000_000, "month", "week",
                "day", 45_000_000L);

        ItemDetails item =
                ItemDetails.create(ItemDetails.create(new SkuDetails((json))).toBundle());
        assertItemDetails(item, "id", "title", "desc", "GBP", "123.000000", "month", "week", "day",
                "GBP", "45.000000");
    }

    @Test
    public void bundleConversion_optionalOnly() throws JSONException {
        String json = createSkuDetailsJson("id", "title", "desc", "GBP", 123_000_000, null, null,
                null, null);

        ItemDetails item =
                ItemDetails.create(ItemDetails.create(new SkuDetails((json))).toBundle());
        assertItemDetails(item, "id", "title", "desc", "GBP", "123.000000", "", "", "", "GBP",
                "0.000000");
    }

    static void assertItemDetails(ItemDetails item, String id, String title,
            String description, String currency, String value, String subscriptionPeriod,
            String freeTrialPeriod, String introductoryPricePeriod,
            String introductoryPriceCurrency, String introductoryPriceValue) {
        assertEquals(item.id, id);
        assertEquals(item.title, title);
        assertEquals(item.description, description);
        assertEquals(item.currency, currency);
        assertEquals(item.value, value);
        assertEquals(item.subscriptionPeriod, subscriptionPeriod);
        assertEquals(item.freeTrialPeriod, freeTrialPeriod);
        assertEquals(item.introductoryPricePeriod, introductoryPricePeriod);
        assertEquals(item.introductoryPriceCurrency, introductoryPriceCurrency);
        assertEquals(item.introductoryPriceValue, introductoryPriceValue);
    }

    static String createSkuDetailsJson(String id, String title, String description,
            String currency, long value, @Nullable String subscriptionPeriod,
            @Nullable String freeTrialPeriod, @Nullable String introductoryPricePeriod,
            @Nullable Long introductoryPriceValue) {
        StringBuilder b = new StringBuilder();

        b.append("{");

        addFieldWithoutLeadingComma(b, "productId", id);
        addField(b, "title", title);
        addField(b, "description", description);
        addField(b, "price_amount_micros", value);
        addField(b, "price_currency_code", currency);

        addOptionalField(b, "subscriptionPeriod", subscriptionPeriod);
        addOptionalField(b, "freeTrialPeriod", freeTrialPeriod);
        addOptionalField(b, "introductoryPricePeriod", introductoryPricePeriod);
        addOptionalField(b, "introductoryPriceAmountMicros", introductoryPriceValue);

        // The Play Billing library requires that all SkuDetails have a type, but we don't use it
        // in our testing, so just set it to an arbitrary type.
        addField(b, "type", BillingClient.SkuType.INAPP);

        b.append("}");
        return b.toString();
    }
}
