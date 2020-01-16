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

package com.google.androidbrowserhelper.playbilling

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import com.google.androidbrowserhelper.playbilling.bundles.QuerySkuDetailsCall
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Test for communicating the parameters and listener for a BillingClient#querySkuDetailsAsync call
 * using a QuerySkuDetailsCall
 */
@RunWith(AndroidJUnit4::class)
internal class QuerySkuDetailsCallTest {
    @Test
    fun querySkuDetailsCall() {
        // This has to be an Android Test (not a Unit Test) because Robolectric doesn't support
        // Bundle#putBinder.

        // It is helpful to think of this code being executed in two processes, the client which
        // initiates the request and the provider which queries to Play Billing and then responds.
        // TODO(peconn): Create a test that actually runs this on different Services/Activities.

        // The parameters the client is going to provide.
        val expParams = SkuDetailsParams.newBuilder()
                .setType("type")
                .setSkusList(listOf("sku1", "sku2"))
                .build()

        // Holders for the results that will be returned to the client.
        val actBillingResult = AtomicReference<BillingResult>()
        val actSkuDetails = AtomicReference<List<SkuDetails>>()

        // A listener (on the client) that will be triggered by the provider.
        val latch = CountDownLatch(1)
        val listener = SkuDetailsResponseListener { billingResult, skuDetails ->
            actBillingResult.set(billingResult)
            actSkuDetails.set(skuDetails)
            latch.countDown()
        }

        // The client wraps the listener and params up into a Bundle.
        val bundle = QuerySkuDetailsCall(expParams, listener).toBundle()

        // Now we move over to the provider.

        // The provider gets the listener and params out of the Bundle.
        val querySkuDetailsCall = QuerySkuDetailsCall.fromBundle(bundle)

        // Check that the params the provider got are what we expected.
        val actParams = querySkuDetailsCall.params
        Assert.assertEquals(expParams.skuType, actParams.skuType)
        Assert.assertArrayEquals(expParams.skusList.toTypedArray(),
                actParams.skusList.toTypedArray())

        // Now the provider goes off and calls the Play Billing APIs, getting some results.

        // The results from the provider.
        val expBillingResult = BillingResult.newBuilder().setResponseCode(23).build()
        val expSkuDetails = listOf(SkuDetails("{}"), SkuDetails("{}"))

        // The provider returns the results to the client through the listener (which forwards
        // the results over a Binder).
        querySkuDetailsCall.listener.onSkuDetailsResponse(expBillingResult, expSkuDetails)

        // And we're back on the client.

        // Wait for the listener to be called.
        Assert.assertTrue(latch.await(3, TimeUnit.SECONDS))

        // Check that the results the client got are actually what the provider sent.
        Assert.assertEquals(expBillingResult.responseCode, actBillingResult.get().responseCode)
        Assert.assertArrayEquals(expSkuDetails.toTypedArray(), actSkuDetails.get().toTypedArray())
    }
}
