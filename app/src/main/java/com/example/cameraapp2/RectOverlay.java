package com.example.cameraapp2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;



public class RectOverlay extends View {

    private Paint paint;

    public RectOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.STROKE);
    }


    public void drawOverlay(Canvas canvas){
        canvas.drawColor(Color.RED);
        invalidate();


    }
    @Override
    protected  void onDraw(Canvas canvas){
        int canvasW = canvas.getWidth();
        int canvasH = canvas.getHeight();
        Point centerOfCanvas = new Point(canvasW / 2, canvasH / 2);
        int rectW = 700;
        int rectH = 300;
        int left = centerOfCanvas.x - (rectW / 2);
        int top = centerOfCanvas.y - (rectH / 2);
        int right = centerOfCanvas.x + (rectW / 2);
        int bottom = centerOfCanvas.y + (rectH / 2);
        Log.d("WidthxHeight", String.valueOf(canvasW)+"x"+String.valueOf(canvasH));
        Rect rect = new Rect(left, top, right, bottom);
        canvas.drawRect(rect, paint);

    }
}