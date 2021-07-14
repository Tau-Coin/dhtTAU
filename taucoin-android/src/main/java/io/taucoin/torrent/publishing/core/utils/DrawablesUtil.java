package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.core.content.ContextCompat;

public class DrawablesUtil {

    public static void setStartDrawable(TextView view, int drawable, float with, float height) {
        Drawable mDrawable = getDrawable(view, drawable, with, height);
        view.setCompoundDrawables(mDrawable, null, null, null);
    }
    public static void setStartDrawable(TextView view, int drawable, float size) {
        Drawable mDrawable = getDrawable(view, drawable, size, size);
        view.setCompoundDrawables(mDrawable, null, null, null);
    }

    public static void setTopDrawable(TextView view, int drawable, float size) {
        Drawable mDrawable = getDrawable(view, drawable, size, size);
        view.setCompoundDrawables(null, mDrawable, null, null);
    }

    public static void setTopDrawable(TextView view, int drawable, float with, float height) {
        Drawable mDrawable = getDrawable(view, drawable, with, height);
        view.setCompoundDrawables(null, mDrawable, null, null);
    }

    public static void setEndDrawable(TextView view, int drawable, float size) {
        Drawable mDrawable = getDrawable(view, drawable, size, size);
        view.setCompoundDrawables(null, null, mDrawable, null);
    }
    public static void setEndDrawable(TextView view, int drawable, float with, float height) {
        Drawable mDrawable = getDrawable(view, drawable, with, height);
        view.setCompoundDrawables(null, null, mDrawable, null);
    }

    public static void setBottomDrawable(TextView view, int drawable, float size) {
        Drawable mDrawable = getDrawable(view, drawable, size, size);
        view.setCompoundDrawables(null, null, null, mDrawable);
    }
    public static void setBottomDrawable(TextView view, int drawable, float with, float height) {
        Drawable mDrawable = getDrawable(view, drawable, with, height);
        view.setCompoundDrawables(null, null, null, mDrawable);
    }
    public static void clearDrawable(TextView view) {
        view.setCompoundDrawables(null, null, null, null);
    }

    private static Drawable getDrawable(TextView view, int drawable, float width, float height) {
        Drawable mDrawable = ContextCompat.getDrawable(view.getContext(), drawable);
        int top = (int) width;
        int bottom = (int) height;
        mDrawable.setBounds(0, 0, top, bottom);
        return mDrawable;
    }
    public static Drawable getDrawable(Context context, int drawable, float width, float height) {
        Drawable mDrawable = ContextCompat.getDrawable(context, drawable);
        int top = (int) width;
        int bottom = (int) height;
        mDrawable.setBounds(0, 0, top, bottom);
        return mDrawable;
    }
    public static void setUnderLine(TextView view) {
        view.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        view.getPaint().setAntiAlias(true);
    }
    public static int[] obtainTypedArray(Context context, @ArrayRes int arrayId) {
        TypedArray ar = context.getResources().obtainTypedArray(arrayId);
        int len = ar.length();
        int[] array = new int[len];
        for (int i = 0; i < len; i++){
            array[i] = ar.getResourceId(i, 0);
        }
        ar.recycle();
        return array;
    }
    public static void setForeground(FrameLayout view, int drawable, float size) {
        Drawable mDrawable = getDrawable(view.getContext(), drawable, size, size);
        view.setForeground(mDrawable);
    }

    public static Drawable textToDrawable(Context context, String text, int size, int textSizeDp) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setTextSize(textSizeDp);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        Paint.FontMetrics fm = paint.getFontMetrics();
//        canvas.drawText(text, 0, 145 + fm.top - fm.ascent, paint);
//        canvas.drawText(text, 0, fm.top - fm.ascent, paint);
        canvas.drawText(text, 0, 0, paint);
        canvas.save();
        Drawable drawableRight = new BitmapDrawable(bitmap);
        drawableRight.setBounds(0, 0, drawableRight.getMinimumWidth(),
                drawableRight.getMinimumHeight());
        return drawableRight;
    }
}
