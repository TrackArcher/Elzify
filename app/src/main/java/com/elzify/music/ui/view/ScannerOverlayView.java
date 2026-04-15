package com.elzify.music.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ScannerOverlayView extends View {

    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path maskPath = new Path();

    private float frameSizePx;
    private float frameRadiusPx;
    private float cornerLengthPx;
    private float cornerStrokePx;

    public ScannerOverlayView(Context context) {
        super(context);
        init();
    }

    public ScannerOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScannerOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_HARDWARE, null);

        int maskColor = resolveThemeColor(android.R.attr.colorBackground);
        maskPaint.setStyle(Paint.Style.FILL);
        maskPaint.setColor(maskColor);

        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setColor(0xFF38D16A);
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);

        frameSizePx = dp(250);
        frameRadiusPx = dp(14);
        cornerLengthPx = dp(26);
        cornerStrokePx = dp(4);
        cornerPaint.setStrokeWidth(cornerStrokePx);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float half = frameSizePx / 2f;
        float left = cx - half;
        float top = cy - half;
        float right = cx + half;
        float bottom = cy + half;

        maskPath.reset();
        maskPath.setFillType(Path.FillType.EVEN_ODD);
        maskPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        maskPath.addRoundRect(left, top, right, bottom, frameRadiusPx, frameRadiusPx, Path.Direction.CW);
        canvas.drawPath(maskPath, maskPaint);

        drawCornerLines(canvas, left, top, right, bottom);
    }

    private void drawCornerLines(Canvas canvas, float left, float top, float right, float bottom) {
        // Top-left
        canvas.drawLine(left, top, left + cornerLengthPx, top, cornerPaint);
        canvas.drawLine(left, top, left, top + cornerLengthPx, cornerPaint);

        // Top-right
        canvas.drawLine(right - cornerLengthPx, top, right, top, cornerPaint);
        canvas.drawLine(right, top, right, top + cornerLengthPx, cornerPaint);

        // Bottom-left
        canvas.drawLine(left, bottom - cornerLengthPx, left, bottom, cornerPaint);
        canvas.drawLine(left, bottom, left + cornerLengthPx, bottom, cornerPaint);

        // Bottom-right
        canvas.drawLine(right, bottom - cornerLengthPx, right, bottom, cornerPaint);
        canvas.drawLine(right - cornerLengthPx, bottom, right, bottom, cornerPaint);
    }

    private float dp(int value) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }

    private int resolveThemeColor(int attrRes) {
        TypedValue typedValue = new TypedValue();
        boolean found = getContext().getTheme().resolveAttribute(attrRes, typedValue, true);
        if (!found) return 0xFF000000;
        return typedValue.data;
    }
}
