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
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.SkuDetails;
import com.google.androidbrowserhelper.playbilling.digitalgoods.BillingResultMerger;

import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;

public class PaymentActivity extends Activity implements BillingWrapper.Listener {
    private static final String TAG = "PaymentActivity";

    private static final String METHOD_NAME = "https://play.google.com/billing";

    static final String PROXY_PACKAGE_KEY = "PROXY_PACKAGE";

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

        getIntent().putExtra(PROXY_PACKAGE_KEY, component.getPackageName());

        mMethodData = MethodData.fromIntent(getIntent());
        if (mMethodData == null) {
            fail("Could not parse SKU.");
            return;
        }

        /**
         * Note that we have temporarily disabled the IMMEDIATE_WITHOUT_PRORATION mode
         * due to a potential for fraud in which a user may upgrade their subscription
         * without paying the upgraded price for one billing cycle. While we work on
         * the fix, please don't use this proration mode.
         *
         * Check chromeos.dev/publish/pwa-play-billing for more info.
         */
        if (mMethodData.prorationMode
                == BillingFlowParams.ProrationMode.IMMEDIATE_WITHOUT_PRORATION) {
            fail("This proration mode is currently disabled. Check " +
                    "chromeos.dev/publish/pwa-play-billing for more info");
            return;
        }

        mWrapper = BillingWrapperFactory.get(this, this);
        mWrapper.connect(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                onConnected();
            }

            @Override
            public void onBillingServiceDisconnected() {
                onDisconnected();
            }
        });
    }

    public void onDisconnected() {
        fail("BillingClient disconnected.");
    }

    public void onConnected() {
        BillingResultMerger<SkuDetails> merger = new BillingResultMerger<>((result, details) -> {
                    if (details == null || details.isEmpty()) {
                        fail("Play Billing returned did not find SKUs.");
                        return;
                    }

                    if (mWrapper.launchPaymentFlow(
                            PaymentActivity.this, details.get(0), mMethodData))
                        return;

                    fail("Payment attempt failed (have you already bought the item?).");
                });

        List<String> ids = Collections.singletonList(mMethodData.sku);
        mWrapper.querySkuDetails(BillingClient.SkuType.INAPP, ids, merger::setInAppResult);
        mWrapper.querySkuDetails(BillingClient.SkuType.SUBS, ids, merger::setSubsResult);
    }

    @Override
    public void onPurchaseFlowComplete(BillingResult result, String purchaseToken) {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            setResultAndFinish(PaymentResult.paymentSuccess(purchaseToken));
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
