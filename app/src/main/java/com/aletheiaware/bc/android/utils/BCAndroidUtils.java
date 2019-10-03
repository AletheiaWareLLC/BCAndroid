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

import android.content.Context;
import android.os.Environment;

import androidx.annotation.WorkerThread;

import com.aletheiaware.bc.Cache;
import com.aletheiaware.bc.android.BuildConfig;
import com.aletheiaware.bc.utils.BCUtils;
import com.aletheiaware.common.android.utils.CommonAndroidUtils;

import java.io.File;
import java.net.InetAddress;
import java.security.KeyPair;

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

    public static boolean copyCache(Context context) {
        if (context != null) {
            String state = Environment.getExternalStorageState();
            if (!Environment.MEDIA_MOUNTED.equals(state)) {
                return false;
            }
            File external = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (external == null) {
                return false;
            }
            File cache = context.getCacheDir();
            if (cache == null) {
                return false;
            }
            return CommonAndroidUtils.recursiveCopy(cache, new File(external, "BCCache"));
        }
        return false;
    }

    public static boolean purgeCache(Context context) {
        if (context != null) {
            File cache = context.getCacheDir();
            if (cache != null) {
                return CommonAndroidUtils.recursiveDelete(cache);
            }
        }
        return false;
    }
}
