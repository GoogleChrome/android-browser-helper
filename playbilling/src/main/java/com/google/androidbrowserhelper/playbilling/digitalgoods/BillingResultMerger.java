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

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.ProductType;
import com.android.billingclient.api.BillingClient.SkuType;
import com.android.billingclient.api.BillingResult;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * Play Billing SKUs are split into purchases and subscriptions. This isn't a distinction that
 * exists in the Digital Goods API, but since SKU ids are unique across both product types, we can
 * just call methods that take a ProductType twice, once with {@link ProductType#INAPP} and once with
 * {@link ProductType#SUBS}. This class exists to combine the results of those calls.
 *
 * Although the Play Billing methods are asynchronous, their callbacks (which will call
 * {@link #setInAppResult} and {@link #setSubsResult}) should both take place on the UI thread, so
 * we don't need to worry about any concurrency.
 */
public class BillingResultMerger<T> {
    private final ResultListener<T> mOnCombinedResult;

    private @Nullable BillingResult mInAppResult;
    private @Nullable List<T> mInAppResultsList;
    private @Nullable BillingResult mSubsResult;
    private @Nullable List<T> mSubsResultsList;

    public interface ResultListener<T> {
        void onResult(BillingResult responseCode, @Nullable List<T> combinedResult);
    }

    public BillingResultMerger(ResultListener<T> onCombinedResult) {
        mOnCombinedResult = onCombinedResult;
    }

    public void setInAppResult(BillingResult result, @Nullable List<T> resultsList) {
        mInAppResult = result;
        mInAppResultsList = resultsList;

        triggerIfReady();
    }

    public void setSubsResult(BillingResult result, @Nullable List<T> resultsList) {
        mSubsResult = result;
        mSubsResultsList = resultsList;

        triggerIfReady();
    }

    private void triggerIfReady() {
        if (mSubsResult == null || mInAppResult == null) return;

        BillingResult result;
        @Nullable List<T> detailsList;
        if (mSubsResultsList == null || mSubsResultsList.isEmpty()) {
            // If one of the lists is null or empty, just use the results from the other call.
            result = mInAppResult;
            detailsList = mInAppResultsList;
        } else if (mInAppResultsList == null || mInAppResultsList.isEmpty()) {
            result = mSubsResult;
            detailsList = mSubsResultsList;
        } else {
            // To merge the BillingResult:
            // - If one call failed, pick that.
            // - Arbitrarily return the INAPP result otherwise.
            if (didSucceed(mInAppResult) && !didSucceed(mSubsResult)) {
                result = mSubsResult;
            } else {
                result = mInAppResult;
            }

            // To merge the lists, we just combine them.
            // Since we're in this branch, we know the two lists are non-null and non-empty.
            detailsList = new ArrayList<>(mInAppResultsList);
            detailsList.addAll(mSubsResultsList);
        }

        mOnCombinedResult.onResult(result, detailsList);
    }

    private static boolean didSucceed(BillingResult result) {
        return result.getResponseCode() == BillingClient.BillingResponseCode.OK;
    }
}
