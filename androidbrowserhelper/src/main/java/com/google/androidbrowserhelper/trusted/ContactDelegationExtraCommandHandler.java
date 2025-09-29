package com.google.androidbrowserhelper.trusted;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.trusted.TrustedWebActivityCallbackRemote;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class ContactDelegationExtraCommandHandler implements ExtraCommandHandler {

    private static final String TAG = "stomoki ContactDeleg";
    static final String COMMAND_CHECK_CONTACT_PERMISSION = "checkContactPermission";
    private static final String COMMAND_GET_CONTACT_DATA = "getContactData";
    private static final String COMMAND_GET_CONTACT_ICON = "getContactIcon";

    private static final String[] PROJECTION = {
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.LOOKUP_KEY,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
    };


    @NonNull
    @Override
    public Bundle handleExtraCommand(Context context, String commandName, Bundle args,
        @Nullable TrustedWebActivityCallbackRemote callback) {
        Bundle result = new Bundle();
        result.putBoolean(EXTRA_COMMAND_SUCCESS, false);

        Bundle callbackResult = new Bundle();

        switch (commandName) {
            case COMMAND_CHECK_CONTACT_PERMISSION:
                Log.d(TAG, "COMMAND_CHECK_CONTACT_PERMISSION received");
                ContactPermissionRequestActivity.requestContactPermisson(context, callback);
                result.putBoolean(EXTRA_COMMAND_SUCCESS, true);
                break;
            case COMMAND_GET_CONTACT_DATA:
                Log.d(TAG, "COMMAND_GET_CONTACT_DATA received");
                callbackResult.putStringArrayList("contactData", getContentData(context));
                try {
                    callback.runExtraCallback(COMMAND_GET_CONTACT_DATA, callbackResult);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                result.putBoolean(EXTRA_COMMAND_SUCCESS, true);
                break;
            case COMMAND_GET_CONTACT_ICON:
                Log.d(TAG, "COMMAND_GET_CONTACT_ICON received");

                String id = args.getString("id");
                int iconSize = args.getInt("size");

                callbackResult.putParcelable("icon", getIcon(context, id, iconSize));
                try {
                    callback.runExtraCallback(COMMAND_GET_CONTACT_ICON, callbackResult);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                result.putBoolean(EXTRA_COMMAND_SUCCESS, true);
                break;
            default:
                Log.d(TAG, "unknown command " + commandName);
                break;
        }

        return result;
    }

    private ArrayList<String> getContentData(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, PROJECTION,
            null, null, ContactsContract.Contacts.SORT_KEY_PRIMARY + " ASC");

        ArrayList<String> result = new ArrayList<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
                result.add(name);
            }

            cursor.close();
        }

        return result;
    }

    private Bitmap getIcon(Context context, String id, int iconSize) {
        ContentResolver contentResolver = context.getContentResolver();

        Uri contactUri =
            ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI, Long.parseLong(id));
        Uri photoUri =
            Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor =
            contentResolver.query(
                photoUri,
                new String[]{ContactsContract.Contacts.Photo.PHOTO},
                null,
                null,
                null);
        if (cursor == null) {
            return null;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    Bitmap icon = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
                    return iconSize > 0
                        ? Bitmap.createScaledBitmap(
                        icon, iconSize, iconSize, true)
                        : icon;
                }
            }
        } finally {
            cursor.close();
        }
        return null;
    }
}
