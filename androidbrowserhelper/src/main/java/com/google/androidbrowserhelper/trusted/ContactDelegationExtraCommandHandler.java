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
import java.util.HashMap;
import java.util.Map;

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
                boolean includeNames = args.getBoolean("includeNames");
                boolean includeEmails = args.getBoolean("includeEmails");
                boolean includeTel = args.getBoolean("includeTel");
                boolean includeAddresses = args.getBoolean("includeAddresses");
                callbackResult.putParcelableArrayList("contacts",
                    getAllContacts(context, includeNames, includeEmails, includeTel,
                        includeAddresses));
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

    private Map<String, ArrayList<String>> getDetails(ContentResolver contentResolver,
        Uri source, String idColumn, String dataColumn, String sortOrder) {
        Map<String, ArrayList<String>> map = new HashMap<>();

        Cursor cursor = contentResolver.query(source, null, null, null, sortOrder);
        ArrayList<String> list = new ArrayList<>();
        String key = "";
        String value;

        assert cursor != null;

        while (cursor.moveToNext()) {
            String id = cursor.getString(cursor.getColumnIndexOrThrow(idColumn));
            value = cursor.getString(cursor.getColumnIndexOrThrow(dataColumn));
            if (value == null) {
                value = "";
            }
            if (key.isEmpty()) {
                key = id;
                list.add(value);
            } else {
                if (key.equals(id)) {
                    list.add(value);
                } else {
                    map.put(key, list);
                    list = new ArrayList<>();
                    list.add(value);
                    key = id;
                }
            }
        }
        map.put(key, list);
        cursor.close();

        return map;
    }

    private Map<String, ArrayList<Bundle>> getAddressDetails(ContentResolver contentResolver) {
        Map<String, ArrayList<Bundle>> map = new HashMap<>();

        String addressSortOrder =
            ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID
                + " ASC, "
                + ContactsContract.CommonDataKinds.StructuredPostal.DATA
                + " ASC";
        Cursor cursor =
            contentResolver.query(
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                null,
                null,
                null,
                addressSortOrder);

        assert cursor != null;

        ArrayList<Bundle> list = new ArrayList<>();
        String key = "";

        while (cursor.moveToNext()) {
            String id =
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID));
            String city =
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.StructuredPostal.CITY));
            String country =
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
            String formattedAddress =
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.StructuredPostal
                            .FORMATTED_ADDRESS));
            String postcode =
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
            String region =
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        ContactsContract.CommonDataKinds.StructuredPostal.REGION));
            Bundle address = new Bundle();

            address.putString("city", city);
            address.putString("country", country);
            address.putString("formattedAddress", formattedAddress);
            address.putString("postcode", postcode);
            address.putString("region", region);

            if (key.isEmpty()) {
                key = id;
                list.add(address);
            } else {
                if (key.equals(id)) {
                    list.add(address);
                } else {
                    map.put(key, list);
                    list = new ArrayList<>();
                    list.add(address);
                    key = id;
                }
            }
        }
        map.put(key, list);
        cursor.close();

        return map;
    }

    public ArrayList<Bundle> getAllContacts(Context context, boolean includeNames,
        boolean includeEmails,
        boolean includeTel, boolean includeAddresses) {
        ContentResolver contentResolver = context.getContentResolver();

        Map<String, ArrayList<String>> emailMap =
            includeEmails
                ? getDetails(contentResolver,
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.DATA,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID
                    + " ASC, "
                    + ContactsContract.CommonDataKinds.Email.DATA
                    + " ASC")
                : null;

        Map<String, ArrayList<String>> phoneMap =
            includeTel
                ? getDetails(contentResolver,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DATA,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                    + " ASC, "
                    + ContactsContract.CommonDataKinds.Phone.NUMBER
                    + " ASC")
                : null;

        Map<String, ArrayList<Bundle>> addressMap =
            includeAddresses ? getAddressDetails(contentResolver) : null;

        Log.d(TAG, "emailMap = " + emailMap);
        Log.d(TAG, "phoneMap = " + phoneMap);
        Log.d(TAG, "addressMap = " + addressMap);

        // A cursor containing the raw contacts data.
        Cursor cursor =
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                PROJECTION,
                null,
                null,
                ContactsContract.Contacts.SORT_KEY_PRIMARY + " ASC");
        assert cursor != null;
        if (!cursor.moveToFirst()) {
            cursor.close();
            return new ArrayList<>();
        }

        ArrayList<Bundle> contacts = new ArrayList<>(cursor.getCount());
        do {
            String id =
                cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
            String name =
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
            ArrayList<String> email = emailMap != null ? emailMap.get(id) : null;
            ArrayList<String> tel = phoneMap != null ? phoneMap.get(id) : null;
            ArrayList<Bundle> address =
                addressMap != null ? addressMap.get(id) : null;

            if (includeNames || email != null || tel != null || address != null) {
                Bundle contact = new Bundle();

                contact.putString("id", id);
                contact.putString("name", name);
                contact.putStringArrayList("email", email);
                contact.putStringArrayList("tel", tel);
                contact.putParcelableArrayList("address", address);

                contacts.add(contact);
            }
        } while (cursor.moveToNext());

        cursor.close();
        return contacts;
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
