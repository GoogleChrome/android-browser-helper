package com.google.androidbrowserhelper.demos.playbilling;

import android.os.Bundle;

import com.google.androidbrowserhelper.playbilling.digitalgoods.DigitalGoodsRequestHandler;
import com.google.androidbrowserhelper.trusted.DelegationService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

public class BillingDelegationService extends DelegationService {
    public static final String KEY_SUCCESS = "success";
    private final DigitalGoodsRequestHandler mHandler;

    public BillingDelegationService() {
        super();

        mHandler = new DigitalGoodsRequestHandler(this);
    }

    @Nullable
    @Override
    public Bundle onExtraCommand(@NonNull String commandName, @NonNull Bundle args,
                                 @Nullable TrustedWebActivityCallbackRemote callbackRemote) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_SUCCESS, mHandler.handle(commandName, args,
                DigitalGoodsCallback.create(callbackRemote)));
        return bundle;
    }

}
