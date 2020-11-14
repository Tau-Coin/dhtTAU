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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.core.content.FileProvider;
import io.taucoin.torrent.publishing.BuildConfig;
import io.taucoin.torrent.publishing.MainApplication;

public class FileUtil {

    public static final String authority = BuildConfig.APPLICATION_ID + ".fileprovider";

    static Bitmap getExternalBitmap(String fileName){
        boolean isSdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        if (isSdCardExist) {
            String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            String filepath = sdPath + File.separator + BuildConfig.APPLICATION_ID + File.separator + "temp" + File.separator + fileName;
            File file = new File(filepath);
            if (file.exists()) {
                return BitmapFactory.decodeFile(filepath);
            }
        }
        return null;
    }

    public static void deleteExternalBitmap(){
        boolean isSdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
        if (isSdCardExist) {
            String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            String filepath = sdPath + File.separator + BuildConfig.APPLICATION_ID + File.separator + "temp";
            File file = new File(filepath);
            deleteFile(file);
        }
    }

    /**
     * Delete File
     * @param file target file
     */
    public static void deleteFile(File file) {
        if (file == null || !file.exists())
            return;

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if(files != null){
                for (File f : files) {
                    deleteFile(f);
                }
            }
//            file.delete();
        } else if (file.exists()) {
            file.delete();
        }
    }

    public static void deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            deleteFile(file);
        }catch (Exception ignore){

        }

    }

    static Bitmap getFilesDirBitmap(String filename){
        Bitmap bitmap = null;
        try {
            Context context = MainApplication.getInstance();
            FileInputStream fis = context.openFileInput(filename);
            bitmap = BitmapFactory.decodeStream(fis);
            fis.close();
        }  catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public static void saveFilesDirBitmap(String filename, Bitmap bitmap) throws Exception{
        FileOutputStream fos = null;
        try {
            File file = new File(filename);
            if (!file.exists()) {
                file.createNewFile();
            }
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos);

        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get QRCode directory
     * @return  path
     */
    public static String getQRCodeFilePath() {
        Context context = MainApplication.getInstance();
        File file = context.getExternalFilesDir(null);
        String path;
        if(file != null && file.exists()){
            path = file.getAbsolutePath();
        }else{
            path = Environment.getExternalStorageDirectory() + File.separator + BuildConfig.APPLICATION_ID;
        }
        path = path + File.separator + "qr";
        createDir(path);

        return path + File.separator;
    }

    /**
     * Get download directory
     * @return  path
     */
    public static String getDownloadFilePath() {
        Context context = MainApplication.getInstance();
        File file = context.getExternalFilesDir(null);
        String path;
        if(file != null && file.exists()){
            path = file.getAbsolutePath();
        }else{
            path = Environment.getExternalStorageDirectory() + File.separator + BuildConfig.APPLICATION_ID;
        }
        path = path + File.separator + "download";
        createDir(path);

        return path + File.separator;
    }

    /**
     * Create Directory
     * @param file target file
     * @return  Directory
     */
    private static File createDir(File file){
        if (file != null && (file.exists() || file.mkdirs())){
            return file;
        }
        return null;
    }

    /**
     * Create Directory
     * @param path target file path
     * @return  Directory
     */
    private static File createDir(String path){
        if (StringUtil.isEmpty(path)){
            return null;
        }
        return createDir(new File(path));
    }

    public static Uri getUriForFile(String path) {
        return getUriForFile(new File(path));
    }

    public static Uri getUriForFile(File file) {
        Context context = MainApplication.getInstance();
        if (context == null || file == null) {
            throw new NullPointerException();
        }
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = BuildConfig.APPLICATION_ID + ".provider";
            uri = FileProvider.getUriForFile(context.getApplicationContext(), authority, file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    public static List<File> getFiles(String fileAbsolutePath) {
        List<File> list = new ArrayList<>();
        File fileDirectory = new File(fileAbsolutePath);
        if(!fileDirectory.exists()){
            fileDirectory.mkdirs();
        }
        File[] subFiles = fileDirectory.listFiles();
        if(subFiles != null){
            for (File file : subFiles) {
                // 判断是否为文件夹
                if (!file.isDirectory()) {
                    list.add(file);
                }
            }
        }
        return list;
    }
}