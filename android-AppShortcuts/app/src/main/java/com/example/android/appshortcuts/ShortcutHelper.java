/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.appshortcuts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.BooleanSupplier;

public class ShortcutHelper {
    private static final String TAG = Main.TAG;

    private static final String EXTRA_LAST_REFRESH =
            "com.example.android.shortcutsample.EXTRA_LAST_REFRESH";

    private static final long REFRESH_INTERVAL_MS = 60 * 60 * 1000;

    private final Context mContext;

    private final ShortcutManager mShortcutManager;

    public ShortcutHelper(Context context) {
        mContext = context;
        //Shortcut管理类，动态管理Shortcut
        mShortcutManager = mContext.getSystemService(ShortcutManager.class);
    }

    //可能用到的恢复动态Shortcuts
    //如果有一个动态Shortcuts总是被创建可在此进行处理
    //注意当应用程序在新设备上“恢复”时，所有动态快捷方式
    //将*不*恢复，但固定的快捷键能恢复。
    public void maybeRestoreAllDynamicShortcuts() {
        if (mShortcutManager.getDynamicShortcuts().size() == 0) {
            // NOTE: If this application is always supposed to have dynamic shortcuts, then publish
            // them here.
            // Note when an application is "restored" on a new device, all dynamic shortcuts
            // will *not* be restored but the pinned shortcuts *will*.
            if (mShortcutManager.getPinnedShortcuts().size() > 0) {
                // 桌面的快捷方式已经被还原了 使用 updateShortcuts(List) 方法确保他们是最新的内容.
            }
        }
    }

    //应用在发布一个快捷方式的时候需要调用这个方法，下面的两种情景都应该调用:
    //1、用户选择给定ID的快捷方式;
    //2、用户打开 app 手动完成对应于相同的快捷方式的操作比如更新删除该快捷方式。
    //这样应用程序就能通过这个信息来构建预加载模块，以便在操作时可以立即响应。
    public void reportShortcutUsed(String id) {
        mShortcutManager.reportShortcutUsed(id);
    }

    /**
     * Use this when interacting with ShortcutManager to show consistent error messages.
     *调用 ShortcutManager 的方法后，判断是否被限制.
     */
    //BooleanSupplier JDK 1.8 新增加的函数接口，用来支持 Java的 函数式编程 代表了boolean值结果的提供方
    private void callShortcutManager(BooleanSupplier r) {
        try {
            if (!r.getAsBoolean()) {
                Utils.showToast(mContext, "Call to ShortcutManager is rate-limited");
            }
        } catch (Exception e) {
            Log.e(TAG, "Caught Exception", e);
            Utils.showToast(mContext, "Error while calling ShortcutManager: " + e.toString());
        }
    }

    /**
     * Return all mutable shortcuts from this app self.
     * 从应用程序自身返回所有可变快捷键。
     */
    public List<ShortcutInfo> getShortcuts() {
        // Load mutable dynamic shortcuts and pinned shortcuts and put them into a single list
        // removing duplicates.
        // 加载可变的动态快捷方式和加载在桌面的快捷方式，
        // 放在一个 list 里并移除重复的（因为动态的也可能被拖放在桌面上）。

        final List<ShortcutInfo> ret = new ArrayList<>();
        final HashSet<String> seenKeys = new HashSet<>();

        // Check existing shortcuts shortcuts
        // 检查存在的快捷方式
        for (ShortcutInfo shortcut : mShortcutManager.getDynamicShortcuts()) {
            // 只拿去可更改的快捷方式
            if (!shortcut.isImmutable()) {
                ret.add(shortcut);
                seenKeys.add(shortcut.getId());
            }
        }
        // 检查所有的固定在桌面上的快捷方式
        for (ShortcutInfo shortcut : mShortcutManager.getPinnedShortcuts()) {
            // 只拿去可更改 和 不重复的快捷方式
            if (!shortcut.isImmutable() && !seenKeys.contains(shortcut.getId())) {
                ret.add(shortcut);
                seenKeys.add(shortcut.getId());
            }
        }
        return ret;
    }

    /**
     * Called when the activity starts.  Looks for shortcuts that have been pushed and refreshes
     * them (but the refresh part isn't implemented yet...).
     * 当 activity 开始的时候调用.  查找已经发布的快捷方式并刷新他们，异步操作
     */
    public void refreshShortcuts(boolean force) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Log.i(TAG, "refreshingShortcuts...");

                final long now = System.currentTimeMillis();
                final long staleThreshold = force ? now : now - REFRESH_INTERVAL_MS;

                // Check all existing dynamic and pinned shortcut, and if their last refresh
                // time is older than a certain threshold, update them.
                // 检测所有存在的动态快捷方式和放在桌面的快捷方式。
                // 如果上次刷新的时间与当前时间间隔超过了阖值 （REFRESH_INTERVAL_MS), 再更新他们。
                final List<ShortcutInfo> updateList = new ArrayList<>();

                for (ShortcutInfo shortcut : getShortcuts()) {
                    // 遍历所有可更改的快捷方式
                    if (shortcut.isImmutable()) {
                        continue;
                    }

                    final PersistableBundle extras = shortcut.getExtras();
                    if (extras != null && extras.getLong(EXTRA_LAST_REFRESH) >= staleThreshold) {
                        // Shortcut still fresh.
                        // Shortcut 是最新的，就不再刷新它。
                        continue;
                    }
                    Log.i(TAG, "Refreshing shortcut: " + shortcut.getId());

                    final ShortcutInfo.Builder b = new ShortcutInfo.Builder(
                            mContext, shortcut.getId());

                    setSiteInformation(b, shortcut.getIntent().getData());
                    setExtras(b);

                    updateList.add(b.build());
                }
                // Call update.
                // 更新
                if (updateList.size() > 0) {
                    callShortcutManager(() -> mShortcutManager.updateShortcuts(updateList));
                }

                return null;
            }
        }.execute();
    }

    /**
     * 通过 url 创建快捷方式
     * @param urlAsString String
     * @return ShortcutInfo
     */
    private ShortcutInfo createShortcutForUrl(String urlAsString) {
        Log.i(TAG, "createShortcutForUrl: " + urlAsString);

        final ShortcutInfo.Builder b = new ShortcutInfo.Builder(mContext, urlAsString);

        final Uri uri = Uri.parse(urlAsString);
        // 设置这个快捷方式的对应的 Intent.
        b.setIntent(new Intent(Intent.ACTION_VIEW, uri));

        setSiteInformation(b, uri);
        setExtras(b);

        return b.build();
    }

    /**
     * 设置站点信息 icon 和 title
     *
     * @param b   ShortcutInfo.Builder
     * @param uri Uri
     * @return ShortcutInfo.Builder
     */
    private ShortcutInfo.Builder setSiteInformation(ShortcutInfo.Builder b, Uri uri) {
        // TODO Get the actual site <title> and use it.
        // TODO Set the current locale to accept-language to get localized title.
        b.setShortLabel(uri.getHost());
        b.setLongLabel(uri.toString());

        Bitmap bmp = fetchFavicon(uri);
        if (bmp != null) {
            b.setIcon(Icon.createWithBitmap(bmp));
        } else {
            b.setIcon(Icon.createWithResource(mContext, R.drawable.link));
        }

        return b;
    }

    /**
     * 存储快捷方式的刷新时间
     *
     * @param b ShortcutInfo.Builder
     * @return ShortcutInfo.Builder
     */
    private ShortcutInfo.Builder setExtras(ShortcutInfo.Builder b) {
        final PersistableBundle extras = new PersistableBundle();
        extras.putLong(EXTRA_LAST_REFRESH, System.currentTimeMillis());
        b.setExtras(extras);
        return b;
    }

    private String normalizeUrl(String urlAsString) {
        if (urlAsString.startsWith("http://") || urlAsString.startsWith("https://")) {
            return urlAsString;
        } else {
            return "http://" + urlAsString;
        }
    }

    /**
     * 添加站点快捷方式
     * @param urlAsString String
     */
    public void addWebSiteShortcut(String urlAsString) {
        final String uriFinal = urlAsString;
        callShortcutManager(() -> {
            final ShortcutInfo shortcut = createShortcutForUrl(normalizeUrl(uriFinal));
            // 添加动态快捷方式
            return mShortcutManager.addDynamicShortcuts(Arrays.asList(shortcut));
        });
    }

    public void removeShortcut(ShortcutInfo shortcut) {
        mShortcutManager.removeDynamicShortcuts(Arrays.asList(shortcut.getId()));
    }

    // 调用 disableShortcuts(List) 或者 disableShortcuts(List, CharSequence) 方法即可
    // disableShortcuts 的第二个参数用于显示提示错误信息，当用户点击这个被禁用的快捷方式时提示这个信息。
    // 当用户把快捷方式放到手机桌面上 即在桌面上生成一个单独的桌面快捷方式时
    // 在这种情况下，禁用并不会移除桌面快捷方式，只会灰度化图标，并不可点击
    public void disableShortcut(ShortcutInfo shortcut) {
        mShortcutManager.disableShortcuts(Arrays.asList(shortcut.getId()));
    }

    public void enableShortcut(ShortcutInfo shortcut) {
        mShortcutManager.enableShortcuts(Arrays.asList(shortcut.getId()));
    }

    /**
     * 获取站点的 favicon.ico 图标
     *
     * @param uri uri
     * @return Bitmap
     */
    private Bitmap fetchFavicon(Uri uri) {
        final Uri iconUri = uri.buildUpon().path("favicon.ico").build();
        Log.i(TAG, "Fetching favicon from: " + iconUri);

        InputStream is = null;
        BufferedInputStream bis = null;
        try
        {
            URLConnection conn = new URL(iconUri.toString()).openConnection();
            conn.connect();
            is = conn.getInputStream();
            bis = new BufferedInputStream(is, 8192);
            return BitmapFactory.decodeStream(bis);
        } catch (IOException e) {
            Log.w(TAG, "Failed to fetch favicon from " + iconUri, e);
            return null;
        }
    }
}
