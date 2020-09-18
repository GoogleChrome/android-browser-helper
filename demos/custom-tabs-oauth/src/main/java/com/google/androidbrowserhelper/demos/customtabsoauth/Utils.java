package com.google.androidbrowserhelper.demos.customtabsoauth;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class Utils {
    public static String fetch(@NonNull Uri uri) throws IOException {
        return fetch(uri, Collections.emptyMap());
    }

    public static String fetch(@NonNull Uri uri, Map<String, String> headers) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(uri.toString());
            connection = (HttpURLConnection)url.openConnection();
            for(Map.Entry<String, String> entry: headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            connection.setDoOutput(true);
            return inputStreamToString(connection.getInputStream());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    public static String inputStreamToString(@NonNull InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        }
    }
}
