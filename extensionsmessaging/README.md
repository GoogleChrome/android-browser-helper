# Extensions Messaging (`extensionsmessaging`)

Standalone Android library (`minSdkVersion 29`, no dependency on `:androidbrowserhelper`) for apps implementing a browser extension native messaging host service (`IBrowserNativeMessageService`) to verify the calling browser and extension.

## Setup

1. Enable AIDL generation and add the dependency in your module's `build.gradle`:

```groovy
android {
    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation 'com.google.androidbrowserhelper:extensionsmessaging:1.0.0'
}
```

2. Copy the `.aidl` files from Chromium's [`org/chromium/chrome/browser/extensions/api/messaging/`](https://source.chromium.org/chromium/chromium/src/+/main:chrome/browser/extensions/api/messaging/android/java/src/org/chromium/chrome/browser/extensions/api/messaging/) directory into `src/main/aidl/org/chromium/chrome/browser/extensions/api/messaging/`.

3. Declare your native messaging `Service` in `AndroidManifest.xml` (`tools:ignore="ExportedService"` suppresses the missing-permission Lint warning since caller authentication happens in `connectExtension`):

```xml
<service
    android:name=".MyNativeMessagingService"
    android:exported="true"
    tools:ignore="ExportedService">
    <intent-filter>
        <action android:name="org.chromium.chrome.browser.extensions.messaging.action.NATIVE_MESSAGING" />
    </intent-filter>
</service>
```

> **Note:** On Android 11+ (API 30+), bound services automatically have package visibility into the binding client, so no `<queries>` entry is needed.

## Usage

### Verifying Browser Callers and Extensions

Call `BrowserIdentityVerifier.isTrustedChromeCaller(context)` (and/or `isCallerSignedBy(context, OTHER_TRUSTED_BROWSERS)`) **synchronously on the incoming Binder thread** inside `connectExtension` (before `Binder.clearCallingIdentity()` or hopping to an executor), then check `isVerifiedExtension(extensionInfo)` and your extension ID allowlist:

```java
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.androidbrowserhelper.extensions.messaging.BrowserIdentityVerifier;

import org.chromium.chrome.browser.extensions.api.messaging.IBrowserNativeMessageService;
import org.chromium.chrome.browser.extensions.api.messaging.IConnectExtensionCallback;
import org.chromium.chrome.browser.extensions.api.messaging.IExtensionNativeMessageService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyNativeMessagingService extends Service {
    private static final Set<String> ALLOWED_EXTENSION_IDS =
            Collections.singleton("abcdefghijklmnopabcdefghijklmnop");
    // TODO: Add package names and 32-byte SHA-256 signing certificate digests of any
    // other browsers you want to support.
    private static final Map<String, List<byte[]>> OTHER_TRUSTED_BROWSERS =
            Collections.emptyMap();

    private final IBrowserNativeMessageService.Stub mBinder =
            new IBrowserNativeMessageService.Stub() {
                @Override
                public void connectExtension(
                        String extensionId,
                        Bundle extensionInfo,
                        IConnectExtensionCallback callback) {
                    if (callback == null) {
                        return;
                    }

                    // 1. Verify the Binder call comes from an official Google-signed Chrome
                    //    release (Stable, Beta, Dev, or Canary) or another configured browser.
                    //    Must run synchronously on the incoming Binder thread before hopping
                    //    to any executor/coroutine.
                    Context appContext = getApplicationContext();
                    boolean isTrustedBrowser =
                            BrowserIdentityVerifier.isTrustedChromeCaller(appContext)
                                    || BrowserIdentityVerifier.isCallerSignedBy(
                                            appContext, OTHER_TRUSTED_BROWSERS);
                    if (!isTrustedBrowser) {
                        notifyError(callback, "Untrusted browser caller");
                        return;
                    }

                    // 2. Reject unverified (e.g. unpacked) extensions.
                    if (!BrowserIdentityVerifier.isVerifiedExtension(extensionInfo)) {
                        notifyError(callback, "Unverified extension");
                        return;
                    }

                    // 3. Check the extension ID against your app's allowlist.
                    if (extensionId == null || !ALLOWED_EXTENSION_IDS.contains(extensionId)) {
                        notifyError(callback, "Extension not allowed");
                        return;
                    }

                    try {
                        callback.onSuccess(createExtensionSession(extensionId));
                    } catch (RemoteException ignored) {
                    }
                }
            };

    private IExtensionNativeMessageService.Stub createExtensionSession(String extensionId) {
        // Return your IExtensionNativeMessageService.Stub implementation here.
        throw new UnsupportedOperationException("Not implemented");
    }

    private static void notifyError(IConnectExtensionCallback callback, String message) {
        try {
            callback.onError(message);
        } catch (RemoteException ignored) {
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
```

### Verifying Other Browsers

To verify another browser against its own pinned 32-byte SHA-256 certificate digests (for example from `apksigner verify --print-certs browser.apk`), map each allowed browser package name to its expected digests and pass the map to `BrowserIdentityVerifier.isCallerSignedBy(context, trustedBrowsers)` on the Binder thread:

```java
Map<String, List<byte[]>> trustedBrowsers =
        Collections.singletonMap("com.example.browser", EXAMPLE_BROWSER_CERT_SHA256S);
if (!BrowserIdentityVerifier.isCallerSignedBy(getApplicationContext(), trustedBrowsers)) {
    notifyError(callback, "Untrusted browser caller");
    return;
}
```
