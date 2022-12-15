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

import com.android.billingclient.api.BillingResult;
import com.google.androidbrowserhelper.playbilling.provider.BillingWrapper;

import androidx.annotation.Nullable;

import static com.google.androidbrowserhelper.playbilling.digitalgoods.DigitalGoodsConverter.toChromiumResponseCode;

/**
 * A class for parsing Digital Goods API calls from the browser and converting them into a format
 * suitable for calling the Play Billing library.
 *
 * This class is kept around for legacy reasons, Consume should be used by clients on DGAPI v2.1.
 */
public class AcknowledgeCall {
    public static final String COMMAND_NAME = "acknowledge";

    private static final String PARAM_ACKNOWLEDGE_PURCHASE_TOKEN = "acknowledge.purchaseToken";
    private static final String PARAM_ACKNOWLEDGE_MAKE_AVAILABLE_AGAIN =
            "acknowledge.makeAvailableAgain";
    static final String RESPONSE_ACKNOWLEDGE = "acknowledge.response";
    static final String RESPONSE_ACKNOWLEDGE_RESPONSE_CODE = "acknowledge.responseCode";

    public final String purchaseToken;
    public final boolean makeAvailableAgain;
    private final DigitalGoodsCallback mCallback;

    private AcknowledgeCall(String purchaseToken, boolean makeAvailableAgain,
            DigitalGoodsCallback mCallback) {
        this.purchaseToken = purchaseToken;
        this.makeAvailableAgain = makeAvailableAgain;
        this.mCallback = mCallback;
    }

    /** Creates this class from a {@link Bundle}, returns {@code null} if the Bundle is invalid. **/
    @Nullable
    public static AcknowledgeCall create(@Nullable Bundle args,
            @Nullable DigitalGoodsCallback callback) {
        if (args == null || callback == null) return null;

        if (!args.containsKey(PARAM_ACKNOWLEDGE_PURCHASE_TOKEN) ||
                !args.containsKey(PARAM_ACKNOWLEDGE_MAKE_AVAILABLE_AGAIN)) {
            return null;
        }

        String token = args.getString(PARAM_ACKNOWLEDGE_PURCHASE_TOKEN);
        boolean makeAvailableAgain = args.getBoolean(PARAM_ACKNOWLEDGE_MAKE_AVAILABLE_AGAIN);

        return new AcknowledgeCall(token, makeAvailableAgain, callback);
    }

    /** Calls the callback provided in the constructor with serialized forms of the parameters. */
    private void respond(BillingResult result) {
        Logging.logAckResponse(result, makeAvailableAgain);

        Bundle args = new Bundle();
        args.putInt(RESPONSE_ACKNOWLEDGE_RESPONSE_CODE,
                DigitalGoodsConverter.toChromiumResponseCode(result));
        mCallback.run(RESPONSE_ACKNOWLEDGE, args);
    }

    /** Calls the appropriate method on {@link BillingWrapper}. */
    public void call(BillingWrapper billing) {
        Logging.logAckCall(purchaseToken, makeAvailableAgain);

        if (makeAvailableAgain) {
            billing.consume(purchaseToken, (result, token) -> respond(result));
        } else {
            billing.acknowledge(purchaseToken, this::respond);
        }
    }

    /** Creates a bundle that can be used with {@link #create}. For testing. */
    static Bundle createBundleForTesting(String token, boolean makeAvailableAgain) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_ACKNOWLEDGE_PURCHASE_TOKEN, token);
        bundle.putBoolean(PARAM_ACKNOWLEDGE_MAKE_AVAILABLE_AGAIN, makeAvailableAgain);
        return bundle;
    }
}
