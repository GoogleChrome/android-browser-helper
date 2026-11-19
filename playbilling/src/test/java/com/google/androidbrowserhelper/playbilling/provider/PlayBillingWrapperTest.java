// Copyright 2026 Google Inc. All Rights Reserved.
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

package com.google.androidbrowserhelper.playbilling.provider;

import android.os.Build;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PlayBillingWrapper}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class PlayBillingWrapperTest {
    @Test
    public void launchPaymentFlow_forwardsObfuscatedAccountId() {
        BillingFlowParams.Builder params = launchPaymentFlow(
                "{ 'sku' = 'mySku', 'obfuscatedAccountId' = 'obfuscatedId123' }");

        InOrder inOrder = inOrder(params);
        inOrder.verify(params).setObfuscatedAccountId("obfuscatedId123");
        inOrder.verify(params).build();
    }

    @Test
    public void launchPaymentFlow_omitsObfuscatedAccountId() {
        BillingFlowParams.Builder params = launchPaymentFlow("{ 'sku' = 'mySku' }");

        verify(params, never()).setObfuscatedAccountId(any());
    }

    /**
     * Launches the payment flow against a mocked Play Billing and returns the builder used for
     * the flow params.
     */
    private static BillingFlowParams.Builder launchPaymentFlow(String methodDataJson) {
        MethodData methodData = MethodData.fromJson(methodDataJson.replace('\'', '\"'));

        ProductDetails productDetails = mock(ProductDetails.class);
        when(productDetails.getProductType()).thenReturn(BillingClient.ProductType.INAPP);

        BillingClient client = mock(BillingClient.class);
        when(client.launchBillingFlow(any(), any()))
                .thenReturn(BillingResult.newBuilder().build());
        BillingClient.Builder clientBuilder = mock(BillingClient.Builder.class, RETURNS_SELF);
        when(clientBuilder.build()).thenReturn(client);

        BillingFlowParams params = mock(BillingFlowParams.class);
        BillingFlowParams.Builder paramsBuilder =
                mock(BillingFlowParams.Builder.class, RETURNS_SELF);
        when(paramsBuilder.build()).thenReturn(params);

        try (MockedStatic<BillingClient> billingClient = mockStatic(BillingClient.class);
             MockedStatic<BillingFlowParams> billingFlowParams =
                     mockStatic(BillingFlowParams.class)) {
            billingClient.when(() -> BillingClient.newBuilder(any())).thenReturn(clientBuilder);
            billingFlowParams.when(BillingFlowParams::newBuilder).thenReturn(paramsBuilder);

            new PlayBillingWrapper(null, (result, purchaseToken) -> {})
                    .launchPaymentFlow(null, productDetails, methodData);
            billingFlowParams.verify(BillingFlowParams::newBuilder);
        }

        verify(client).launchBillingFlow(null, params);
        return paramsBuilder;
    }
}
