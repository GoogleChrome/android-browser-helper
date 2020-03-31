package com.google.androidbrowserhelper.playbilling.provider;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.androidbrowserhelper.playbilling.IIsReadyToPayService;
import com.google.androidbrowserhelper.playbilling.IIsReadyToPayServiceCallback;

import androidx.annotation.Nullable;

/**
 * A Service that tells the browser whether this Payment App will accept a payment request from it.
 * It will only accept payment requests from the verified browser (the last browser to launch the
 * Trusted Web Activity).
 */
public class IsReadyToPayService extends Service {
    private static final String TAG = "PaymentService";

    private final IBinder mBinder = new IIsReadyToPayService.Stub() {
        @Override
        public void isReadyToPay(IIsReadyToPayServiceCallback callback) throws RemoteException {
            Context context = IsReadyToPayService.this;
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
