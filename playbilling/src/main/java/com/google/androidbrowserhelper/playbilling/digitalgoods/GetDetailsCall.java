package com.google.androidbrowserhelper.playbilling.digitalgoods;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.Nullable;

/**
 * A class for parsing Digital Goods API calls from the browser and converting them into a format
 * suitable for calling the Play Biling library.
 */
public class GetDetailsCall {
    public static final String COMMAND_NAME = "getDetails";

    static final String PARAM_GET_DETAILS_ITEM_IDS = "getDetails.itemIds";
    static final String RESPONSE_GET_DETAILS = "getDetails.response";
    static final String RESPONSE_GET_DETAILS_RESPONSE_CODE = "getDetails.responseCode";
    static final String RESPONSE_GET_DETAILS_DETAILS_LIST = "getDetails.detailsList";

    public final String[] itemIds;
    private final DigitalGoodsCallback mCallback;

    private GetDetailsCall(String[] itemIds, DigitalGoodsCallback callback) {
        this.itemIds = itemIds;
        this.mCallback = callback;
    }

    /** Creates this class from a {@link Bundle}, returns {@code null} if the Bundle is invalid. **/
    @Nullable
    public static GetDetailsCall create(@Nullable Bundle args,
            @Nullable DigitalGoodsCallback callback) {
        if (args == null || callback == null) return null;

        String[] itemIds = args.getStringArray(PARAM_GET_DETAILS_ITEM_IDS);
        if (itemIds == null) return null;

        return new GetDetailsCall(itemIds, callback);
    }

    /** Calls the callback provided in the constructor with serialized forms of the parameters. */
    public void respond(int responseCode, ItemDetails... itemDetails) {
        Bundle args = new Bundle();
        args.putInt(RESPONSE_GET_DETAILS_RESPONSE_CODE, responseCode);
        args.putParcelableArray(RESPONSE_GET_DETAILS_DETAILS_LIST,
                toParcelableArray(itemDetails));
        mCallback.run(RESPONSE_GET_DETAILS, args);
    }

    private static Parcelable[] toParcelableArray(ItemDetails... itemDetails) {
        Parcelable[] out = new Parcelable[itemDetails.length];

        for (int i = 0; i < itemDetails.length; i++) {
            out[i] = itemDetails[i].toBundle();
        }

        return out;
    }
}
