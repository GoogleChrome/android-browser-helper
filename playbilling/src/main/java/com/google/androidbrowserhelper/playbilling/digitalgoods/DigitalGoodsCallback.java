package com.google.androidbrowserhelper.playbilling.digitalgoods;

import android.os.Bundle;

/**
 * A wrapper for {@link androidx.browser.trusted.TrustedWebActivityCallbackRemote} that we can
 * construct and use for testing.
 */
public interface DigitalGoodsCallback {
    /** Runs the callback with the given name and args. **/
    void run(String callbackName, Bundle args);
}
