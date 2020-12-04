package com.google.androidbrowserhelper.demos.playbilling;

import com.google.androidbrowserhelper.playbilling.digitalgoods.DigitalGoodsRequestHandler;
import com.google.androidbrowserhelper.trusted.DelegationService;

public class ExtraFeaturesService extends DelegationService {
    @Override
    public void onCreate() {
        super.onCreate();

        registerExtraCommandHandler(new DigitalGoodsRequestHandler(getApplicationContext()));
    }
}
