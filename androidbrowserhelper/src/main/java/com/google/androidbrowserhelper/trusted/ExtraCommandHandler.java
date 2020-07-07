package com.google.androidbrowserhelper.trusted;


import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

/**
 * An interface for handling extraCommand in {@link androidx.browser.trusted.TrustedWebActivityService}.
 */
public interface ExtraCommandHandler {
    String EXTRA_COMMAND_SUCCESS = "success";

    /**
     * Handles the free form command from the browser. The return bundle should contain a boolean
     * {@code EXTRA_COMMAND_SUCCESS}, if the command is handled, set the value to {@code true};
     * otherwise, set to {@code false}.
     */
    @NonNull
    Bundle handleExtraCommand(Context context, String commandName, Bundle args,
                              @Nullable TrustedWebActivityCallbackRemote callback);
}
