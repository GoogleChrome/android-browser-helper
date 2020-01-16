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

import android.os.Bundle
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener

private const val CLASS_NAME = "QuerySkuDetailsCall"
private const val PARAMS_KEY = "params"
private const val CALLBACK_KEY = "callback"

/** Holds the parameters and result callback for a call to PlayBilling#querySkuDetailsAsync. */
class QuerySkuDetailsCall(
        val params: SkuDetailsParams,
        val listener: SkuDetailsResponseListener) {

    fun toBundle() = makeBundle(CLASS_NAME) {
        putBundle(PARAMS_KEY, params.toBundle())
        putBinder(CALLBACK_KEY, listener.toBinder())
    }

    companion object {
        @JvmStatic
        fun fromBundle(bundle: Bundle): QuerySkuDetailsCall {
            checkBundle(bundle, CLASS_NAME)

            return QuerySkuDetailsCall(
                    bundle.getBundle(PARAMS_KEY)!!.toSkuDetailsParams(),
                    binderToListener(bundle.getBinder(CALLBACK_KEY)!!)
            )
        }
    }
}
