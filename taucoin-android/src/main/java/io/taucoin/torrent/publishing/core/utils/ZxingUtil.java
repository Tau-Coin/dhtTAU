///**
// * Copyright 2018 Taucoin Core Developers.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *    http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package io.taucoin.torrent.publishing.core.utils;
//
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.text.TextUtils;
//
//import com.google.zxing.BarcodeFormat;
//import com.google.zxing.BinaryBitmap;
//import com.google.zxing.ChecksumException;
//import com.google.zxing.DecodeHintType;
//import com.google.zxing.EncodeHintType;
//import com.google.zxing.FormatException;
//import com.google.zxing.NotFoundException;
//import com.google.zxing.RGBLuminanceSource;
//import com.google.zxing.Result;
//import com.google.zxing.WriterException;
//import com.google.zxing.common.BitMatrix;
//import com.google.zxing.common.HybridBinarizer;
//import com.google.zxing.qrcode.QRCodeReader;
//import com.google.zxing.qrcode.QRCodeWriter;
//import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
//
//import java.util.HashMap;
//import java.util.Hashtable;
//import java.util.Map;
//
///**
// * Description: ZXing tools
// * Author:yang
// * Date: 2019/01/02
// */
//public class ZxingUtil {
//    /**
//     * 生成二维码
//     *
//     * @param contents 二维码内容
//     * @return 二维码的描述对象 BitMatrix
//     * @throws WriterException 编码时出错
//     */
//    public static BitMatrix encode(String contents) throws WriterException {
//        final Map<EncodeHintType, Object> hints = new HashMap<>();
//        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
//        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
//        return new QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, 480, 480, hints);
//    }
//
//    /**
//     * BitMatrix 转 Bitmap
//     * @param bitMatrix
//     * @return
//     */
//    public static Bitmap bitMatrixToBitmap(BitMatrix bitMatrix) {
//        final int width = bitMatrix.getWidth();
//        final int height = bitMatrix.getHeight();
//
//        final int[] pixels = new int[width * height];
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                pixels[y * width + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
//            }
//        }
//        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
//
//        return bitmap;
//    }
//
//    /**
//     * 从图片中的二维码中解析内容
//     * @param imgPath
//     */
//    public static Result scanningImage(String imgPath) {
//        if (TextUtils.isEmpty(imgPath)) {
//            return null;
//        }
//        // DecodeHintType 和 EncodeHintType
//        Hashtable<DecodeHintType, String> hints = new Hashtable<>();
//        hints.put(DecodeHintType.CHARACTER_SET, "utf-8"); // 设置二维码内容的编码
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true; // 先获取原大小
//        BitmapFactory.decodeFile(imgPath, options);
//        options.inJustDecodeBounds = false; // 获取新的大小
//
//        int sampleSize = (int) (options.outHeight / (float) 200);
//
//        if (sampleSize <= 0){
//            sampleSize = 1;
//        }
//        options.inSampleSize = sampleSize;
//        Bitmap scanBitmap = BitmapFactory.decodeFile(imgPath, options);
//
//        int[] px = new int[scanBitmap.getWidth() * scanBitmap.getHeight()];
//        scanBitmap.getPixels(px, 0, scanBitmap.getWidth(), 0, 0,
//                scanBitmap.getWidth(), scanBitmap.getHeight());
//        RGBLuminanceSource source = new RGBLuminanceSource(
//                scanBitmap.getWidth(), scanBitmap.getHeight(), px);
//
//        BinaryBitmap tempBitmap = new BinaryBitmap(new HybridBinarizer(source));
//        QRCodeReader reader = new QRCodeReader();
//        try {
//            return reader.decode(tempBitmap, hints);
//        } catch (NotFoundException | FormatException | ChecksumException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//}