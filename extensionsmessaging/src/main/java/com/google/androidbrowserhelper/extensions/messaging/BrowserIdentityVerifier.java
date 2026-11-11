// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.androidbrowserhelper.extensions.messaging;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;

import androidx.annotation.BinderThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utilities for Android apps implementing a browser extension native messaging host service
 * ({@code IBrowserNativeMessageService}) to verify the identity of the calling browser and the
 * verification status of the connecting extension.
 *
 * <p>Because {@link Binder#getCallingUid()} relies on thread-local Binder IPC state, {@link
 * #isTrustedChromeCaller(Context)} and {@link #isCallerSignedBy(Context, List)} must be invoked
 * synchronously on the incoming Binder thread (for example, inside {@code connectExtension}) before
 * calling {@link Binder#clearCallingIdentity()} or posting work to another thread or executor.
 */
public final class BrowserIdentityVerifier {
    /**
     * Boolean extra key in the {@code extensionInfo} {@link Bundle} passed to {@code
     * connectExtension} indicating whether the browser verified the extension's contents against a
     * trusted store or policy source.
     */
    public static final String EXTRA_IS_VERIFIED = "isVerified";

    private static final String CHROME_STABLE_PACKAGE = "com.android.chrome";
    private static final String CHROME_BETA_PACKAGE = "com.chrome.beta";
    private static final String CHROME_DEV_PACKAGE = "com.chrome.dev";
    private static final String CHROME_CANARY_PACKAGE = "com.chrome.canary";

    // Official Google production release signing certificate SHA-256 digests.
    // Debug / userdebug test certificates are intentionally excluded.
    private static final String CERT_SHA256_CHROME_STABLE =
            "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83";
    private static final String CERT_SHA256_CHROME_BETA =
            "da633d34b69e63ae2103b49d53ce052fc5f7f3c53aab94fdc2a208bdfd14249c";
    private static final String CERT_SHA256_CHROME_DEV =
            "9044ee5fee4bbc5e21dd44665431c4eb1f1f71a32716a0bc927bcbb39233cabf";
    private static final String CERT_SHA256_CHROME_CANARY =
            "2019dfa1fb23efbf70c5bcd1443c5beab04f3f2ff4366e9ac1e3457639a24cfc";
    // Legacy Google release certificate (pulse-release) accepted for Beta, Dev, and Canary.
    private static final String CERT_SHA256_CHROME_LEGACY_CHANNEL =
            "3d7a1223019aa39d9ea0e3436ab7c0896bfb4fb679f4de5fe7c23f326c8f994a";

    private static final int SHA256_BYTE_LENGTH = 32;

    // Trusted Chrome packages -> SHA-256 digests of acceptable signing certs (any match suffices).
    // Channel-specific release certificates are listed first.
    private static final Map<String, List<byte[]>> TRUSTED_CHROME_CERTIFICATES =
            createTrustedChromeCertificates();

    private BrowserIdentityVerifier() {}

    /**
     * Returns whether the current incoming Binder call comes from an official, Google-signed
     * release of Chrome (Stable, Beta, Dev, or Canary).
     *
     * <p>Must be called synchronously on the Binder thread before calling {@link
     * Binder#clearCallingIdentity()} or hopping to another thread.
     *
     * @param context Application or Service {@link Context} used to access {@link PackageManager}.
     * @return {@code true} if the calling UID belongs to an official Chrome package signed with a
     *     pinned Google release certificate; {@code false} otherwise.
     * @throws NullPointerException if {@code context} is {@code null}.
     * @throws IllegalStateException if not called during an incoming cross-process Binder call
     *     (i.e. {@code Binder.getCallingUid() == Process.myUid()}).
     */
    @BinderThread
    public static boolean isTrustedChromeCaller(@NonNull Context context) {
        Objects.requireNonNull(context, "context must not be null");
        int callingUid = requireCrossProcessCallingUid();

        PackageManager pm = context.getPackageManager();
        String[] packages = pm.getPackagesForUid(callingUid);
        if (packages == null) {
            return false;
        }
        for (String pkg : packages) {
            if (pkg == null) {
                continue;
            }
            List<byte[]> certs = TRUSTED_CHROME_CERTIFICATES.get(pkg);
            if (certs == null) {
                continue;
            }
            for (byte[] cert : certs) {
                if (pm.hasSigningCertificate(pkg, cert, PackageManager.CERT_INPUT_SHA256)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Browser-agnostic helper returning whether the app making the current incoming Binder call is
     * signed by at least one of the supplied 32-byte SHA-256 certificate digests.
     *
     * <p>Uses {@link PackageManager#hasSigningCertificate(int, byte[], int)} with {@link
     * PackageManager#CERT_INPUT_SHA256}, which supports APK Signature Scheme v3 key rotation (and
     * returns {@code false} for multi-signer APKs).
     *
     * <p><strong>Security note:</strong> This method checks the calling UID's signing certificate
     * without checking its package name. If a browser vendor signs other non-browser apps with the
     * same release key, callers should also verify the expected package name via {@link
     * PackageManager#getPackagesForUid(int)}.
     *
     * <p>Must be called synchronously on the Binder thread before calling {@link
     * Binder#clearCallingIdentity()} or hopping to another thread.
     *
     * @param context Application or Service {@link Context} used to access {@link PackageManager}.
     * @param certSha256s Expected 32-byte SHA-256 signing certificate digests.
     * @return {@code true} if the calling UID is signed by at least one digest in {@code
     *     certSha256s}; {@code false} otherwise (including if {@code certSha256s} is empty).
     * @throws NullPointerException if {@code context}, {@code certSha256s}, or any element of
     *     {@code certSha256s} is {@code null}.
     * @throws IllegalArgumentException if any element of {@code certSha256s} is not 32 bytes long.
     * @throws IllegalStateException if not called during an incoming cross-process Binder call
     *     (i.e. {@code Binder.getCallingUid() == Process.myUid()}).
     */
    @BinderThread
    public static boolean isCallerSignedBy(
            @NonNull Context context, @NonNull List<byte[]> certSha256s) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(certSha256s, "certSha256s must not be null");
        List<byte[]> certsSnapshot = new ArrayList<>(certSha256s);
        for (byte[] cert : certsSnapshot) {
            Objects.requireNonNull(cert, "certSha256s elements must not be null");
            if (cert.length != SHA256_BYTE_LENGTH) {
                throw new IllegalArgumentException(
                        "Certificate SHA-256 digest must be 32 bytes, got " + cert.length);
            }
        }
        int callingUid = requireCrossProcessCallingUid();

        if (certsSnapshot.isEmpty()) {
            return false;
        }
        PackageManager pm = context.getPackageManager();
        for (byte[] cert : certsSnapshot) {
            if (pm.hasSigningCertificate(callingUid, cert, PackageManager.CERT_INPUT_SHA256)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the calling browser reports the connecting extension as store/policy verified
     * rather than an unverified local/unpacked install (which can choose an arbitrary extension ID
     * via its manifest {@code key}).
     *
     * <p><strong>Security note:</strong> Do not call this method (or inspect {@code extensionInfo})
     * until <em>after</em> authenticating the calling browser via {@link
     * #isTrustedChromeCaller(Context)} (or {@link #isCallerSignedBy(Context, List)}), both because
     * {@link #EXTRA_IS_VERIFIED} is populated by the caller and to avoid unparceling {@link Bundle}
     * payloads from untrusted local apps. This flag assumes Chrome's extension verification model
     * (other browsers must enforce equivalent content verification for this flag to be meaningful),
     * and callers must still validate {@code extensionId} against their own allowlist.
     *
     * @param extensionInfo The {@link Bundle} passed to {@code connectExtension}, or {@code null}.
     * @return {@code true} if {@code extensionInfo} is non-null, readable, and {@link
     *     #EXTRA_IS_VERIFIED} is {@code true}; {@code false} otherwise.
     */
    public static boolean isVerifiedExtension(@Nullable Bundle extensionInfo) {
        if (extensionInfo == null) {
            return false;
        }
        try {
            return extensionInfo.getBoolean(EXTRA_IS_VERIFIED, false);
        } catch (RuntimeException e) {
            // Fail closed if a malformed or unreadable Bundle throws (e.g. BadParcelableException)
            // during lazy unparceling.
            return false;
        }
    }

    private static int requireCrossProcessCallingUid() {
        int callingUid = Binder.getCallingUid();
        if (callingUid == Process.myUid()) {
            throw new IllegalStateException(
                    "Must be called synchronously during an incoming cross-process Binder call"
                            + " (before Binder.clearCallingIdentity() or posting work to another"
                            + " thread)");
        }
        return callingUid;
    }

    private static Map<String, List<byte[]>> createTrustedChromeCertificates() {
        Map<String, List<byte[]>> map = new HashMap<>();
        map.put(
                CHROME_STABLE_PACKAGE,
                Collections.singletonList(hexToBytes(CERT_SHA256_CHROME_STABLE)));
        map.put(
                CHROME_BETA_PACKAGE,
                Collections.unmodifiableList(
                        Arrays.asList(
                                hexToBytes(CERT_SHA256_CHROME_BETA),
                                hexToBytes(CERT_SHA256_CHROME_LEGACY_CHANNEL))));
        map.put(
                CHROME_DEV_PACKAGE,
                Collections.unmodifiableList(
                        Arrays.asList(
                                hexToBytes(CERT_SHA256_CHROME_DEV),
                                hexToBytes(CERT_SHA256_CHROME_LEGACY_CHANNEL))));
        map.put(
                CHROME_CANARY_PACKAGE,
                Collections.unmodifiableList(
                        Arrays.asList(
                                hexToBytes(CERT_SHA256_CHROME_CANARY),
                                hexToBytes(CERT_SHA256_CHROME_LEGACY_CHANNEL))));
        return Collections.unmodifiableMap(map);
    }

    @VisibleForTesting
    static byte[] hexToBytes(@NonNull String hex) {
        Objects.requireNonNull(hex, "hex must not be null");
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length: " + len);
        }
        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low = Character.digit(hex.charAt(i + 1), 16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException(
                        "Invalid hex character at index " + (high == -1 ? i : i + 1));
            }
            result[i / 2] = (byte) ((high << 4) + low);
        }
        return result;
    }
}
