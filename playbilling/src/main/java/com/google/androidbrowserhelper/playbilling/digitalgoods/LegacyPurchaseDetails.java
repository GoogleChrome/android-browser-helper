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

/**
 * An old version of {@link PurchaseDetails} that was used for
 * {@link DigitalGoodsRequestHandler#DIGITAL_GOODS_API_VERSION} 1. This is kept around so that newer
 * TWA shells will still work with older versions of Chrome, but will eventually be deleted.
 */
public class LegacyPurchaseDetails extends PurchaseDetails {
    private static final String KEY_ACKNOWLEDGED = "purchaseDetails.acknowledged";
    private static final String KEY_PURCHASE_STATE = "purchaseDetails.purchaseState";
    private static final String KEY_PURCHASE_TIME_MICROSECONDS_PAST_UNIX_EPOCH =
            "purchaseDetails.purchaseTimeMicrosecondsPastUnixEpoch";
    private static final String KEY_WILL_AUTO_RENEW = "purchaseDetails.willAutoRenew";

    static final int CHROMIUM_PURCHASE_STATE_UNKNOWN = 0;
    static final int CHROMIUM_PURCHASE_STATE_PURCHASED = 1;
    static final int CHROMIUM_PURCHASE_STATE_PENDING = 2;

    public final boolean acknowledged;
    public final int purchaseState;
    public final long purchaseTimeMicrosecondsPastUnixEpoch;
    public final boolean willAutoRenew;

    private LegacyPurchaseDetails(String id, String purchaseToken, boolean acknowledged,
            int purchaseState, long purchaseTimeMicrosecondsPastUnixEpoch, boolean willAutoRenew) {
        super(id, purchaseToken);
        this.acknowledged = acknowledged;
        this.purchaseState = purchaseState;
        this.purchaseTimeMicrosecondsPastUnixEpoch = purchaseTimeMicrosecondsPastUnixEpoch;
        this.willAutoRenew = willAutoRenew;
    }

    /**
     * Creates this class from a Play Billing {@link Purchase}.
     */
    public static LegacyPurchaseDetails create(Purchase purchase) {
        return new LegacyPurchaseDetails(
                purchase.getSkus().get(0),
                purchase.getPurchaseToken(),
                purchase.isAcknowledged(),
                toChromiumPurchaseState(purchase.getPurchaseState()),
                millisecondsToMicroseconds(purchase.getPurchaseTime()),
                purchase.isAutoRenewing()
        );
    }

    /**
     * Creates this class from a {@link Bundle} previously created by {@link #toBundle}.
     */
    public static LegacyPurchaseDetails create(Bundle bundle) {
        String id = bundle.getString(PurchaseDetails.KEY_ITEM_ID);
        String token = bundle.getString(PurchaseDetails.KEY_PURCHASE_TOKEN);
        boolean acknowledged = bundle.getBoolean(KEY_ACKNOWLEDGED);
        int state = bundle.getInt(KEY_PURCHASE_STATE);
        long timeMsPastUnixEpoch = bundle.getLong(KEY_PURCHASE_TIME_MICROSECONDS_PAST_UNIX_EPOCH);
        boolean willAutoRenew = bundle.getBoolean(KEY_WILL_AUTO_RENEW);
        return new LegacyPurchaseDetails(id, token, acknowledged, state, timeMsPastUnixEpoch,
                willAutoRenew);
    }

    /**
     * Serializes this object to a {@link Bundle} for sending to Chromium.
     */
    public Bundle toBundle() {
        Bundle bundle = super.toBundle();

        bundle.putBoolean(KEY_ACKNOWLEDGED, acknowledged);
        bundle.putInt(KEY_PURCHASE_STATE, purchaseState);
        bundle.putLong(KEY_PURCHASE_TIME_MICROSECONDS_PAST_UNIX_EPOCH,
                purchaseTimeMicrosecondsPastUnixEpoch);
        bundle.putBoolean(KEY_WILL_AUTO_RENEW, willAutoRenew);

        return bundle;
    }

    private static long millisecondsToMicroseconds(long milliseconds) {
        return milliseconds * 1000;
    }

    private static int toChromiumPurchaseState(@Purchase.PurchaseState int purchaseState) {
        switch (purchaseState) {
            case Purchase.PurchaseState.PENDING:
                return CHROMIUM_PURCHASE_STATE_PENDING;
            case Purchase.PurchaseState.PURCHASED:
                return CHROMIUM_PURCHASE_STATE_PURCHASED;
            default:
                return CHROMIUM_PURCHASE_STATE_UNKNOWN;
        }
    }
}
