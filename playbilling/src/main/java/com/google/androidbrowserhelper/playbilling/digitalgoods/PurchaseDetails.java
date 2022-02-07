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

import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryRecord;

/**
 * A data class representing a purchase from the Play Store.
 *
 * Its main purpose is to serialize {@link Purchase} into {@link Bundle}s in such a way that
 * Chromium can read it for the Digital Goods API. See:
 * https://source.chromium.org/chromium/chromium/src/+/master:chrome/android/java/src/org/chromium/chrome/browser/browserservices/digitalgoods/ListPurchasesConverter.java;drc=a04f522e96fc0eaa0bbcb6eafa96d02aabe5452a
 */
public class PurchaseDetails {
    static final String KEY_ITEM_ID = "purchaseDetails.itemId";
    static final String KEY_PURCHASE_TOKEN = "purchaseDetails.purchaseToken";

    /**
     * This is the id according to Chromium, which corresponds to {@link Purchase#getSku}, not to
     * {@link Purchase#getOrderId}.
     */
    public final String id;
    public final String purchaseToken;

    protected PurchaseDetails(String id, String purchaseToken) {
        this.id = id;
        this.purchaseToken = purchaseToken;
    }

    /**
     * Creates this class from a Play Billing {@link Purchase}.
     */
    public static PurchaseDetails create(Purchase purchase) {
        return new PurchaseDetails(purchase.getSkus().get(0), purchase.getPurchaseToken());
    }

    /**
     * Creates this class from a Play Billing {@link PurchaseHistoryRecord}.
     */
    public static PurchaseDetails create(PurchaseHistoryRecord record) {
        return new PurchaseDetails(record.getSkus().get(0), record.getPurchaseToken());
    }

    /**
     * Creates this class from a {@link Bundle} previously created by {@link #toBundle}.
     */
    public static PurchaseDetails create(Bundle bundle) {
        String id = bundle.getString(KEY_ITEM_ID);
        String token = bundle.getString(KEY_PURCHASE_TOKEN);
        return new PurchaseDetails(id, token);
    }

    /**
     * Serializes this object to a {@link Bundle} for sending to Chromium.
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();

        bundle.putString(KEY_ITEM_ID, id);
        bundle.putString(KEY_PURCHASE_TOKEN, purchaseToken);

        return bundle;
    }
}
