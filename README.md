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
    implementation 'com.google.androidbrowserhelper:androidbrowserhelper:2.7.4'
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
  
## Quality enforcement (`QualityEnforcer`)

By design, `QualityEnforcer` is the default `CustomTabsCallback` wired by `LauncherActivity.getCustomTabsCallback()` (which returns `new QualityEnforcer()`), `ShortcutTrampolineActivity`, and the convenience `TwaLauncher.launch(Uri)` overload. When the connected Trusted Web Activity provider sends the `quality_enforcement.crash` extra callback (`QualityEnforcer.CRASH`, e.g. when a verified-origin navigation fails or violates TWA quality criteria), `QualityEnforcer` deliberately throws an uncaught `RuntimeException` on the host application's main thread. This behaviour is pre-existing and supported; a host application that wants to opt out of provider-triggered quality-enforcement crashes can override the `protected` `LauncherActivity.getCustomTabsCallback()` method and return a plain `CustomTabsCallback` (or pass a plain `CustomTabsCallback` to the full `TwaLauncher.launch(...)` overload). Overriding `LauncherActivity.getCustomTabsCallback()` does **not** affect shortcut launches, because `ShortcutTrampolineActivity` constructs `new QualityEnforcer()` itself.

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

## Inbound Intent validation

`LauncherActivity` and `ShortcutTrampolineActivity` validate inbound `Intent` data before forwarding it to the browser:

* **Inbound `https` launch URIs:** `LauncherActivity` (including URLs synthesized by `getUrlForIntent()`) and `ShortcutTrampolineActivity` verify that the target origin matches `android.support.customtabs.trusted.DEFAULT_URL` or an entry in `android.support.customtabs.trusted.ADDITIONAL_TRUSTED_ORIGINS`, or that the URI matches a `BROWSABLE` `<intent-filter>` with a concrete `android:host` declared on the launcher activity (or its `<activity-alias>`, including any `android:path` / `pathPrefix` / `pathPattern` constraints on that filter). Filters whose host is the wildcard `*` are ignored. Schemes and hosts are compared ASCII-case-insensitively; a URI or configured origin whose scheme or host contains non-ASCII characters (including percent-encoded ones) never matches, so declare internationalised domains in punycode (`xn--`) form. `LauncherActivity` falls back to `DEFAULT_URL` for an untrusted `https` URI and does not forward it via `EXTRA_ORIGINAL_LAUNCH_URL`; `ShortcutTrampolineActivity` drops the launch. `ShortcutTrampolineActivity` does not consult `LauncherActivity.isTrustedIntentUrl(Uri)` overrides.
* **`WebViewFallbackActivity`** trusts the launch URL and `EXTRA_ORIGINS` it is started with: they come from the app's own configuration or from URLs already validated by `LauncherActivity` or `ShortcutTrampolineActivity`. Declare it without an `<intent-filter>` and do not set `android:exported="true"`.
* **Inbound `content://` URIs (share & file handling):** `LauncherActivity` requires shared (`EXTRA_STREAM`) and file-handling `content://` URIs to carry a live read permission grant (`FLAG_GRANT_READ_URI_PERMISSION`) and to be backed by an external `ContentProvider` (not owned by the host application's package or UID, preventing confused-deputy re-grants of internal `FileProvider` paths). Cross-profile URIs, which Android rewrites to `content://<userId>@<authority>/…`, are accepted; the ownership check applies to the authority without the user-id prefix. Rejected URIs are stripped from the share or file-handling payload (and omitted from `EXTRA_ORIGINAL_LAUNCH_URL`).
* **Development-time diagnostics:** `LauncherActivity` rejections are logged at `ERROR` (naming the rejected URI and the applicable remedy) in all builds, and additionally throw a `SecurityException` in debuggable builds (`ApplicationInfo.FLAG_DEBUGGABLE`) **only when the rejection changed the launch outcome** (an untrusted launch URL falling back to `DEFAULT_URL` (`RejectionOutcome.LAUNCH_URL_SUBSTITUTED`), or a share / file-handling payload dropped in its entirety (`RejectionOutcome.PAYLOAD_DROPPED`)). A share that proceeds with its surviving URIs (or with `title`/`text` when all URIs are rejected) reports `RejectionOutcome.DATA_FILTERED`, logs at `ERROR`, and never throws, so the diagnostic does not alter the launch behaviour it is reporting on. Subclasses can override `LauncherActivity.reportRejection(String, RejectionOutcome)` to route these diagnostics elsewhere or suppress the debuggable-build exception; overriding affects reporting only and does **not** relax enforcement. `ShortcutTrampolineActivity` logs rejections at `ERROR` and never throws. In release builds, rejection messages include only the scheme, host and port of the rejected URI.

| Situation | Remedy |
| --- | --- |
| Second web origin, inbound deep links | `ADDITIONAL_TRUSTED_ORIGINS` (full origins such as `https://sub.example.com` preferred; scheme-less entries are accepted by inbound validation with a deprecation warning, but are not verified by the browser or used by `WebViewFallbackActivity`) |
| Second host already in the manifest | `BROWSABLE` `<intent-filter>` with a concrete `android:host` (wildcard `android:host="*"` is ignored) on `LauncherActivity` or its `<activity-alias>` (matches the full filter, including any `android:path*` constraints; use `ADDITIONAL_TRUSTED_ORIGINS` to trust the entire origin regardless of path) |
| Arbitrary partner / callback origins | Override `LauncherActivity.isTrustedIntentUrl(Uri)` (not applied to `ShortcutTrampolineActivity`) |
| App passes its own `FileProvider` content to the TWA | Override `LauncherActivity.isTrustedContentUri(Uri)` |
| Diagnostics must not throw in a debuggable build | Override `LauncherActivity.reportRejection(String, RejectionOutcome)` |

## Browser provider selection and delegation token lifecycle

* **Provider selection (`TwaProviderPicker`):** When the user has a single configured default browser that supports Trusted Web Activities (`MATCH_DEFAULT_ONLY` returning a single authoritative entry, `defaultOrderedCount == 1`), it is selected immediately in `LaunchMode.TRUSTED_WEB_ACTIVITY` regardless of install source. When no single default browser is set (`defaultOrderedCount != 1`) or the default browser is not TWA-capable, `TwaProviderPicker` requires a non-authoritative TWA candidate (including `ChromeLegacyUtils` local-build package names `org.chromium.chrome` and `com.google.android.apps.chrome`) to be preinstalled on the system image (`FLAG_SYSTEM` / `FLAG_UPDATED_SYSTEM_APP`) or installed by Google Play (`com.android.vending`) to launch in `LaunchMode.TRUSTED_WEB_ACTIVITY`. Unprivileged (sideloaded or alternative-store) non-default TWA candidates are excluded from `LaunchMode.TRUSTED_WEB_ACTIVITY` and downgraded to `LaunchMode.CUSTOM_TAB` (which clears `TokenStore` so the unprivileged package never receives `DelegationService` or Play Billing rights). Likewise, when no single default browser is set, the fallback Custom Tabs provider (`LaunchMode.CUSTOM_TAB`) and plain browser (`LaunchMode.BROWSER`) are also chosen preferring system- or Play-installed packages, because the chosen package receives the launch URL directly; the authoritative single default browser still always wins. **Opt-outs:** to launch a sideloaded or alternative-store browser in `LaunchMode.TRUSTED_WEB_ACTIVITY`, either (1) set that browser as the default browser in Android OS settings (`defaultOrderedCount == 1`), or (2) declare `android.support.customtabs.trusted.LAUNCHING_BROWSER` in `AndroidManifest.xml` (or pass `providerPackage` to `TwaLauncher`).
* **Explicit browser targeting (`LAUNCHING_BROWSER`):** `android.support.customtabs.trusted.LAUNCHING_BROWSER` is treated as a developer-declared target (for example in enterprise or kiosk deployments) and bypasses `TwaProviderPicker`'s category check, but still must bind a `CustomTabsService` and return a `CustomTabsSession` before receiving a delegation token.
* **Delegation token lifecycle (`TwaLauncher`):** On non-ARC devices, `TwaLauncher` stores the provider's delegation `Token` in `TokenStore` (gating `DelegationService` notification delegation and Play Billing verification) inside `launchWhenSessionEstablished()` once a `CustomTabsSession` has been created. On any non-TWA launch (`CUSTOM_TAB` or `BROWSER` fallback) or when session establishment fails, `TwaLauncher` clears the stored token (`mTokenStore.store(null)`) and emits a `Log.d` diagnostic (`TwaLauncher` logcat tag) naming the provider and launch mode. Service disconnection (`onServiceDisconnected`) does not clear the token, and ChromeOS/ARC (`ChromeOsSupport.isRunningOnArc`) is untouched because `DelegationService` manages the ARC token directly.
* **Transient failure and live-session revocation:** Because `SharedPreferencesTokenStore` holds a single slot, a transient bind/session failure (such as a browser updating in the background) or a secondary launch that resolves to `CUSTOM_TAB`/`BROWSER` mode while a TWA session is already active will clear the stored token for the remainder of that session until the next successful TWA-mode launch restores it.
* **Custom `TokenStore` escape hatch and `ShortcutTrampolineActivity` limitation:** There is no manifest metadata flag to retain stale tokens across non-TWA launches. An app that needs to suppress `store(null)` on `LauncherActivity` fallback launches can override `LauncherActivity.createTwaLauncher()` (using `getMetadata()` to inspect parsed manifest metadata) and pass a `TokenStore` decorator to the 4-argument `TwaLauncher` constructor. **Limitation:** `ShortcutTrampolineActivity` constructs its `TwaLauncher` internally with `new SharedPreferencesTokenStore(context)` and does not call `LauncherActivity.createTwaLauncher()`, so a shortcut launch that falls back to a non-TWA mode will still clear the shared `SharedPreferencesTokenStore`.

## WebView fallback security (`WebViewFallbackActivity`)

When `android.support.customtabs.trusted.FALLBACK_STRATEGY` is set to `"webview"` and no Trusted Web Activity provider is available, `WebViewFallbackActivity` hardens its in-process `WebView`:

* **`WebSettings` file and content access lockdown:** `WebViewFallbackActivity.setupWebSettings(WebSettings)` disables `file://` and `content://` access by default (`setAllowFileAccess(false)`, `setAllowContentAccess(false)`, `setAllowFileAccessFromFileURLs(false)`, and `setAllowUniversalAccessFromFileURLs(false)`), both on initial creation and when recreating the `WebView` after renderer process termination (`onRenderProcessGone`). **Opt-out:** subclasses that intentionally load local assets in a custom `WebViewFallbackActivity` can override `protected void setupWebSettings(@NonNull WebSettings webSettings)`.
* **Off-origin navigation and scheme allowlist (`shouldOverrideUrlLoading`):** Only navigations to `https` URLs matching the app's configured trusted origins (`mLaunchUrl` or `mExtraOrigins`, checked via `FallbackWebViewClient.isTrustedOrigin(Uri)`), `data:` URIs (used by inline web features such as SVGOMG's Demo loader), `about:blank` / `about:srcdoc`, and `blob:` URIs whose inner origin is `https` and trusted (`isTrustedOrigin(inner)`) are loaded inside the host `WebView` (`return false`). On the main frame, untrusted `http` and `https` navigations are handed off externally via `CustomTabsIntent`, while other external schemes (`tel:`, `mailto:`, `sms:`, `geo:`, `market:`, or custom app schemes) launch an external `Intent.ACTION_VIEW` with `Intent.CATEGORY_BROWSABLE` and no Custom Tab extras. `file:`, `content:`, `javascript:`, `intent:`, untrusted `blob:`, and other `about:` URIs are blocked (`return true`) without firing an external `Intent`. Subframe navigations (`!request.isForMainFrame()`) never launch an external activity (`return true` without firing an `Intent`), and all external dispatches **fail closed** (`return true`, cancelling the in-WebView load) even if launching an external handler throws `ActivityNotFoundException` or `SecurityException`. **Opt-out:** subclasses that require custom off-origin or custom-scheme navigation handling can override `protected WebViewClient createWebViewClient()` and return a subclass of `protected class FallbackWebViewClient extends WebViewClient` overriding `protected boolean shouldOverrideUrlLoading(@NonNull Uri url, boolean isMainFrame)` (with `protected boolean isTrustedOrigin(@Nullable Uri uri)` available to inspect origin trust); `WebViewFallbackActivity` caches the returned `WebViewClient` and re-attaches it across renderer crashes in `onRenderProcessGone`:

```java
@Override
protected WebViewClient createWebViewClient() {
    return new FallbackWebViewClient() {
        @Override
        protected boolean shouldOverrideUrlLoading(@NonNull Uri url, boolean isMainFrame) {
            if ("myapp".equalsIgnoreCase(url.getScheme())) {
                return false;
            }
            return super.shouldOverrideUrlLoading(url, isMainFrame);
        }
    };
}
```

## Launching in a Specific Browser

By default, Android Browser Helper uses `TwaProviderPicker` to pick an installed browser that supports Trusted Web Activities (preferring the user's default browser).

You can instead require that the TWA is launched in a specific browser, and optionally verify that browser's signing certificate.

### Configuring the browser in `AndroidManifest.xml`

Add the following `<meta-data>` tags to your `LauncherActivity` declaration:

```xml
<activity android:name="com.google.androidbrowserhelper.trusted.LauncherActivity" ...>
    ...
    <!-- The package name of the browser to launch the TWA in. -->
    <meta-data
        android:name="android.support.customtabs.trusted.LAUNCHING_BROWSER"
        android:value="com.example.browser" />

    <!-- Optional: The browser name shown to the user if the browser can't be used. -->
    <meta-data
        android:name="android.support.customtabs.trusted.LAUNCHING_BROWSER_NAME"
        android:value="Example Browser" />

    <!-- Optional: The expected identity of the browser, see below. -->
    <meta-data
        android:name="android.support.customtabs.trusted.LAUNCHING_BROWSER_TOKEN"
        android:value="BASE64_SERIALIZED_TOKEN" />
</activity>
```

- If only `LAUNCHING_BROWSER` is set, the TWA is launched in the app installed under that package name.
- If `LAUNCHING_BROWSER_TOKEN` is also set (recommended), the TWA is only launched if the app installed under the `LAUNCHING_BROWSER` package name matches the token, i.e. it has the same package name and signing certificate as the browser the token was created for.
- `LAUNCHING_BROWSER_TOKEN` must be used together with `LAUNCHING_BROWSER`.

If the browser is not installed, does not match the token, or the token is invalid or set without `LAUNCHING_BROWSER`, the TWA is not launched, and a dialog is shown to the user instead (or, for some app shortcut launches, nothing happens).

> **Warning:** Test your token before releasing your app. If the token is wrong (or blank), the TWA won't launch at all, and only an app update can fix this.

### Generating the `LAUNCHING_BROWSER_TOKEN`

The token is a standard (not URL-safe) base64-encoded, serialized `androidx.browser.trusted.Token`. You can generate it on a device where a trusted copy of the browser is installed (e.g. from the Play Store). On Android 11 (API 30) and above, the app generating the token must be able to [see the browser package](https://developer.android.com/training/package-visibility).

```java
import android.util.Base64;
import android.util.Log;
import androidx.browser.trusted.Token;

Token token = Token.create("com.example.browser", context.getPackageManager());
if (token != null) {
    String base64Token = Base64.encodeToString(token.serialize(), Base64.NO_WRAP);
    Log.d("TokenGenerator", "LAUNCHING_BROWSER_TOKEN: " + base64Token);
}
```

On Android 9 (API 28) and above, verification of browsers with a single signer uses `PackageManager.hasSigningCertificate()`, so it keeps working if the browser rotates its signing key using APK Signature Scheme v3. Note that this also means that the browser's original signing key keeps being accepted after a rotation. Browsers with multiple signers, and all browsers on older Android versions, must have exactly the same signing certificates as when the token was created.

### Using `TwaLauncher` directly

If you use `TwaLauncher` directly rather than `LauncherActivity`, pass the package name and expected `Token` to its constructor. Use the blocked dialog fallback strategy (which requires an `Activity` context), so that the URL isn't opened in a different browser if verification fails:

```java
Token expectedToken = Token.deserialize(Base64.decode(base64Token, Base64.DEFAULT));
TwaLauncher launcher = new TwaLauncher(context, "com.example.browser", sessionId, tokenStore,
        expectedToken);

launcher.launch(
        twaBuilder,
        customTabsCallback,
        splashScreenStrategy,
        completionCallback,
        TwaLauncher.getBlockedDialogFallbackStrategy("Example Browser"));
```

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
