package io.taucoin.torrent.publishing.service;

import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;

import static android.os.Build.CPU_ABI;
import static java.lang.Runtime.getRuntime;

public class LibJpegManager {
    private static final Logger logger = LoggerFactory.getLogger("libjpeg");
    private static volatile Process process = null;
    private static String binFile = "jpegtran";
    private static String storeFile = "libjpeg";
    private static String scriptFile = "script.txt";
    private static String compressFile = "img_compress.jpg";
    private static String progressiveFile = "img_progressive.jpg";

    static void init() {
        logger.debug("init");
        install();
    }

    public static File getBinFile() {
        Context context = MainApplication.getInstance();
        File file = context.getFilesDir();
        return new File(file.getAbsolutePath() + File.separator + binFile);
    }

    public static File getScriptFile() {
        Context context = MainApplication.getInstance();
        File file = context.getFilesDir();
        return new File(file.getAbsolutePath()  + File.separator + scriptFile);
    }

    public static File getStoreFile() {
        Context context = MainApplication.getInstance();
        File file = context.getExternalFilesDir(null);
        return new File(file.getAbsolutePath()  + File.separator + storeFile);
    }

    public static String getCompressFilePath() {
        return getStoreFile().getAbsolutePath() + File.separator + compressFile;
    }

    public static String getProgressiveFilePath() {
        return getStoreFile().getAbsolutePath() + File.separator + progressiveFile;
    }

    private static void install() {
        Flowable.create(emitter -> {
            try {
                logger.debug("install start");
                Context context = MainApplication.getInstance();
                String type = CPU_ABI;
                if (type.startsWith("arm")) {
                    type = "jpegtran-static-arm";
                } else if (type.startsWith("x86")) {
                    type = "jpegtran-static-x86";
                } else {
                    logger.error("Unsupported ABI::{}", type);
                }
                File bin = getBinFile();
                if (bin.exists()) {
                    bin.delete();
                }
                bin.createNewFile();

                File script = getScriptFile();
                if (script.exists()) {
                    script.delete();
                }
                script.createNewFile();


                File store = getStoreFile();
                if (!store.exists()) {
                    store.mkdirs();
                }
                copyFileTo(context.getAssets().open(type), bin);
                copyFileTo(context.getAssets().open(scriptFile), script);

                bin.setExecutable(true);
                logger.info("Installed binary");
                jpegHelp();
            } catch (Exception e) {
                logger.error("libjpeg init error", e);
            } finally {
                emitter.onComplete();
            }
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    public static void jpegScans(String originalImage, String newImage) throws Exception {
        String scriptFile = getScriptFile().getAbsolutePath();
        String cmd = "-outfile %s -verbose -scans %s %s";
        cmd = String.format(cmd, newImage, scriptFile, originalImage);
        execCommands(cmd);
    }

    private static void jpegHelp() {
        Flowable.create(emitter -> {
            execCommands("-help");
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    private static void execCommands(String cmd) throws Exception {
        try {
            long startTime = System.currentTimeMillis();
            String binFile = getBinFile().getAbsolutePath();
            String commands = String.format("%s %s", binFile, cmd);
            process = getRuntime().exec(commands);
            consumerStream(process);
            process.waitFor();
            long endTime = System.currentTimeMillis();
            logger.debug("commands end times::{}ms, commands::{}", (endTime - startTime), commands);
        } catch (Exception e) {
            throw new Exception("execCommands error:" + e.getMessage());
        }
    }

    private static void consumerStream(Process process) {
        // 获取标准输出
        BufferedReader readStdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        // 获取错误输出
        BufferedReader readStderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        try {
            String lineOut = null;
            String lineErr = null;
            // 逐行读取
            while((lineOut = readStdout.readLine()) != null
                    || (lineErr = readStderr.readLine()) != null){
                if(lineOut != null){
                    logger.debug(lineOut);
                }
                if(lineErr != null){
                    logger.debug(lineErr);
                }
            }
        } catch (IOException e) {
            logger.error("consumerStream error", e);
        }
    }

    private void stop() {
        logger.info("stop libjpeg process");
        if (process != null) {
            process.destroy();
            process = null;
        }
        // TODO: 强制杀死进程
//        shutdown();
    }

    private static void copyFileTo(InputStream input, File file) {
        OutputStream output = null;
        try {
            byte[] bytes = new byte[1024];
            output = new FileOutputStream(file);
            int num;
            while ((num = input.read(bytes)) != -1) {
                output.write(bytes, 0, num);
            }
            input.close();
            output.close();
        } catch (IOException e) {
            logger.error("copyFileTo error", e);
        }
    }
}