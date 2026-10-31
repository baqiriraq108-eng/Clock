/*
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Locale;

/**
 * Tier 3: In-App 60fps Smooth Hardware Rendering for the Model 3 Real-Time Clock.
 *
 * <p>Uses {@link Choreographer.FrameCallback} gated by {@link #onVisibilityAggregated(boolean)}
 * to sweep the blue second hand smoothly at 60fps when visible and 0fps when hidden.</p>
 */
public class Model3ClockView extends View implements Choreographer.FrameCallback {

    private final Model3ClockRenderer mRenderer = new Model3ClockRenderer();
    private boolean mIsAnimating = false;
    private boolean mSmoothSweep = true;
    private boolean mReceiverRegistered = false;
    private int mLastMinute = -1;

    private final BroadcastReceiver mTimeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (context != null) {
                LiveLauncherIconManager.updateLiveLauncherIcon(context, true);
            }
            updateContentDescription();
            invalidate();
        }
    };

    public Model3ClockView(@NonNull Context context) {
        this(context, null);
    }

    public Model3ClockView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Model3ClockView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int size = Math.min(view.getWidth(), view.getHeight());
                int left = (view.getWidth() - size) / 2;
                int top = (view.getHeight() - size) / 2;
                outline.setRoundRect(left, top, left + size, top + size, size * 0.22f);
            }
        });
        setClipToOutline(true);
        setOnClickListener(v -> LiveLauncherIconManager.showLauncherStatusDialog(v.getContext()));
        updateContentDescription();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerTimeReceiver();
        updateContentDescription();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimation();
        unregisterTimeReceiver();
        super.onDetachedFromWindow();
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        if (isVisible) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && isShown()) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    private void startAnimation() {
        if (!mIsAnimating && isShown() && mSmoothSweep) {
            mIsAnimating = true;
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    private void stopAnimation() {
        if (mIsAnimating) {
            mIsAnimating = false;
            Choreographer.getInstance().removeFrameCallback(this);
        }
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!mIsAnimating || !isShown()) {
            mIsAnimating = false;
            return;
        }
        Calendar now = Calendar.getInstance();
        int currentMin = now.get(Calendar.MINUTE);
        if (currentMin != mLastMinute) {
            mLastMinute = currentMin;
            updateContentDescription();
        }
        invalidate();
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);

        int defaultPx = (int) (180 * getResources().getDisplayMetrics().density);
        int width = (wMode == MeasureSpec.EXACTLY || wMode == MeasureSpec.AT_MOST) ? wSize : defaultPx;
        int height = (hMode == MeasureSpec.EXACTLY || hMode == MeasureSpec.AT_MOST) ? hSize : defaultPx;
        int square = Math.min(width, height);
        setMeasuredDimension(square, square);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h);
        float left = (w - size) * 0.5f;
        float top = (h - size) * 0.5f;

        mRenderer.drawLiveClock(getContext(), canvas, left, top, size, mSmoothSweep);
    }

    private void updateContentDescription() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        Calendar now = Calendar.getInstance();
        int h24 = now.get(Calendar.HOUR_OF_DAY);
        int min = now.get(Calendar.MINUTE);
        boolean is24 = DateFormat.is24HourFormat(context);
        String timeStr = is24
            ? String.format(Locale.US, "%02d:%02d", h24, min)
            : String.format(Locale.US, "%d:%02d", (h24 % 12 == 0 ? 12 : h24 % 12), min);
        setContentDescription("Model 3 Live Clock " + timeStr);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerTimeReceiver() {
        if (mReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getContext().registerReceiver(mTimeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getContext().registerReceiver(mTimeChangeReceiver, filter);
        }
        mReceiverRegistered = true;
    }

    private void unregisterTimeReceiver() {
        if (!mReceiverRegistered) {
            return;
        }
        try {
            getContext().unregisterReceiver(mTimeChangeReceiver);
        } catch (IllegalArgumentException ignored) {
        }
        mReceiverRegistered = false;
    }
}
