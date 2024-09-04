# Android Browser Helper

![CI Status Badge](https://github.com/GoogleChrome/android-browser-helper/workflows/Android%20CI/badge.svg)

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
    implementation 'com.google.androidbrowserhelper:androidbrowserhelper:2.4.0'
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
