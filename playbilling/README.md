# Play Billing

Play Billing module provides capabilities for your TWA app to connect with [PlayBilling library](https://developer.android.com/google/play/billing) through APIs defined based on the library version, for example you can:

* Query Purchases history
* Initialize a payment
* Query SKU details (to be moved to ProductDetails later)

The module uses [Version 6](https://developer.android.com/google/play/billing/migrate-gpblv6) of PlayBilling Lib, 
currently we are not supporting [the new multi pricing subscription model](https://support.google.com/googleplay/android-developer/answer/12124625) but that will be fixed soon. 

You can find a working demo [here](https://github.com/GoogleChrome/android-browser-helper/tree/main/demos/twa-play-billing)