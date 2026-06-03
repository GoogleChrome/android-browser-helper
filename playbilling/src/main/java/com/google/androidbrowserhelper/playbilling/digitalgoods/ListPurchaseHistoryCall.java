// Copyright 2022 Google Inc. All Rights Reserved.
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
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.androidbrowserhelper.playbilling.provider.BillingWrapper;

/**
 * A class for parsing Digital Goods API calls from the browser and converting them into a format
 * suitable for calling the Play Billing library.
 */
public class ListPurchaseHistoryCall {
    public static final String COMMAND_NAME = "listPurchaseHistory";

    static final String RESPONSE_COMMAND = "listPurchaseHistory.response";
    static final String KEY_PURCHASE_HISTORY_LIST = "listPurchaseHistory.purchasesList";
    static final String KEY_RESPONSE_CODE = "listPurchaseHistory.responseCode";

    private final DigitalGoodsCallback mCallback;

    private ListPurchaseHistoryCall(DigitalGoodsCallback callback) {
        mCallback = callback;
    }

    /** Creates this class from a {@link DigitalGoodsCallback}. */
    @Nullable
    public static ListPurchaseHistoryCall create(@Nullable DigitalGoodsCallback callback) {
        if (callback == null) return null;

        return new ListPurchaseHistoryCall(callback);
    }

    /** Calls the appropriate method on {@link BillingWrapper}. */
    public void call(BillingWrapper billing) {
        Logging.logListPurchaseHistoryCall();

        Bundle args = new Bundle();
        args.putInt(KEY_RESPONSE_CODE, DigitalGoodsConverter.CHROMIUM_RESULT_ERROR);
        args.putParcelableArray(KEY_PURCHASE_HISTORY_LIST, new Parcelable[0]);
        mCallback.run(RESPONSE_COMMAND, args);
    }
}