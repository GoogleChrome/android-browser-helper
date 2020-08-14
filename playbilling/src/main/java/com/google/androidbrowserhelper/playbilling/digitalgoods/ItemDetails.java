package com.google.androidbrowserhelper.playbilling.digitalgoods;

import android.os.Bundle;

public class ItemDetails {
    public static ItemDetails APPLE =
            new ItemDetails("apple", "Apple", "A tasty apple", "GBP", "0.30");

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

    public ItemDetails(String id, String title, String description, String currency, String value) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.currency = currency;
        this.value = value;
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
