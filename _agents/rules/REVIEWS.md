# Code Review Checklist

This document provides a checklist for reviewing code changes in the Android Browser Helper project.

## Security & Privacy

*   **URL Validation**: Ensure any URLs passed to Custom Tabs or TWAs are validated and secure (e.g., HTTPS).
*   **Data Sharing**: Be mindful of what data is shared with the browser. Refer to the data disclosure section in `README.md`.
*   **Intent Spoofing**: Validate incoming intents if the app exposes activities that can be launched by other apps.

## Architecture & Interoperability

*   **Browser Compatibility**: Verify that changes do not assume Chrome is the only browser on the device. Use `TwaProviderPicker` to select the provider. Refer to [TWA Browser Support](../../docs/trusted-web-activity-browser-support.md) for known browser compatibility.
*   **AndroidX Browser Library**: Ensure compatibility with the version of `androidx.browser` being used.
*   **Backward Compatibility**: The library has a `minSdkVersion` of 23. Ensure new APIs used are guarded by SDK version checks if they are not available on older versions.

## Performance

*   **Warmup and MayLaunchUrl**: Use `CustomTabsClient.warmup` and `CustomTabsSession.mayLaunchUrl` where appropriate to improve launch performance.
*   **Resource Leakage**: Ensure Custom Tabs sessions and service connections are properly released when no longer needed.

## Testing & Quality

*   **Test coverage**: Ensure new code is covered by unit or instrumentation tests.
*   **Documentation**: Update `README.md` or other documentation if public APIs are changed or added.
