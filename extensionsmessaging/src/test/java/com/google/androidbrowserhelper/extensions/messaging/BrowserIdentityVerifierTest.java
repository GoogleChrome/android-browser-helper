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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowBinder;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Unit tests for {@link BrowserIdentityVerifier}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(
        sdk = {Build.VERSION_CODES.Q},
        manifest = Config.NONE)
public class BrowserIdentityVerifierTest {
    private static final int REMOTE_UID = Process.myUid() + 1000;

    private static final byte[] CERT_CHROME_STABLE =
            decodeHex("f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83");
    private static final byte[] CERT_CHROME_BETA =
            decodeHex("da633d34b69e63ae2103b49d53ce052fc5f7f3c53aab94fdc2a208bdfd14249c");
    private static final byte[] CERT_CHROME_DEV =
            decodeHex("9044ee5fee4bbc5e21dd44665431c4eb1f1f71a32716a0bc927bcbb39233cabf");
    private static final byte[] CERT_CHROME_CANARY =
            decodeHex("2019dfa1fb23efbf70c5bcd1443c5beab04f3f2ff4366e9ac1e3457639a24cfc");
    private static final byte[] CERT_CHROME_LEGACY_CHANNEL =
            decodeHex("3d7a1223019aa39d9ea0e3436ab7c0896bfb4fb679f4de5fe7c23f326c8f994a");
    private static final byte[] CERT_USERDEBUG_TEST_KEY =
            decodeHex("1975b2f17177bc89a5dff31f9e64a6cae281a53dc1d1d59b1d147fe1c82afa00");
    private static final byte[] CERT_OTHER_BROWSER =
            decodeHex("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

    private Context mMockContext;
    private PackageManager mMockPackageManager;

    @Before
    public void setUp() {
        mMockContext = mock(Context.class);
        resetMockPackageManager();
        ShadowBinder.setCallingUid(REMOTE_UID);
    }

    @After
    public void tearDown() {
        ShadowBinder.reset();
    }

    @Test
    public void isTrustedChromeCaller_nullContext_throwsNullPointerException() {
        assertThrows(
                NullPointerException.class,
                () -> BrowserIdentityVerifier.isTrustedChromeCaller(null));
    }

    @Test
    public void isTrustedChromeCaller_sameProcessUid_throwsIllegalStateException() {
        ShadowBinder.setCallingUid(Process.myUid());
        assertThrows(
                IllegalStateException.class,
                () -> BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));
    }

    @Test
    public void callerChecks_outsideBinderTransactionDefaultState_throwIllegalStateException() {
        ShadowBinder.reset();
        assertThrows(
                IllegalStateException.class,
                () -> BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));
        assertThrows(
                IllegalStateException.class,
                () ->
                        BrowserIdentityVerifier.isCallerSignedBy(
                                mMockContext, Collections.singletonList(CERT_OTHER_BROWSER)));
    }

    @Test
    public void isTrustedChromeCaller_chromeStablePinnedCert_returnsTrue() {
        mockCallingPackageWithCert("com.android.chrome", CERT_CHROME_STABLE);
        assertTrue(BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));
    }

    @Test
    public void isTrustedChromeCaller_chromeBetaCurrentAndLegacyCerts_returnsTrue() {
        mockCallingPackageWithCert("com.chrome.beta", CERT_CHROME_BETA);
        assertTrue(BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));

        mockCallingPackageWithCert("com.chrome.beta", CERT_CHROME_LEGACY_CHANNEL);
        assertTrue(BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));
    }

    @Test
    public void isTrustedChromeCaller_chromeDevCurrentAndLegacyCerts_returnsTrue() {
        mockCallingPackageWithCert("com.chrome.dev", CERT_CHROME_DEV);
        assertTrue(BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));

        mockCallingPackageWithCert("com.chrome.dev", CERT_CHROME_LEGACY_CHANNEL);
        assertTrue(BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));
    }

    @Test
    public void isTrustedChromeCaller_chromeCanaryCurrentAndLegacyCerts_returnsTrue() {
        mockCallingPackageWithCert("com.chrome.canary", CERT_CHROME_CANARY);
        assertTrue(BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));

        mockCallingPackageWithCert("com.chrome.canary", CERT_CHROME_LEGACY_CHANNEL);
        assertTrue(BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));
    }

    @Test
    public void isTrustedChromeCaller_rejectsAllCrossChannelAndUserdebugCertCombinations() {
        assertPackageRejectsCerts(
                "com.android.chrome",
                Arrays.asList(
                        CERT_CHROME_BETA,
                        CERT_CHROME_DEV,
                        CERT_CHROME_CANARY,
                        CERT_CHROME_LEGACY_CHANNEL,
                        CERT_USERDEBUG_TEST_KEY));
        assertPackageRejectsCerts(
                "com.chrome.beta",
                Arrays.asList(
                        CERT_CHROME_STABLE,
                        CERT_CHROME_DEV,
                        CERT_CHROME_CANARY,
                        CERT_USERDEBUG_TEST_KEY));
        assertPackageRejectsCerts(
                "com.chrome.dev",
                Arrays.asList(
                        CERT_CHROME_STABLE,
                        CERT_CHROME_BETA,
                        CERT_CHROME_CANARY,
                        CERT_USERDEBUG_TEST_KEY));
        assertPackageRejectsCerts(
                "com.chrome.canary",
                Arrays.asList(
                        CERT_CHROME_STABLE,
                        CERT_CHROME_BETA,
                        CERT_CHROME_DEV,
                        CERT_USERDEBUG_TEST_KEY));
    }

    @Test
    public void isTrustedChromeCaller_rejectsNonOfficialPackagesEvenWithChromeCert() {
        for (String untrustedPkg :
                Arrays.asList(
                        "org.chromium.chrome",
                        "com.google.android.apps.chrome",
                        "com.example.spoof")) {
            mockCallingPackageWithCert(untrustedPkg, CERT_CHROME_STABLE);

            assertFalse(BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));
            verify(mMockPackageManager, never())
                    .hasSigningCertificate(eq(untrustedPkg), any(byte[].class), anyInt());
        }
    }

    @Test
    public void isTrustedChromeCaller_nullOrEmptyPackagesForUid_returnsFalse() {
        when(mMockPackageManager.getPackagesForUid(REMOTE_UID)).thenReturn(null);
        assertFalse(BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));

        when(mMockPackageManager.getPackagesForUid(REMOTE_UID)).thenReturn(new String[0]);
        assertFalse(BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));
    }

    @Test
    public void isTrustedChromeCaller_multiplePackagesInUidMatchesValidChromePackage() {
        when(mMockPackageManager.getPackagesForUid(REMOTE_UID))
                .thenReturn(
                        new String[] {
                            null, "com.example.helper", "com.android.chrome", "com.chrome.beta"
                        });
        when(mMockPackageManager.hasSigningCertificate(
                        eq("com.chrome.beta"),
                        eq(CERT_CHROME_BETA),
                        eq(PackageManager.CERT_INPUT_SHA256)))
                .thenReturn(true);

        assertTrue(BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));
    }

    @Test
    public void isCallerSignedBy_nullArguments_throwNullPointerException() {
        assertThrows(
                NullPointerException.class,
                () ->
                        BrowserIdentityVerifier.isCallerSignedBy(
                                null, Collections.singletonList(CERT_OTHER_BROWSER)));
        assertThrows(
                NullPointerException.class,
                () -> BrowserIdentityVerifier.isCallerSignedBy(mMockContext, null));
        assertThrows(
                NullPointerException.class,
                () ->
                        BrowserIdentityVerifier.isCallerSignedBy(
                                mMockContext, Arrays.asList(CERT_OTHER_BROWSER, null)));
        verify(mMockPackageManager, never())
                .hasSigningCertificate(anyInt(), any(byte[].class), anyInt());
    }

    @Test
    public void isCallerSignedBy_malformedDigestLength_throwsIllegalArgumentException() {
        List<byte[]> malformedDigests =
                Arrays.asList(new byte[0], new byte[31], new byte[33], new byte[64]);
        for (byte[] malformed : malformedDigests) {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            BrowserIdentityVerifier.isCallerSignedBy(
                                    mMockContext, Collections.singletonList(malformed)));
        }
        verify(mMockPackageManager, never())
                .hasSigningCertificate(anyInt(), any(byte[].class), anyInt());
    }

    @Test
    public void isCallerSignedBy_sameProcessUid_throwsIllegalStateException() {
        ShadowBinder.setCallingUid(Process.myUid());
        assertThrows(
                IllegalStateException.class,
                () ->
                        BrowserIdentityVerifier.isCallerSignedBy(
                                mMockContext, Collections.singletonList(CERT_OTHER_BROWSER)));
        assertThrows(
                IllegalStateException.class,
                () ->
                        BrowserIdentityVerifier.isCallerSignedBy(
                                mMockContext, Collections.emptyList()));
    }

    @Test
    public void isCallerSignedBy_matchingDigest_returnsTrue() {
        when(mMockPackageManager.hasSigningCertificate(
                        eq(REMOTE_UID),
                        eq(CERT_OTHER_BROWSER),
                        eq(PackageManager.CERT_INPUT_SHA256)))
                .thenReturn(true);

        assertTrue(
                BrowserIdentityVerifier.isCallerSignedBy(
                        mMockContext,
                        Arrays.asList(CERT_CHROME_STABLE, CERT_OTHER_BROWSER)));
    }

    @Test
    public void isCallerSignedBy_nonMatchingOrEmptyDigests_returnsFalse() {
        assertFalse(
                BrowserIdentityVerifier.isCallerSignedBy(mMockContext, Collections.emptyList()));
        verify(mMockPackageManager, never())
                .hasSigningCertificate(anyInt(), any(byte[].class), anyInt());

        assertFalse(
                BrowserIdentityVerifier.isCallerSignedBy(
                        mMockContext, Collections.singletonList(CERT_OTHER_BROWSER)));
    }

    @Test
    public void certificateChecks_requireCertInputSha256Mode() {
        when(mMockPackageManager.getPackagesForUid(REMOTE_UID))
                .thenReturn(new String[] {"com.android.chrome"});
        when(mMockPackageManager.hasSigningCertificate(
                        eq("com.android.chrome"),
                        eq(CERT_CHROME_STABLE),
                        eq(PackageManager.CERT_INPUT_RAW_X509)))
                .thenReturn(true);
        when(mMockPackageManager.hasSigningCertificate(
                        eq(REMOTE_UID),
                        eq(CERT_OTHER_BROWSER),
                        eq(PackageManager.CERT_INPUT_RAW_X509)))
                .thenReturn(true);

        assertFalse(BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));
        assertFalse(
                BrowserIdentityVerifier.isCallerSignedBy(
                        mMockContext, Collections.singletonList(CERT_OTHER_BROWSER)));
    }

    @Test
    public void isVerifiedExtension_handlesNullMissingWrongTypeAndBooleanValues() {
        assertFalse(BrowserIdentityVerifier.isVerifiedExtension(null));

        Bundle emptyBundle = new Bundle();
        assertFalse(BrowserIdentityVerifier.isVerifiedExtension(emptyBundle));

        Bundle unverifiedBundle = new Bundle();
        unverifiedBundle.putBoolean("isVerified", false);
        assertFalse(BrowserIdentityVerifier.isVerifiedExtension(unverifiedBundle));

        Bundle wrongTypeStringBundle = new Bundle();
        wrongTypeStringBundle.putString("isVerified", "true");
        assertFalse(BrowserIdentityVerifier.isVerifiedExtension(wrongTypeStringBundle));

        Bundle wrongTypeIntBundle = new Bundle();
        wrongTypeIntBundle.putInt("isVerified", 1);
        assertFalse(BrowserIdentityVerifier.isVerifiedExtension(wrongTypeIntBundle));

        Bundle verifiedBundle = new Bundle();
        verifiedBundle.putBoolean(BrowserIdentityVerifier.EXTRA_IS_VERIFIED, true);
        Parcel parcel = Parcel.obtain();
        final Bundle unparceledVerifiedBundle;
        try {
            verifiedBundle.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            unparceledVerifiedBundle = parcel.readBundle(getClass().getClassLoader());
        } finally {
            parcel.recycle();
        }
        assertTrue(BrowserIdentityVerifier.isVerifiedExtension(unparceledVerifiedBundle));
    }

    @Test
    public void isVerifiedExtension_malformedParcelableBundle_returnsFalse() {
        Bundle source = new Bundle();
        source.putBoolean(BrowserIdentityVerifier.EXTRA_IS_VERIFIED, true);
        source.putParcelable("unreadable", new ThrowingParcelable());

        Parcel parcel = Parcel.obtain();
        final Bundle lazyUnparceledBundle;
        try {
            source.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            lazyUnparceledBundle = parcel.readBundle(ThrowingParcelable.class.getClassLoader());
        } finally {
            parcel.recycle();
        }

        assertFalse(BrowserIdentityVerifier.isVerifiedExtension(lazyUnparceledBundle));
    }

    @Test
    public void hexToBytes_parsesValidHexAndRejectsMalformedInput() {
        assertArrayEquals(new byte[0], BrowserIdentityVerifier.hexToBytes(""));
        assertArrayEquals(
                new byte[] {(byte) 0x00, (byte) 0xab, (byte) 0xcd, (byte) 0xff},
                BrowserIdentityVerifier.hexToBytes("00AbCdFF"));
        assertArrayEquals(
                CERT_CHROME_STABLE,
                BrowserIdentityVerifier.hexToBytes(
                        "f0fd6c5b410f25cb25c3b53346c8972fae30f8ee7411df910480ad6b2d60db83"));

        assertThrows(NullPointerException.class, () -> BrowserIdentityVerifier.hexToBytes(null));
        assertThrows(
                IllegalArgumentException.class, () -> BrowserIdentityVerifier.hexToBytes("abc"));
        assertThrows(
                IllegalArgumentException.class, () -> BrowserIdentityVerifier.hexToBytes("00gg"));
        assertThrows(
                IllegalArgumentException.class, () -> BrowserIdentityVerifier.hexToBytes("000g"));
    }

    private void assertPackageRejectsCerts(String packageName, List<byte[]> rejectedCerts) {
        for (byte[] cert : rejectedCerts) {
            mockCallingPackageWithCert(packageName, cert);
            assertFalse(
                    "Expected " + packageName + " to reject non-pinned certificate",
                    BrowserIdentityVerifier.isTrustedChromeCaller(mMockContext));
        }
    }

    private void resetMockPackageManager() {
        mMockPackageManager = mock(PackageManager.class);
        when(mMockContext.getPackageManager()).thenReturn(mMockPackageManager);
    }

    private void mockCallingPackageWithCert(String packageName, byte[] certSha256) {
        resetMockPackageManager();
        when(mMockPackageManager.getPackagesForUid(REMOTE_UID))
                .thenReturn(new String[] {packageName});
        when(mMockPackageManager.hasSigningCertificate(
                        eq(packageName),
                        eq(certSha256),
                        eq(PackageManager.CERT_INPUT_SHA256)))
                .thenReturn(true);
    }

    private static final class ThrowingParcelable implements Parcelable {
        public static final Creator<ThrowingParcelable> CREATOR =
                new Creator<ThrowingParcelable>() {
                    @Override
                    public ThrowingParcelable createFromParcel(Parcel in) {
                        throw new android.os.BadParcelableException("Corrupt parcel payload");
                    }

                    @Override
                    public ThrowingParcelable[] newArray(int size) {
                        return new ThrowingParcelable[size];
                    }
                };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(1);
        }
    }

    private static byte[] decodeHex(String hex) {
        byte[] raw = new BigInteger("01" + hex, 16).toByteArray();
        return Arrays.copyOfRange(raw, 1, raw.length);
    }

    private static <T extends Throwable> void assertThrows(
            Class<T> expectedType, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable actual) {
            if (expectedType.isInstance(actual)) {
                return;
            }
            throw new AssertionError(
                    "Expected " + expectedType.getName() + " but caught " + actual, actual);
        }
        fail("Expected " + expectedType.getName() + " to be thrown, but nothing was thrown");
    }
}
