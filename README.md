# Android Browser Helper

![CI Status Badge](https://github.com/GoogleChrome/android-browser-helper/actions/workflows/android.yml/badge.svg?branch=main)

The Android Browser Helper library helps developers use Custom Tabs and Trusted
Web Activities on top of the AndroidX browser support library.
It contains default implementations of many of the common tasks a
developer will find themselves requiring, for example:

* Creating a Launcher Activity that simply launches a Trusted Web Activity.
* Code for choosing an appropriate Custom Tabs provider.
* Creating an Activity to launch the browser's site settings for a TWA.

## Adding Android Browser Helper to an Android project

Android Browser helper is available on the Google Maven. To use it, modify your application's
`build.gradle` and add the library as a dependency, as described below:

```gradle
dependencies {
    //...
    implementation 'com.google.androidbrowserhelper:androidbrowserhelper:2.7.3'
}

``` 

## Information for Google Play's data disclosure requirements

The Android Browser Helper library is intended to allow Android applications to interact with
browsers on the device. As such, it will share certain types of information with the browser.

### Data types collected / shared

**Web browsing:** URLs handled by the application are shared with the browser when a Custom Tab
or a Trusted Web Activity are launched.

URLs are also shared with the browser by certain features like mayLaunchUrl(), so that the
browser can speed up loading performance of those pages.

When the WebView fallback feature  is enabled by the developer, the application may store the
navigation history and browser storage, like cookies on the device.

**User location (Optional):** The SDK may share location data with the host browser, when the
location delegation library is used. Users can control sharing of the location using the
Android permission dialogs and the System settings. 

**Purchase History (Optional):** The SDK may share purchase history data with the host browser
when the Google Play billing library is used. Only purchases made within the application are
shared.

This SDK does not transfer any information over the network. Web browsing information may be
stored if the WebView fallback is enabled. The permission to read the location can be managed
via the usual Android settings.
  
## Using Shortcuts in Trusted Web Activities

When implementing shortcuts (e.g. from `shortcuts.xml`) in a Trusted Web Activity (TWA) application, launching the TWA through `LauncherActivity` on Android Desktop (such as ChromeOS) can result in unresponsive windows due to window manager interactions with translucent activities.

To prevent this issue, you should use the dedicated `ShortcutTrampolineActivity` for all your app's shortcut intents.

### 1. Create a `shortcuts.xml` resource

Create `res/xml/shortcuts.xml` and target `ShortcutTrampolineActivity` as the `targetClass`, passing the shortcut target URL in the `android:data` field:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="twa_shortcut"
        android:enabled="true"
        android:icon="@mipmap/ic_launcher"
        android:shortcutShortLabel="@string/shortcut_label">
        <intent
            android:action="android.intent.action.VIEW"
            android:targetPackage="YOUR_PACKAGE_NAME"
            android:targetClass="com.google.androidbrowserhelper.trusted.ShortcutTrampolineActivity"
            android:data="https://your-twa-domain.com/shortcut-target-url" />
    </shortcut>
</shortcuts>
```

### 2. Reference the shortcuts in your Launcher Activity

In your `AndroidManifest.xml`, reference `shortcuts.xml` within the `<activity>` tag of your main launcher activity:

```xml
        <activity android:name=".MyLauncherActivity" ...>
            <meta-data android:name="android.app.shortcuts"
                android:resource="@xml/shortcuts" />
            ...
        </activity>
```

`ShortcutTrampolineActivity` runs with `Theme.NoDisplay` and will process the shortcut launch securely by validating the URL against your configured TWA domains, routing the launch asynchronously using the application context, and closing itself instantly before any window transitions are impacted.

## Source Code Headers

Every file containing source code must include copyright and license
information. This includes any JS/CSS files that you might be serving out to
browsers. (This is to help well-intentioned people avoid accidental copying that
doesn't comply with the license.)

Apache header:

    Copyright 2019 Google LLC

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
