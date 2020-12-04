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

package com.google.androidbrowserhelper.playbilling.digitalgoods;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;

import static com.google.androidbrowserhelper.playbilling.digitalgoods.Logging.logUnknownResultCode;

/**
 * This class contains common functionality that is used by the various *Call classes.
 */
public class DigitalGoodsConverter {
    // These values should be in sync with the ones used here:
    // https://source.chromium.org/chromium/chromium/src/+/master:chrome/android/java/src/org/chromium/chrome/browser/browserservices/digitalgoods/DigitalGoodsConverter.java;drc=58413e78345f4783c1e4d276300c5d283e9d3dba
    public static final int CHROMIUM_RESULT_OK = 0;
    public static final int CHROMIUM_RESULT_ERROR = 1;
    public static final int CHROMIUM_RESULT_ITEM_ALREADY_OWNED = 2;
    public static final int CHROMIUM_RESULT_ITEM_NOT_OWNED = 3;
    public static final int CHROMIUM_RESULT_ITEM_UNAVAILABLE = 4;

    private DigitalGoodsConverter() { }

    static int toChromiumResponseCode(BillingResult result) {
        return toChromiumResponseCode(result.getResponseCode());
    }

    static int toChromiumResponseCode(int playBillingResponseCode) {
        switch (playBillingResponseCode) {
            case BillingClient.BillingResponseCode.OK:
                return CHROMIUM_RESULT_OK;
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                return CHROMIUM_RESULT_ITEM_ALREADY_OWNED;
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
                return CHROMIUM_RESULT_ITEM_NOT_OWNED;
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return CHROMIUM_RESULT_ITEM_UNAVAILABLE;
            case BillingClient.BillingResponseCode.ERROR:
                return CHROMIUM_RESULT_ERROR;
            default:
                logUnknownResultCode(playBillingResponseCode);
                return CHROMIUM_RESULT_ERROR;
        }
    }
}
