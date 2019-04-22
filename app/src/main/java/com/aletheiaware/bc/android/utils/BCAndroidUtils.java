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
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.aletheiaware.bc.android.BuildConfig;
import com.aletheiaware.bc.android.R;

import com.aletheiaware.bc.BC.Channel;
import com.aletheiaware.bc.BC.Channel.RecordCallback;
import com.aletheiaware.bc.BC.Node;
import com.aletheiaware.bc.BCProto.Block;
import com.aletheiaware.bc.BCProto.BlockEntry;
import com.aletheiaware.bc.android.ui.StripeDialog;
import com.aletheiaware.bc.utils.BCUtils;
import com.aletheiaware.finance.FinanceProto.Customer;
import com.aletheiaware.finance.utils.FinanceUtils;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.stripe.android.model.Token;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Map;

public class BCAndroidUtils {

    public static final int ACCESS_ACTIVITY = 100;
    public static final int ACCOUNT_ACTIVITY = 101;

    private static String alias = null;
    private static KeyPair keyPair = null;
    private static Node node = null;

    private BCAndroidUtils() {}

    public static boolean isInitialized() {
        return alias != null && keyPair != null;
    }

    public static void initialize(String alias, KeyPair keyPair) {
        BCAndroidUtils.alias = alias;
        BCAndroidUtils.keyPair = keyPair;
        node = new Node(alias, keyPair);
    }

    public static String getAlias() {
        return alias;
    }

    public static KeyPair getKeyPair() {
        return keyPair;
    }

    public static Node getNode() {
        return node;
    }

    public static String getBCHostname() {
        return BuildConfig.DEBUG ? BCUtils.BC_HOST_TEST : BCUtils.BC_HOST;
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

    public static long getCacheSize(Context context) {
        if (context != null) {
            File cache = context.getCacheDir();
            if (cache != null) {
                return calculateSize(cache);
            }
        }
        return 0L;
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

    @SuppressLint("ApplySharedPref")
    public static void setPreference(Context context, String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).commit();
    }

    public static String getPreference(Context context, String key, String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue);
    }

    public static boolean isCustomer(File cache) throws IOException {
        final Channel customers = new Channel(FinanceUtils.CUSTOMER_CHANNEL, BCUtils.THRESHOLD_STANDARD, cache, getBCHost());
        final String alias = BCAndroidUtils.getAlias();
        final KeyPair keys = BCAndroidUtils.getKeyPair();
        final Customer.Builder cb = Customer.newBuilder();
        customers.read(alias, keys, null, new RecordCallback() {
            @Override
            public boolean onRecord(ByteString blockHash, Block block, BlockEntry blockEntry, byte[] key, byte[] payload) {
                try {
                    cb.mergeFrom(payload);
                    return false;
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
        final Customer customer = cb.build();
        String customerId = customer.getCustomerId();
        return customerId != null && !customerId.isEmpty();
    }

    public interface RegistrationCallback {
        void onRegistered(String customerId);
    }

    public static void registerCustomer(final Activity parent, final RegistrationCallback callback) {
        parent.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new StripeDialog(parent, null) {
                    @Override
                    public void onSubmit(String email, Token token) {
                        String website = parent.getString(R.string.register_account_address);
                        String alias = getAlias();
                        String customerId = null;
                        try {
                            customerId = BCUtils.register(website, alias, email, token.getId());
                        } catch (IOException e) {
                            showErrorDialog(parent, R.string.error_registering, e);
                        }
                        if (customerId != null && !customerId.isEmpty()) {
                            callback.onRegistered(customerId);
                        }
                    }
                }.create();
            }
        });
    }

    public static void showErrorDialog(final Activity parent, final int resource, final Exception exception) {
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
                        .setMessage(resource)
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
        content.append("======== App Info ========\n");
        content.append("Build: ").append(BuildConfig.BUILD_TYPE).append("\n");
        content.append("App ID: ").append(BuildConfig.APPLICATION_ID).append("\n");
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            content.append("CPU ABI: ").append(Build.CPU_ABI).append("\n");
            content.append("CPU ABI2: ").append(Build.CPU_ABI2).append("\n");
        } else {
            content.append("Supported ABIs: ").append(Arrays.toString(Build.SUPPORTED_ABIS)).append("\n");
        }
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
