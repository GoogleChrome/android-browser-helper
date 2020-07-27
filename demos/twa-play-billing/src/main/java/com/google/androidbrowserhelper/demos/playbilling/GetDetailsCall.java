package com.google.androidbrowserhelper.demos.playbilling;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

public class GetDetailsCall {
    public static final String COMMAND_NAME = "getDetails";

    private static final String PARAM_GET_DETAILS_ITEM_IDS = "getDetails.itemIds";
    private static final String RESPONSE_GET_DETAILS = "getDetails.response";
    private static final String RESPONSE_GET_DETAILS_RESPONSE_CODE = "getDetails.responseCode";
    private static final String RESPONSE_GET_DETAILS_DETAILS_LIST = "getDetails.detailsList";

    public final String[] itemIds;
    private final TrustedWebActivityCallbackRemote mCallback;

    private GetDetailsCall(String[] itemIds, TrustedWebActivityCallbackRemote callback) {
        this.itemIds = itemIds;
        this.mCallback = callback;
    }

    @Nullable
    public static GetDetailsCall create(@Nullable Bundle args,
            @Nullable TrustedWebActivityCallbackRemote callback) {
        if (args == null || callback == null) return null;

        String[] itemIds = args.getStringArray(PARAM_GET_DETAILS_ITEM_IDS);
        if (itemIds == null) return null;

        return new GetDetailsCall(itemIds, callback);
    }

    public void respond(int responseCode, ItemDetails... itemDetails) {
        try {
            Bundle args = new Bundle();
            args.putInt(RESPONSE_GET_DETAILS_RESPONSE_CODE, responseCode);
            args.putParcelableArray(RESPONSE_GET_DETAILS_DETAILS_LIST,
                    toParcelableArray(itemDetails));
            mCallback.runExtraCallback(RESPONSE_GET_DETAILS, args);
        } catch (Exception e) {
            // TODO: Something...
            throw new RuntimeException(e);
        }
    }

    private static Parcelable[] toParcelableArray(ItemDetails... itemDetails) {
        Parcelable[] out = new Parcelable[itemDetails.length];

        for (int i = 0; i < itemDetails.length; i++) {
            out[i] = itemDetails[i].toBundle();
        }

        return out;
    }
}
