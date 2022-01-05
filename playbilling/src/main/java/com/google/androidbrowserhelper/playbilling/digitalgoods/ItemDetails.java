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

import com.android.billingclient.api.SkuDetails;

/**
 * A data class representing an item (or SKU) from the Play Store.
 *
 * Its main purpose is to serialize {@link SkuDetails} into {@link Bundle}s in such a way that
 * Chromium can read it for the Digital Goods API. See:
 * https://source.chromium.org/chromium/chromium/src/+/master:chrome/android/java/src/org/chromium/chrome/browser/browserservices/digitalgoods/GetDetailsConverter.java;drc=a04f522e96fc0eaa0bbcb6eafa96d02aabe5452a
 */
public class ItemDetails {
    private static final String KEY_ID = "itemDetails.id";
    private static final String KEY_TITLE = "itemDetails.title";
    private static final String KEY_DESC = "itemDetails.description";
    private static final String KEY_CURRENCY = "itemDetails.currency";
    private static final String KEY_VALUE = "itemDetails.value";
    private static final String KEY_TYPE = "itemDetails.type";
    private static final String KEY_ICON_URL = "itemDetails.url";

    private static final String KEY_SUBS_PERIOD = "itemDetails.subsPeriod";
    private static final String KEY_FREE_TRIAL_PERIOD = "itemDetails.freeTrialPeriod";
    private static final String KEY_INTRO_PERIOD = "itemDetails.introPricePeriod";
    private static final String KEY_INTRO_CURRENCY = "itemDetails.introPriceCurrency";
    private static final String KEY_INTRO_VALUE = "itemDetails.introPriceValue";
    private static final String KEY_INTRO_CYCLES = "itemDetails.introPriceCycles";

    public final String id;
    public final String title;
    public final String description;
    public final String currency;
    public final String value;
    public final String type;
    public final String iconUrl;

    public final String subscriptionPeriod;
    public final String freeTrialPeriod;
    public final String introductoryPricePeriod;
    public final String introductoryPriceCurrency;
    public final String introductoryPriceValue;
    public final int introductoryPriceCycles;

    private ItemDetails(String id, String title, String description, String currency, String value,
                        String type, String iconUrl, String subscriptionPeriod,
                        String freeTrialPeriod, String introductoryPricePeriod,
                        String introductoryPriceCurrency, String introductoryPriceValue,
                        int introductoryPriceCycles) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.currency = currency;
        this.value = value;
        this.type = type;
        this.iconUrl = iconUrl;
        this.subscriptionPeriod = subscriptionPeriod;
        this.freeTrialPeriod = freeTrialPeriod;
        this.introductoryPricePeriod = introductoryPricePeriod;
        this.introductoryPriceCurrency = introductoryPriceCurrency;
        this.introductoryPriceValue = introductoryPriceValue;
        this.introductoryPriceCycles = introductoryPriceCycles;
    }

    /**
     * Creates this class from a Play Billing {@link SkuDetails}.
     */
    public static ItemDetails create(SkuDetails skuDetails) {
        return new ItemDetails(
                skuDetails.getSku(),
                skuDetails.getTitle(),
                skuDetails.getDescription(),
                skuDetails.getPriceCurrencyCode(),
                toPrice(skuDetails.getPriceAmountMicros()),
                skuDetails.getType(), skuDetails.getIconUrl(), skuDetails.getSubscriptionPeriod(),
                skuDetails.getFreeTrialPeriod(),
                skuDetails.getIntroductoryPricePeriod(),
                skuDetails.getPriceCurrencyCode(),
                toPrice(skuDetails.getIntroductoryPriceAmountMicros()),
                skuDetails.getIntroductoryPriceCycles());
    }

    /**
     * Creates this class from a {@link Bundle} previously created by {@link #toBundle};
     */
    public static ItemDetails create(Bundle bundle) {
        String id = bundle.getString(KEY_ID);
        String title = bundle.getString(KEY_TITLE);
        String description = bundle.getString(KEY_DESC);
        String currency = bundle.getString(KEY_CURRENCY);
        String value = bundle.getString(KEY_VALUE);
        String type = bundle.getString(KEY_TYPE);
        String iconUrl = bundle.getString(KEY_ICON_URL);

        String subscriptionPeriod = bundle.getString(KEY_SUBS_PERIOD);
        String freeTrialPeriod = bundle.getString(KEY_FREE_TRIAL_PERIOD);
        String introductoryPricePeriod = bundle.getString(KEY_INTRO_PERIOD);
        String introductoryPriceCurrency = bundle.getString(KEY_INTRO_CURRENCY);
        String introductoryPriceValue = bundle.getString(KEY_INTRO_VALUE);
        int introductoryPriceCycles = bundle.getInt(KEY_INTRO_CYCLES);

        return new ItemDetails(id, title, description, currency, value, type, iconUrl,
                subscriptionPeriod, freeTrialPeriod, introductoryPricePeriod,
                introductoryPriceCurrency, introductoryPriceValue, introductoryPriceCycles);
    }

    /**
     * Serializes this object to a {@link Bundle} in such a way that Chromium can read it
     * (see class javadoc).
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();

        bundle.putString(KEY_ID, id);
        bundle.putString(KEY_TITLE, title);
        bundle.putString(KEY_DESC, description);
        bundle.putString(KEY_CURRENCY, currency);
        bundle.putString(KEY_VALUE, value);
        bundle.putString(KEY_TYPE, type);
        bundle.putString(KEY_ICON_URL, iconUrl);

        bundle.putString(KEY_SUBS_PERIOD, subscriptionPeriod);
        bundle.putString(KEY_FREE_TRIAL_PERIOD, freeTrialPeriod);
        bundle.putString(KEY_INTRO_PERIOD, introductoryPricePeriod);
        bundle.putString(KEY_INTRO_CURRENCY, introductoryPriceCurrency);
        bundle.putString(KEY_INTRO_VALUE, introductoryPriceValue);
        bundle.putInt(KEY_INTRO_CYCLES, introductoryPriceCycles);

        return bundle;
    }

    /**
     * Takes a price amount in micro units (1,000,000 micro units = 1 unit) and converts it to a
     * String representing the price amount in units.
     *
     * The reason we need this method is because {@link SkuDetails} provides either a price amount
     * in units *with* the currency symbol, or a price amount in micro units (without the currency
     * symbol) while we need a price without the currency symbol.
     *
     * We have three options:
     * 1. Manually remove the currency symbol from the price.
     * 2. Use numeric division to convert the price in micros to a price in units.
     * 3. Perform string division by converting the price in micros to a string and moving the
     *    decimal point around.
     *
     * Option 1 seems quite error prone and option 2 could lead to rounding errors, so we chose
     * option 3.
     *
     * This may produce an ugly looking number, eg turning "£7.50" into "7.500000" but the website
     * receiving the value can use
     * <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat">
     * Intl.NumberFormat</a> to display the price nicely.
     */
    static String toPrice(long priceAmountMicros) {
        StringBuilder sb = new StringBuilder(String.valueOf(priceAmountMicros));

        // We want to perform a "string division" by inserting a decimal point 6 digits from the
        // end. We may have to pad the string with leading zeros so that there is space to do this.

        // If the number is positive, we need the string to be at least 7 characters long, so that
        // there will be a digit to the left of the decimal point. Negative numbers need to be 8
        // characters long to account for the minus sign.
        int desiredLength = priceAmountMicros >= 0 ? 7 : 8;

        // For positive numbers we can insert zeros at the start, for negative numbers we must
        // insert them after the minus sign.
        int insertionIndex = priceAmountMicros >= 0 ? 0 : 1;

        // Add leading zeros until we have the right amount of characters.
        //      "200" ->  "0000200"
        //     "-200" -> "-0000200"
        // "90000000" -> "90000000"
        while (sb.length() < desiredLength) sb.insert(insertionIndex, "0");

        // Perform a "string division" by 1,000,000 by inserting a decimal point 6 characters from
        // the end.
        //  "0000200" ->  "0.000200"
        // "-0000200" -> "-0.000200"
        // "90000000" -> "90.000000"
        sb.insert(sb.length() - 6, ".");

        return sb.toString();
    }
}
