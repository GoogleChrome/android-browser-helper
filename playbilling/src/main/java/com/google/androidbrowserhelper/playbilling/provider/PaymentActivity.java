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

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.SkuDetails;

import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class PaymentActivity extends AppCompatActivity implements BillingWrapper.Listener {
    private static final String TAG = "PaymentActivity";

    private static final String METHOD_NAME = "https://beer.conn.dev";

    private BillingWrapper mWrapper;
    private MethodData mMethodData;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ComponentName component = getCallingActivity();
        if (component == null) {
            fail("Must be launched with startActivityForResult.");
            return;
        }

        if (!PaymentVerifier.shouldAllowPayments(this, component.getPackageName(), TAG)) {
            fail("Launching app is not verified.");
            return;
        }

        mMethodData = MethodData.fromIntent(getIntent());
        if (mMethodData == null) {
            fail("Could not parse SKU.");
            return;
        }

        mWrapper = BillingWrapperFactory.get(this, this);
        mWrapper.connect();
    }

    @Override
    public void onDisconnected() {
        fail("BillingClient disconnected.");
    }

    @Override
    public void onConnected() {
        mWrapper.querySkuDetails(Collections.singletonList(mMethodData.sku),
                (BillingResult result, List<SkuDetails> details) -> {
            if (details == null || details.isEmpty()) {
                fail("Play Billing returned did not find SKUs.");
                return;
            }

            if (mWrapper.launchPaymentFlow(this, details.get(0))) return;

            fail("Payment attempt failed (have you already bought the item?).");
        });
    }

    @Override
    public void onPurchaseFlowComplete(int result) {
        if (result == BillingClient.BillingResponseCode.OK) {
            setResultAndFinish(PaymentResult.success("success"));
        } else {
            fail("Purchase flow ended with result: " + result);
        }
    }

    private void fail(String reason) {
        setResultAndFinish(PaymentResult.failure(reason));
    }

    private void setResultAndFinish(PaymentResult result) {
        result.log();

        Intent intent = new Intent();
        intent.putExtra("methodName", METHOD_NAME);
        intent.putExtra("details", result.getDetails());
        setResult(result.getActivityResult(), intent);
        finish();
    }
}
