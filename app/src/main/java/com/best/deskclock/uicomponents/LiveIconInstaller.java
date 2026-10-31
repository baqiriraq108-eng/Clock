/*
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.best.deskclock.R;
import com.best.deskclock.widgets.LiveClockWidgetProvider;
import com.best.deskclock.widgets.LiveWidgetPinnedReceiver;

/**
 * Provides one-tap installation of the 1x1 Zero-Drain Real-Time Model 3 Live Micro-Tile
 * onto the user's home screen via {@link AppWidgetManager#requestPinAppWidget}.
 */
public final class LiveIconInstaller {

    private LiveIconInstaller() {
    }

    /**
     * Prompts the native OS dialog to place the 1x1 Model 3 Live Micro-Tile directly on the
     * home screen with zero background battery drain and no shortcut watermark badge.
     */
    public static boolean pinLiveMicroTile(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(context, R.string.live_clock_pin_not_supported, Toast.LENGTH_SHORT).show();
            return false;
        }

        AppWidgetManager appWidgetManager = context.getSystemService(AppWidgetManager.class);
        ComponentName provider = new ComponentName(context, LiveClockWidgetProvider.class);

        if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported()) {
            Intent pinnedIntent = new Intent(context, LiveWidgetPinnedReceiver.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }
            PendingIntent successCallback = PendingIntent.getBroadcast(context, 0, pinnedIntent, flags);
            boolean requested = appWidgetManager.requestPinAppWidget(provider, null, successCallback);
            if (requested) {
                return true;
            }
        }

        Toast.makeText(context, R.string.live_clock_pin_not_supported, Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * Returns true if at least one 1x1 Model 3 Live Micro-Tile is currently placed on the home screen.
     */
    public static boolean isLiveMicroTileAdded(@NonNull Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) {
            return false;
        }
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, LiveClockWidgetProvider.class));
        return ids != null && ids.length > 0;
    }
}
