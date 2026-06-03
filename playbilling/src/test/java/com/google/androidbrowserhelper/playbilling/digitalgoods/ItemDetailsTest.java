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
import com.android.billingclient.api.ProductDetails;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    public void create_inApp() {
        ProductDetails productDetails = mockInAppProductDetails(ID, TITLE, DESC, CURRENCY, VALUE);
        ItemDetails item = ItemDetails.create(productDetails);
        assertTestItemDetails_inApp(item);
    }

    @Test
    public void create_subscription() {
        List<ProductDetails.PricingPhase> phases = new ArrayList<>();
        phases.add(mockPricingPhase(FREE_PERIOD, CURRENCY, 0, 1));
        phases.add(mockPricingPhase(INTRO_PERIOD, CURRENCY, INTRO_VALUE, INTRO_CYCLES));
        phases.add(mockPricingPhase(SUBS_PERIOD, CURRENCY, VALUE, 1));

        ProductDetails productDetails = mockSubscriptionProductDetails(ID, TITLE, DESC,
                "base_plan", "offer_id", phases);

        ItemDetails item = ItemDetails.create(productDetails);
        assertTestItemDetails(item);
    }

    @Test
    public void create_mandatoryOnly() {
        ProductDetails productDetails = mockInAppProductDetails(ID, TITLE, DESC, CURRENCY, VALUE);
        ItemDetails item = ItemDetails.create(productDetails);
        assertTestItemDetails_inApp(item);
    }

    @Test
    public void bundleConversion_inApp() {
        ProductDetails productDetails = mockInAppProductDetails(ID, TITLE, DESC, CURRENCY, VALUE);
        ItemDetails item = ItemDetails.create(ItemDetails.create(productDetails).toBundle());
        assertTestItemDetails_inApp(item);
    }

    @Test
    public void bundleConversion_subscription() {
        List<ProductDetails.PricingPhase> phases = new ArrayList<>();
        phases.add(mockPricingPhase(FREE_PERIOD, CURRENCY, 0, 1));
        phases.add(mockPricingPhase(INTRO_PERIOD, CURRENCY, INTRO_VALUE, INTRO_CYCLES));
        phases.add(mockPricingPhase(SUBS_PERIOD, CURRENCY, VALUE, 1));

        ProductDetails productDetails = mockSubscriptionProductDetails(ID, TITLE, DESC,
                "base_plan", "offer_id", phases);

        ItemDetails item = ItemDetails.create(ItemDetails.create(productDetails).toBundle());
        assertTestItemDetails(item);
    }

    @Test
    public void create_subscription_no_offers() {
        List<ProductDetails.PricingPhase> phases = new ArrayList<>();
        phases.add(mockPricingPhase(SUBS_PERIOD, CURRENCY, VALUE, 1));

        ProductDetails productDetails = mockSubscriptionProductDetails(ID, TITLE, DESC,
                "base_plan", null, phases);

        ItemDetails item = ItemDetails.create(productDetails);
        assertItemDetails(item, ID, TITLE, DESC, CURRENCY, VALUE_STR, "subs", "",
                SUBS_PERIOD, "", "", CURRENCY, "0.000000", 0);
    }

    @Test
    public void create_subscription_no_base_plans() {
        ProductDetails productDetails = mock(ProductDetails.class);
        when(productDetails.getProductId()).thenReturn(ID);
        when(productDetails.getTitle()).thenReturn(TITLE);
        when(productDetails.getDescription()).thenReturn(DESC);
        when(productDetails.getProductType()).thenReturn(BillingClient.ProductType.SUBS);
        when(productDetails.getSubscriptionOfferDetails()).thenReturn(null);

        ItemDetails item = ItemDetails.create(productDetails);
        assertItemDetails(item, ID, TITLE, DESC, "", "0.000000", "subs", "",
                "", "", "", "", "0.000000", 0);
    }

    public static void assertTestItemDetails(ItemDetails item) {
        assertItemDetails(item, ID, TITLE, DESC, CURRENCY, VALUE_STR, "subs", "",
                SUBS_PERIOD, FREE_PERIOD, INTRO_PERIOD, INTRO_CURRENCY, INTRO_VALUE_STR,
                INTRO_CYCLES);
    }

    public static void assertTestItemDetails_inApp(ItemDetails item) {
        assertItemDetails(item, ID, TITLE, DESC, CURRENCY, VALUE_STR, "inapp", "",
                "", "", "", CURRENCY, "0.000000", 0);
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

    public static ProductDetails mockInAppProductDetails(String id, String title, String description,
            String currency, long priceAmountMicros) {
        ProductDetails productDetails = mock(ProductDetails.class);
        when(productDetails.getProductId()).thenReturn(id);
        when(productDetails.getTitle()).thenReturn(title);
        when(productDetails.getDescription()).thenReturn(description);
        when(productDetails.getProductType()).thenReturn(BillingClient.ProductType.INAPP);

        ProductDetails.OneTimePurchaseOfferDetails oneTimeDetails = mock(ProductDetails.OneTimePurchaseOfferDetails.class);
        when(oneTimeDetails.getPriceCurrencyCode()).thenReturn(currency);
        when(oneTimeDetails.getPriceAmountMicros()).thenReturn(priceAmountMicros);
        when(productDetails.getOneTimePurchaseOfferDetails()).thenReturn(oneTimeDetails);

        return productDetails;
    }

    public static ProductDetails mockSubscriptionProductDetails(String id, String title, String description,
            String basePlanId, @Nullable String offerId, List<ProductDetails.PricingPhase> phases) {
        ProductDetails productDetails = mock(ProductDetails.class);
        when(productDetails.getProductId()).thenReturn(id);
        when(productDetails.getTitle()).thenReturn(title);
        when(productDetails.getDescription()).thenReturn(description);
        when(productDetails.getProductType()).thenReturn(BillingClient.ProductType.SUBS);

        ProductDetails.SubscriptionOfferDetails offerDetails = mock(ProductDetails.SubscriptionOfferDetails.class);
        when(offerDetails.getBasePlanId()).thenReturn(basePlanId);
        when(offerDetails.getOfferId()).thenReturn(offerId);
        if (offerId != null && !offerId.isEmpty()) {
            when(offerDetails.getOfferToken()).thenReturn("offer_token_for_" + offerId);
        }

        ProductDetails.PricingPhases pricingPhases = mock(ProductDetails.PricingPhases.class);
        when(pricingPhases.getPricingPhaseList()).thenReturn(phases);
        when(offerDetails.getPricingPhases()).thenReturn(pricingPhases);

        List<ProductDetails.SubscriptionOfferDetails> offerDetailsList = new ArrayList<>();
        offerDetailsList.add(offerDetails);
        when(productDetails.getSubscriptionOfferDetails()).thenReturn(offerDetailsList);

        return productDetails;
    }

    public static ProductDetails.PricingPhase mockPricingPhase(String billingPeriod, String priceCurrencyCode,
            long priceAmountMicros, int billingCycleCount) {
        ProductDetails.PricingPhase phase = mock(ProductDetails.PricingPhase.class);
        when(phase.getBillingPeriod()).thenReturn(billingPeriod);
        when(phase.getPriceCurrencyCode()).thenReturn(priceCurrencyCode);
        when(phase.getPriceAmountMicros()).thenReturn(priceAmountMicros);
        when(phase.getBillingCycleCount()).thenReturn(billingCycleCount);
        return phase;
    }
}
