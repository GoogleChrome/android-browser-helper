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

    public static PaymentResult success(String id) {
        return new Success(id);
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

    private static class Success extends PaymentResult {
        private final String mId;

        private Success(String id) {
            mId = id;
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
            obj.put("id", mId);
            return obj;
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
