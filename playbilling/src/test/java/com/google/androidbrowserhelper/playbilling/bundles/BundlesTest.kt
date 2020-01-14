package com.google.androidbrowserhelper.playbilling.bundles

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.SkuDetailsResponseListener
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(manifest = Config.NONE)
class BundlesTest {
    @Test
    fun checkBundle_valid() {
        val className = "ClassName"
        val bundle = makeBundle(className) {}
        checkBundle(bundle, className)
    }

    @Test(expected = IllegalArgumentException::class)
    fun checkBundle_invalid() {
        val className = "ClassName"
        val wrongName = "OtherClassName"
        val bundle = makeBundle(className) {}
        checkBundle(bundle, wrongName)
    }

    @Test
    fun billingResult() {
        val expected = BillingResult.newBuilder()
                .setResponseCode(25)
                .setDebugMessage("debug message")
                .build()

        val actual = expected.toBundle().toBillingResult()

        assertEquals(expected.responseCode, actual.responseCode)
        assertEquals(expected.debugMessage, actual.debugMessage)
    }

    @Test
    fun skuDetailsParams() {
        val expected = SkuDetailsParams.newBuilder()
                .setType("type")
                .setSkusList(listOf("sku1", "sku2"))
                .build()

        val actual = expected.toBundle().toSkuDetailsParams()

        assertEquals(expected.skuType, actual.skuType)
        assertArrayEquals(expected.skusList.toTypedArray(), actual.skusList.toTypedArray())
    }

    @Test
    fun skuDetails() {
        val json = """
            {
                productId: "id",
                type: "type",
                title: "Title",
                description: "This is a description"
            }
        """.trimIndent()

        val expected = SkuDetails(json)
        val actual = expected.toBundle().toSkuDetails()

        assertEquals(expected.sku, actual.sku)
        assertEquals(expected.type, actual.type)
        assertEquals(expected.title, actual.title)
        assertEquals(expected.description, actual.description)
        assertEquals(expected.price, actual.price)
    }

    @Test
    fun skuDetailsResponseListener() {
        val latch = CountDownLatch(1)

        val expBillingResult = BillingResult.newBuilder().setResponseCode(4).build()
        val expSkuDetails = listOf(SkuDetails("{}"), SkuDetails("{}"))

        val listener = SkuDetailsResponseListener { actBillingResult, actSkuDetails ->
            assertEquals(expBillingResult.responseCode, actBillingResult.responseCode)
            assertEquals(expSkuDetails.size, actSkuDetails.size)
            latch.countDown()
        }

        val boundListener = binderToListener(listener.toBinder())
        boundListener.onSkuDetailsResponse(expBillingResult, expSkuDetails)

        assertTrue(latch.await(3, TimeUnit.SECONDS))
    }
}
