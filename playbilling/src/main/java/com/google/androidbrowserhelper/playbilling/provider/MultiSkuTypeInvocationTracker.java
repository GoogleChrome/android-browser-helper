// Copyright 2022 Google Inc. All Rights Reserved.
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

import com.android.billingclient.api.BillingClient;

/**
 * A helper class for {@link MockBillingWrapper} that helps keep track of which methods were called
 * and the arguments they were called with. This class keeps track of two calls - one for the inapp
 * SkuType and one for the subs SkuType.
 */
public class MultiSkuTypeInvocationTracker<Argument, Callback> {
    private final InvocationTracker<Argument, Callback> mInAppInvocation =
            new InvocationTracker<>();
    private final InvocationTracker<Argument, Callback> mSubsInvocation = new InvocationTracker<>();

    private InvocationTracker<Argument, Callback>
            getTracker(@BillingClient.SkuType String skuType) {
        return BillingClient.SkuType.INAPP.equals(skuType) ? mInAppInvocation : mSubsInvocation;
    }

    public void call(@BillingClient.SkuType String skuType, Argument arg, Callback callback) {
        getTracker(skuType).call(arg, callback);
    }

    public Argument getArgument(@BillingClient.SkuType String skuType) {
        return getTracker(skuType).getArgument();
    }

    public Callback getCallback(@BillingClient.SkuType String skuType) {
        return getTracker(skuType).getCallback();
    }

    public boolean waitUntilCalled() throws InterruptedException {
        return mInAppInvocation.waitUntilCalled() && mSubsInvocation.waitUntilCalled();
    }
}
