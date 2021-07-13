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

import android.app.Activity;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Takes care of packing the information we send back to the website into JSON.
 */
public abstract class PaymentResult {
    private static final String TAG = "PaymentResult";

    public static PaymentResult failure(String reason) {
        return new Failure(reason);
    }

    public static PaymentResult paymentSuccess(String purchaseToken) {
        return new PaymentSuccess(purchaseToken);
    }

    public static PaymentResult priceChangeSuccess() {
        return new PriceChangeSuccess();
    }

    public abstract int getActivityResult();
    public abstract void log();

    public final String getDetails() {
        try {
            return toJson().toString();
        } catch (JSONException e) {
            // This is the recommended way of dealing with JSONExceptions.
            // https://developer.android.com/reference/org/json/JSONException
            throw new RuntimeException(e);
        }
    }

    protected abstract JSONObject toJson() throws JSONException;

    private static class PaymentSuccess extends PaymentResult {
        private final String mPurchaseToken;

        private PaymentSuccess(String purchaseToken) {
            mPurchaseToken = purchaseToken;
        }

        @Override
        public int getActivityResult() {
            return Activity.RESULT_OK;
        }

        @Override
        public void log() {
            Log.d(TAG, "Payment successful");
        }

        @Override
        protected JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            // "token" is deprecated, but kept around for backwards compatibility.
            obj.put("token", mPurchaseToken);
            obj.put("purchaseToken", mPurchaseToken);
            return obj;
        }

    }

    private static class PriceChangeSuccess extends PaymentResult {
        @Override
        public int getActivityResult() {
            return Activity.RESULT_OK;
        }

        @Override
        public void log() {
            Log.d(TAG, "Price change successful");
        }

        @Override
        protected JSONObject toJson() throws JSONException {
            return new JSONObject();
        }
    }

    private static class Failure extends PaymentResult {
        private final String mError;

        private Failure(String error) {
            mError = error;
        }

        @Override
        public int getActivityResult() {
            return Activity.RESULT_CANCELED;
        }

        @Override
        public void log() {
            Log.w(TAG, mError);
        }

        @Override
        protected JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("error", mError);
            return obj;
        }
    }
}
