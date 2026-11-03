// Copyright 2026 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.androidbrowserhelper.trusted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.browser.trusted.FileHandlingData;
import androidx.browser.trusted.TrustedWebActivityIntent;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;
import androidx.browser.trusted.sharing.ShareData;

import com.google.androidbrowserhelper.trusted.splashscreens.SplashScreenStrategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(sdk = {Build.VERSION_CODES.O_MR1})
public class LauncherActivityContentUriValidationTest {
    private static final String DEFAULT_URL = "https://www.example.com/twa/home";
    private static final int SHARE_TARGET_RES_ID = 0x7f040001;
    private static final String SHARE_TARGET_JSON =
            "{\"action\":\"https://www.example.com/share\","
                    + "\"method\":\"POST\","
                    + "\"enctype\":\"multipart/form-data\","
                    + "\"params\":{\"title\":\"title\",\"text\":\"text\","
                    + "\"files\":[{\"name\":\"file\",\"accept\":[\"image/*\"]}]}}";

    private static final String INTERNAL_AUTHORITY = "com.example.host.fileprovider";
    private static final String EXTERNAL_AUTHORITY = "com.external.sender.provider";

    public static class TestShareLauncherActivity extends LauncherActivity {
        public TrustedWebActivityIntent mCapturedTwaIntent;
        public int mGrantedModeFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

        @Override
        public Resources getResources() {
            Resources spied = spy(super.getResources());
            doReturn(SHARE_TARGET_JSON).when(spied).getString(SHARE_TARGET_RES_ID);
            return spied;
        }

        @Override
        public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
            // Simulate Android's bitmask check: all requested mode bits must be present in the
            // active grant. Defaults to READ|WRITE so existing tests isolate ContentProvider
            // ownership unless a test narrows mGrantedModeFlags.
            return (mGrantedModeFlags & modeFlags) == modeFlags
                    ? PackageManager.PERMISSION_GRANTED
                    : PackageManager.PERMISSION_DENIED;
        }

        @Override
        protected TwaLauncher createTwaLauncher() {
            return new TwaLauncher(this) {
                @Override
                public void launch(
                        TrustedWebActivityIntentBuilder twaBuilder,
                        CustomTabsCallback customTabsCallback,
                        @Nullable SplashScreenStrategy splashScreenStrategy,
                        @Nullable Runnable completionCallback,
                        FallbackStrategy fallbackStrategy) {
                    CustomTabsSession session = CustomTabsSession.createMockSessionForTesting(
                            new ComponentName("com.android.chrome", "CustomTabsService"));
                    mCapturedTwaIntent = twaBuilder.build(session);
                }
            };
        }
    }

    private Context mContext;
    private ShadowPackageManager mShadowPackageManager;

    @Before
    public void setUp() {
        Utils.resetOwnProviderAuthoritiesCacheForTesting();
        mContext = RuntimeEnvironment.application;
        mContext.getApplicationInfo().flags &= ~ApplicationInfo.FLAG_DEBUGGABLE;
        mShadowPackageManager = shadowOf(mContext.getPackageManager());

        PackageInfo hostPackage = new PackageInfo();
        hostPackage.packageName = mContext.getPackageName();

        ActivityInfo launcherActivityInfo = new ActivityInfo();
        launcherActivityInfo.packageName = mContext.getPackageName();
        launcherActivityInfo.name = TestShareLauncherActivity.class.getName();
        launcherActivityInfo.metaData = new Bundle();
        launcherActivityInfo.metaData.putString(
                "android.support.customtabs.trusted.DEFAULT_URL", DEFAULT_URL);
        launcherActivityInfo.metaData.putInt(
                "android.support.customtabs.trusted.METADATA_SHARE_TARGET", SHARE_TARGET_RES_ID);

        ProviderInfo internalProvider = new ProviderInfo();
        internalProvider.authority = INTERNAL_AUTHORITY;
        internalProvider.packageName = mContext.getPackageName();
        internalProvider.applicationInfo = new ApplicationInfo();
        internalProvider.applicationInfo.packageName = mContext.getPackageName();
        internalProvider.applicationInfo.uid = Process.myUid();

        hostPackage.activities = new ActivityInfo[]{launcherActivityInfo};
        hostPackage.providers = new ProviderInfo[]{internalProvider};
        mShadowPackageManager.addPackage(hostPackage);

        PackageInfo externalPackage = new PackageInfo();
        externalPackage.packageName = "com.external.sender";
        ProviderInfo externalProvider = new ProviderInfo();
        externalProvider.authority = EXTERNAL_AUTHORITY;
        externalProvider.packageName = "com.external.sender";
        externalProvider.applicationInfo = new ApplicationInfo();
        externalProvider.applicationInfo.packageName = "com.external.sender";
        externalProvider.applicationInfo.uid = Process.myUid() + 1000;
        externalPackage.providers = new ProviderInfo[]{externalProvider};
        mShadowPackageManager.addPackage(externalPackage);
    }

    @Test
    public void isSafeExternalContentUri_rejectsSamePackageOrSameUidProvider() throws Exception {
        Context mockContext = mock(Context.class);
        PackageManager mockPm = mock(PackageManager.class);
        when(mockContext.getPackageName()).thenReturn("com.host.app");
        when(mockContext.getPackageManager()).thenReturn(mockPm);
        when(mockContext.checkUriPermission(
                org.mockito.ArgumentMatchers.any(Uri.class), anyInt(), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        when(mockPm.getPackagesForUid(eq(Process.myUid())))
                .thenReturn(new String[]{"com.host.app", "com.shared.uid.app"});

        PackageInfo hostPkgInfo = new PackageInfo();
        ProviderInfo hostProvider = new ProviderInfo();
        hostProvider.packageName = "com.host.app";
        hostProvider.authority = "com.host.app.files; com.host.app.secondary";
        hostPkgInfo.providers = new ProviderInfo[]{hostProvider};
        when(mockPm.getPackageInfo(eq("com.host.app"), eq(PackageManager.GET_PROVIDERS)))
                .thenReturn(hostPkgInfo);

        PackageInfo sharedUidPkgInfo = new PackageInfo();
        ProviderInfo sharedUidProvider = new ProviderInfo();
        sharedUidProvider.packageName = "com.shared.uid.app";
        sharedUidProvider.authority = "com.shared.uid.app.files";
        sharedUidPkgInfo.providers = new ProviderInfo[]{sharedUidProvider};
        when(mockPm.getPackageInfo(eq("com.shared.uid.app"), eq(PackageManager.GET_PROVIDERS)))
                .thenReturn(sharedUidPkgInfo);

        assertFalse(Utils.isSafeExternalContentUri(
                mockContext,
                Uri.parse("content://com.host.app.files/secret.txt"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION));

        // Semicolon-separated second authority and case-insensitive match must also be rejected.
        assertFalse(Utils.isSafeExternalContentUri(
                mockContext,
                Uri.parse("content://COM.HOST.APP.SECONDARY/secret.txt"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION));

        assertFalse(Utils.isSafeExternalContentUri(
                mockContext,
                Uri.parse("content://com.shared.uid.app.files/secret.txt"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION));

        assertFalse(Utils.isSafeExternalContentUri(
                mockContext,
                Uri.parse("content://0@com.host.app.files/secret.txt"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION));

        assertFalse(Utils.isSafeExternalContentUri(
                mockContext,
                Uri.parse("content://10@com.host.app.files/secret.txt"),
                Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }

    @Test
    public void isSafeExternalContentUri_acceptsCrossProfileUserIdPrefix() {
        Context mockContext = mock(Context.class);
        PackageManager mockPm = mock(PackageManager.class);
        when(mockContext.getPackageName()).thenReturn("com.host.app");
        when(mockContext.getPackageManager()).thenReturn(mockPm);

        ProviderInfo externalProvider = new ProviderInfo();
        externalProvider.packageName = "com.external.app";
        externalProvider.applicationInfo = new ApplicationInfo();
        externalProvider.applicationInfo.uid = Process.myUid() + 500;
        when(mockPm.resolveContentProvider(eq("com.external.app.files"), eq(0)))
                .thenReturn(externalProvider);

        Uri crossProfileUri = Uri.parse("content://10@com.external.app.files/photo.png");
        Uri userZeroUri = Uri.parse("content://0@com.external.app.files/photo.png");

        when(mockContext.checkUriPermission(
                eq(crossProfileUri), eq(Process.myPid()), eq(Process.myUid()),
                eq(Intent.FLAG_GRANT_READ_URI_PERMISSION)))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        when(mockContext.checkUriPermission(
                eq(userZeroUri), eq(Process.myPid()), eq(Process.myUid()),
                eq(Intent.FLAG_GRANT_READ_URI_PERMISSION)))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        assertTrue(Utils.isSafeExternalContentUri(
                mockContext, crossProfileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION));
        assertTrue(Utils.isSafeExternalContentUri(
                mockContext, userZeroUri, Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }

    @Test
    public void isSafeExternalContentUri_rejectsMalformedUserIdPrefix() {
        Context mockContext = mock(Context.class);
        PackageManager mockPm = mock(PackageManager.class);
        when(mockContext.getPackageName()).thenReturn("com.host.app");
        when(mockContext.getPackageManager()).thenReturn(mockPm);
        when(mockContext.checkUriPermission(
                org.mockito.ArgumentMatchers.any(Uri.class), anyInt(), anyInt(), anyInt()))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        String[] malformedUris = new String[]{
                "content://@com.external.app.files/photo.png",
                "content://abc@com.external.app.files/photo.png",
                "content://-2@com.external.app.files/photo.png",
                "content://+0@com.external.app.files/photo.png",
                "content://0@1@com.external.app.files/photo.png",
                "content://10@/photo.png",
                "content://99999999999@com.external.app.files/photo.png",
                "content://0@com.external.app.files:123/photo.png",
        };
        for (String uriStr : malformedUris) {
            assertFalse(
                    "Expected rejection for " + uriStr,
                    Utils.isSafeExternalContentUri(
                            mockContext,
                            Uri.parse(uriStr),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION));
        }
    }

    @Test
    public void stripContentUserId_parsesValidPrefixesAndRejectsMalformedOnes() {
        assertEquals("com.external.app.files", Utils.stripContentUserId("com.external.app.files"));
        assertEquals(
                "com.external.app.files", Utils.stripContentUserId("0@com.external.app.files"));
        assertEquals(
                "com.external.app.files", Utils.stripContentUserId("10@com.external.app.files"));
        assertEquals(
                "com.external.app.files",
                Utils.stripContentUserId("2147483647@com.external.app.files"));

        assertNull(Utils.stripContentUserId("@com.external.app.files"));
        assertNull(Utils.stripContentUserId("10@"));
        assertNull(Utils.stripContentUserId("abc@com.external.app.files"));
        assertNull(Utils.stripContentUserId("-2@com.external.app.files"));
        assertNull(Utils.stripContentUserId("+0@com.external.app.files"));
        assertNull(Utils.stripContentUserId("0@1@com.external.app.files"));
        assertNull(Utils.stripContentUserId("2147483648@com.external.app.files"));
        assertNull(Utils.stripContentUserId("99999999999@com.external.app.files"));
    }

    @Test
    public void isSafeExternalContentUri_requiresExternalProviderAndGrantedPermission() {
        Context mockContext = mock(Context.class);
        PackageManager mockPm = mock(PackageManager.class);
        when(mockContext.getPackageName()).thenReturn("com.host.app");
        when(mockContext.getPackageManager()).thenReturn(mockPm);

        ProviderInfo externalProvider = new ProviderInfo();
        externalProvider.packageName = "com.external.app";
        externalProvider.applicationInfo = new ApplicationInfo();
        externalProvider.applicationInfo.uid = Process.myUid() + 500;
        when(mockPm.resolveContentProvider(eq("com.external.app.files"), eq(0)))
                .thenReturn(externalProvider);

        Uri externalUri = Uri.parse("content://com.external.app.files/photo.png");

        when(mockContext.checkUriPermission(
                eq(externalUri), eq(Process.myPid()), eq(Process.myUid()),
                eq(Intent.FLAG_GRANT_READ_URI_PERMISSION)))
                .thenReturn(PackageManager.PERMISSION_DENIED);
        assertFalse(Utils.isSafeExternalContentUri(
                mockContext, externalUri, Intent.FLAG_GRANT_READ_URI_PERMISSION));

        when(mockContext.checkUriPermission(
                eq(externalUri), eq(Process.myPid()), eq(Process.myUid()),
                eq(Intent.FLAG_GRANT_READ_URI_PERMISSION)))
                .thenReturn(PackageManager.PERMISSION_GRANTED);
        assertTrue(Utils.isSafeExternalContentUri(
                mockContext, externalUri, Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }

    @Test
    public void isSafeExternalContentUri_allowsGrantedExternalUriWhenProviderFilteredByVisibility()
            throws Exception {
        Context mockContext = mock(Context.class);
        PackageManager mockPm = mock(PackageManager.class);
        when(mockContext.getPackageName()).thenReturn("com.host.app");
        when(mockContext.getPackageManager()).thenReturn(mockPm);

        PackageInfo hostPkgInfo = new PackageInfo();
        ProviderInfo hostProvider = new ProviderInfo();
        hostProvider.packageName = "com.host.app";
        hostProvider.authority = "com.host.app.fileprovider";
        hostPkgInfo.providers = new ProviderInfo[]{hostProvider};
        when(mockPm.getPackagesForUid(eq(Process.myUid())))
                .thenReturn(new String[]{"com.host.app"});
        when(mockPm.getPackageInfo(eq("com.host.app"), eq(PackageManager.GET_PROVIDERS)))
                .thenReturn(hostPkgInfo);

        // Simulate Android 11+ package visibility filtering: resolveContentProvider returns null
        // for the external sender's authority even though a valid URI grant exists.
        Uri externalUri = Uri.parse("content://com.unseen.external.provider/shared/image.jpg");
        when(mockPm.resolveContentProvider(eq("com.unseen.external.provider"), eq(0)))
                .thenReturn(null);
        when(mockContext.checkUriPermission(
                eq(externalUri), eq(Process.myPid()), eq(Process.myUid()),
                eq(Intent.FLAG_GRANT_READ_URI_PERMISSION)))
                .thenReturn(PackageManager.PERMISSION_GRANTED);

        assertTrue(Utils.isSafeExternalContentUri(
                mockContext, externalUri, Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }

    @Test
    public void addShareDataIfPresent_dropsInternalProviderUriFromExtraStream() {
        Uri internalUri = Uri.parse("content://" + INTERNAL_AUTHORITY + "/private/token.json");
        Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, internalUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, shareIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        assertNull(captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA));
    }

    @Test
    public void addShareDataIfPresent_allowsExternalGrantedProviderUri() {
        Uri externalUri = Uri.parse("content://" + EXTERNAL_AUTHORITY + "/shared/photo.png");
        Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, externalUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, shareIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        Bundle shareDataBundle = captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA);
        assertNotNull(shareDataBundle);
        ShareData forwardedShareData = ShareData.fromBundle(shareDataBundle);
        assertEquals(Collections.singletonList(externalUri), forwardedShareData.uris);
        assertNotSame(externalUri, forwardedShareData.uris.get(0));
    }

    @Test
    public void addShareDataIfPresent_allowsCrossProfileExternalGrantedProviderUri() {
        Uri crossProfileUri = Uri.parse("content://10@" + EXTERNAL_AUTHORITY + "/photo.jpg");
        Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .setType("image/jpeg")
                .putExtra(Intent.EXTRA_STREAM, crossProfileUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, shareIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        Bundle shareDataBundle = captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA);
        assertNotNull(shareDataBundle);
        ShareData forwardedShareData = ShareData.fromBundle(shareDataBundle);
        assertEquals(Collections.singletonList(crossProfileUri), forwardedShareData.uris);
    }

    @Test
    public void getLaunchingUrl_returnsFileHandlingActionUrlForUngrantedContentUri() {
        String actionUrl = "https://www.example.com/open-file";
        PackageInfo hostPackage = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        hostPackage.activities[0].metaData.putString(
                "android.support.customtabs.trusted.FILE_HANDLING_ACTION_URL",
                actionUrl);

        Uri internalUri = Uri.parse("content://" + INTERNAL_AUTHORITY + "/private/token.json");
        Intent fileIntent = new Intent(Intent.ACTION_VIEW, internalUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, fileIntent);
        controller.create();

        assertEquals(Uri.parse(actionUrl), controller.get().getLaunchingUrl());
        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        assertEquals(Uri.parse(actionUrl), captured.getIntent().getData());
        assertNull(captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA));
        assertNull(captured.getIntent().getParcelableExtra(
                TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
    }

    @Test
    public void fileHandling_allowsExternalGrantedProviderUriAndSetsOriginalLaunchUrl() {
        String actionUrl = "https://www.example.com/open-file";
        PackageInfo hostPackage = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        hostPackage.activities[0].metaData.putString(
                "android.support.customtabs.trusted.FILE_HANDLING_ACTION_URL",
                actionUrl);

        Uri externalUri = Uri.parse("content://" + EXTERNAL_AUTHORITY + "/shared/doc.pdf");
        Intent fileIntent = new Intent(Intent.ACTION_VIEW, externalUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, fileIntent);
        controller.create();

        assertEquals(Uri.parse(actionUrl), controller.get().getLaunchingUrl());
        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        assertEquals(Uri.parse(actionUrl), captured.getIntent().getData());
        assertEquals(
                externalUri,
                captured.getIntent().getParcelableExtra(
                        TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
        Bundle fileHandlingBundle = captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA);
        assertNotNull(fileHandlingBundle);
        FileHandlingData forwardedFileData = FileHandlingData.fromBundle(fileHandlingBundle);
        assertEquals(Collections.singletonList(externalUri), forwardedFileData.uris);
    }

    @Test
    public void addFileDataIfPresent_honoursExtraFileHandlingDataWithoutActionUrl() {
        Uri externalUri = Uri.parse("content://" + EXTERNAL_AUTHORITY + "/shared/doc.pdf");
        FileHandlingData fileData =
                new FileHandlingData(Collections.singletonList(externalUri));
        Intent explicitIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEFAULT_URL))
                .putExtra(
                        TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA,
                        fileData.toBundle())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, explicitIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        Bundle fileHandlingBundle = captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA);
        assertNotNull(fileHandlingBundle);
        FileHandlingData forwardedFileData = FileHandlingData.fromBundle(fileHandlingBundle);
        assertEquals(Collections.singletonList(externalUri), forwardedFileData.uris);
    }

    @Test
    public void addFileDataIfPresent_isSilentAndNoOpForPlainHttpsLaunch() {
        PackageInfo hostPackage = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        hostPackage.activities[0].metaData.putString(
                "android.support.customtabs.trusted.FILE_HANDLING_ACTION_URL",
                "https://www.example.com/open-file");

        // Enable FLAG_DEBUGGABLE as well: a plain https launch must not trigger reportRejection
        // or log any file-handling failure when FILE_HANDLING_ACTION_URL is configured.
        mContext.getApplicationInfo().flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        org.robolectric.shadows.ShadowLog.clear();

        Uri httpsDeepLink = Uri.parse("https://www.example.com/twa/article");
        Intent httpsIntent = new Intent(Intent.ACTION_VIEW, httpsDeepLink)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, httpsIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        assertNull(captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA));

        for (org.robolectric.shadows.ShadowLog.LogItem item :
                org.robolectric.shadows.ShadowLog.getLogsForTag("TWALauncherActivity")) {
            assertFalse(item.msg.contains("Failed to open files"));
        }
    }

    @Test
    public void addShareDataIfPresent_forwardsEmptyActionSendUnchanged() {
        Intent emptyShareIntent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, emptyShareIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        Bundle shareDataBundle = captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA);
        assertNotNull(shareDataBundle);
        ShareData forwardedShareData = ShareData.fromBundle(shareDataBundle);
        assertNull(forwardedShareData.title);
        assertNull(forwardedShareData.text);
        assertNull(forwardedShareData.uris);
    }

    @Test
    public void addShareDataIfPresent_keepsTitleAndTextWhenAllUrisRejected() {
        Uri internalUri = Uri.parse("content://" + INTERNAL_AUTHORITY + "/private/token.json");
        Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_SUBJECT, "Shared Title")
                .putExtra(Intent.EXTRA_TEXT, "Shared Body Text")
                .putExtra(Intent.EXTRA_STREAM, internalUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, shareIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        Bundle shareDataBundle = captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA);
        assertNotNull(shareDataBundle);
        ShareData forwardedShareData = ShareData.fromBundle(shareDataBundle);
        assertEquals("Shared Title", forwardedShareData.title);
        assertEquals("Shared Body Text", forwardedShareData.text);
        assertNull(forwardedShareData.uris);
    }

    public static class OwnFileProviderLauncherActivity extends TestShareLauncherActivity {
        @Override
        protected boolean isTrustedContentUri(@Nullable Uri uri) {
            return uri != null && INTERNAL_AUTHORITY.equals(uri.getAuthority());
        }
    }

    @Test
    public void isTrustedContentUri_overrideRestoresOwnFileProviderShare() {
        PackageInfo hostPackage = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        ActivityInfo customActivityInfo = new ActivityInfo();
        customActivityInfo.packageName = mContext.getPackageName();
        customActivityInfo.name = OwnFileProviderLauncherActivity.class.getName();
        customActivityInfo.metaData = new Bundle(hostPackage.activities[0].metaData);
        hostPackage.activities = new ActivityInfo[]{
                hostPackage.activities[0], customActivityInfo
        };

        Uri ownFileProviderUri = Uri.parse("content://" + INTERNAL_AUTHORITY + "/exports/report.pdf");
        Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, ownFileProviderUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<OwnFileProviderLauncherActivity> controller =
                Robolectric.buildActivity(OwnFileProviderLauncherActivity.class, shareIntent);
        // Simulate real Android behaviour for own FileProvider: no explicit UriPermission record.
        controller.get().mGrantedModeFlags = 0;
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        Bundle shareDataBundle = captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA);
        assertNotNull(shareDataBundle);
        ShareData forwardedShareData = ShareData.fromBundle(shareDataBundle);
        assertEquals(Collections.singletonList(ownFileProviderUri), forwardedShareData.uris);
    }

    @Test
    public void fileHandling_allowsReadOnlyGrantedExternalUri() {
        String actionUrl = "https://www.example.com/open-file";
        PackageInfo hostPackage = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        hostPackage.activities[0].metaData.putString(
                "android.support.customtabs.trusted.FILE_HANDLING_ACTION_URL",
                actionUrl);

        Uri externalUri = Uri.parse("content://" + EXTERNAL_AUTHORITY + "/shared/readonly.pdf");
        Intent fileIntent = new Intent(Intent.ACTION_VIEW, externalUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, fileIntent);
        controller.get().mGrantedModeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        assertEquals(
                PackageManager.PERMISSION_GRANTED,
                controller.get().checkUriPermission(
                        externalUri,
                        Process.myPid(),
                        Process.myUid(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION));
        assertEquals(
                PackageManager.PERMISSION_DENIED,
                controller.get().checkUriPermission(
                        externalUri,
                        Process.myPid(),
                        Process.myUid(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        controller.create();

        assertEquals(Uri.parse(actionUrl), controller.get().getLaunchingUrl());
        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        assertEquals(Uri.parse(actionUrl), captured.getIntent().getData());
        assertEquals(
                externalUri,
                captured.getIntent().getParcelableExtra(
                        TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
        Bundle fileHandlingBundle = captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA);
        assertNotNull(fileHandlingBundle);
        FileHandlingData forwardedFileData = FileHandlingData.fromBundle(fileHandlingBundle);
        assertEquals(Collections.singletonList(externalUri), forwardedFileData.uris);
    }

    @Test
    public void isSafeExternalContentUri_usesApplicationPackageManagerAndCachesByPackageName()
            throws Exception {
        Context appContext = mock(Context.class);
        PackageManager appPm = mock(PackageManager.class);
        when(appContext.getPackageManager()).thenReturn(appPm);

        Context activityContext1 = mock(Context.class);
        PackageManager activityPm1 = mock(PackageManager.class);
        when(activityContext1.getPackageName()).thenReturn("com.host.app");
        when(activityContext1.getApplicationContext()).thenReturn(appContext);
        when(activityContext1.getPackageManager()).thenReturn(activityPm1);

        Context activityContext2 = mock(Context.class);
        PackageManager activityPm2 = mock(PackageManager.class);
        when(activityContext2.getPackageName()).thenReturn("com.host.app");
        when(activityContext2.getApplicationContext()).thenReturn(appContext);
        when(activityContext2.getPackageManager()).thenReturn(activityPm2);

        PackageInfo hostPkgInfo = new PackageInfo();
        ProviderInfo hostProvider = new ProviderInfo();
        hostProvider.packageName = "com.host.app";
        hostProvider.authority = "com.host.app.files";
        hostPkgInfo.providers = new ProviderInfo[]{hostProvider};
        when(appPm.getPackagesForUid(eq(Process.myUid())))
                .thenReturn(new String[]{"com.host.app"});
        when(appPm.getPackageInfo(eq("com.host.app"), eq(PackageManager.GET_PROVIDERS)))
                .thenReturn(hostPkgInfo);

        Uri internalUri = Uri.parse("content://com.host.app.files/secret.txt");

        assertFalse(Utils.isSafeExternalContentUri(
                activityContext1, internalUri, Intent.FLAG_GRANT_READ_URI_PERMISSION));
        assertFalse(Utils.isSafeExternalContentUri(
                activityContext2, internalUri, Intent.FLAG_GRANT_READ_URI_PERMISSION));

        // Authority discovery must use appContext.getPackageManager(), never Activity's PM,
        // and must hit the package-name cache on the second Activity instance.
        verify(activityPm1, never()).getPackageInfo(
                org.mockito.ArgumentMatchers.anyString(), anyInt());
        verify(activityPm2, never()).getPackageInfo(
                org.mockito.ArgumentMatchers.anyString(), anyInt());
        verify(appPm, times(1)).getPackageInfo(
                eq("com.host.app"), eq(PackageManager.GET_PROVIDERS));
    }

    @Test
    public void addShareDataIfPresent_forwardsSurvivingUrisInReleaseBuild() {
        Uri externalUri = Uri.parse("content://" + EXTERNAL_AUTHORITY + "/shared/photo.png");
        Uri internalUri = Uri.parse("content://" + INTERNAL_AUTHORITY + "/private/token.json");
        ArrayList<Uri> streamUris = new ArrayList<>(Arrays.asList(externalUri, internalUri));
        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("image/png")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, streamUris)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, shareIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        Bundle shareDataBundle = captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA);
        assertNotNull(shareDataBundle);
        ShareData forwardedShareData = ShareData.fromBundle(shareDataBundle);
        assertEquals(Collections.singletonList(externalUri), forwardedShareData.uris);
    }

    @Test
    public void addShareDataIfPresent_forwardsSurvivingUrisInDebuggableBuild() {
        mContext.getApplicationInfo().flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        ShadowLog.clear();

        Uri externalUri = Uri.parse("content://" + EXTERNAL_AUTHORITY + "/shared/photo.png");
        Uri internalUri = Uri.parse("content://" + INTERNAL_AUTHORITY + "/private/token.json");
        ArrayList<Uri> streamUris = new ArrayList<>(Arrays.asList(externalUri, internalUri));
        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("image/png")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, streamUris)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, shareIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        Bundle shareDataBundle = captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA);
        assertNotNull(shareDataBundle);
        ShareData forwardedShareData = ShareData.fromBundle(shareDataBundle);
        assertEquals(Collections.singletonList(externalUri), forwardedShareData.uris);

        List<ShadowLog.LogItem> errorLogs = new ArrayList<>();
        for (ShadowLog.LogItem item : ShadowLog.getLogsForTag("TWALauncherActivity")) {
            if (item.type == Log.ERROR && item.msg != null
                    && item.msg.contains(internalUri.toString())) {
                errorLogs.add(item);
            }
        }
        assertEquals(1, errorLogs.size());
    }

    @Test
    public void addShareDataIfPresent_keepsTitleAndTextWhenAllUrisRejectedInDebuggableBuild() {
        mContext.getApplicationInfo().flags |= ApplicationInfo.FLAG_DEBUGGABLE;

        Uri internalUri = Uri.parse("content://" + INTERNAL_AUTHORITY + "/private/token.json");
        Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_SUBJECT, "Shared Title")
                .putExtra(Intent.EXTRA_TEXT, "Shared Body Text")
                .putExtra(Intent.EXTRA_STREAM, internalUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, shareIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        Bundle shareDataBundle = captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA);
        assertNotNull(shareDataBundle);
        ShareData forwardedShareData = ShareData.fromBundle(shareDataBundle);
        assertEquals("Shared Title", forwardedShareData.title);
        assertEquals("Shared Body Text", forwardedShareData.text);
        assertNull(forwardedShareData.uris);
    }

    @Test(expected = SecurityException.class)
    public void addShareDataIfPresent_throwsInDebuggableBuildWhenEntireShareIsDropped() {
        mContext.getApplicationInfo().flags |= ApplicationInfo.FLAG_DEBUGGABLE;

        Uri internalUri = Uri.parse("content://" + INTERNAL_AUTHORITY + "/private/token.json");
        Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, internalUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, shareIntent);
        controller.create();
    }

    public static class SuppressingRejectionLauncherActivity extends TestShareLauncherActivity {
        public final List<String> mReportedMessages = new ArrayList<>();
        public final List<RejectionOutcome> mReportedOutcomes = new ArrayList<>();

        @Override
        protected void reportRejection(String message, RejectionOutcome outcome) {
            mReportedMessages.add(message);
            mReportedOutcomes.add(outcome);
        }
    }

    private void registerCustomActivity(Class<? extends TestShareLauncherActivity> activityClass) {
        PackageInfo hostPackage = mShadowPackageManager.getInternalMutablePackageInfo(
                mContext.getPackageName());
        ActivityInfo customActivityInfo = new ActivityInfo();
        customActivityInfo.packageName = mContext.getPackageName();
        customActivityInfo.name = activityClass.getName();
        customActivityInfo.metaData = new Bundle(hostPackage.activities[0].metaData);
        hostPackage.activities = new ActivityInfo[]{
                hostPackage.activities[0], customActivityInfo
        };
    }

    @Test
    public void reportRejection_overrideSuppressesThrowWithoutRelaxingShareEnforcement() {
        mContext.getApplicationInfo().flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        registerCustomActivity(SuppressingRejectionLauncherActivity.class);

        Uri internalUri = Uri.parse("content://" + INTERNAL_AUTHORITY + "/private/token.json");
        Intent dropAllShareIntent = new Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, internalUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<SuppressingRejectionLauncherActivity> dropAllController =
                Robolectric.buildActivity(
                        SuppressingRejectionLauncherActivity.class, dropAllShareIntent);
        dropAllController.create();

        TrustedWebActivityIntent capturedDropped = dropAllController.get().mCapturedTwaIntent;
        assertNotNull(capturedDropped);
        assertNull(capturedDropped.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA));
        assertEquals(
                Collections.singletonList(LauncherActivity.RejectionOutcome.PAYLOAD_DROPPED),
                dropAllController.get().mReportedOutcomes);
        assertEquals(1, dropAllController.get().mReportedMessages.size());
        assertTrue(dropAllController.get().mReportedMessages.get(0).contains(
                internalUri.toString()));

        // Positive control: when an external URI is shared alongside the rejected internal URI,
        // the override receives DATA_FILTERED and the external URI still flows through.
        Uri externalUri = Uri.parse("content://" + EXTERNAL_AUTHORITY + "/shared/photo.png");
        ArrayList<Uri> mixedUris = new ArrayList<>(Arrays.asList(externalUri, internalUri));
        Intent partialShareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE)
                .setType("image/png")
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, mixedUris)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<SuppressingRejectionLauncherActivity> partialController =
                Robolectric.buildActivity(
                        SuppressingRejectionLauncherActivity.class, partialShareIntent);
        partialController.create();

        TrustedWebActivityIntent capturedPartial = partialController.get().mCapturedTwaIntent;
        assertNotNull(capturedPartial);
        Bundle shareDataBundle = capturedPartial.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA);
        assertNotNull(shareDataBundle);
        ShareData forwardedShareData = ShareData.fromBundle(shareDataBundle);
        assertEquals(Collections.singletonList(externalUri), forwardedShareData.uris);
        assertEquals(
                Collections.singletonList(LauncherActivity.RejectionOutcome.DATA_FILTERED),
                partialController.get().mReportedOutcomes);
    }

    @Test
    public void reportRejection_overrideSuppressesThrowWithoutRelaxingOriginEnforcement() {
        mContext.getApplicationInfo().flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        registerCustomActivity(SuppressingRejectionLauncherActivity.class);

        Uri untrustedUri = Uri.parse("https://www.evil.com/phish");
        Intent viewIntent = new Intent(Intent.ACTION_VIEW, untrustedUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<SuppressingRejectionLauncherActivity> controller =
                Robolectric.buildActivity(SuppressingRejectionLauncherActivity.class, viewIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        assertEquals(Uri.parse(DEFAULT_URL), captured.getIntent().getData());
        assertNull(captured.getIntent().getParcelableExtra(
                TrustedWebActivityIntentBuilder.EXTRA_ORIGINAL_LAUNCH_URL));
        assertEquals(
                Collections.singletonList(LauncherActivity.RejectionOutcome.LAUNCH_URL_SUBSTITUTED),
                controller.get().mReportedOutcomes);
        assertEquals(1, controller.get().mReportedMessages.size());
        assertTrue(controller.get().mReportedMessages.get(0).contains(untrustedUri.toString()));
    }

    @Test(expected = SecurityException.class)
    public void addFileDataIfPresent_throwsInDebuggableBuildWhenFileUriRejected() {
        mContext.getApplicationInfo().flags |= ApplicationInfo.FLAG_DEBUGGABLE;

        Uri internalUri = Uri.parse("content://" + INTERNAL_AUTHORITY + "/private/token.json");
        FileHandlingData fileData =
                new FileHandlingData(Collections.singletonList(internalUri));
        Intent fileIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEFAULT_URL))
                .putExtra(
                        TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA,
                        fileData.toBundle())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, fileIntent);
        controller.create();
    }

    @Test
    public void reportRejection_overrideSuppressesThrowWithoutRelaxingFileHandlingEnforcement() {
        mContext.getApplicationInfo().flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        registerCustomActivity(SuppressingRejectionLauncherActivity.class);

        Uri internalUri = Uri.parse("content://" + INTERNAL_AUTHORITY + "/private/token.json");
        FileHandlingData fileData =
                new FileHandlingData(Collections.singletonList(internalUri));
        Intent fileIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEFAULT_URL))
                .putExtra(
                        TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA,
                        fileData.toBundle())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<SuppressingRejectionLauncherActivity> controller =
                Robolectric.buildActivity(SuppressingRejectionLauncherActivity.class, fileIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        assertNull(captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA));
        assertEquals(
                Collections.singletonList(LauncherActivity.RejectionOutcome.PAYLOAD_DROPPED),
                controller.get().mReportedOutcomes);
        assertEquals(1, controller.get().mReportedMessages.size());
        assertTrue(controller.get().mReportedMessages.get(0).contains(internalUri.toString()));
    }

    @Test
    public void addFileDataIfPresent_reportsAllRejectedUrisInSingleCall() {
        mContext.getApplicationInfo().flags |= ApplicationInfo.FLAG_DEBUGGABLE;
        registerCustomActivity(SuppressingRejectionLauncherActivity.class);

        Uri internalUri1 = Uri.parse("content://" + INTERNAL_AUTHORITY + "/private/first.json");
        Uri internalUri2 = Uri.parse("content://" + INTERNAL_AUTHORITY + "/private/second.json");
        FileHandlingData fileData =
                new FileHandlingData(Arrays.asList(internalUri1, internalUri2));
        Intent fileIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEFAULT_URL))
                .putExtra(
                        TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA,
                        fileData.toBundle())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<SuppressingRejectionLauncherActivity> controller =
                Robolectric.buildActivity(SuppressingRejectionLauncherActivity.class, fileIntent);
        controller.create();

        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        assertNull(captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA));
        assertEquals(
                Collections.singletonList(LauncherActivity.RejectionOutcome.PAYLOAD_DROPPED),
                controller.get().mReportedOutcomes);
        assertEquals(1, controller.get().mReportedMessages.size());
        String message = controller.get().mReportedMessages.get(0);
        assertTrue(message.contains(internalUri1.toString()));
        assertTrue(message.contains(internalUri2.toString()));
    }

    public static class RecordingTrustedContentUriLauncherActivity extends TestShareLauncherActivity {
        public final List<Uri> mRecordedUris = new ArrayList<>();

        @Override
        protected boolean isTrustedContentUri(Uri uri) {
            mRecordedUris.add(uri);
            return super.isTrustedContentUri(uri);
        }
    }

    @Test
    public void isTrustedContentUri_receivesReparsedUriForwardedToOutgoingShareAndFileData() {
        registerCustomActivity(RecordingTrustedContentUriLauncherActivity.class);

        Uri hierarchicalShareUri = new Uri.Builder()
                .scheme("content")
                .authority(EXTERNAL_AUTHORITY)
                .appendPath("shared")
                .appendPath("photo.png")
                .build();
        Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, hierarchicalShareUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<RecordingTrustedContentUriLauncherActivity> shareController =
                Robolectric.buildActivity(
                        RecordingTrustedContentUriLauncherActivity.class, shareIntent);
        shareController.create();

        assertEquals(1, shareController.get().mRecordedUris.size());
        Uri recordedShareUri = shareController.get().mRecordedUris.get(0);
        Bundle shareDataBundle = shareController.get().mCapturedTwaIntent.getIntent()
                .getBundleExtra(TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA);
        assertNotNull(shareDataBundle);
        ShareData forwardedShareData = ShareData.fromBundle(shareDataBundle);
        assertEquals(1, forwardedShareData.uris.size());
        assertEquals(recordedShareUri, forwardedShareData.uris.get(0));
        org.junit.Assert.assertSame(recordedShareUri, forwardedShareData.uris.get(0));

        Uri hierarchicalFileUri = new Uri.Builder()
                .scheme("content")
                .authority(EXTERNAL_AUTHORITY)
                .appendPath("shared")
                .appendPath("doc.pdf")
                .build();
        FileHandlingData fileData =
                new FileHandlingData(Collections.singletonList(hierarchicalFileUri));
        Intent fileIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(DEFAULT_URL))
                .putExtra(
                        TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA,
                        fileData.toBundle())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<RecordingTrustedContentUriLauncherActivity> fileController =
                Robolectric.buildActivity(
                        RecordingTrustedContentUriLauncherActivity.class, fileIntent);
        fileController.create();

        assertEquals(1, fileController.get().mRecordedUris.size());
        Uri recordedFileUri = fileController.get().mRecordedUris.get(0);
        Bundle fileHandlingBundle = fileController.get().mCapturedTwaIntent.getIntent()
                .getBundleExtra(TrustedWebActivityIntentBuilder.EXTRA_FILE_HANDLING_DATA);
        assertNotNull(fileHandlingBundle);
        FileHandlingData forwardedFileData = FileHandlingData.fromBundle(fileHandlingBundle);
        assertEquals(1, forwardedFileData.uris.size());
        assertEquals(recordedFileUri, forwardedFileData.uris.get(0));
        org.junit.Assert.assertSame(recordedFileUri, forwardedFileData.uris.get(0));
    }

    @Test
    public void isSafeExternalContentUri_rejectsAuthorityWithPortEvenWithReadGrant() {
        Uri uriWithPort = Uri.parse("content://" + EXTERNAL_AUTHORITY + ":123/x");
        Intent shareIntent = new Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uriWithPort)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityController<TestShareLauncherActivity> controller =
                Robolectric.buildActivity(TestShareLauncherActivity.class, shareIntent);
        controller.get().mGrantedModeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
        assertFalse(Utils.isSafeExternalContentUri(
                controller.get(), uriWithPort, Intent.FLAG_GRANT_READ_URI_PERMISSION));

        controller.create();
        TrustedWebActivityIntent captured = controller.get().mCapturedTwaIntent;
        assertNotNull(captured);
        assertNull(captured.getIntent().getBundleExtra(
                TrustedWebActivityIntentBuilder.EXTRA_SHARE_DATA));
    }
}
