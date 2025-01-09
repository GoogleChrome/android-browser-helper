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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import org.chromium.IsReadyToPayService;
import org.chromium.IsReadyToPayServiceCallback;

import androidx.annotation.Nullable;

/**
 * A Service that tells the browser whether this Payment App will accept a payment request from it.
 * It will only accept payment requests from the verified browser (the last browser to launch the
 * Trusted Web Activity).
 */
public class PaymentService extends Service {
    private static final String TAG = "PaymentService";

    private final IBinder mBinder = new IsReadyToPayService.Stub() {
        @Override
        public void isReadyToPay(IsReadyToPayServiceCallback callback) throws RemoteException {
            Context context = PaymentService.this;
            String packageName = getPackageManager().getNameForUid(Binder.getCallingUid());
            callback.handleIsReadyToPay(
                    PaymentVerifier.shouldAllowPayments(context, packageName, TAG));
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
