/*
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.best.deskclock.R;

/**
 * Callback receiver invoked when the user confirms pinning the 1x1 Model 3 Live Micro-Tile
 * to their home screen.
 */
public class LiveWidgetPinnedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        LiveClockWidgetProvider.updateAllLiveMicroTiles(context);
        Toast.makeText(
            context,
            context.getString(R.string.live_clock_tile_pinned_toast),
            Toast.LENGTH_SHORT
        ).show();
    }
}
