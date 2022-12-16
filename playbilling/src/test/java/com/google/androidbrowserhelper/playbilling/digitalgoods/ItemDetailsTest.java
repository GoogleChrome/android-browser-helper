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
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class ItemDetailsTest {
    private static final String ID = "id";
    private static final String TITLE = "title";
    private static final String DESC = "desc";
    private static final String CURRENCY = "GBP";
    private static final long VALUE = 123_000_000;
    private static final String TYPE = "inapp";
    private static final String ICON_URL = "https://www.example.com/image.png";

    private static final String SUBS_PERIOD = "month";
    private static final String FREE_PERIOD = "week";
    private static final String INTRO_PERIOD = "day";
    private static final long INTRO_VALUE = 45_000_000;
    private static final int INTRO_CYCLES = 3;

    // Variables only used in checking the output;
    private static final String INTRO_CURRENCY = CURRENCY;
    private static final String VALUE_STR = "123.000000";
    private static final String INTRO_VALUE_STR = "45.000000";

    @Test
    public void create() throws JSONException {
        String json = createTestJsonSkuDetails();
        ItemDetails item = ItemDetails.create(new SkuDetails(json));

        assertTestItemDetails(item);
    }

    @Test
    public void create_mandatoryOnly() throws JSONException {
        String json = createTestJsonSkuDetails_mandatoryOnly();

        ItemDetails item = ItemDetails.create(new SkuDetails(json));
        assertTestItemDetails_mandatoryOnly(item);
    }

    @Test
    public void bundleConversion() throws JSONException {
        String json = createTestJsonSkuDetails();

        ItemDetails item =
                ItemDetails.create(ItemDetails.create(new SkuDetails((json))).toBundle());
        assertTestItemDetails(item);
    }

    @Test
    public void bundleConversion_mandatoryOnly() throws JSONException {
        String json = createTestJsonSkuDetails_mandatoryOnly();

        ItemDetails item =
                ItemDetails.create(ItemDetails.create(new SkuDetails((json))).toBundle());
        assertTestItemDetails_mandatoryOnly(item);
    }

    public static String createTestJsonSkuDetails() {
        return createSkuDetailsJson(ID, TITLE, DESC, CURRENCY, VALUE, TYPE,
                ICON_URL, SUBS_PERIOD, FREE_PERIOD, INTRO_PERIOD, INTRO_VALUE, INTRO_CYCLES);
    }

    public static void assertTestItemDetails(ItemDetails item) {
        assertItemDetails(item, ID, TITLE, DESC, CURRENCY, VALUE_STR, TYPE, ICON_URL,
                SUBS_PERIOD, FREE_PERIOD, INTRO_PERIOD, INTRO_CURRENCY, INTRO_VALUE_STR,
                INTRO_CYCLES);
    }

    private static String createTestJsonSkuDetails_mandatoryOnly() {
        return createSkuDetailsJson(ID, TITLE, DESC, CURRENCY, VALUE, TYPE,
                null, null, null, null, null, 0);
    }

    private static void assertTestItemDetails_mandatoryOnly(ItemDetails item) {
        assertItemDetails(item, ID, TITLE, DESC, CURRENCY, VALUE_STR, TYPE, "",
                "", "", "", INTRO_CURRENCY, "0.000000", 0);
    }

    static void assertItemDetails(ItemDetails item, String id, String title,
            String description, String currency, String value, String type, String iconUrl,
            String subscriptionPeriod,
            String freeTrialPeriod, String introductoryPricePeriod,
            String introductoryPriceCurrency, String introductoryPriceValue,
            int introductoryPriceCycles) {
        assertEquals(item.id, id);
        assertEquals(item.title, title);
        assertEquals(item.description, description);
        assertEquals(item.currency, currency);
        assertEquals(item.value, value);
        assertEquals(item.type, type);
        assertEquals(item.iconUrl, iconUrl);
        assertEquals(item.subscriptionPeriod, subscriptionPeriod);
        assertEquals(item.freeTrialPeriod, freeTrialPeriod);
        assertEquals(item.introductoryPricePeriod, introductoryPricePeriod);
        assertEquals(item.introductoryPriceCurrency, introductoryPriceCurrency);
        assertEquals(item.introductoryPriceValue, introductoryPriceValue);
        assertEquals(item.introductoryPriceCycles, introductoryPriceCycles);
    }

    static String createSkuDetailsJson(String id, String title, String description,
            String currency, long value, String type, @Nullable String iconUrl,
            @Nullable String subscriptionPeriod, @Nullable String freeTrialPeriod,
            @Nullable String introductoryPricePeriod, @Nullable Long introductoryPriceValue,
            int introductoryPriceCycles) {
        StringBuilder b = new StringBuilder();

        b.append("{");

        addFieldWithoutLeadingComma(b, "productId", id);
        addField(b, "title", title);
        addField(b, "description", description);
        addField(b, "price_amount_micros", value);
        addField(b, "price_currency_code", currency);
        addField(b, "type", type);
        addOptionalField(b, "iconUrl", iconUrl);

        addOptionalField(b, "subscriptionPeriod", subscriptionPeriod);
        addOptionalField(b, "freeTrialPeriod", freeTrialPeriod);
        addOptionalField(b, "introductoryPricePeriod", introductoryPricePeriod);
        addOptionalField(b, "introductoryPriceAmountMicros", introductoryPriceValue);
        addField(b, "introductoryPriceCycles", introductoryPriceCycles);

        // The Play Billing library requires that all SkuDetails have a type, but we don't use it
        // in our testing, so just set it to an arbitrary type.
        addField(b, "type", BillingClient.SkuType.INAPP);

        b.append("}");
        return b.toString();
    }
}
