package io.taucoin.torrent.publishing.ui.customviews;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.minminaya.library.LGMRoundButtonDrawable;

import androidx.appcompat.widget.AppCompatButton;
import com.minminaya.library.R.attr;

public class LGMRoundButton extends AppCompatButton {
    private ColorStateList colorStateListForTextColorForPressed;
    private ColorStateList colorStateListTemp = null;

    public LGMRoundButton(Context context) {
        super(context);
        this.init(context, (AttributeSet)null, 0);
    }

    public LGMRoundButton(Context context, AttributeSet attrs) {
        super(context, attrs, attr.LGMRoundButtonStyle);
        this.init(context, attrs, attr.LGMRoundButtonStyle);
    }

    public LGMRoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        LGMRoundButtonDrawable mRoundButtonDrawable = LGMRoundButtonDrawable.obtainAttributeData(context, attrs, defStyleAttr);
        this.colorStateListForTextColorForPressed = mRoundButtonDrawable.getColorStateListForTextColorForPressed();
        int[] padding = new int[]{this.getPaddingLeft(), this.getPaddingTop(), this.getPaddingRight(), this.getPaddingBottom()};
        this.setBackground(mRoundButtonDrawable);
        this.setPadding(padding[0], padding[1], padding[2], padding[3]);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
            case 0:
                this.colorStateListTemp = this.getTextColors();
                this.setTextColor(this.colorStateListForTextColorForPressed);
                break;
            case 1:
                this.setTextColor(this.colorStateListTemp);
        }
        return super.onTouchEvent(event);
    }
}
