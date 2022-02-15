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

import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingResult;
import com.google.androidbrowserhelper.playbilling.provider.BillingWrapper;

/**
 * A class for parsing Digital Goods API calls from the browser and converting them into a format
 * suitable for calling the Play Billing library.
 */
public class ConsumeCall {
    public static final String COMMAND_NAME = "consume";

    public static final String PARAM_CONSUME_PURCHASE_TOKEN = "consume.purchaseToken";
    public static final String RESPONSE_CONSUME = "consume.response";
    public static final String RESPONSE_CONSUME_RESPONSE_CODE = "consume.responseCode";

    public final String purchaseToken;
    private final DigitalGoodsCallback mCallback;

    public ConsumeCall(String purchaseToken, DigitalGoodsCallback callback) {
        this.purchaseToken = purchaseToken;
        this.mCallback = callback;
    }

    /** Creates this class from a {@link Bundle}, returns {@code null} if the Bundle is invalid. **/
    @Nullable
    public static ConsumeCall create(@Nullable Bundle args,
                                     @Nullable DigitalGoodsCallback callback) {
        if (args == null || callback == null) return null;

        if (!args.containsKey(PARAM_CONSUME_PURCHASE_TOKEN)) return null;

        String token = args.getString(PARAM_CONSUME_PURCHASE_TOKEN);

        return new ConsumeCall(token, callback);
    }

    /** Calls the callback provided in the constructor with serialized forms of the parameters. */
    private void respond(BillingResult result) {
        Logging.logConsumeResponse(result);

        Bundle args = new Bundle();
        args.putInt(RESPONSE_CONSUME_RESPONSE_CODE,
                DigitalGoodsConverter.toChromiumResponseCode(result));
        mCallback.run(RESPONSE_CONSUME, args);
    }

    /** Calls the appropriate method on {@link BillingWrapper}. */
    public void call(BillingWrapper billing) {
        Logging.logConsumeCall(purchaseToken);

        billing.consume(purchaseToken, (result, token) -> respond(result));
    }

    /** Creates a bundle that can be used with {@link #create}. For testing. */
    static Bundle createBundleForTesting(String token) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_CONSUME_PURCHASE_TOKEN, token);
        return bundle;
    }
}
