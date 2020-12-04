/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.torrent.publishing.core.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * Multimedia processing related logic processing:
 * compression, clipping, image extraction, audio extraction
 *
 * */
public class MultimediaUtil {
    private static final Logger logger = LoggerFactory.getLogger("MultimediaUtil");
    private static final int MAX_IMAGE_SIZE = 100 * 1024;        // byte
    private static final int MAX_IMAGE_WIDTH = 480;              // px
    private static final int MAX_IMAGE_HEIGHT = 800;             // px

    private static int calculateInSampleSize(BitmapFactory.Options options) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > MAX_IMAGE_HEIGHT || width > MAX_IMAGE_WIDTH) {
            final int heightRatio = Math.round((float) height/ (float) MAX_IMAGE_HEIGHT);
            final int widthRatio = Math.round((float) width / (float) MAX_IMAGE_WIDTH);
            inSampleSize = Math.min(heightRatio, widthRatio);
        }
        return inSampleSize;
    }

    private static Bitmap imageScale(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float compressRatio = 1;
        if (h > MAX_IMAGE_HEIGHT || w > MAX_IMAGE_WIDTH) {
            if(w >= h){
                compressRatio = (float) MAX_IMAGE_WIDTH / w;
            }else{
                compressRatio = (float) MAX_IMAGE_HEIGHT / h;
            }
        }
        Matrix matrix = new Matrix();
        matrix.postScale(compressRatio, compressRatio);
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
    }

    public static void compressImage(String originPath, String compressPath)
            throws IOException {
        Bitmap tagBitmap = getSmallBitmap(originPath);
        tagBitmap = imageScale(tagBitmap);
        int quality = 100;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tagBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        // 循环判断如果压缩后图片是否大于100kb,大于继续压缩
        while (baos.toByteArray().length > MAX_IMAGE_SIZE) {
            baos.reset();
            quality -= 5;
            logger.debug("compressImage::bytesCount::{}, quality::{}",
                    baos.toByteArray().length, quality);
            tagBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        }
        tagBitmap.recycle();

        FileOutputStream fos = new FileOutputStream(compressPath);
        fos.write(baos.toByteArray());
        fos.flush();
        fos.close();
        baos.close();
    }

    private static Bitmap getSmallBitmap(String filePath) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(filePath, options);
    }
}
