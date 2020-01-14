package com.google.androidbrowserhelper.playbilling.bundles

import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.android.billingclient.api.SkuDetailsResponseListener
import com.google.androidbrowserhelper.playbilling.ICallback

private const val CLASS_NAME = "SkuDetailsResponseListener"
private const val BILLING_RESULT_KEY = "billing_result"
private const val SKU_DETAILS_LIST = "sku_details_list"

/**
 * Turns a local SkuDetailsResponseListener into a Binder that can be passed over to another app
 * and passed to binderToListener.
 */
fun SkuDetailsResponseListener.toBinder(): Binder {
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
fun binderToListener(binder: IBinder): SkuDetailsResponseListener {
    val callback = ICallback.Stub.asInterface(binder)

    return SkuDetailsResponseListener { billingResult, skuDetails ->
        val argsBundle = makeBundle(CLASS_NAME) {
            put(BILLING_RESULT_KEY, billingResult.toBundle())
            put(SKU_DETAILS_LIST, skuDetails!!.toParcelableArray())
        }

        Log.d("Peter", "Binder: $binder, Callback: $callback")
        callback.onResult(argsBundle)
    }
}
