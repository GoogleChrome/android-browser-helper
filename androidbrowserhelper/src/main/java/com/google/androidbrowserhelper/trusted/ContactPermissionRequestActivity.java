package com.google.androidbrowserhelper.trusted;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;
import androidx.core.app.ActivityCompat;

public class ContactPermissionRequestActivity extends Activity {

    private static final String TAG = "stomoki ContactReq";

    private static final String[] CONTACT_PERMISSION = {Manifest.permission.READ_CONTACTS};

    private static final String CONTACT_PERMISSION_RESULT = "contactPermissionResult";

    private static final String EXTRA_RESULT_RECEIVER = "EXTRA_RESULT_RECEIVER";
    private static final String EXTRA_PERMISSIONS = "EXTRA_PERMISSIONS";
    private static final String EXTRA_GRANT_RESULTS = "EXTRA_GRANTED_RESULT";

    private Messenger mMessenger;

    public static void requestContactPermisson(Context context,
        TrustedWebActivityCallbackRemote callback) {
        Log.d(TAG, "Permission request started");

        Handler handler = new Handler(Looper.getMainLooper(), message -> {
            Bundle data = message.getData();
            Log.d(TAG, "Intent result: " + data);

            Bundle result = new Bundle();

            result.putBoolean(CONTACT_PERMISSION_RESULT, false);

            String[] permissions = data.getStringArray(EXTRA_PERMISSIONS);
            int[] grantedResult = data.getIntArray(EXTRA_GRANT_RESULTS);

            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(CONTACT_PERMISSION[0])) {
                    result.putBoolean(CONTACT_PERMISSION_RESULT,
                        grantedResult[i] == PackageManager.PERMISSION_GRANTED);
                    break;
                }
            }

            try {
                callback.runExtraCallback(
                    ContactDelegationExtraCommandHandler.COMMAND_CHECK_CONTACT_PERMISSION, result);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            
            return true;
        });

        final Messenger messenger = new Messenger(handler);
        Intent intent = new Intent(context, ContactPermissionRequestActivity.class);
        intent.putExtra(EXTRA_PERMISSIONS, CONTACT_PERMISSION);
        intent.putExtra(EXTRA_RESULT_RECEIVER, messenger);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String[] permissions = getIntent().getStringArrayExtra(EXTRA_PERMISSIONS);
        mMessenger = getIntent().getParcelableExtra(EXTRA_RESULT_RECEIVER);
        ActivityCompat.requestPermissions(this, permissions, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
        int[] grantResults) {
        Message message = new Message();
        Bundle data = new Bundle();
        data.putStringArray(EXTRA_PERMISSIONS, permissions);
        data.putIntArray(EXTRA_GRANT_RESULTS, grantResults);
        message.setData(data);
        try {
            mMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            finish();
        }
    }
}
