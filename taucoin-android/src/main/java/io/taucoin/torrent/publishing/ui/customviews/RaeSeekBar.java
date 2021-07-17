package io.taucoin.torrent.publishing.ui.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;

import org.slf4j.LoggerFactory;

import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.content.ContextCompat;
import io.taucoin.torrent.publishing.R;

/**
 * 字体大小选择条
 */
public class RaeSeekBar extends AppCompatSeekBar {
    // 默认字体大小
    private static final int mDefaultTextSize = R.dimen.widget_size_16;
    //  刻度说明文本，数组数量跟刻度数量一致，跟mTextSize的长度要一致
    private String[] mTickMarkTitles = new String[]{
            "A",
            "Default",
            "",
            "",
            "A"
    };

    // 刻度代表的字体大小
    private int[] mTextSize;
    // 刻度代表的字体大小
    private String[] mFontScaleSize = new String[]{
            "0.85",
            "1.0",
            "1.15",
            "1.3",
            "1.45"
    };

    // 刻度文本画笔
    private final Paint mTickMarkTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    // 刻度线的高度
    private int mLineHeight = R.dimen.widget_size_10;
    // 保存位置大小信息
    private final Rect mRect = new Rect();

    public RaeSeekBar(Context context) {
        this(context, null);
    }

    public RaeSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RaeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        mTextSize = new int[mFontScaleSize.length];
        float defaultTextSize = getResources().getDimension(mDefaultTextSize);
        for (int i = 0; i < mFontScaleSize.length; i++) {
            mTextSize[i] = (int) (defaultTextSize * Float.parseFloat(mFontScaleSize[i]));
        }
        // 刻度线的高度
        mLineHeight = getSize(mLineHeight);
        // 刻度文字的对齐方式为居中对齐
        mTickMarkTitlePaint.setTextAlign(Paint.Align.CENTER);
        // 设置最大刻度值为字体大小数组的长度
        setMax(mFontScaleSize.length - 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
        // 刻度长度
        int maxLength = getMax();
        int width = getWidth();
        int height = getHeight();
        int h2 = height / 2; // 居中

        // 画刻度背景
        mRect.left = getPaddingLeft();
        mRect.right = width - getPaddingRight();
        mRect.top = h2 - getSize(R.dimen.widget_size_1); // 居中
        mRect.bottom = mRect.top + getSize(R.dimen.widget_size_2); // 1.5f为直线的高度
        // 直线的长度
        int lineWidth = mRect.width();
        // 画直线
        mTickMarkTitlePaint.setColor(ContextCompat.getColor(getContext(), R.color.gray_light));
        canvas.drawRect(mRect, mTickMarkTitlePaint);

        //  遍历刻度，画分割线和刻度文本
        for (int i = 0; i <= maxLength; i++) {

            // 刻度的起始间隔 = 左间距 + (线条的宽度 * 当前刻度位置 / 刻度长度)
            int thumbPos = getPaddingLeft() + (lineWidth * i / maxLength);
            // 画分割线
            mRect.top = h2 - mLineHeight / 2;
            mRect.bottom = h2 + mLineHeight / 2;
            mRect.left = thumbPos;
            mRect.right = thumbPos + getSize(R.dimen.widget_size_2); // 直线的宽度为1.5
            mTickMarkTitlePaint.setColor(ContextCompat.getColor(getContext(), R.color.gray_light));
            canvas.drawRect(mRect, mTickMarkTitlePaint);

            // 画刻度文本
            String title = mTickMarkTitles[i % mTickMarkTitles.length]; // 拿到刻度文本
            mTickMarkTitlePaint.setColor(ContextCompat.getColor(getContext(), R.color.color_black));
            mTickMarkTitlePaint.getTextBounds(title, 0, title.length(), mRect); // 计算刻度文本的大小以及位置
            mTickMarkTitlePaint.setTextSize(mTextSize[i % mTickMarkTitles.length]); // 设置刻度文字大小
            // 画文本
            canvas.drawText(title, thumbPos, mTextSize[mTextSize.length - 1], mTickMarkTitlePaint);
        }
        } catch (Exception e) {
            LoggerFactory.getLogger("SeekBar").error("test=", e);
        }
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 加上字体大小
        int wm = MeasureSpec.getMode(widthMeasureSpec);
        int hm = MeasureSpec.getMode(heightMeasureSpec);
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        // 以最大的字体为基础，加上刻度字体大小
        h += mTextSize[mTextSize.length - 1];
        // 加上与刻度之间的间距大小
        // 刻度文本跟刻度之间的间隔
        int mOffsetY = R.dimen.widget_size_60;
        h += getSize(mOffsetY);
        // 保存测量结果
        setMeasuredDimension(MeasureSpec.makeMeasureSpec(w, wm), MeasureSpec.makeMeasureSpec(h, hm));
    }

    protected int getSize(int id) {
        return getResources().getDimensionPixelSize(id);
    }

    public float getFontScaleSize(int progress) {
        return Float.parseFloat(mFontScaleSize[progress % mFontScaleSize.length]);
    }

    public void setFontScaleSize(float scaleSize) {
        for (int i = 0; i < mFontScaleSize.length; i++) {
            float fontScaleSize = Float.parseFloat(mFontScaleSize[i]);
            if (fontScaleSize == scaleSize) {
                setProgress(i);
                break;
            }
        }
    }

    public void setFontScaleSizes(String[] scaleSizes) {
        this.mFontScaleSize = scaleSizes;
        invalidate();
    }

    public void setFontScaleTitles(String[] scaleTitles) {
        this.mTickMarkTitles = scaleTitles;
        invalidate();
    }
}
