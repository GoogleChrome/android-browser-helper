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
import android.os.Parcelable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.SkuDetails;
import com.google.androidbrowserhelper.playbilling.provider.BillingWrapper;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;

import static com.google.androidbrowserhelper.playbilling.digitalgoods.DigitalGoodsConverter.toChromiumResponseCode;

/**
 * A class for parsing Digital Goods API calls from the browser and converting them into a format
 * suitable for calling the Play Billing library.
 */
public class GetDetailsCall {
    public static final String COMMAND_NAME = "getDetails";

    static final String PARAM_GET_DETAILS_ITEM_IDS = "getDetails.itemIds";
    static final String RESPONSE_GET_DETAILS = "getDetails.response";
    static final String RESPONSE_GET_DETAILS_RESPONSE_CODE = "getDetails.responseCode";
    static final String RESPONSE_GET_DETAILS_DETAILS_LIST = "getDetails.detailsList";

    private final List<String> mItemIds;
    private final DigitalGoodsCallback mCallback;

    private GetDetailsCall(List<String> itemIds, DigitalGoodsCallback callback) {
        mItemIds = itemIds;
        mCallback = callback;
    }

    /** Creates this class from a {@link Bundle}, returns {@code null} if the Bundle is invalid. **/
    @Nullable
    public static GetDetailsCall create(@Nullable Bundle args,
            @Nullable DigitalGoodsCallback callback) {
        if (args == null || callback == null) return null;

        String[] itemIds = args.getStringArray(PARAM_GET_DETAILS_ITEM_IDS);
        if (itemIds == null) return null;

        return new GetDetailsCall(Arrays.asList(itemIds), callback);
    }

    /** Calls the callback provided in the constructor with serialized forms of the parameters. */
    private void respond(BillingResult result, @Nullable List<SkuDetails> detailsList) {
        Logging.logGetDetailsResponse(result);

        Parcelable[] parcelables = new Parcelable[0];
        if (detailsList != null) {
            parcelables = new Parcelable[detailsList.size()];

            int index = 0;
            for (SkuDetails details : detailsList) {
                parcelables[index++] = ItemDetails.create(details).toBundle();
            }
        }

        Bundle args = new Bundle();
        args.putInt(RESPONSE_GET_DETAILS_RESPONSE_CODE,
                DigitalGoodsConverter.toChromiumResponseCode(result));
        args.putParcelableArray(RESPONSE_GET_DETAILS_DETAILS_LIST, parcelables);
        mCallback.run(RESPONSE_GET_DETAILS, args);
    }

    /** Calls the appropriate method on {@link BillingWrapper}. */
    public void call(BillingWrapper billing) {
        Logging.logGetDetailsCall(mItemIds);

        BillingResultMerger<SkuDetails> merger = new BillingResultMerger<>(this::respond);

        billing.querySkuDetails(BillingClient.SkuType.INAPP, mItemIds, merger::setInAppResult);
        billing.querySkuDetails(BillingClient.SkuType.SUBS, mItemIds, merger::setSubsResult);
    }

    /** Creates a Bundle that can be used with {@link #create}. For testing. */
    static Bundle createBundleForTesting(String... itemIds) {
        Bundle bundle = new Bundle();
        bundle.putStringArray(PARAM_GET_DETAILS_ITEM_IDS, itemIds);
        return bundle;
    }
}
