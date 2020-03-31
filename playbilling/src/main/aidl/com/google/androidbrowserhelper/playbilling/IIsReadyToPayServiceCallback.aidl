package com.google.androidbrowserhelper.playbilling;

interface IIsReadyToPayServiceCallback {
    oneway void handleIsReadyToPay(boolean isReadyToPay);
}
