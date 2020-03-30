package com.google.androidbrowserhelper.playbilling.provider;

import android.app.Activity;

import androidx.annotation.Nullable;

/**
 * Provides the appropriate {@link BillingWrapper}.
 */
public class BillingWrapperFactory {
    @Nullable
    private static MockBillingWrapper sTestingBillingWrapper;

    /**
     * Returns the appropriate {@link BillingWrapper} - this will be {@link PlayBillingWrapper} in
     * production code and {@link MockBillingWrapper} in tests.
     */
    public static BillingWrapper get(Activity activity, BillingWrapper.Listener listener) {
        if (sTestingBillingWrapper != null) {
            sTestingBillingWrapper.setListener(listener);
            return sTestingBillingWrapper;
        }

        return new PlayBillingWrapper(activity, listener);
    }

    /**
     * Sets the {@link BillingWrapper} to be used for testing. It can be {@code null} to reset to
     * using the production version.
     */
    public static void setBillingWrapperForTesting(@Nullable MockBillingWrapper wrapper) {
        sTestingBillingWrapper = wrapper;
    }
}
