package com.google.androidbrowserhelper.playbilling.digitalgoods;

import android.os.Bundle;

import com.android.billingclient.api.SkuDetails;

public class ItemDetails {
    private static final String ITEM_DETAILS_ID = "itemDetails.id";
    private static final String ITEM_DETAILS_TITLE = "itemDetails.title";
    private static final String ITEM_DETAILS_DESC = "itemDetails.description";
    private static final String ITEM_DETAILS_CURRENCY = "itemDetails.currency";
    private static final String ITEM_DETAILS_VALUE = "itemDetails.value";

    public final String id;
    public final String title;
    public final String description;
    public final String currency;
    public final String value;

    private ItemDetails(String id, String title, String description, String currency, String value) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.currency = currency;
        this.value = value;
    }

    public static ItemDetails create(SkuDetails skuDetails) {
        return new ItemDetails(skuDetails.getSku(), skuDetails.getTitle(),
                skuDetails.getDescription(), skuDetails.getPriceCurrencyCode(),
                skuDetails.getPrice());
    }

    public static ItemDetails create(Bundle bundle) {
        String id = bundle.getString(ITEM_DETAILS_ID);
        String title = bundle.getString(ITEM_DETAILS_TITLE);
        String description = bundle.getString(ITEM_DETAILS_DESC);
        String currency = bundle.getString(ITEM_DETAILS_CURRENCY);
        String value = bundle.getString(ITEM_DETAILS_VALUE);
        return new ItemDetails(id, title, description, currency, value);
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();

        bundle.putString(ITEM_DETAILS_ID, id);
        bundle.putString(ITEM_DETAILS_TITLE, title);
        bundle.putString(ITEM_DETAILS_DESC, description);
        bundle.putString(ITEM_DETAILS_CURRENCY, currency);
        bundle.putString(ITEM_DETAILS_VALUE, value);

        return bundle;
    }
}
