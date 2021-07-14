package io.taucoin.torrent.publishing.ui.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import io.taucoin.torrent.publishing.R;

/**
 * Circle progress
 */
public class CircleProgress extends View {
    private static final int DEFAULT_MIN_WIDTH = 400;
    private int RED = 241, GREEN = 147, BLUE = 34;
    private static final int MIN_ALPHA = 90;
    private static final int MID_ALPHA = 230;
    private static final int MAX_ALPHA = 255;
    private static float circleWidth = 5;

    // Ring color
    private int[] doughnutColors = new int[]{
            Color.argb(MIN_ALPHA, RED, GREEN, BLUE),
            Color.argb(MID_ALPHA, RED, GREEN, BLUE),
            Color.argb(MAX_ALPHA, RED, GREEN, BLUE)};

    private Paint paint = new Paint();
    private float width;
    private float height;
    private float currentAngle = 0f;
    private float radius;

    public void setOff() {
        RED = 169;
        GREEN = 169;
        BLUE = 169;
    }

    public void setOn() {
        RED = 241;
        GREEN = 147;
        BLUE = 34;
    }

    public void setConnecting() {
        RED = 241;
        GREEN = 147;
        BLUE = 34;
        init();
    }

//    private void closeConnecting() {
//        if(!isLoading){
//            postInvalidate();
//        }
//        isLoading = false;
//    }
//
//    public void closeLoading() {
//        isLoading = false;
//    }

    public void setError() {
        RED = 255;
        GREEN = 0;
        BLUE = 0;
    }

    public CircleProgress(Context context) {
        this(context, null);
    }

    public CircleProgress(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleProgress(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        circleWidth = getContext().getResources().getDimension(R.dimen.widget_size_8);
        sweepGradient = new SweepGradient(0, 0, doughnutColors, null);
    }

    private void resetParams() {
        width = getWidth();
        height = getHeight();
        radius = Math.min(width, height)/2;
//        float size = radius * doughnutRadiusPercent;
        float size = radius - circleWidth / 2;
        rectF = new RectF(-size, -size, size, size);
    }

    private void initPaint() {
        paint.reset();
        paint.setAntiAlias(true);
    }
    private RectF rectF;
    private SweepGradient sweepGradient;
    @Override
    protected void onDraw(Canvas canvas) {
        resetParams();

        //Set the center of the canvas as the origin (0,0)
        canvas.translate(width / 2, height / 2);

        // draw background
//        if(!isLoading) {
            initPaint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(MAX_ALPHA, RED, GREEN, BLUE));
            float bgRadius = radius;
//            if (isLoading) {
//                bgRadius = radius * doughnutRadiusPercent;
                bgRadius = radius - circleWidth;
//            }
            canvas.drawCircle(0, 0, bgRadius, paint);
//        }
//        if(isLoading){
            // Turn around
            canvas.rotate(currentAngle, 0, 0);
            if (currentAngle >= 360f){
                currentAngle = currentAngle - 360f;
            } else{
                currentAngle = currentAngle + 5f;
            }


            // Draw Gradient Rings
//            float doughnutWidth = radius * doughnutWidthPercent;//圆环宽度
//            float doughnutWidth = dp2px(getContext(), 10);
            // Circle circumscribed rectangle
            initPaint();
            paint.setStrokeWidth(circleWidth);
            paint.setStyle(Paint.Style.STROKE);
            paint.setShader(sweepGradient);
            canvas.drawArc(rectF, 0, 360, false, paint);

            // Draw a circle with a rotating head
            initPaint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(MAX_ALPHA, RED, GREEN, BLUE));
//            canvas.drawCircle(radius * doughnutRadiusPercent, 0, doughnutWidth / 2, paint);
            canvas.drawCircle(radius - circleWidth / 2, 0, circleWidth / 2, paint);
//        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measure(widthMeasureSpec), measure(heightMeasureSpec));
    }

    private int measure(int origin) {
        int result = DEFAULT_MIN_WIDTH;
        int specMode = MeasureSpec.getMode(origin);
        int specSize = MeasureSpec.getSize(origin);
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onDetachedFromWindow() {
//        closeLoading();
        super.onDetachedFromWindow();
    }
}
