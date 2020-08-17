package com.google.androidbrowserhelper.playbilling.digitalgoods;

import android.content.Context;
import android.os.Bundle;

import com.android.billingclient.api.SkuDetails;
import com.google.androidbrowserhelper.playbilling.provider.BillingWrapper;
import com.google.androidbrowserhelper.playbilling.provider.BillingWrapperFactory;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DigitalGoodsRequestHandler {
    private final BillingWrapper mWrapper;
    private final BillingWrapper.Listener mListener = new BillingWrapper.Listener() {
        @Override
        public void onConnected() {

        }

        @Override
        public void onDisconnected() {
            // TODO: Respond failure
        }

        @Override
        public void onPurchaseFlowComplete(int result) { }
    };

    public interface Callback {
        void run(String callbackName, Bundle args);
    }

    public DigitalGoodsRequestHandler(Context context) {
        mWrapper = BillingWrapperFactory.get(context, mListener);
    }

    public boolean handle(@NonNull String commandName, @NonNull Bundle args,
            @Nullable Callback callback) {
        switch (commandName) {
            case GetDetailsCall.COMMAND_NAME:
                GetDetailsCall getDetailsCall = GetDetailsCall.create(args, callback);
                if (getDetailsCall == null) break;

                mWrapper.querySkuDetails(Arrays.asList(getDetailsCall.itemIds), (code, details) -> {
                    int responseCode = code.getResponseCode();
                    ItemDetails[] itemDetails = new ItemDetails[details.size()];

                    int index = 0;
                    for (SkuDetails skuDetails : details) {
                        itemDetails[index++] = ItemDetails.create(skuDetails);
                    }

                    getDetailsCall.respond(responseCode, itemDetails);
                });

                return true;
            case AcknowledgeCall.COMMAND_NAME:
                AcknowledgeCall ackCall = AcknowledgeCall.create(args, callback);
                if (ackCall == null) break;

                // TODO(peconn): Implement
                return true;
        }
        return false;
    }
}
