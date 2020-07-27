package com.google.androidbrowserhelper.demos.playbilling;

import android.os.Bundle;

import com.google.androidbrowserhelper.trusted.DelegationService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

public class BillingDelegationService extends DelegationService {
    public static final String KEY_SUCCESS = "success";

    @Nullable
    @Override
    public Bundle onExtraCommand(@NonNull String commandName, @NonNull Bundle args,
                                 @Nullable TrustedWebActivityCallbackRemote callbackRemote) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_SUCCESS, false);

        switch (commandName) {
            case GetDetailsCall.COMMAND_NAME:
                GetDetailsCall getDetailsCall = GetDetailsCall.create(args, callbackRemote);
                if (getDetailsCall == null) break;

                getDetailsCall.respond(0, ItemDetails.APPLE);
                bundle.putBoolean(KEY_SUCCESS, true);
                break;
            case AcknowledgeCall.COMMAND_NAME:
                AcknowledgeCall ackCall = AcknowledgeCall.create(args, callbackRemote);
                if (ackCall == null) break;

                ackCall.respond(0);
                bundle.putBoolean(KEY_SUCCESS, true);
                break;
        }

        return bundle;
    }

}
