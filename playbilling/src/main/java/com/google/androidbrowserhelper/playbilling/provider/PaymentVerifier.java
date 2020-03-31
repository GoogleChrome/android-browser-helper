package com.google.androidbrowserhelper.playbilling.provider;

import android.content.Context;
import android.util.Log;

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
