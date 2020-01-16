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
import com.android.billingclient.api.BillingResult

private const val CLASS_NAME = "BillingResult"
private const val DEBUG_MESSAGE_KEY = "debug_message"
private const val RESPONSE_CODE_KEY = "response_code"

/** Encodes the BillingResult into a Bundle. */
internal fun BillingResult.toBundle() = makeBundle(CLASS_NAME) {
    put(DEBUG_MESSAGE_KEY, debugMessage)
    put(RESPONSE_CODE_KEY, responseCode)
}

/** Reconstructs a BillingResult from a Bundle created by toBundle. */
internal fun Bundle.toBillingResult(): BillingResult {
    checkBundle(this, CLASS_NAME)

    val builder = BillingResult.newBuilder()

    builder.setDebugMessage(getString(DEBUG_MESSAGE_KEY))
    builder.setResponseCode(getInt(RESPONSE_CODE_KEY))

    return builder.build()
}
