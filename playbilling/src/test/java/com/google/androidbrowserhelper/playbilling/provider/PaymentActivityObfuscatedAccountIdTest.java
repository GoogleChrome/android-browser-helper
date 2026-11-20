// Copyright 2026 Google Inc. All Rights Reserved.
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
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;

import com.google.androidbrowserhelper.trusted.SharedPreferencesTokenStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

/**
 * Tests for the obfuscatedAccountId validation in {@link PaymentActivity}.
 */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class PaymentActivityObfuscatedAccountIdTest {
    private static final String CALLER = "com.test.browser";
    private static final String PAYMENT_METHOD = "https://play.google.com/billing";

    private final MockBillingWrapper mWrapper = mock(MockBillingWrapper.class);

    @Before
    public void setUp() {
        Application context = RuntimeEnvironment.getApplication();

        PackageInfo caller = new PackageInfo();
        caller.packageName = CALLER;
        caller.signatures = new Signature[]{new Signature("abcdef1234567890")};
        shadowOf(context.getPackageManager()).installPackage(caller);
        new SharedPreferencesTokenStore(context)
                .setVerifiedProvider(CALLER, context.getPackageManager());

        BillingWrapperFactory.setBillingWrapperForTesting(mWrapper);
    }

    @After
    public void tearDown() {
        BillingWrapperFactory.setBillingWrapperForTesting(null);
    }

    @Test
    public void noId_connectsToPlayBilling() {
        PaymentActivity activity = launchActivity("{ 'sku' = 'mySku' }");

        assertFalse(activity.isFinishing());
        verify(mWrapper).connect(any());
    }

    @Test
    public void maxLength_connectsToPlayBilling() {
        PaymentActivity activity = launchActivity(methodDataWithIdOfLength(64));

        assertFalse(activity.isFinishing());
        verify(mWrapper).connect(any());
    }

    @Test
    public void tooLong_fails() {
        PaymentActivity activity = launchActivity(methodDataWithIdOfLength(65));

        assertTrue(activity.isFinishing());
        assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).getResultCode());
        verify(mWrapper, never()).connect(any());

        String details = shadowOf(activity).getResultIntent().getStringExtra("details");
        assertTrue(details.contains("obfuscatedAccountId"));
    }

    private static PaymentActivity launchActivity(String methodDataJson) {
        Bundle methodData = new Bundle();
        methodData.putString(PAYMENT_METHOD, methodDataJson.replace('\'', '\"'));

        Intent intent = new Intent();
        intent.putStringArrayListExtra("methodNames",
                new ArrayList<>(Collections.singletonList(PAYMENT_METHOD)));
        intent.putExtra("methodData", methodData);

        ActivityController<PaymentActivity> controller =
                Robolectric.buildActivity(PaymentActivity.class, intent);
        shadowOf(controller.get()).setCallingActivity(new ComponentName(CALLER, "Caller"));
        return controller.create().get();
    }

    private static String methodDataWithIdOfLength(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, 'a');
        return "{ 'sku' = 'mySku', 'obfuscatedAccountId' = '" + new String(chars) + "' }";
    }
}
