/*
 *  Copyright (C) Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo Caddisfly
 *
 *  Akvo Caddisfly is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo Caddisfly is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.caddisfly.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import org.akvo.caddisfly.AppConfig;
import org.akvo.caddisfly.R;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Manages update checking, downloading and installing of the update
 *
 * @author Raghav Sood
 * @version API 2
 * @since API 1
 */
class UpdateChecker {

    private final Context mContext;

    private boolean haveValidContext = false;

    private boolean useToasts = false;

    /**
     * Constructor for UpdateChecker
     * <p/>
     * Example call:
     * UpdateChecker updateChecker = new UpdateChecker(this, false);
     *
     * @param context An instance of your Activity's context
     * @param toasts  True if you want toast notifications, false by default
     * @since API 2
     */
    @SuppressWarnings("SameParameterValue")
    public UpdateChecker(Context context, boolean toasts) {
        mContext = context;
        if (mContext != null) {
            haveValidContext = true;
            useToasts = toasts;
        }
    }

    /**
     * Checks for app update by version code.
     * <p/>
     * Example call:
     * updateChecker.checkForUpdateByVersionCode("http://www.example.com/version.txt");
     *
     * @param url URL at which the text file containing your latest version code is located.
     * @return true if update exists otherwise false
     * @since API 1
     */
    @SuppressWarnings("SameParameterValue")
    public JSONObject checkForUpdateByVersionCode(String url) {

        JSONObject json;

        if (NetworkUtils.checkInternetConnection(mContext)) {
            if (haveValidContext) {
                int versionCode = getVersionCode();
                if (versionCode >= 0) {
                    try {

                        String response = readFile(url);

                        json = new JSONObject(response);
                        int version = Integer.parseInt(json.getString("version"));

                        // Check if update is available.
                        if (version > versionCode) {
                            return json;
                        }
                    } catch (Exception e) {
                        Log.e(AppConfig.DEBUG_TAG, "Invalid number online");
                    }
                } else {
                    Log.e(AppConfig.DEBUG_TAG, "Invalid version code in app");
                }
                return null;
            }
            return null;
        } else {
            if (useToasts) {
                makeToastFromString(mContext.getString(R.string.updateFailedNoInternet))
                        .show();
            }
            return null;
        }
    }

    /**
     * Gets the version code of your app by the context passed in the constructor
     *
     * @return The version code if successful, -1 if not
     * @since API 1
     */
    private int getVersionCode() {
        int code;
        try {
            code = mContext.getPackageManager()
                    .getPackageInfo(mContext.getPackageName(), 0).versionCode;
            return code; // Found the code!
        } catch (Exception e) {
            Log.e(AppConfig.DEBUG_TAG, "Version Code not available");
        }

        return -1; // There was a problem.
    }

    /**
     * Downloads and installs the update apk from the URL
     *
     * @param apkUrl URL at which the update is located
     * @since API 1
     */
    @SuppressWarnings("SameParameterValue")
    public void downloadAndInstall(String apkUrl, String checksum, String version) {
        if (NetworkUtils.isOnline(mContext)) {
            DownloadManager downloadManager = new DownloadManager(mContext);
            downloadManager.execute(apkUrl, checksum, version);
        } else {
            makeToastFromString(mContext.getString(R.string.updateFailedNoInternet)).show();
        }
    }

    /**
     * Makes a toast message with a short duration from the given text.
     *
     * @param text The text to be displayed by the toast
     * @return The toast object.
     * @since API 2
     */
    @SuppressLint("ShowToast")
    private Toast makeToastFromString(String text) {
        return Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
    }

    /**
     * Reads a file at the given URL
     *
     * @param url The URL at which the file is located
     * @return Returns the content of the file if successful
     * @since API 1
     */

    //http://developer.android.com/reference/java/net/URLConnection.html#setConnectTimeout(int)
    private String readFile(String url) {
        String result;
        InputStream inputStream;
        try {
            inputStream = new URL(url).openStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            result = bufferedReader.readLine();
            return result;
        } catch (MalformedURLException e) {
            Log.e(AppConfig.DEBUG_TAG, "Invalid URL");
        } catch (IOException e) {
            Log.e(AppConfig.DEBUG_TAG, "IO exception");
        } catch (Exception e) {
            Log.e(AppConfig.DEBUG_TAG, e.getMessage());
        }
        return "File read error";
    }

}
