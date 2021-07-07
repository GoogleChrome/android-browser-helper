// Copyright 2021 Google Inc. All Rights Reserved.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link PaymentResult}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class PaymentResultTest {
    @Test
    public void paymentSuccess_containsToken() {
        String json = PaymentResult.paymentSuccess("abc").getDetails();
        String expected = "'purchaseToken':'abc'".replace('\'', '\"');
        assertTrue(json.contains(expected));
    }

    @Test
    public void paymentSuccess_containsTokenLegacy() {
        String json = PaymentResult.paymentSuccess("abc").getDetails();
        String expected = "'token':'abc'".replace('\'', '\"');
        assertTrue(json.contains(expected));
    }

    @Test
    public void priceChangeSuccess() {
        String json = PaymentResult.priceChangeSuccess().getDetails();
        assertEquals("{}", json);
    }

    @Test
    public void failure_containsMessage() {
        String json = PaymentResult.failure("some error").getDetails();
        String expected = "'error':'some error'".replace('\'', '\"');
        assertTrue(json.contains(expected));
    }
}
