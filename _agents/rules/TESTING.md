# Testing Guide

This document explains how to run and write tests for the Android Browser Helper project.

## Types of Tests

The project contains two types of tests:

1.  **Unit Tests (Robolectric)**: Located in `src/test/`. These run on the JVM using Robolectric to simulate the Android environment. They are fast and should be used for testing business logic that doesn't require a real device.
2.  **Instrumentation Tests (AndroidX Test)**: Located in `src/androidTest/`. These run on a physical device or emulator. They are slower but test real integration with the Android OS.

## Running Tests

### Unit Tests

To run all unit tests in the project:

```sh
./gradlew test
```

To run unit tests for a specific module (e.g., `:androidbrowserhelper`):

```sh
./gradlew :androidbrowserhelper:test
```

### Instrumentation Tests

To run instrumentation tests, you must have an Android emulator running or a physical device connected via ADB.

To run all instrumentation tests:

```sh
./gradlew connectedAndroidTest
```

To run instrumentation tests for a specific module:

```sh
./gradlew :androidbrowserhelper:connectedAndroidTest
```

## Testing Frameworks

*   **JUnit 4**: The standard testing framework.
*   **Robolectric**: For running Android tests on the JVM.
*   **Mockito**: For mocking dependencies.

## Best Practices

*   **Test Coverage**: New features and bug fixes MUST be accompanied by corresponding tests.
*   **Modify Existing Tests**: If you modify existing code, check if there are existing tests for it and update them to cover the new behavior.
*   **Fakes and Mocks**: Use Mockito to mock system services or external dependencies when writing unit tests.

## Manual Testing & Debugging (TWAs)

When testing Trusted Web Activities (TWA) manually on a device or emulator, you often need to bypass Digital Asset Link (DAL) verification and inspect logs.

### Bypassing Digital Asset Link (DAL) Verification

Chrome requires DAL verification to enable features like notification delegation. To bypass this for a test domain (e.g., `example.com`):

1.  **Enable command line on non-rooted devices:**
    *   Open Chrome on the device/emulator.
    *   Navigate to `chrome://flags/#enable-command-line-on-non-rooted-devices`.
    *   Set it to **Enabled** and relaunch Chrome.
2.  **Set the bypass flag for your test domain:**
    ```sh
    adb shell "echo '_ --disable-digital-asset-link-verification-for-url=\"https://example.com\"' > /data/local/tmp/chrome-command-line"
    adb shell am force-stop com.android.chrome
    ```
3.  **Grant runtime permissions (if testing notifications):**
    ```sh
    adb shell pm grant <YOUR_PACKAGE_NAME> android.permission.POST_NOTIFICATIONS
    ```

### Checking App Links Verification Status

To verify if the OS has verified your app links:

```sh
# Get verification state
adb shell pm get-app-links <YOUR_PACKAGE_NAME>

# Force re-verification
adb shell pm verify-app-links --re-verify <YOUR_PACKAGE_NAME>
```

### Resetting App State for Testing

To ensure a clean state before running a test:

```sh
# Clear app data
adb shell pm clear <YOUR_PACKAGE_NAME>

# Uninstall the app
adb uninstall <YOUR_PACKAGE_NAME>

# Clear Chrome data (to reset verification cache)
adb shell pm clear com.android.chrome
```

### Useful Logcat Filters

Use these filters to debug TWA connection and delegation issues:

```sh
# Stream TWA delegation and activity logs
adb logcat -s NotificationDelegation:V TWALauncherActivity:V

# Stream TWA + Chrome logs together
adb logcat -s NotificationDelegation:V TWALauncherActivity:V chromium:V

# Dump logs for a specific TWA process
adb logcat -d --pid=$(adb shell pidof <YOUR_PACKAGE_NAME>)
```

