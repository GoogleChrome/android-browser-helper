// Copyright 2020 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.androidbrowserhelper.playbilling.provider;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingFlowParams.ProrationMode;

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
 *     supportedMethods: 'https://play.google.com/billing',
 *     data: {
 *         sku: "android.test.purchased",
 *         oldSku: "oldSku"
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
    public final boolean isPriceChangeConfirmation;

    // These three optional fields are to do with upgrading/downgrading subscriptions.
    @Nullable public final String oldSku;
    @Nullable public final String purchaseToken;
    @Nullable public final Integer prorationMode;

    private MethodData(String sku, boolean isPriceChangeConfirmation, @Nullable String oldSku,
                       @Nullable String purchaseToken, @Nullable Integer prorationMode) {
        this.sku = sku;
        this.isPriceChangeConfirmation = isPriceChangeConfirmation;
        this.oldSku = oldSku;
        this.purchaseToken = purchaseToken;
        this.prorationMode = prorationMode;
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

        boolean isPriceChangeConfirmation = dataObject.optBoolean("priceChangeConfirmation");

        String oldSku = getString(dataObject, "oldSku");
        String purchaseToken = getString(dataObject, "purchaseToken");
        Integer prorationMode = getProration(dataObject);

        return new MethodData(sku, isPriceChangeConfirmation, oldSku, purchaseToken, prorationMode);
    }

    @Nullable
    public static MethodData fromIntent(Intent intent) {
        // TODO: This should probably be in another class.
        ArrayList<String> methods = intent.getStringArrayListExtra("methodNames");
        if (methods == null || methods.isEmpty()) return null;

        String method = methods.get(0);

        Bundle methodDatas = intent.getBundleExtra("methodData");
        if (methodDatas == null) return null;

        String methodDataJson = methodDatas.getString(method);
        if (TextUtils.isEmpty(methodDataJson)) return null;

        return fromJson(methodDataJson);
    }

    private static @Nullable String getString(JSONObject object, String key) {
        String value = object.optString(key);
        if (TextUtils.isEmpty(value)) return null;
        return value;
    }

    private static @Nullable Integer getProration(JSONObject object) {
        String proration = getString(object, "prorationMode");

        if (proration == null) return null;

        switch (proration) {
            case "deferred":
                return ProrationMode.DEFERRED;
            case "immediateAndChargeProratedPrice":
                return ProrationMode.IMMEDIATE_AND_CHARGE_PRORATED_PRICE;
            case "immediateWithoutProration":
                return ProrationMode.IMMEDIATE_WITHOUT_PRORATION;
            case "immediateWithTimeProration":
                return ProrationMode.IMMEDIATE_WITH_TIME_PRORATION;
            case "unknownSubscriptionUpgradeDowngradePolicy":
                return ProrationMode.UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY;
            case "immediateAndChargeFullPrice":
                return ProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE;
            default:
                return null;
        }
    }
}
