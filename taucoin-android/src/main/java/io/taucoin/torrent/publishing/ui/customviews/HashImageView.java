package io.taucoin.torrent.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.MsgBlock;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.types.HashList;
import io.taucoin.util.ByteUtil;

/**
 * 根据图片信息的Hash，递归获取全部信息并显示
 */
@SuppressLint("AppCompatCustomView")
public class HashImageView extends RoundImageView {

    private static final Logger logger = LoggerFactory.getLogger("HashImageView");
    private TauDaemon daemon;
    private String imageHash;
    private byte[] totalBytes;
    private Disposable disposable;
    private boolean reload = false;
    private BitmapFactory.Options options;
    private int heightLimit = 300;
    private int widthLimit = 300;

    public HashImageView(Context context) {
        this(context, null);
    }

    public HashImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HashImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        daemon = TauDaemon.getInstance(context);
        options = new BitmapFactory.Options();
    }

    private void showImage() {
        setImageBitmap(loadImageView());
    }

    /**
     * 设置ImageHash
     */
    public void setImageHash(String imageHash) {
        this.imageHash = imageHash;
        setImageHash(ByteUtil.toByte(imageHash));
    }

    private void setImageHash(byte[] imageHash) {
        logger.debug("setImageHash start::{}", imageHash);
        totalBytes = null;
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        disposable = Flowable.create((FlowableOnSubscribe<Bitmap>) emitter -> {
            try {
                if (imageHash != null) {
                    logger.debug("showHorizontalData start");
                    long startTime = System.currentTimeMillis();
                    showHorizontalData(imageHash, emitter);
                    long endTime = System.currentTimeMillis();
                    logger.debug("showImageFromDB times::{}ms", endTime - startTime);
                }
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                logger.error("showImageFromDB error", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setImageBitmap);
    }

    private void showHorizontalData(byte[] imageHash, FlowableEmitter<Bitmap> emitter) throws Exception {
        byte[] msgBlockEncoded = queryDataLoop(imageHash);
        MsgBlock block = new MsgBlock(msgBlockEncoded);
        if (block.isHaveHorizontalHash()) {
            byte[] hashListEncoded = queryDataLoop(block.getHorizontalHash());
            HashList hashList = new HashList(hashListEncoded);
            List<byte[]> msgList = hashList.getHashList();
            if (msgList != null && msgList.size() > 0) {
                for (byte[] msgHash : msgList) {
                    byte[] msgContent = queryDataLoop(msgHash);
                    if (!emitter.isCancelled()) {
                        refreshImageView(msgContent, emitter);
                    }
                }
            }
        }
        if (block.isHaveVerticalHash() && !emitter.isCancelled()) {
            showHorizontalData(block.getVerticalHash(), emitter);
        }
    }

    /**
     * 循环查询数据
     * 如果查询不到或或者异常，1s后重试
     * @param hash
     * @return
     * @throws InterruptedException
     */
    private byte[] queryDataLoop(byte[] hash) throws InterruptedException {
        while (true) {
            try {
                byte[] data = daemon.getMsg(hash);
                if (null == data) {
                    daemon.requestMessageData(hash);
                } else {
                    return data;
                }
            } catch (Exception e) {
                logger.debug("queryDataLoop error::{}", hash);
            }
            // 如果获取不到，1秒后重试
            Thread.sleep(1000);
        }
    }

    private void refreshImageView(byte[] msgContent, FlowableEmitter<Bitmap> emitter) {
        if (totalBytes == null) {
            totalBytes = new byte[msgContent.length];
            System.arraycopy(msgContent, 0, totalBytes, 0, totalBytes.length);
        } else {
            byte[] tempBytes = new byte[totalBytes.length + msgContent.length];
            System.arraycopy(totalBytes, 0, tempBytes, 0, totalBytes.length);
            System.arraycopy(msgContent, 0, tempBytes, totalBytes.length, msgContent.length);
            totalBytes = tempBytes;
        }
        Bitmap bitmap = loadImageView();
        logger.debug("RefreshScanImages, bitmap::{}, size::{}", bitmap,
                Formatter.formatFileSize(getContext(), totalBytes.length));
        if (bitmap != null) {
            emitter.onNext(bitmap);
        }
    }

    private Bitmap loadImageView() {
        byte lastTwo = totalBytes[totalBytes.length - 2];
        byte lastOne = totalBytes[totalBytes.length - 1];
        totalBytes[totalBytes.length - 2] = -1;
        totalBytes[totalBytes.length - 1] = -39;
        options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(totalBytes, 0, totalBytes.length, options);
        int heightRatio = options.outHeight / heightLimit;
        int widthRatio = options.outWidth / widthLimit;
        if (heightRatio > 0 || widthRatio > 1) {
            options.inSampleSize = Math.max(heightRatio, widthRatio);
        }
        logger.debug("loadImageView::{}, {}, {}", options.outHeight, widthLimit, options.inSampleSize);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(totalBytes, 0, totalBytes.length, options);
        totalBytes[totalBytes.length - 2] = lastTwo;
        totalBytes[totalBytes.length - 1] = lastOne;
        return bitmap;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 加在View
        if (reload && StringUtil.isNotEmpty(imageHash)) {
            setImageHash(imageHash);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 销毁View
        reload = true;
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}