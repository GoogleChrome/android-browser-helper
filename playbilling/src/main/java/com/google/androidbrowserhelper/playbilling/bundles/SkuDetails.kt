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
import android.os.Parcelable
import com.android.billingclient.api.SkuDetails

private const val CLASS_NAME = "SkuDetails"
private const val ORIGINAL_JSON_KEY = "original_json"

/** Encodes the SkuDetails into a Bundle. */
internal fun SkuDetails.toBundle() = makeBundle(CLASS_NAME) {
    put(ORIGINAL_JSON_KEY, originalJson)
}

/** Reconstructs a SkuDetails from a Bundle created by toBundle. */
internal fun Bundle.toSkuDetails(): SkuDetails {
    checkBundle(this, CLASS_NAME)

    return SkuDetails(getString(ORIGINAL_JSON_KEY))
}

internal fun MutableList<SkuDetails>.toParcelableArray() =
    listToParcelableArray(this, SkuDetails::toBundle)

// TODO(peconn): Get rid of the ugly cast.
internal fun Array<Parcelable>.toSkuDetailsList(): MutableList<SkuDetails> =
    map { (it as Bundle).toSkuDetails() }.toMutableList()
