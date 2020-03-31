package org.chromium;

import org.chromium.IsReadyToPayServiceCallback;

interface IsReadyToPayService {
    oneway void isReadyToPay(IsReadyToPayServiceCallback callback);
}
