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

package com.google.androidbrowserhelper.playbilling.bundles

import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import com.android.billingclient.api.SkuDetailsResponseListener
import com.google.androidbrowserhelper.playbilling.ICallback

private const val CLASS_NAME = "SkuDetailsResponseListener"
private const val BILLING_RESULT_KEY = "billing_result"
private const val SKU_DETAILS_LIST = "sku_details_list"

/**
 * Turns a local SkuDetailsResponseListener into a Binder that can be passed over to another app
 * and passed to binderToListener.
 */
internal fun SkuDetailsResponseListener.toBinder(): Binder {
    return object : ICallback.Stub() {
        override fun onResult(bundle: Bundle) {
            val billingResult = bundle.getBundle(BILLING_RESULT_KEY)!!.toBillingResult()
            val skuDetailsList = bundle.getParcelableArray(SKU_DETAILS_LIST)!!.toSkuDetailsList()

            onSkuDetailsResponse(billingResult, skuDetailsList)
        }
    }
}

/**
 * Takes a binder created from toBinder and wraps it in a SkuDetailsResponseListener.
 */
internal fun binderToListener(binder: IBinder): SkuDetailsResponseListener {
    val callback = ICallback.Stub.asInterface(binder)

    return SkuDetailsResponseListener { billingResult, skuDetails ->
        val argsBundle = makeBundle(CLASS_NAME) {
            put(BILLING_RESULT_KEY, billingResult.toBundle())
            put(SKU_DETAILS_LIST, skuDetails!!.toParcelableArray())
        }

        callback.onResult(argsBundle)
    }
}
