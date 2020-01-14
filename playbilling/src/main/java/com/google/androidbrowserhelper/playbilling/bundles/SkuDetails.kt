package com.google.androidbrowserhelper.playbilling.bundles

import android.os.Bundle
import android.os.Parcelable
import com.android.billingclient.api.SkuDetails

private const val CLASS_NAME = "SkuDetails"
private const val ORIGINAL_JSON_KEY = "original_json"

/** Encodes the SkuDetails into a Bundle. */
fun SkuDetails.toBundle() = makeBundle(CLASS_NAME) {
    put(ORIGINAL_JSON_KEY, originalJson)
}

/** Reconstructs a SkuDetails from a Bundle created by toBundle. */
fun Bundle.toSkuDetails(): SkuDetails {
    checkBundle(this, CLASS_NAME)

    return SkuDetails(getString(ORIGINAL_JSON_KEY))
}

fun MutableList<SkuDetails>.toParcelableArray() =
    listToParcelableArray(this, SkuDetails::toBundle)

// TODO(peconn): Get rid of the ugly cast.
fun Array<Parcelable>.toSkuDetailsList(): MutableList<SkuDetails> =
    map { (it as Bundle).toSkuDetails() }.toMutableList()
