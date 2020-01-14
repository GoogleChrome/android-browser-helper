package com.google.androidbrowserhelper.playbilling.bundles

import android.os.Bundle
import com.android.billingclient.api.BillingResult

private const val CLASS_NAME = "BillingResult"
private const val DEBUG_MESSAGE_KEY = "debug_message"
private const val RESPONSE_CODE_KEY = "response_code"

/** Encodes the BillingResult into a Bundle. */
fun BillingResult.toBundle() = makeBundle(CLASS_NAME) {
    put(DEBUG_MESSAGE_KEY, debugMessage)
    put(RESPONSE_CODE_KEY, responseCode)
}

/** Reconstructs a BillingResult from a Bundle created by toBundle. */
fun Bundle.toBillingResult(): BillingResult {
    checkBundle(this, CLASS_NAME)

    val builder = BillingResult.newBuilder()

    builder.setDebugMessage(getString(DEBUG_MESSAGE_KEY))
    builder.setResponseCode(getInt(RESPONSE_CODE_KEY))

    return builder.build()
}