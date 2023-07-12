# Play Billing

The Play Billing module provides capabilities for your TWA app to connect with [Google Play Billing library](https://developer.android.com/google/play/billing), for example you can:

* Query purchase history
* Initialize a payment
* Query SKU details

The module uses [Version 5](https://developer.android.com/google/play/billing/release-notes#5-2-1)  of Play Billing library.


## How to use it? (Website)
To use this API from your website you can do it as follows:
```js
const PAYMENT_METHOD = 'https://play.google.com/billing'; 
const SKUS = [  
     'android.test.purchased',  
     'android.test.canceled', 
] 

const service = await window.getDigitalGoodsService(PAYMENT_METHOD); 
const details = await service.getDetails(SKUS);
console.log(details);
```

## How to use it? (Android Project)
To use it from your Android project you will need to do two steps:
Add this to your `AndroidManifest.xml`

```xml
      <activity
            android:name="com.google.androidbrowserhelper.playbilling.provider.PaymentActivity"
            android:theme="@android:style/Theme.Translucent.NoTitleBar"
            android:configChanges="keyboardHidden|keyboard|orientation|screenLayout|screenSize"
            android:exported="true">

            <intent-filter>
                <action android:name="org.chromium.intent.action.PAY" />
            </intent-filter>

            <meta-data
                android:name="org.chromium.default_payment_method_name"
                android:value="https://play.google.com/billing" />
      </activity>

      <service
            android:name="com.google.androidbrowserhelper.playbilling.provider.PaymentService"
            android:exported="true" >
            <intent-filter>
                <action android:name="org.chromium.intent.action.IS_READY_TO_PAY" />
            </intent-filter>
      </service>
```

Create a new Service that extends `DelegationService` with this line in `onCreate()` method:
```java
registerExtraCommandHandler(new DigitalGoodsRequestHandler(getApplicationContext()));
```
Then add it to your AndroidManifest and make it exported (`android:exported="true"`).



You can find a working demo [here](https://github.com/GoogleChrome/android-browser-helper/tree/main/demos/twa-play-billing)
