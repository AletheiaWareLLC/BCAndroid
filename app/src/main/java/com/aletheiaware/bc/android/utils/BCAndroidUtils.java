/*
 * Copyright 2019 Aletheia Ware LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aletheiaware.bc.android.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;

import com.aletheiaware.bc.Cache;
import com.aletheiaware.bc.android.BuildConfig;
import com.aletheiaware.bc.android.R;
import com.aletheiaware.bc.utils.BCUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class BCAndroidUtils {

    public static final int ACCESS_ACTIVITY = 100;
    public static final int ACCOUNT_ACTIVITY = 101;

    private static String alias = null;
    private static KeyPair keyPair = null;
    private static Cache cache = null;

    private BCAndroidUtils() {}

    public static boolean isInitialized() {
        return alias != null && keyPair != null;
    }

    public static void initialize(String alias, KeyPair keyPair, Cache cache) {
        BCAndroidUtils.alias = alias;
        BCAndroidUtils.keyPair = keyPair;
        BCAndroidUtils.cache = cache;
    }

    public static String getAlias() {
        return alias;
    }

    public static KeyPair getKeyPair() {
        return keyPair;
    }

    public static Cache getCache() {
        return cache;
    }

    public static String getBCHostname() {
        return BCUtils.getBCHostname(BuildConfig.DEBUG);
    }

    @WorkerThread
    public static InetAddress getBCHost() {
        try {
            return InetAddress.getByName(getBCHostname());
        } catch (Exception e) {
            /* Ignored */
            e.printStackTrace();
        }
        return null;
    }

    public static String getBCWebsite() {
        return "https://" + getBCHostname();
    }

    public static long getCacheSize(Context context) {
        if (context != null) {
            File cache = context.getCacheDir();
            if (cache != null) {
                return calculateSize(cache);
            }
        }
        return 0L;
    }

    private static long calculateSize(File file) {
        if (file.isDirectory()) {
            long sum = 0L;
            for (File f : file.listFiles()) {
                sum += calculateSize(f);
            }
            return sum;
        }
        return file.length();
    }

    public static boolean purgeCache(Context context) {
        if (context != null) {
            File cache = context.getCacheDir();
            if (cache != null) {
                return recursiveDelete(cache);
            }
        }
        return false;
    }

    private static boolean recursiveDelete(File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (!recursiveDelete(f)) {
                    return false;
                }
            }
        } else {
            return file.delete();
        }
        return true;
    }

    @SuppressLint("ApplySharedPref")
    public static void setPreference(Context context, String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).commit();
    }

    public static String getPreference(Context context, String key, String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue);
    }

    @SuppressLint("ApplySharedPref")
    public static void setPreferences(Context context, String key, Set<String> values) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putStringSet(key, values).commit();
    }

    public static Set<String> getPreferences(Context context, String key, Set<String> defaultValues) {
        return PreferenceManager.getDefaultSharedPreferences(context).getStringSet(key, defaultValues);
    }

    public static void captureScreenshot(View view, String name) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        Bitmap cache = view.getDrawingCache();
        if (cache == null) {
            Log.e(BCUtils.TAG, "Drawing cache null");
            return;
        }
        Bitmap bitmap = Bitmap.createBitmap(cache);
        view.setDrawingCacheEnabled(false);
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File file = new File(dir, name);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    /* Ignored */
                }
            }
        }
    }

    public static void showErrorDialog(final Activity parent, final int resource, final Exception exception) {
        showErrorDialog(parent, parent.getString(resource), exception);
    }

    public static void showErrorDialog(final Activity parent, final String message, final Exception exception) {
        exception.printStackTrace();
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createErrorDialog(parent)
                        .setNeutralButton(R.string.error_report, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("======== Exception ========");
                                StringWriter sw = new StringWriter();
                                exception.printStackTrace(new PrintWriter(sw));
                                sb.append(sw.toString());
                                support(parent, sb);
                            }
                        })
                        .setMessage(message)
                        .show();
            }
        });
    }

    public static void showErrorDialog(final Activity parent, final String message) {
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createErrorDialog(parent)
                        .setMessage(message)
                        .show();
            }
        });
    }

    private static AlertDialog.Builder createErrorDialog(Activity parent) {
        return new AlertDialog.Builder(parent, R.style.AlertDialogTheme)
                .setTitle(R.string.title_dialog_error)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
    }

    @SuppressWarnings("deprecation")
    public static void support(Activity parent, StringBuilder content) {
        content.append("\n\n\n");
        content.append("======== Account Info ========\n");
        content.append("Alias: ").append(getAlias()).append("\n");
        // TODO content.append("Public Key: ").append(getPublicKey()).append("\n");
        // TODO content.append("Customer ID: ").append(getCustomerId()).append("\n");
        // TODO content.append("Subscription ID: ").append(getSubscriptionId()).append("\n");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(parent);
        Map<String, ?> map = sharedPrefs.getAll();
        content.append("======== Preferences ========\n");
        for (String key : map.keySet()) {
            content.append(key).append(":").append(map.get(key)).append("\n");
        }
        try {
            PackageInfo info = parent.getPackageManager().getPackageInfo(parent.getPackageName(), 0);
            content.append("======== App Info ========\n");
            content.append("App ID: ").append(info.packageName).append("\n");
            content.append("Version: ").append(info.versionName).append("\n");
            content.append("Installed: ").append(BCUtils.FORMATTER.format(new Date(info.firstInstallTime))).append("\n");
            content.append("Updated: ").append(BCUtils.FORMATTER.format(new Date(info.lastUpdateTime))).append("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        content.append("======== Library Info ========\n");
        content.append("Library ID: ").append(BuildConfig.APPLICATION_ID).append("\n");
        content.append("Variant: ").append(BuildConfig.BUILD_TYPE).append("\n");
        content.append("Version: ").append(BuildConfig.VERSION_NAME).append("\n");
        content.append("======== Device Info ========\n");
        content.append("Board: ").append(Build.BOARD).append("\n");
        content.append("Bootloader: ").append(Build.BOOTLOADER).append("\n");
        content.append("Brand: ").append(Build.BRAND).append("\n");
        content.append("Build ID: ").append(Build.ID).append("\n");
        content.append("Device: ").append(Build.DEVICE).append("\n");
        content.append("Display: ").append(Build.DISPLAY).append("\n");
        content.append("Fingerprint: ").append(Build.FINGERPRINT).append("\n");
        content.append("Hardware: ").append(Build.HARDWARE).append("\n");
        content.append("Host: ").append(Build.HOST).append("\n");
        content.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        content.append("Model: ").append(Build.MODEL).append("\n");
        content.append("Product: ").append(Build.PRODUCT).append("\n");
        content.append("Supported ABIs: ").append(Arrays.toString(Build.SUPPORTED_ABIS)).append("\n");
        content.append("Tags: ").append(Build.TAGS).append("\n");
        content.append("Type: ").append(Build.TYPE).append("\n");
        content.append("User: ").append(Build.USER).append("\n");
        content.append("\n\n\n");
        Log.d(BCUtils.TAG, content.toString());
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{parent.getString(R.string.support_email)});
        intent.putExtra(Intent.EXTRA_SUBJECT, parent.getString(R.string.support_subject) + parent.getString(R.string.app_name));
        intent.putExtra(Intent.EXTRA_TEXT, content.toString());
        parent.startActivity(intent);
    }
}
