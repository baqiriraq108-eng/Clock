/*
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;

import java.util.Calendar;

/**
 * Tier 2: The 1x1 Real-Time Live Clock Widget.
 *
 * <p>Combines Android's native {@link android.widget.AnalogClock} for smooth real-time hand
 * rotation with {@link LiveBubbleTimeRenderer} for 100% reference-identical thick, stepped
 * (non-horizontal) numerals inside the sea-glass green bubble.</p>
 */
public class LiveClockWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_LIVE_WIDGET_MINUTE_TICK =
            "com.best.deskclock.widgets.ACTION_LIVE_WIDGET_MINUTE_TICK";

    @Override
    public void onUpdate(@NonNull Context context,
                         @NonNull AppWidgetManager appWidgetManager,
                         @NonNull int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateLiveMicroTile(context, appWidgetManager, appWidgetId);
        }
        scheduleNextMinuteTick(context);
    }

    @Override
    public void onEnabled(@NonNull Context context) {
        super.onEnabled(context);
        scheduleNextMinuteTick(context);
    }

    @Override
    public void onDisabled(@NonNull Context context) {
        cancelMinuteTick(context);
        super.onDisabled(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(@NonNull Context context,
                                          @NonNull AppWidgetManager appWidgetManager,
                                          int appWidgetId,
                                          @NonNull Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        updateLiveMicroTile(context, appWidgetManager, appWidgetId);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_LIVE_WIDGET_MINUTE_TICK.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)
                || Intent.ACTION_CONFIGURATION_CHANGED.equals(action)
                || Intent.ACTION_USER_PRESENT.equals(action)) {
            updateAllLiveMicroTiles(context);
        }
    }

    public static void updateAllLiveMicroTiles(@NonNull Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) {
            return;
        }
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, LiveClockWidgetProvider.class));
        if (ids != null && ids.length > 0) {
            for (int id : ids) {
                updateLiveMicroTile(context, manager, id);
            }
            scheduleNextMinuteTick(context);
        }
    }

    public static void updateLiveMicroTile(@NonNull Context context,
                                           @NonNull AppWidgetManager appWidgetManager,
                                           int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_model3_live_clock);

        // Compute exact 1:1 square bounds
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        applyExactSquareAndTypographyMetrics(context, views, options);

        // Render high-DPI stepped thick numerals matching the reference image ("1:38" style)
        Bitmap steppedTimeBitmap = LiveBubbleTimeRenderer.renderCurrentTimeBitmap(context);
        views.setImageViewBitmap(R.id.widget_digital_clock_canvas, steppedTimeBitmap);

        Intent launchIntent = new Intent(context, DeskClock.class)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Replicate standard launcher icon scale-up physics
        View dummyAnchor = new View(context);
        Bundle scaleUpBundle = ActivityOptionsCompat.makeScaleUpAnimation(
            dummyAnchor, 0, 0, 108, 108).toBundle();

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE,
            scaleUpBundle
        );

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void scheduleNextMinuteTick(@NonNull Context context) {
        try {
            AlarmManager am = context.getSystemService(AlarmManager.class);
            if (am == null) {
                return;
            }
            Calendar nextMin = Calendar.getInstance();
            nextMin.add(Calendar.MINUTE, 1);
            nextMin.set(Calendar.SECOND, 0);
            nextMin.set(Calendar.MILLISECOND, 50);

            Intent tickIntent = new Intent(context, LiveClockWidgetProvider.class)
                    .setAction(ACTION_LIVE_WIDGET_MINUTE_TICK);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    70138,
                    tickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            am.setWindow(AlarmManager.RTC, nextMin.getTimeInMillis(), 1500L, pi);
        } catch (Exception ignored) {
        }
    }

    private static void cancelMinuteTick(@NonNull Context context) {
        try {
            AlarmManager am = context.getSystemService(AlarmManager.class);
            if (am == null) {
                return;
            }
            Intent tickIntent = new Intent(context, LiveClockWidgetProvider.class)
                    .setAction(ACTION_LIVE_WIDGET_MINUTE_TICK);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    70138,
                    tickIntent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pi != null) {
                am.cancel(pi);
            }
        } catch (Exception ignored) {
        }
    }

    private static void applyExactSquareAndTypographyMetrics(@NonNull Context context,
                                                             @NonNull RemoteViews views,
                                                             Bundle options) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        boolean isLandscape = context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        int minW = 0;
        int maxW = 0;
        int minH = 0;
        int maxH = 0;
        if (options != null) {
            minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
            maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
            minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
            maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        }

        int cellWidthDp = isLandscape ? (maxW > 0 ? maxW : minW) : (minW > 0 ? minW : maxW);
        int cellHeightDp = isLandscape ? (minH > 0 ? minH : maxH) : (maxH > 0 ? maxH : minH);

        if (cellWidthDp <= 0) {
            cellWidthDp = 64;
        }
        if (cellHeightDp <= 0) {
            cellHeightDp = 64;
        }

        int squareDp = Math.min(cellWidthDp, cellHeightDp);
        int padHDp = Math.max(0, (cellWidthDp - squareDp) / 2);
        int padVDp = Math.max(0, (cellHeightDp - squareDp) / 2);

        int padHPx = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, padHDp, dm));
        int padVPx = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, padVDp, dm));

        views.setViewPadding(R.id.widget_square_container, padHPx, padVPx, padHPx, padVPx);

        float textSizeDp = Math.max(6.5f, Math.min(44.0f, squareDp * 0.115f));
        views.setTextViewTextSize(R.id.widget_digital_clock, TypedValue.COMPLEX_UNIT_DIP, textSizeDp);
    }
}
