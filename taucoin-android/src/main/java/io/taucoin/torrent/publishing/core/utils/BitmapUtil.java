package io.taucoin.torrent.publishing.core.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.View;
import android.widget.ImageView;

//import com.huawei.hms.hmsscankit.ScanUtil;
//import com.huawei.hms.hmsscankit.WriterException;
//import com.huawei.hms.ml.scan.HmsBuildBitmapOption;
//import com.huawei.hms.ml.scan.HmsScan;

import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.DrawBean;

/**
 * Bitmap工具类
 */
public class BitmapUtil {
    /**
     * 处理模糊Bitmap
     * @param sentBitmap
     * @param radius
     * @param canReuseInBitmap
     * @return
     */
    public static Bitmap blurBitmap(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {
        Bitmap bitmap;
        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }
        if (radius < 1) {
            return (null);
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16)
                        | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return (bitmap);
    }

    public static Bitmap blurBitmap(Bitmap bitmap, boolean isRecycle){

        // 计算图片缩小后的长宽
        int width = Math.round(bitmap.getWidth() * 0.3f);
        int height = Math.round(bitmap.getHeight() * 0.3f);
        // 将缩小后的图片做为预渲染的图片
        Bitmap inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);

        //Let's create an empty bitmap with the same size of the bitmap we want to blur
        Bitmap outBitmap = Bitmap.createBitmap(inputBitmap);

        //Instantiate a new Renderscript
        Context context = MainApplication.getInstance();
        RenderScript rs = RenderScript.create(context.getApplicationContext());

        //Create an Intrinsic Blur Script using the Renderscript
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        //Create the Allocations (in/out) with the Renderscript and the in/out bitmaps
        Allocation allIn = Allocation.createFromBitmap(rs, inputBitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);

        //Set the radius of the blur
        blurScript.setRadius(25.f);

        //Perform the Renderscript
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);

        //Copy the final bitmap created by the out Allocation to the outBitmap
        allOut.copyTo(outBitmap);

        //recycle the original bitmap
        if (isRecycle) {
            bitmap.recycle();
        }
        inputBitmap.recycle();

        //After finishing everything, we destroy the Renderscript.
        rs.destroy();
        allIn.destroy();
        allOut.destroy();

        return outBitmap;
    }

    /**
     * @param bgBitmap 背景图片
     * @param qrbgBitmap  二维码背景
     * @param originalQrBitmap 原始二维码
     * @param drawBean  尺寸资源名字
     * @return
     */
    public static Bitmap drawStyleQRcode(Bitmap bgBitmap, Bitmap qrbgBitmap,
                                         Bitmap originalQrBitmap, DrawBean drawBean) {

        Bitmap qrBitmap;
        if (qrbgBitmap != null) {
            qrBitmap = drawQRcode(qrbgBitmap, originalQrBitmap);//花式二维码
        } else {
            qrBitmap = originalQrBitmap;
        }

        int size = drawBean.getSize();
        int x = drawBean.getX();
        int y = drawBean.getY();

        Bitmap bitmap = Bitmap.createBitmap(bgBitmap.getWidth(), bgBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        //绘制背景
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        canvas.drawBitmap(bgBitmap, null, rect, null);

        //绘制二维码
        Rect mRectDst1 = new Rect(x, y, x + size, y + size);//绘制头像的位置
        canvas.drawBitmap(qrBitmap, null, mRectDst1, null);
        return bitmap;
    }


    /**
     * @param qrbgBitmap 二维码背景图片
     * @param originalQrBitmap 原始二维码图片
     * @return 花式二维码
     */
    private static Bitmap drawQRcode(Bitmap qrbgBitmap, Bitmap originalQrBitmap) {
        Bitmap bitmap = Bitmap.createBitmap(originalQrBitmap.getWidth(), originalQrBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        canvas.drawBitmap(qrbgBitmap, null, rect, null);
        canvas.drawBitmap(originalQrBitmap, null, rect, null);
        return bitmap;

    }

    /**
     * 生成二维码
     * @param content 二维码的内容
     * @param heightPix 二维码的高
     * @param logo 二维码中间的logo
     * @param codeColor 二维码的颜色
     * @return
     */
//    public static Bitmap createQRCode(String content, int heightPix, Bitmap logo,
//                                      int codeColor) throws WriterException {
//        //Generate the barcode.
//        HmsBuildBitmapOption.Creator creator = new HmsBuildBitmapOption.Creator()
//                .setBitmapMargin(1)
//                .setBitmapColor(codeColor)
//                .setQRLogoBitmap(logo)
//                .setBitmapBackgroundColor(Color.WHITE);
//        creator.setBitmapColor(codeColor);
//        HmsBuildBitmapOption options = creator.create();
//        return ScanUtil.buildBitmap(content, HmsScan.QRCODE_SCAN_TYPE,
//                heightPix, heightPix, options);
//    }

    /**
     * 创建指定宽高纯色的Bitmap
     * @param width
     * @param height
     * @param color
     * @return
     */
    public static Bitmap createBitmap(int width, int height, int color) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);//填充颜色
        return bitmap;

    }

    /**
     * 创建用户头像Bitmap
//     * @param bitmap
     * @return
     */
    public static Bitmap createLogoBitmap(int bgColor, String text) {
        Context context = MainApplication.getInstance();
        int width = context.getResources().getDimensionPixelSize(R.dimen.widget_size_50);
        int height = width;
        Bitmap bitmap = createBitmap(width, height, Color.WHITE);

        Canvas canvas = new Canvas(bitmap);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = context.getResources().getDimension(R.dimen.widget_size_7);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(bgColor);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        final Paint textPaint = new Paint();
        textPaint.setTextSize(context.getResources().getDimension(R.dimen.widget_size_14));
        textPaint.setColor(Color.WHITE);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // 计算baseline
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float distance= (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom;
        float baseline = rectF.centerY() + distance;
        canvas.drawText(text, rectF.centerX(), baseline, textPaint);

        // 画两条线标记位置
//        textPaint.setStrokeWidth(4);
//        textPaint.setColor(Color.RED);
//        canvas.drawLine(0, y, 2000, y, textPaint);
//        paint.setColor(Color.BLUE);
//        canvas.drawLine(x, 0, x, 2000, textPaint);

        canvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));
        return bitmap;
    }

    // Drawable----> Bitmap
    public static Bitmap drawableToBitmap(Drawable drawable) {

        // 获取 drawable 长宽
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        drawable.setBounds(0, 0, width, height);

        // 获取drawable的颜色格式
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;
        // 创建bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        // 创建bitmap画布
        Canvas canvas = new Canvas(bitmap);
        // 将drawable 内容画到画布中
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 回收ImageView内存
     * @param view
     */
    public static void recycleImageView(View view) {
        if (view == null) return;
        if (view instanceof ImageView) {
            Drawable drawable = ((ImageView) view).getDrawable();
            if (drawable instanceof BitmapDrawable) {
                Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
                if (bmp != null && !bmp.isRecycled()) {
                    ((ImageView) view).setImageBitmap(null);
                    bmp.recycle();
                    bmp = null;
                }
            }
        }
    }
}
