# Android Browser Helper /  Multi Domain Trusted Web Activities (TWA) Demo

This demo app shows how a developer can configure the TWA Launcher Activity in their Android
Manifest to launch a Trusted Web Activity that supports multiple origins.

## What are the use-cases?

Sometimes developers use different domains for different parts of their application. A developer
could use `login.example.com` when users are logging in into their service or `checkout.example.com`
in their shopping cart experience.

When using Trusted Web Activities, developers want to maintain the user in full screen when
navigating across those domains.

TWAs offer this possiblity, by allowing developers to add additional trusted origins, when launching
the Trusted Web Activity. All origins must be validated using Digital Asset Links. 

## How to enable Multi Domain TWAs
The first step to enable additional origins is the ensure that the `asset_statements` declaration
contains all origins to be validated. Example:

```xml
    <string name="asset_statements">
    [{
        \"relation\": [\"delegate_permission/common.handle_all_urls\"],
        \"target\": {
            \"namespace\": \"web\",
            \"site\": \"https://github.com\"
        }
    }],
    [{
        \"relation\": [\"delegate_permission/common.handle_all_urls\"],
        \"target\": {
            \"namespace\": \"web\",
            \"site\": \"https://www.google.com\"
        }
    }],
    [{
        \"relation\": [\"delegate_permission/common.handle_all_urls\"],
        \"target\": {
            \"namespace\": \"web\",
            \"site\": \"https://www.wikipedia.com\"
        }
    }],
    </string>
```

The second step is to create a string-array, inside `src/main/values/strings.xml`, with containing
the extra origins to be validated. Adding the origin referenced in the launch URL is not necessary:
```xml
    <string-array name="additional_trusted_origins">
        <item>https://www.google.com</item>
        <item>https://www.wikipedia.com</item>
    </string-array>
```

The last step to add support for multiple domains is achieved by adding an extra meta-tag to
 `AndroidManifest.xml,`, inside the `activity` tag for the Launcher Activity:
 
 ```xml
 <activity android:name="com.google.androidbrowserhelper.trusted.LauncherActivity"
      ...
     <meta-data android:name="android.support.customtabs.trusted.ADDITIONAL_TRUSTED_ORIGINS"
         android:resource="@array/additional_trusted_origins" /> 
     ...
 </activity>
 ```
 
 Finally, don't forget that each origin must have its own assetlinks file, [connecting the origin to
 the the application](https://developers.google.com/web/updates/2019/02/using-twa#link-site-to-app).
 The file for each domain will look exactly the same.
