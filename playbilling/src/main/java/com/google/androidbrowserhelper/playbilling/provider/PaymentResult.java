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

    public static PaymentResult success() {
        return new Success();
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
