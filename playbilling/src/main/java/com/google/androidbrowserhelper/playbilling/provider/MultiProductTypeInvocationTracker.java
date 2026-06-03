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
 * ProductType and one for the subs ProductType.
 */
class MultiProductTypeInvocationTracker<Argument, Callback> {
    private final InvocationTracker<Argument, Callback> mInAppInvocation =
            new InvocationTracker<>();
    private final InvocationTracker<Argument, Callback> mSubsInvocation = new InvocationTracker<>();

    private InvocationTracker<Argument, Callback>
            getTracker(@BillingClient.ProductType String productType) {
        return BillingClient.ProductType.INAPP.equals(productType) ? mInAppInvocation : mSubsInvocation;
    }

    /** Pretend that the method was called with the given product type, argument and callback. */
    public void call(@BillingClient.ProductType String productType, Argument arg, Callback callback) {
        getTracker(productType).call(arg, callback);
    }

    /** Returns the argument that the method was previously called with, if any. */
    public Argument getArgument(@BillingClient.ProductType String productType) {
        return getTracker(productType).getArgument();
    }

    /** Returns the callback that the method was previously called with, if any. */
    public Callback getCallback(@BillingClient.ProductType String productType) {
        return getTracker(productType).getCallback();
    }

    /** Wait until the method was called, returns {@code false} if the wait timed out. */
    public boolean waitUntilCalled() throws InterruptedException {
        return mInAppInvocation.waitUntilCalled() && mSubsInvocation.waitUntilCalled();
    }
}
