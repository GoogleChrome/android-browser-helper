package com.google.androidbrowserhelper.playbilling.provider;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import androidx.annotation.Nullable;

/**
 * Contains the data we parsed out of the methodData of the Payment Request Intent.
 *
 * The JavaScript used will look something like this:
 *
 * <pre>
 * {@code
 *
 * const supportedInstruments = [{
 *     supportedMethods: 'https://beer.conn/dev',
 *     data: {
 *         sku: "android.test.purchased"
 *     }
 * }]
 *
 * }
 * </pre>
 *
 * This class will hold the contents of the {@code data} object.
 *
 * To get the {@code data} (in string form) out of the Intent used to launch the Activity, you would
 * do:
 *
 * <pre>
 * {@code
 *
 * getIntent()
 *     .getBundleExtra("methodData")
 *     .getString("https://beer.conn.dev");
 *
 * }
 * </pre>
 *
 */
public class MethodData {
    public final String sku;

    private MethodData(String sku) {
        this.sku = sku;
    }

    @Nullable
    public static MethodData fromJson(String json) {
        try {
            return fromJsonInner(json);
        } catch (JSONException e) {
            // TODO: Give feedback on the errors somewhere.
            return null;
        }
    }

    @Nullable
    private static MethodData fromJsonInner(String json) throws JSONException {
        JSONObject dataObject = new JSONObject(json);

        String sku = dataObject.optString("sku");
        if (TextUtils.isEmpty(sku)) return null;

        return new MethodData(sku);
    }

    @Nullable
    public static MethodData fromIntent(Intent intent) {
        // TODO: This should probably be in another class.
        ArrayList<String> methods = intent.getStringArrayListExtra("methodNames");
        if (methods == null || methods.isEmpty()) return null;

        // TODO: Check that this value matches something...
        String method = methods.get(0);

        Bundle methodDatas = intent.getBundleExtra("methodData");
        if (methodDatas == null) return null;

        String methodDataJson = methodDatas.getString(method);
        if (TextUtils.isEmpty(methodDataJson)) return null;

        return fromJson(methodDataJson);
    }
}
