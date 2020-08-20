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

package com.google.androidbrowserhelper.playbilling.provider;

import android.app.Activity;

import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.List;

/**
 * Wraps communication with Play Billing to provide a simpler interface and allowing mocking in
 * tests.
 */
interface BillingWrapper {
    /**
     * Callbacks for connection state and for purchase flow completion.
     */
    interface Listener {
        /** Will be called when connected to the Play Billing client. */
        void onConnected();

        /** Will be called when the Play Billing client disconnects. */
        void onDisconnected();

        /** Will be called after a call to {@link #launchPaymentFlow} that returns {@code true}. */
        void onPurchaseFlowComplete(int result);
    }

    /** Connect to the Play Billing client. */
    void connect();

    /**
     * Get {@link SkuDetails} objects for the provided SKUs.
     */
    void querySkuDetails(List<String> skus, SkuDetailsResponseListener callback);

    /**
     * Launches the Payment Flow. If it returns {@code true},
     * {@link Listener#onPurchaseFlowComplete} should be called.
     */
    boolean launchPaymentFlow(Activity activity, SkuDetails sku);
}
