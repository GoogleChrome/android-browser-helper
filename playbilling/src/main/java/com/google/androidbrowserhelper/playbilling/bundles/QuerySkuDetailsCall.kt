package com.google.androidbrowserhelper.playbilling.bundles

import android.os.Bundle
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener

private const val CLASS_NAME = "QuerySkuDetailsCall"
private const val PARAMS_KEY = "params"
private const val CALLBACK_KEY = "callback"

/** Holds the parameters and result callback for a call to PlayBilling#querySkuDetailsAsync. */
internal data class QuerySkuDetailsCall(
        val params: SkuDetailsParams,
        val listener: SkuDetailsResponseListener) {

    fun toBundle() = makeBundle(CLASS_NAME) {
        putBundle(PARAMS_KEY, params.toBundle())
        putBinder(CALLBACK_KEY, listener.toBinder())
    }

    companion object {
        fun fromBundle(bundle: Bundle): QuerySkuDetailsCall {
            checkBundle(bundle, CLASS_NAME)

            return QuerySkuDetailsCall(
                    bundle.getBundle(PARAMS_KEY)!!.toSkuDetailsParams(),
                    binderToListener(bundle.getBinder(CALLBACK_KEY)!!)
            )
        }
    }
}