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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class PlayBillingWrapperTest {
    private final SubscriptionOfferDetails mIntro = offer("intro");
    private final SubscriptionOfferDetails mBasePlan = offer(null);
    private final List<SubscriptionOfferDetails> mOffers = Arrays.asList(mIntro, mBasePlan);

    @Test
    public void findOffer_noOfferId_picksBasePlan() {
        assertSame(mBasePlan, PlayBillingWrapper.findOffer(mOffers, null));
    }

    @Test
    public void findOffer_offerId_picksMatchOrNothing() {
        assertSame(mIntro, PlayBillingWrapper.findOffer(mOffers, "intro"));
        assertNull(PlayBillingWrapper.findOffer(mOffers, "unknown"));
        assertNull(PlayBillingWrapper.findOffer(null, "intro"));
    }

    private static SubscriptionOfferDetails offer(@Nullable String offerId) {
        SubscriptionOfferDetails offer = mock(SubscriptionOfferDetails.class);
        when(offer.getOfferId()).thenReturn(offerId);
        return offer;
    }
}
