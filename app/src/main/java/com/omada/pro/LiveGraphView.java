package com.omada.pro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class LiveGraphView extends View {
    private final List<Float> series1 = new ArrayList<>();
    private final List<Float> series2 = new ArrayList<>();
    private final Paint linePaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color1 = Color.parseColor("#00FF41");
    private int color2 = Color.parseColor("#38BDF8");
    private float maxVal = 100f;

    public LiveGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        for (int i = 0; i < 30; i++) {
            series1.add(0f);
            series2.add(0f);
        }
        linePaint1.setStyle(Paint.Style.STROKE);
        linePaint1.setStrokeWidth(5f);
        linePaint2.setStyle(Paint.Style.STROKE);
        linePaint2.setStrokeWidth(4f);
        gridPaint.setColor(Color.parseColor("#22FFFFFF"));
        gridPaint.setStrokeWidth(2f);
    }

    public void setColors(int c1, int c2) {
        this.color1 = c1;
        this.color2 = c2;
        invalidate();
    }

    public void setMaxVal(float max) {
        this.maxVal = Math.max(10f, max);
    }

    public void addPoint(float v1, float v2) {
        if (series1.size() >= 30) series1.remove(0);
        if (series2.size() >= 30) series2.remove(0);
        series1.add(v1);
        series2.add(v2);

        float peak = 10f;
        for (float f : series1) if (f > peak) peak = f;
        for (float f : series2) if (f > peak) peak = f;
        if (maxVal != 100f) maxVal = peak * 1.25f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Draw Grid
        for (int i = 1; i <= 3; i++) {
            float y = (h / 4f) * i;
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        linePaint1.setColor(color1);
        linePaint2.setColor(color2);
        fillPaint1.setShader(new LinearGradient(0, 0, 0, h,
                Color.argb(70, Color.red(color1), Color.green(color1), Color.blue(color1)),
                Color.TRANSPARENT, Shader.TileMode.CLAMP));

        drawSeries(canvas, series1, linePaint1, fillPaint1, w, h);
        drawSeries(canvas, series2, linePaint2, null, w, h);
    }

    private void drawSeries(Canvas canvas, List<Float> data, Paint stroke, Paint fill, float w, float h) {
        if (data.size() < 2) return;
        Path path = new Path();
        Path fillPath = new Path();
        float stepX = w / (data.size() - 1);

        for (int i = 0; i < data.size(); i++) {
            float x = i * stepX;
            float norm = Math.min(1f, Math.max(0f, data.get(i) / maxVal));
            float y = h - (norm * (h - 16f)) - 8f;
            if (i == 0) {
                path.moveTo(x, y);
                fillPath.moveTo(x, h);
                fillPath.lineTo(x, y);
            } else {
                path.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }
        if (fill != null) {
            fillPath.lineTo(w, h);
            fillPath.close();
            canvas.drawPath(fillPath, fill);
        }
        canvas.drawPath(path, stroke);
    }
}