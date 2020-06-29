package io.taucoin.torrent.publishing.ui.customviews;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.minminaya.library.LGMRoundButton;
import com.minminaya.library.LGMRoundButtonDrawable;

public class RoundButton extends LGMRoundButton {

    public RoundButton(Context context) {
        super(context);
    }

    public RoundButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBgColor(int bgColor){
        Drawable background = getBackground();
        if(background instanceof LGMRoundButtonDrawable){
            ((LGMRoundButtonDrawable) background).setColor(bgColor);
        }
    }
}
