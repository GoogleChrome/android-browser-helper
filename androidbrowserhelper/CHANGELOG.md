## Unreleased

### Behaviour changes

1. **Inbound `https` Intent origin validation:** Inbound `https` Intent URIs delivered to
   `LauncherActivity` and `ShortcutTrampolineActivity` are validated against `DEFAULT_URL`,
   `ADDITIONAL_TRUSTED_ORIGINS` (full origins preferred; legacy scheme-less host entries are
   normalised to `https://` with a deprecation warning), and any `BROWSABLE` `<intent-filter>` with
   a concrete (non-wildcard) `android:host` declared on `LauncherActivity` (or its
   `<activity-alias>`, matching the full filter including any `android:path`, `pathPrefix`, or
   `pathPattern` constraints; declare the origin in `ADDITIONAL_TRUSTED_ORIGINS` to trust all paths
   on that origin). Untrusted `https` URIs fall back to `DEFAULT_URL` in `LauncherActivity`
   (throwing `SecurityException` in debuggable builds) or cause `ShortcutTrampolineActivity` to
   drop the launch.
   To allow additional origins, configure `ADDITIONAL_TRUSTED_ORIGINS`, declare a matching
   `BROWSABLE` `<intent-filter>`, or override `LauncherActivity.isTrustedIntentUrl(Uri)`.
   `ShortcutTrampolineActivity` now also accepts scheme-less `ADDITIONAL_TRUSTED_ORIGINS` entries
   and `https` hosts from the intent-filters of the app's TWA launcher activities; it does not
   consult `isTrustedIntentUrl(Uri)` overrides. `https` and `content` schemes are now matched
   case-insensitively. Schemes and hosts are compared ASCII-case-insensitively; a URI or configured
   origin whose scheme or host contains non-ASCII characters (including percent-encoded ones) never
   matches, so declare internationalised domains in punycode (`xn--`) form.
2. **`EXTRA_ORIGINAL_LAUNCH_URL` suppression for rejected URIs:** An inbound `https` URI rejected
   by origin validation, or a `content://` URI rejected by content validation, is no longer
   attached to the outbound browser Intent via
   `TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL`. All other Intent URIs (including
   `http` and custom schemes) are forwarded as before.
3. **Inbound `content://` URI validation for share and file handling:** `content://` URIs without a
   live read grant (`FLAG_GRANT_READ_URI_PERMISSION`), or backed by a `ContentProvider` owned by the
   host app's package or UID, are dropped from share (`EXTRA_STREAM`) and file-handling
   flows to prevent confused-deputy re-grants of internal `FileProvider` files. Cross-profile URIs,
   which Android rewrites to `content://<userId>@<authority>/…`, are accepted; the ownership check
   applies to the authority without the user-id prefix. Shared `content://`
   URIs forwarded to the browser are always re-parsed (`Uri.parse(uri.toString())`) before being
   attached, including when every URI passes validation, so the canonicalisation applied at the
   validation site is also applied at the forwarding site. Apps that deliberately pass their own
   `FileProvider` content to their TWA after verifying the caller can opt out by overriding
   `LauncherActivity.isTrustedContentUri(Uri)`.
4. **Partial and empty share filtering:** When only some shared `content://` URIs are rejected, the
   share proceeds with the surviving URIs; when all shared URIs are rejected, the share still
   proceeds if `title` or `text` is present and is dropped only when neither `title` nor `text`
   remains. This partial/empty-share fallback holds in both release and debuggable builds (recovered
   shares log the rejected URIs at `ERROR` without throwing `SecurityException`). File handling is
   deliberately **all-or-nothing**: if any file-handling `content://` URI is rejected, the launch
   proceeds with no file-handling data attached (logged at `ERROR`, and throwing `SecurityException`
   in debuggable builds). Unlike shares, surviving file URIs are not forwarded.
5. **File-handling URI permission relaxed to read-only:** File handling now requires only
   `FLAG_GRANT_READ_URI_PERMISSION`, where `checkCallingOrSelfUriPermission(uri, READ | WRITE)`
   previously required both read and write mode bits. This relaxation is deliberate: most external
   senders grant read-only access, so files now open in the TWA (even if they cannot be saved back
   in-place) rather than failing to open at all.
6. **`getUrlForIntent()` overrides subject to origin validation:** Custom `LauncherActivity`
   subclasses that override `getUrlForIntent(Intent)` and synthesize a cross-origin `https` URL are
   now subject to the same origin validation as `Intent.getData()`. Override
   `LauncherActivity.isTrustedIntentUrl(Uri)` to permit trusted cross-origin targets.
7. **Overridable rejection reporting hook (`LauncherActivity.reportRejection`):** Added
   `protected void reportRejection(@NonNull String message, @NonNull RejectionOutcome outcome)` and
   `LauncherActivity.RejectionOutcome` (`DATA_FILTERED`, `PAYLOAD_DROPPED`,
   `LAUNCH_URL_SUBSTITUTED`) so subclasses can route rejection diagnostics or suppress the
   debuggable-build `SecurityException`. Overriding the hook affects reporting only; origin and
   `content://` enforcement are unchanged. Rejected shared URIs are now reported in a single
   aggregated `ERROR` log line rather than one line per URI. In release builds, rejection messages
   include only the scheme, host and port of the rejected URI.
8. **Delegation `Token` tied to `CustomTabsSession` establishment and cleared on fallback:**
   `TwaLauncher` now stores the delegation `Token` in `TokenStore` inside
   `launchWhenSessionEstablished()`—only after the chosen provider has bound its `CustomTabsService`
   and returned a non-null `CustomTabsSession`—rather than unconditionally at the end of `launch()`.
   On any non-TWA launch (`CUSTOM_TAB` or `BROWSER` mode) or when session creation fails
   (synchronous `bindCustomTabsServicePreservePriority()` failure or asynchronous `newSession()`
   returning `null`/throwing), `TwaLauncher` clears the stored slot (`mTokenStore.store(null)`) and
   logs a `Log.d` diagnostic under `TwaLauncher` so a fallback or unreachable provider does not
   acquire or retain `DelegationService` / Play Billing rights. Transient `onServiceDisconnected()`
   callbacks do **not** clear the token, and ChromeOS/ARC (`ChromeOsSupport.isRunningOnArc`) remains
   untouched because `DelegationService` manages the ARC token independently.
   * **Transient-failure and live-session revocation:** If a browser is transiently unavailable (for
     example mid-update or under memory pressure) when a launch occurs, or if a secondary launch
     (such as a deep link or shortcut) falls back to `CUSTOM_TAB`/`BROWSER` while a Trusted Web
     Activity session is already running, the single `SharedPreferencesTokenStore` slot is cleared
     out from under the live session (with a `Log.d` diagnostic), suspending notification delegation
     for the remainder of that session until the next successful TWA-mode launch self-heals the
     slot.
   * **Escape hatch and `ShortcutTrampolineActivity` limitation:** There is no manifest flag to
     retain stale delegation tokens across non-TWA launches. Apps that need to preserve the stored
     token across `LauncherActivity` fallback launches can override
     `LauncherActivity.createTwaLauncher()` (using the new `protected`
     `LauncherActivity.getMetadata()` accessor) and inject a `TokenStore` decorator that drops
     `store(null)` calls. **Note:** `ShortcutTrampolineActivity` constructs its `TwaLauncher`
     internally with `new SharedPreferencesTokenStore(context)` and does not invoke
     `createTwaLauncher()`, so shortcut launches that fall back to a non-TWA mode will still clear
     the shared token store.
9. **Added `protected LauncherActivity.getMetadata()` accessor:** Subclasses overriding
   `LauncherActivity.createTwaLauncher()` (for example to supply a custom `TokenStore`) can now read
   the parsed `LauncherActivityMetadata` via `getMetadata()` instead of re-parsing the manifest via
   `LauncherActivityMetadata.parse(this)`.
10. **`LAUNCHING_BROWSER` manifest metadata remains developer-declared:** When
    `android.support.customtabs.trusted.LAUNCHING_BROWSER` is set, `TwaLauncher` targets that
    package directly without running `TwaProviderPicker`'s category filter (preserving enterprise
    and kiosk configurations that target a specific browser), while still requiring the package to
    establish a `CustomTabsSession` before its delegation token is stored.
11. **Require system or Play Store installation for non-default TWA providers:**
    When no single default browser is set (`MATCH_DEFAULT_ONLY` returning multiple
    `CATEGORY_DEFAULT` browsers, so `defaultOrderedCount != 1`) or the user's default browser does
    not support Trusted Web Activities, `TwaProviderPicker.pickProvider()` now requires a
    non-authoritative TWA candidate (including `ChromeLegacyUtils` local-build package names
    `org.chromium.chrome` and
    `com.google.android.apps.chrome`) to be preinstalled on the system image (`FLAG_SYSTEM` /
    `FLAG_UPDATED_SYSTEM_APP`) or installed by Google Play (`com.android.vending` via
    `PackageManager.getInstallerPackageName()`) in order to enter `LaunchMode.TRUSTED_WEB_ACTIVITY`.
    Unprivileged (sideloaded or alternative-store) non-default TWA candidates are excluded from
    `LaunchMode.TRUSTED_WEB_ACTIVITY` and downgraded to `LaunchMode.CUSTOM_TAB` (`bestCctProvider`),
    which clears `TokenStore` (`mTokenStore.store(null)`) so an unprivileged package cannot acquire
    `DelegationService` or Play Billing capabilities. When no single default browser is set, the
    fallback Custom Tabs provider (`LaunchMode.CUSTOM_TAB`) and plain browser (`LaunchMode.BROWSER`)
    are also chosen preferring system- or Play-installed packages, because the chosen package
    receives the launch URL directly; the authoritative single default browser still always wins.
    * **Opt-outs:** (1) When the user explicitly sets a browser (including a sideloaded or
      alternative-store build) as their default browser in Android OS settings (`MATCH_DEFAULT_ONLY`
      returning a single authoritative entry, `defaultOrderedCount == 1`), that browser still wins
      `LaunchMode.TRUSTED_WEB_ACTIVITY` regardless of install source. (2) Developers targeting a
      specific browser can declare `android.support.customtabs.trusted.LAUNCHING_BROWSER` in
      `AndroidManifest.xml` or pass `providerPackage` directly to `TwaLauncher`.
    * **"Manage space" and site-settings shortcut:** When a launch resolves to a non-TWA mode
      (`CUSTOM_TAB` or `BROWSER`), `LauncherActivity` no longer records that package as the
      "Manage space" / site-settings target and clears the stored record, so a transient fallback
      removes the site-settings shortcut until the next `TRUSTED_WEB_ACTIVITY` launch.

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