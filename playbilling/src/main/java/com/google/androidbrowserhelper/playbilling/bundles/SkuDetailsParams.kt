package com.google.androidbrowserhelper.playbilling.bundles

import android.os.Bundle
import com.android.billingclient.api.SkuDetailsParams

private const val CLASS_NAME = "SkuDetailsParams"
private const val SKU_TYPE = "sku_type"
private const val SKU_LIST = "sku_list"

/** Encodes the SkuDetailsParams into a Bundle. */
fun SkuDetailsParams.toBundle() = makeBundle(CLASS_NAME) {
    put(SKU_TYPE, skuType)
    put(SKU_LIST, skusList)
}

/** Reconstructs a SkuDetailsParams from a Bundle created by toBundle. */
fun Bundle.toSkuDetailsParams(): SkuDetailsParams {
    checkBundle(this, CLASS_NAME)

    var builder = SkuDetailsParams.newBuilder()

    builder.setType(getString(SKU_TYPE))
    builder.setSkusList(getStringArray(SKU_LIST)?.toList())

    return builder.build()
}

