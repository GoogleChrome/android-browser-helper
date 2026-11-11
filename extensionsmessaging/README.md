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

### Verifying Official Chrome Callers and Extensions

Call `BrowserIdentityVerifier.isTrustedChromeCaller(context)` **synchronously on the incoming Binder thread** inside `connectExtension` (before `Binder.clearCallingIdentity()` or hopping to an executor), then check `isVerifiedExtension(extensionInfo)` and your extension ID allowlist:

```java
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.androidbrowserhelper.extensions.messaging.BrowserIdentityVerifier;

import org.chromium.chrome.browser.extensions.api.messaging.IBrowserNativeMessageService;
import org.chromium.chrome.browser.extensions.api.messaging.IConnectExtensionCallback;
import org.chromium.chrome.browser.extensions.api.messaging.IExtensionNativeMessageService;

import java.util.Collections;
import java.util.Set;

public class MyNativeMessagingService extends Service {
    private static final Set<String> ALLOWED_EXTENSION_IDS =
            Collections.singleton("abcdefghijklmnopabcdefghijklmnop");

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
                    //    release (Stable, Beta, Dev, or Canary). Must run synchronously on the
                    //    incoming Binder thread before hopping to any executor/coroutine.
                    if (!BrowserIdentityVerifier.isTrustedChromeCaller(
                            getApplicationContext())) {
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

To verify another browser against your own pinned 32-byte SHA-256 certificate digests (for example from `apksigner verify --print-certs browser.apk`), match the calling package name and pass its expected digests to `BrowserIdentityVerifier.isCallerSignedBy(context, expectedCerts)` on the Binder thread:

```java
List<byte[]> expectedCerts = Collections.emptyList();
String[] packages = getPackageManager().getPackagesForUid(Binder.getCallingUid());
if (packages != null) {
    for (String pkg : packages) {
        if ("com.example.browser".equals(pkg)) {
            expectedCerts = EXAMPLE_BROWSER_CERT_SHA256S;
            break;
        }
    }
}
if (!BrowserIdentityVerifier.isCallerSignedBy(getApplicationContext(), expectedCerts)) {
    notifyError(callback, "Untrusted browser caller");
    return;
}
```
