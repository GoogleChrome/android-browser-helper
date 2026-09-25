## Unreleased

### Behaviour changes

1. **Inbound `https` Intent origin validation:** Inbound `https` Intent URIs delivered to
   `LauncherActivity` and `ShortcutTrampolineActivity` are validated against `DEFAULT_URL`,
   `ADDITIONAL_TRUSTED_ORIGINS` (full origins preferred; legacy scheme-less host entries are
   normalised to `https://` with a deprecation warning), and any `BROWSABLE` `<intent-filter>` with
   a concrete `android:host` declared on `LauncherActivity` (or its `<activity-alias>`). Untrusted
   `https` URIs fall back to `DEFAULT_URL` in `LauncherActivity` (throwing `SecurityException` in
   debuggable builds) or cause `ShortcutTrampolineActivity` to drop the launch.
   To allow additional origins, configure `ADDITIONAL_TRUSTED_ORIGINS`, declare a matching
   `BROWSABLE` `<intent-filter>`, or override `LauncherActivity.isTrustedIntentUrl(Uri)`.
2. **`EXTRA_ORIGINAL_LAUNCH_URL` suppression for rejected URIs:** Rejected inbound Intent URIs
   are no longer attached to the outbound browser Intent via
   `TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL`.
3. **`getUrlForIntent()` overrides subject to origin validation:** Custom `LauncherActivity`
   subclasses that override `getUrlForIntent(Intent)` and synthesize a cross-origin `https` URL are
   now subject to the same origin validation as `Intent.getData()`. Override
   `LauncherActivity.isTrustedIntentUrl(Uri)` to permit trusted cross-origin targets.
4. **`WebViewFallbackActivity` launch URL and `EXTRA_ORIGINS` validation:**
   `WebViewFallbackActivity` validates its launch URL against the app's configured TWA origins /
   manifest `<intent-filter>`s (falling back to `DEFAULT_URL` if untrusted) and drops any
   `EXTRA_ORIGINS` Intent extras that are not declared in `ADDITIONAL_TRUSTED_ORIGINS`.
5. **Overridable rejection reporting hook (`LauncherActivity.reportRejection`):** Added
   `protected void reportRejection(@NonNull String message, @NonNull RejectionOutcome outcome)` and
   `LauncherActivity.RejectionOutcome` (`LAUNCH_URL_SUBSTITUTED`) so subclasses can route rejection
   diagnostics or suppress the debuggable-build `SecurityException`. Overriding the hook affects
   reporting only; origin enforcement is unchanged.

## 2.6.2

* [#520](https://github.com/GoogleChrome/android-browser-helper/pull/520). The TWA launcher will
no longer wait for the animation to finish before launching the Custom Tabs Activity, which
improves launch time. This change does not affect the user-visible launch animation.

This behavior can be reverted by add following lines in TWA app's AndroidManifest.xml inside
the application tag: 
```
<meta-data
    android:name="android.support.customtabs.trusted.START_CHROME_BEFORE_ANIMATION_COMPLETE"
    android:value="false"
/>
```

## 1.1.0 (2020-01-22)

* [#53](https://github.com/GoogleChrome/android-browser-helper/pull/53) Provide an Intent to the 
  browser to focus the TWA. #53 ([@PEConn](https://github.com/PEConn))
  
## 1.0.0 (2020-01-08)

* android-browser-helper is stable :rocket:
* Uses androidx.browser.1.2.0
* Added Dark Mode Support for the Navigation bar and Status bar