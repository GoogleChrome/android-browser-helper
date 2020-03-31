package com.google.androidbrowserhelper.playbilling;

import com.google.androidbrowserhelper.playbilling.IIsReadyToPayServiceCallback;

interface IIsReadyToPayService {
    oneway void isReadyToPay(IIsReadyToPayServiceCallback callback);
}
