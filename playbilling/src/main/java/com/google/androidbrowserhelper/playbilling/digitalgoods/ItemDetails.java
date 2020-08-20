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
                skuDetails.getPrice());
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
}
