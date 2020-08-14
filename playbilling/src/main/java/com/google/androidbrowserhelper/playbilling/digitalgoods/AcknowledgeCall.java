package com.google.androidbrowserhelper.playbilling.digitalgoods;

import android.os.Bundle;

import androidx.annotation.Nullable;

public class AcknowledgeCall {
    public static final String COMMAND_NAME = "acknowledge";

    private static final String PARAM_ACKNOWLEDGE_PURCHASE_TOKEN = "acknowledge.purchaseToken";
    private static final String PARAM_ACKNOWLEDGE_MAKE_AVAILABLE_AGAIN =
            "acknowledge.makeAvailableAgain";
    private static final String RESPONSE_ACKNOWLEDGE = "acknowledge.response";
    private static final String RESPONSE_ACKNOWLEDGE_RESPONSE_CODE = "acknowledge.responseCode";

    public final String purchaseToken;
    public final boolean makeAvailableAgain;
    private final DigitalGoodsRequestHandler.Callback mCallback;

    private AcknowledgeCall(String purchaseToken, boolean makeAvailableAgain,
            DigitalGoodsRequestHandler.Callback mCallback) {
        this.purchaseToken = purchaseToken;
        this.makeAvailableAgain = makeAvailableAgain;
        this.mCallback = mCallback;
    }

    @Nullable
    public static AcknowledgeCall create(@Nullable Bundle args,
            @Nullable DigitalGoodsRequestHandler.Callback callback) {
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
        Bundle args = new Bundle();
        args.putInt(RESPONSE_ACKNOWLEDGE_RESPONSE_CODE, responseCode);
        mCallback.run(RESPONSE_ACKNOWLEDGE, args);
    }
}
