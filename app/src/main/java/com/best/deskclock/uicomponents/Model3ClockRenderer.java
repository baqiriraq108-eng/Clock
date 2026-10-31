/*
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.R;
import com.best.deskclock.widgets.LiveBubbleTimeRenderer;

import java.util.Calendar;

/**
 * High-precision renderer for the Unified SVG Master Specification Live Real-Time Clock.
 *
 * <p>Uses the 4x Super-Resolution clean base plate ({@code R.drawable.model3_widget_base_bitmap})
 * with hardware mipmapping, dithering, and bilinear filtering enabled, and dynamically renders
 * the live wavy vector digital time via {@link LiveBubbleTimeRenderer} inside the lower-left
 * sea-glass emerald bubble along with synchronized live hour, minute, and second hands on the
 * right-side circular analog dial ({@code cx=654, cy=472}).</p>
 */
public final class Model3ClockRenderer {

    /** Normalized coordinate space matching the 1024x1024 SVG viewBox. */
    public static final float REF_SIZE = 1024.0f;

    /** Exact analog clock pivot center in the 1024x1024 SVG specification. */
    public static final float ANALOG_CENTER_X = 654.0f;
    public static final float ANALOG_CENTER_Y = 472.0f;

    /** Exact digital time visual center inside the emerald bubble in 1024x1024 space. */
    public static final float DIGITAL_CENTER_X = 400.0f;
    public static final float DIGITAL_CENTER_Y = 678.0f;

    private static Bitmap sBasePlateBitmap;
    private static Bitmap sEmeraldBubbleBitmap;

    private static final int SMOOTH_PAINT_FLAGS =
            Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG;

    private final Paint mBitmapPaint = new Paint(SMOOTH_PAINT_FLAGS);
    private final Paint mHandShadowPaint = new Paint(SMOOTH_PAINT_FLAGS);
    private final Paint mWhiteHandPaint = new Paint(SMOOTH_PAINT_FLAGS);
    private final Paint mSecondHandPaint = new Paint(SMOOTH_PAINT_FLAGS);
    private final Paint mPivotPaint = new Paint(SMOOTH_PAINT_FLAGS);
    private final Paint mPivotGlintPaint = new Paint(SMOOTH_PAINT_FLAGS);

    private final Rect mSrcRect = new Rect();
    private final RectF mDstRect = new RectF();
    private final RectF mHandRect = new RectF();

    public Model3ClockRenderer() {
        mHandShadowPaint.setStyle(Paint.Style.FILL);
        mHandShadowPaint.setColor(Color.argb(191, 1, 5, 20)); // #010514 at 0.75 opacity

        mWhiteHandPaint.setStyle(Paint.Style.FILL);
        mSecondHandPaint.setStyle(Paint.Style.FILL);
        mPivotPaint.setStyle(Paint.Style.FILL);

        mPivotGlintPaint.setStyle(Paint.Style.FILL);
        mPivotGlintPaint.setColor(Color.argb(166, 224, 242, 254)); // #E0F2FE at 0.65 opacity
    }

    @Nullable
    public static synchronized Bitmap getBasePlateBitmap(@NonNull Context context) {
        if (sBasePlateBitmap == null || sBasePlateBitmap.isRecycled()) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            sBasePlateBitmap = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.model3_widget_base_bitmap, opts);
            if (sBasePlateBitmap != null) {
                sBasePlateBitmap.setHasMipMap(true);
            }
        }
        return sBasePlateBitmap;
    }

    @Nullable
    public static synchronized Bitmap getEmeraldBubbleBitmap(@NonNull Context context) {
        if (sEmeraldBubbleBitmap == null || sEmeraldBubbleBitmap.isRecycled()) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            sEmeraldBubbleBitmap = BitmapFactory.decodeResource(
                context.getResources(), R.drawable.model3_emerald_bubble_bitmap, opts);
            if (sEmeraldBubbleBitmap != null) {
                sEmeraldBubbleBitmap.setHasMipMap(true);
            }
        }
        return sEmeraldBubbleBitmap;
    }

    /**
     * Renders the complete live Model 3 clock onto the given Canvas using the current system time.
     */
    public void drawLiveClock(@NonNull Context context,
                              @NonNull Canvas canvas,
                              float left,
                              float top,
                              float size,
                              boolean smoothSubSeconds) {
        Calendar now = Calendar.getInstance();
        int hour24 = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        int second = now.get(Calendar.SECOND);
        int millis = smoothSubSeconds ? now.get(Calendar.MILLISECOND) : 0;
        boolean is24Hour = DateFormat.is24HourFormat(context);

        drawClockAtTime(context, canvas, left, top, size, hour24, minute, second, millis, is24Hour);
    }

    /**
     * Renders the Model 3 clock for an explicit timestamp.
     */
    public void drawClockAtTime(@NonNull Context context,
                                @NonNull Canvas canvas,
                                float left,
                                float top,
                                float size,
                                int hour24,
                                int minute,
                                int second,
                                int millis,
                                boolean is24Hour) {
        if (size <= 0f) {
            return;
        }

        // Layer 1: Clean base plate (Squircle, Waves, Orange Arrow, Empty Dial Ring & Ticks)
        Bitmap basePlate = getBasePlateBitmap(context);
        if (basePlate != null) {
            mSrcRect.set(0, 0, basePlate.getWidth(), basePlate.getHeight());
            mDstRect.set(left, top, left + size, top + size);
            canvas.drawBitmap(basePlate, mSrcRect, mDstRect, mBitmapPaint);
        } else {
            drawFallbackBasePlate(canvas, left, top, size);
        }

        float scale = size / REF_SIZE;

        // Layer 2: Real-time analog hands & domed azure pivot inside the right circular clock
        drawLiveAnalogHands(canvas, left, top, scale, hour24, minute, second, millis);

        // Layer 3: Organic sea-glass emerald bubble overlay (occludes the second hand when it sweeps underneath)
        Bitmap emeraldBubble = getEmeraldBubbleBitmap(context);
        if (emeraldBubble != null) {
            mSrcRect.set(0, 0, emeraldBubble.getWidth(), emeraldBubble.getHeight());
            mDstRect.set(left, top, left + size, top + size);
            canvas.drawBitmap(emeraldBubble, mSrcRect, mDstRect, mBitmapPaint);
        }

        // Layer 4: Real-time wavy vector digital numerals on top of the sea-glass emerald bubble
        LiveBubbleTimeRenderer.drawSteppedBubbleTimeOnSquircle(
                canvas, left, top, size, hour24, minute, is24Hour);
    }

    private void drawLiveAnalogHands(@NonNull Canvas canvas,
                                     float left,
                                     float top,
                                     float scale,
                                     int hour24,
                                     int minute,
                                     int second,
                                     int millis) {
        float cx = left + ANALOG_CENTER_X * scale;
        float cy = top + ANALOG_CENTER_Y * scale;

        float exactSeconds = second + (millis / 1000.0f);
        float exactMinutes = minute + (exactSeconds / 60.0f);
        float exactHours = (hour24 % 12) + (exactMinutes / 60.0f);

        float hourDeg = exactHours * 30.0f;
        float minDeg = exactMinutes * 6.0f;
        float secDeg = exactSeconds * 6.0f;

        float shadowDy = 6.0f * scale;
        float blurRadius = Math.max(0.8f, 6.0f * scale);
        mHandShadowPaint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));

        // 1. Hour hand: <rect x="641" y="366" width="26" height="118" rx="13" fill="url(#whiteHandGrad)" />
        drawRotatedCapsuleHand(canvas, cx, cy, hourDeg,
                -13.0f * scale, -106.0f * scale, 13.0f * scale, 12.0f * scale, 13.0f * scale,
                shadowDy, Color.WHITE, Color.rgb(220, 230, 247));

        // 2. Minute hand: <rect x="644" y="332" width="20" height="152" rx="10" fill="url(#whiteHandGrad)" />
        drawRotatedCapsuleHand(canvas, cx, cy, minDeg,
                -10.0f * scale, -140.0f * scale, 10.0f * scale, 12.0f * scale, 10.0f * scale,
                shadowDy, Color.WHITE, Color.rgb(220, 230, 247));

        // 3. Electric Azure Second hand: <rect x="650.5" y="324" width="7" height="184" rx="3.5" fill="url(#blueSecondGrad)" />
        {
            float l = -3.5f * scale;
            float t = -148.0f * scale;
            float r = 3.5f * scale;
            float b = 36.0f * scale;
            float rx = 3.5f * scale;

            // Shadow
            canvas.save();
            canvas.translate(cx, cy + shadowDy);
            canvas.rotate(secDeg);
            mHandRect.set(l, t, r, b);
            canvas.drawRoundRect(mHandRect, rx, rx, mHandShadowPaint);
            canvas.restore();

            // Foreground with SVG blueSecondGrad (#7DD3FC -> #38BDF8 -> #0284C7)
            canvas.save();
            canvas.translate(cx, cy);
            canvas.rotate(secDeg);
            mHandRect.set(l, t, r, b);
            mSecondHandPaint.setShader(new LinearGradient(
                    l, t, r, b,
                    new int[]{Color.rgb(125, 211, 252), Color.rgb(56, 189, 248), Color.rgb(2, 132, 199)},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawRoundRect(mHandRect, rx, rx, mSecondHandPaint);
            canvas.restore();
        }

        // 4. Central Domed Azure Pivot Cap (r=19 in 1024 space, pivotCapGrad: #BAE6FD -> #2979FF -> #1E3A8A)
        float pivotRadius = 19.0f * scale;
        canvas.drawCircle(cx, cy + shadowDy, pivotRadius, mHandShadowPaint);

        RadialGradient pivotGrad = new RadialGradient(
            cx - pivotRadius * 0.30f,
            cy - pivotRadius * 0.40f,
            pivotRadius * 1.40f,
            new int[]{Color.rgb(186, 230, 253), Color.rgb(41, 121, 255), Color.rgb(30, 58, 138)},
            new float[]{0f, 0.45f, 1f},
            Shader.TileMode.CLAMP
        );
        mPivotPaint.setShader(pivotGrad);
        canvas.drawCircle(cx, cy, pivotRadius, mPivotPaint);

        // Specular glint on pivot cap: <circle cx="649" cy="467" r="5" fill="#E0F2FE" opacity="0.65" />
        canvas.drawCircle(cx - 5.0f * scale, cy - 5.0f * scale, 5.0f * scale, mPivotGlintPaint);
    }

    private void drawRotatedCapsuleHand(@NonNull Canvas canvas,
                                        float cx,
                                        float cy,
                                        float angleDeg,
                                        float l,
                                        float t,
                                        float r,
                                        float b,
                                        float rx,
                                        float shadowDy,
                                        int startColor,
                                        int endColor) {
        // Shadow pass
        canvas.save();
        canvas.translate(cx, cy + shadowDy);
        canvas.rotate(angleDeg);
        mHandRect.set(l, t, r, b);
        canvas.drawRoundRect(mHandRect, rx, rx, mHandShadowPaint);
        canvas.restore();

        // Foreground pass
        canvas.save();
        canvas.translate(cx, cy);
        canvas.rotate(angleDeg);
        mHandRect.set(l, t, r, b);
        mWhiteHandPaint.setShader(new LinearGradient(
                l, t, r, b, startColor, endColor, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(mHandRect, rx, rx, mWhiteHandPaint);
        canvas.restore();
    }

    private void drawFallbackBasePlate(@NonNull Canvas canvas, float left, float top, float size) {
        float s = size / REF_SIZE;
        Paint p = new Paint(SMOOTH_PAINT_FLAGS);

        RectF sq = new RectF(left + 64 * s, top + 64 * s, left + 960 * s, top + 960 * s);
        p.setShader(new LinearGradient(sq.left, sq.top, sq.right, sq.bottom,
            Color.rgb(30, 64, 175), Color.rgb(7, 17, 51), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(sq, 220 * s, 220 * s, p);

        float cx = left + ANALOG_CENTER_X * s;
        float cy = top + ANALOG_CENTER_Y * s;
        p.setShader(new LinearGradient(cx - 222 * s, cy - 222 * s, cx + 222 * s, cy + 222 * s,
            Color.rgb(199, 210, 254), Color.rgb(79, 70, 229), Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, 222 * s, p);

        p.setShader(new LinearGradient(cx, cy - 176 * s, cx, cy + 176 * s,
            Color.rgb(15, 30, 74), Color.rgb(4, 9, 26), Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, 176 * s, p);

        Path bubble = new Path();
        bubble.moveTo(left + 278 * s, top + 486 * s);
        bubble.cubicTo(left + 332 * s, top + 482 * s, left + 378 * s, top + 518 * s, left + 432 * s, top + 538 * s);
        bubble.cubicTo(left + 488 * s, top + 558 * s, left + 552 * s, top + 566 * s, left + 594 * s, top + 598 * s);
        bubble.cubicTo(left + 638 * s, top + 632 * s, left + 652 * s, top + 692 * s, left + 630 * s, top + 742 * s);
        bubble.cubicTo(left + 608 * s, top + 792 * s, left + 546 * s, top + 822 * s, left + 468 * s, top + 822 * s);
        bubble.cubicTo(left + 372 * s, top + 822 * s, left + 254 * s, top + 792 * s, left + 196 * s, top + 728 * s);
        bubble.cubicTo(left + 142 * s, top + 668 * s, left + 148 * s, top + 582 * s, left + 188 * s, top + 532 * s);
        bubble.cubicTo(left + 214 * s, top + 500 * s, left + 244 * s, top + 489 * s, left + 278 * s, top + 486 * s);
        bubble.close();
        p.setShader(new LinearGradient(left + 148 * s, top + 486 * s, left + 642 * s, top + 822 * s,
            Color.rgb(167, 243, 208), Color.rgb(6, 78, 59), Shader.TileMode.CLAMP));
        canvas.drawPath(bubble, p);
    }

    @NonNull
    public Bitmap renderLiveIconBitmap(@NonNull Context context, int iconSizePx) {
        Bitmap bitmap = Bitmap.createBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888);
        bitmap.setHasMipMap(true);
        Canvas canvas = new Canvas(bitmap);
        drawLiveClock(context, canvas, 0f, 0f, iconSizePx, false);
        return bitmap;
    }
}
