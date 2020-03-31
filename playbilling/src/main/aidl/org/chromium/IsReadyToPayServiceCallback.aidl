package org.chromium;

interface IsReadyToPayServiceCallback {
    oneway void handleIsReadyToPay(boolean isReadyToPay);
}
