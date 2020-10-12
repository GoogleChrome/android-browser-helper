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
 * https://source.chromium.org/chromium/chromium/src/+/master:chrome/android/java/src/org/chromium/chrome/browser/browserservices/digitalgoods/DigitalGoodsConverter.java;drc=a9e30a32540072b3b33d94435a42bef974b13a95
 */
public class ItemDetails {
    private static final String ITEM_DETAILS_ID = "itemDetails.id";
    private static final String ITEM_DETAILS_TITLE = "itemDetails.title";
    private static final String ITEM_DETAILS_DESC = "itemDetails.description";
    private static final String ITEM_DETAILS_CURRENCY = "itemDetails.currency";
    private static final String ITEM_DETAILS_VALUE = "itemDetails.value";

    public final String id;
    public final String title;
    public final String description;
    public final String currency;
    public final String value;

    private ItemDetails(String id, String title, String description, String currency, String value) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.currency = currency;
        this.value = value;
    }

    /**
     * Creates this class from a PlayBilling {@link SkuDetails}.
     */
    public static ItemDetails create(SkuDetails skuDetails) {
        return new ItemDetails(skuDetails.getSku(), skuDetails.getTitle(),
                skuDetails.getDescription(), skuDetails.getPriceCurrencyCode(),
                toPrice(skuDetails.getPriceAmountMicros()));
    }

    /**
     * Creates this class from a {@link Bundle} previously created by {@link #toBundle};
     */
    public static ItemDetails create(Bundle bundle) {
        String id = bundle.getString(ITEM_DETAILS_ID);
        String title = bundle.getString(ITEM_DETAILS_TITLE);
        String description = bundle.getString(ITEM_DETAILS_DESC);
        String currency = bundle.getString(ITEM_DETAILS_CURRENCY);
        String value = bundle.getString(ITEM_DETAILS_VALUE);
        return new ItemDetails(id, title, description, currency, value);
    }

    /**
     * Serializes this object to a {@link Bundle} in such a way that Chromium can read it
     * (see class javadoc).
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();

        bundle.putString(ITEM_DETAILS_ID, id);
        bundle.putString(ITEM_DETAILS_TITLE, title);
        bundle.putString(ITEM_DETAILS_DESC, description);
        bundle.putString(ITEM_DETAILS_CURRENCY, currency);
        bundle.putString(ITEM_DETAILS_VALUE, value);

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
     * on the other can use Intl.NumberFormat to display the value nicely.
     */
    static String toPrice(long priceAmountMicros) {
        StringBuilder sb = new StringBuilder(String.valueOf(priceAmountMicros));

        // We want to perform a "string division" by inserting a decimal point 6 digits from the
        // end. We may have to pad the string with leading zeros so that there is space to do this.

        // If the number is positive, we need the string to be at least 7 characters long, so that
        // there will be a digit to the left of the decimal point. Negative numbers need to be 8
        // characters long to account for the minus sign.
        int desiredLength = priceAmountMicros >= 0 ? 7 : 8;

        // For positive numbers, we can insert zeros at the start, for negative numbers we must
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
