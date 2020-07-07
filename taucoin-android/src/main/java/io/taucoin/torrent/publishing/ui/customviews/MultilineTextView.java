package io.taucoin.torrent.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.TextView;

@SuppressLint("AppCompatCustomView")
public class MultilineTextView extends TextView {

    private boolean calculatedLines = false;

    public MultilineTextView(Context context) {
        super(context);
    }

    public MultilineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultilineTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!calculatedLines) {
            calculateLines();
            calculatedLines = true;
        }
        super.onDraw(canvas);
    }

    private void calculateLines() {
        int mHeight = getMeasuredHeight() + (int)getLineSpacingExtra();
        int lHeight = getLineHeight();
        int lines = mHeight / lHeight;
        setLines(lines);
    }
}