package io.taucoin.torrent.publishing.core.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.taucoin.torrent.publishing.R;

@SuppressLint("NewApi")
public class CameraFinderLayout extends FrameLayout {
    private static final int POINT_SIZE = 20;
    // 扫描动画延迟间隔时间 默认15毫秒
    private static final int scannerAnimationDelay = 15;
    private Paint paint = new Paint();
    private Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Path path = new Path();
    private Rect frame;
    // 扫描线开始位置
    public int scannerStart = 0;
    // 扫描线结束位置
    public int scannerEnd = 0;
    // 扫描线高度
    private int scannerLineHeight;
    // 扫描线每次移动距离
    private int scannerLineMoveDistance;
    // 扫描线颜色
    private int laserColor;

    public CameraFinderLayout(@NonNull Context context) {
        this(context, null);
    }

    public CameraFinderLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraFinderLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setColor(0x7f000000);
        cornerPaint.setColor(0xffffffff);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(AndroidUtilities.dimen(R.dimen.widget_size_4));
        cornerPaint.setStrokeJoin(Paint.Join.ROUND);

        scannerLineHeight = AndroidUtilities.dimen(R.dimen.widget_size_5);
        scannerLineMoveDistance = AndroidUtilities.dimen(R.dimen.widget_size_2);
        laserColor = getContext().getApplicationContext().getResources().getColor(R.color.color_blue_light);
        laserPaint.setColor(laserColor);
        setBackgroundColor(0xff000000);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result = super.drawChild(canvas, child, drawingTime);
        if (child instanceof CameraView) {
            int size = (int) (Math.min(child.getWidth(), child.getHeight()) / 1.5f);
            int x = (child.getWidth() - size) / 2;
            int y = (child.getHeight() - size) / 2;
            canvas.drawRect(0, 0, child.getMeasuredWidth(), y, paint);
            canvas.drawRect(0, y + size, child.getMeasuredWidth(), child.getMeasuredHeight(), paint);
            canvas.drawRect(0, y, x, y + size, paint);
            canvas.drawRect(x + size, y, child.getMeasuredWidth(), y + size, paint);

            if (null == frame) {
                frame = new Rect(x, y, x + size, y + size);
            }

            if(scannerStart == 0 || scannerEnd == 0) {
                scannerStart = frame.top;
                scannerEnd = frame.bottom - scannerLineHeight;
            }

            // 绘制激光线
            drawLaserScanner(canvas, frame);

            path.reset();
            path.moveTo(x, y + AndroidUtilities.dimen(R.dimen.widget_size_20));
            path.lineTo(x, y);
            path.lineTo(x + AndroidUtilities.dimen(R.dimen.widget_size_20), y);
            canvas.drawPath(path, cornerPaint);

            path.reset();
            path.moveTo(x + size, y + AndroidUtilities.dimen(R.dimen.widget_size_20));
            path.lineTo(x + size, y);
            path.lineTo(x + size - AndroidUtilities.dimen(R.dimen.widget_size_20), y);
            canvas.drawPath(path, cornerPaint);

            path.reset();
            path.moveTo(x, y + size - AndroidUtilities.dimen(R.dimen.widget_size_20));
            path.lineTo(x, y + size);
            path.lineTo(x + AndroidUtilities.dimen(R.dimen.widget_size_20), y + size);
            canvas.drawPath(path, cornerPaint);

            path.reset();
            path.moveTo(x + size, y + size - AndroidUtilities.dimen(R.dimen.widget_size_20));
            path.lineTo(x + size, y + size);
            path.lineTo(x + size - AndroidUtilities.dimen(R.dimen.widget_size_20), y + size);
            canvas.drawPath(path, cornerPaint);

            postInvalidateDelayed(scannerAnimationDelay,
                    frame.left - POINT_SIZE,
                    frame.top - POINT_SIZE,
                    frame.right + POINT_SIZE,
                    frame.bottom + POINT_SIZE);
        }
        return result;
    }

    /**
     * 绘制激光扫描线
     * @param canvas
     * @param frame
     */
    private void drawLaserScanner(Canvas canvas, Rect frame) {
        drawLineScanner(canvas,frame);
        laserPaint.setShader(null);
    }

    /**
     * 绘制线性式扫描
     * @param canvas
     * @param frame
     */
    private void drawLineScanner(Canvas canvas,Rect frame){
        //线性渐变
        LinearGradient linearGradient = new LinearGradient(
                frame.left, scannerStart,
                frame.left, scannerStart + scannerLineHeight,
                shadeColor(laserColor),
                laserColor,
                Shader.TileMode.MIRROR);

        laserPaint.setShader(linearGradient);
        if(scannerStart <= scannerEnd) {
            //椭圆
            RectF rectF = new RectF(frame.left + 2 * scannerLineHeight, scannerStart,
                    frame.right - 2 * scannerLineHeight, scannerStart + scannerLineHeight);
            canvas.drawOval(rectF, laserPaint);
            scannerStart += scannerLineMoveDistance;
        } else {
            scannerStart = frame.top;
        }
    }

    /**
     * 处理颜色模糊
     * @param color
     * @return
     */
    public int shadeColor(int color) {
        String hax = Integer.toHexString(color);
        String result = "01"+hax.substring(2);
        return Integer.valueOf(result, 16);
    }
}
