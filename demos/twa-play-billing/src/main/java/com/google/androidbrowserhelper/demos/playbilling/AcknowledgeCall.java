package com.google.androidbrowserhelper.demos.playbilling;

import android.os.Bundle;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

public class AcknowledgeCall {
    public static final String COMMAND_NAME = "acknowledge";

    private static final String PARAM_ACKNOWLEDGE_PURCHASE_TOKEN = "acknowledge.purchaseToken";
    private static final String PARAM_ACKNOWLEDGE_MAKE_AVAILABLE_AGAIN =
            "acknowledge.makeAvailableAgain";
    private static final String RESPONSE_ACKNOWLEDGE = "acknowledge.response";
    private static final String RESPONSE_ACKNOWLEDGE_RESPONSE_CODE = "acknowledge.responseCode";

    public final String purchaseToken;
    public final boolean makeAvailableAgain;
    private final TrustedWebActivityCallbackRemote mCallback;

    private AcknowledgeCall(String purchaseToken, boolean makeAvailableAgain,
            TrustedWebActivityCallbackRemote mCallback) {
        this.purchaseToken = purchaseToken;
        this.makeAvailableAgain = makeAvailableAgain;
        this.mCallback = mCallback;
    }

    @Nullable
    public static AcknowledgeCall create(@Nullable Bundle args,
            @Nullable TrustedWebActivityCallbackRemote callback) {
        if (args == null || callback == null) return null;

        if (!args.containsKey(PARAM_ACKNOWLEDGE_PURCHASE_TOKEN) ||
                !args.containsKey(PARAM_ACKNOWLEDGE_MAKE_AVAILABLE_AGAIN)) {
            return null;
        }

        String token = args.getString(PARAM_ACKNOWLEDGE_PURCHASE_TOKEN);
        boolean makeAvailableAgain = args.getBoolean(PARAM_ACKNOWLEDGE_MAKE_AVAILABLE_AGAIN);

        return new AcknowledgeCall(token, makeAvailableAgain, callback);
    }

    public void respond(int responseCode) {
        try {
            Bundle args = new Bundle();
            args.putInt(RESPONSE_ACKNOWLEDGE_RESPONSE_CODE, responseCode);
            mCallback.runExtraCallback(RESPONSE_ACKNOWLEDGE, args);
        } catch (RemoteException e) {
            // TODO: Something...
            throw new RuntimeException(e);
        }
    }
}
