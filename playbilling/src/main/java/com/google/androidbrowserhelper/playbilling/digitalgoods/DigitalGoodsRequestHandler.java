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

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.google.androidbrowserhelper.playbilling.provider.BillingWrapper;
import com.google.androidbrowserhelper.playbilling.provider.BillingWrapperFactory;
import com.google.androidbrowserhelper.trusted.ExtraCommandHandler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;

public class DigitalGoodsRequestHandler implements ExtraCommandHandler {
    /**
     * A version number used to track the communication format between the TWA shell and the
     * browser.
     *
     * Version 1 of this API corresponds to DGAPI v1 and v2.0 (there were no changes in
     * shell/browser communication between DGAPI v1 and v2.0). However, we didn't put the API
     * version into the Bundle at that point, so v1 is implied by this extra missing from the
     * returned Bundle.
     *
     * Version 2 corresponds to DGAPI v2.1.
     */
    private static final String EXTRA_DIGITAL_GOODS_API_VERSION = "digital_goods_api_version";
    private static final int DIGITAL_GOODS_API_VERSION = 2;

    private final BillingWrapper mWrapper;
    private final BillingWrapper.Listener mListener = (result, token) -> { };

    public DigitalGoodsRequestHandler(Context context) {
        mWrapper = new ConnectedBillingWrapper(BillingWrapperFactory.get(context, mListener));
    }

    @NonNull
    @Override
    public Bundle handleExtraCommand(Context context, String commandName, Bundle args,
            @Nullable TrustedWebActivityCallbackRemote callback) {
        DigitalGoodsCallback wrappedCallback = (callbackName, callbackArgs) -> {
            try {
                callback.runExtraCallback(callbackName, callbackArgs);
            } catch (RemoteException e) {
                // The remote app crashed/got shut down, not much we can do.
            }
        };

        boolean success = handle(commandName, args, wrappedCallback);

        Bundle bundle = new Bundle();
        bundle.putBoolean(EXTRA_COMMAND_SUCCESS, success);
        bundle.putInt(EXTRA_DIGITAL_GOODS_API_VERSION, DIGITAL_GOODS_API_VERSION);
        return bundle;
    }

    public boolean handle(@NonNull String commandName, @NonNull Bundle args,
            @Nullable DigitalGoodsCallback callback) {
        switch (commandName) {
            case GetDetailsCall.COMMAND_NAME:
                GetDetailsCall getDetailsCall = GetDetailsCall.create(args, callback);
                if (getDetailsCall == null) break;
                getDetailsCall.call(mWrapper);
                return true;
            case AcknowledgeCall.COMMAND_NAME:
                AcknowledgeCall acknowledgeCall = AcknowledgeCall.create(args, callback);
                if (acknowledgeCall == null) break;
                acknowledgeCall.call(mWrapper);
                return true;
            case ConsumeCall.COMMAND_NAME:
                ConsumeCall consumeCall = ConsumeCall.create(args, callback);
                if (consumeCall == null) break;
                consumeCall.call(mWrapper);
                return true;
            case ListPurchasesCall.COMMAND_NAME:
                ListPurchasesCall listPurchasesCall = ListPurchasesCall.create(callback);
                if (listPurchasesCall == null) break;
                listPurchasesCall.call(mWrapper);
                return true;
            case ListPurchaseHistoryCall.COMMAND_NAME:
                ListPurchaseHistoryCall call = ListPurchaseHistoryCall.create(callback);
                call.call(mWrapper);
                return true;
        }

        Logging.logUnknownCommand(commandName);
        return false;
    }
}
