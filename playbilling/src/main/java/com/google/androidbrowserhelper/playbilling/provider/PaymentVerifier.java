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

import android.content.Context;
import android.util.Log;

import com.google.androidbrowserhelper.trusted.ChromeOsSupport;
import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.Token;
import androidx.browser.trusted.TokenStore;

/**
 * Contains logic for determining whether an app should be allowed to make a payment request.
 */
public class PaymentVerifier {
    // TODO: Should this be an instance class (eg PaymentStrategy)?
    // It would allow developers to override verification behaviour more easily.

    /**
     * Determines whether the given package name should be allowed to trigger Payment Requests.
     * A package can only trigger payment requests if it is the verified provider for the Trusted
     * Web Activity.
     */
    static boolean shouldAllowPayments(@NonNull Context context, @Nullable String packageName,
            @NonNull String logTag) {
        // TODO: Should I also check whether the TWA is currently running? If so, how?
        if (packageName == null) return false;

        if (ChromeOsSupport.isRunningOnArc(context.getPackageManager())
                && packageName.equals(ChromeOsSupport.ARC_PAYMENT_APP)) {
            return true;
        }

        TokenStore tokenStore = new SharedPreferencesTokenStore(context);
        Token verifiedPackage = tokenStore.load();
        if (verifiedPackage == null) {
            Log.w(logTag, "Denied payment as no verified app set.");
            return false;
        }

        boolean verified = verifiedPackage.matches(packageName, context.getPackageManager());

        if (!verified) {
            Log.w(logTag, "Denied payment to unverified app (" + packageName + ").");
        }

        return verified;
    }
}
