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

private const val CLASS_NAME = "SkuDetailsParams"
private const val SKU_TYPE = "sku_type"
private const val SKU_LIST = "sku_list"

/** Encodes the SkuDetailsParams into a Bundle. */
internal fun SkuDetailsParams.toBundle() = makeBundle(CLASS_NAME) {
    put(SKU_TYPE, skuType)
    put(SKU_LIST, skusList)
}

/** Reconstructs a SkuDetailsParams from a Bundle created by toBundle. */
internal fun Bundle.toSkuDetailsParams(): SkuDetailsParams {
    checkBundle(this, CLASS_NAME)

    var builder = SkuDetailsParams.newBuilder()

    builder.setType(getString(SKU_TYPE))
    builder.setSkusList(getStringArray(SKU_LIST)?.toList())

    return builder.build()
}
