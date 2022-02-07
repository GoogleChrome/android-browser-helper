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
import android.os.Bundle;
import android.os.Parcelable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.google.androidbrowserhelper.playbilling.provider.BillingWrapper;

import java.util.List;

import androidx.annotation.Nullable;

import static com.google.androidbrowserhelper.playbilling.digitalgoods.DigitalGoodsConverter.toChromiumResponseCode;

/**
 * A class for parsing Digital Goods API calls from the browser and converting them into a format
 * suitable for calling the Play Billing library.
 */
public class ListPurchasesCall {
    public static final String COMMAND_NAME = "listPurchases";

    static final String RESPONSE_COMMAND = "listPurchases.response";
    static final String KEY_PURCHASES_LIST = "listPurchases.purchasesList";
    static final String KEY_RESPONSE_CODE = "listPurchases.responseCode";

    private final DigitalGoodsCallback mCallback;

    private ListPurchasesCall(DigitalGoodsCallback callback) {
        mCallback = callback;
    }

    /** Creates this class from a {@link Bundle}. */
    @Nullable
    public static ListPurchasesCall create(@Nullable DigitalGoodsCallback callback) {
        if (callback == null) return null;

        return new ListPurchasesCall(callback);
    }

    /* Calls the appropriate method on {@link BillingWrapper}. */
    public void call(BillingWrapper billing) {
        Logging.logListPurchasesCall();

        BillingResultMerger<Purchase> merger = new BillingResultMerger<>(this::respond);

        billing.queryPurchases(BillingClient.SkuType.INAPP, merger::setInAppResult);
        billing.queryPurchases(BillingClient.SkuType.SUBS, merger::setSubsResult);
    }

    private void respond(BillingResult result, @Nullable List<Purchase> purchaseList) {
        Logging.logListPurchasesResult(result);

        Parcelable[] parcelables = new Parcelable[0];
        if (purchaseList != null) {
            parcelables = new Parcelable[purchaseList.size()];

            int index = 0;
            for (Purchase purchase : purchaseList) {
                parcelables[index++] = LegacyPurchaseDetails.create(purchase).toBundle();
            }
        }

        Bundle args = new Bundle();
        args.putInt(KEY_RESPONSE_CODE, DigitalGoodsConverter.toChromiumResponseCode(result));
        args.putParcelableArray(KEY_PURCHASES_LIST, parcelables);
        mCallback.run(RESPONSE_COMMAND, args);
    }
}
