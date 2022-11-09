package com.example.cameraapp2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;


public class RectOverlay extends View {
    private final Paint paint;

    public RectOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
    }

    public void drawOverlay(Canvas canvas) {
        canvas.drawColor(Color.RED);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int canvasW = getWidth() / 2;
        int canvasH = getHeight() / 2;
        int rectW = 700;
        int rectH = 300;
        int left = canvasW - (rectW / 2);
        int top = canvasH - (rectH / 2);
        int right = canvasW + (rectW / 2);
        int bottom = canvasH + (rectH / 2);
        Log.d("WidthxHeight", "" + canvasW + "x" + canvasH);
        Rect rect = new Rect(left, top, right, bottom);
        canvas.drawRect(rect, paint);
    }
}