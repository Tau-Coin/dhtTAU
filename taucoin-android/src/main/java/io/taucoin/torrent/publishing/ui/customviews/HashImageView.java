package io.taucoin.torrent.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.List;

import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.db.DBException;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.MsgBlock;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.types.HashList;
import io.taucoin.util.ByteUtil;

@SuppressLint("AppCompatCustomView")
public class HashImageView extends ImageView {

    private static final Logger logger = LoggerFactory.getLogger("HashImageView");
    private TauDaemon daemon;
    private String imageHash;
    private byte[] totalBytes;
    private Disposable disposable;
    private boolean reload = false;

    public HashImageView(Context context) {
        this(context, null);
    }

    public HashImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HashImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        daemon = TauDaemon.getInstance(context);
    }

    private void showImage() {
        setImageBitmap(loadImageView());
    }

    /**
     * 设置ImageHash
     */
    public void setImageHash(String imageHash) {
        if (StringUtil.isEquals(this.imageHash, imageHash) && totalBytes != null) {
            showImage();
            return;
        }
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
            } catch (Exception e) {
                logger.error("showImageFromDB error", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setImageBitmap);
    }

    private void showHorizontalData(byte[] imageHash, FlowableEmitter<Bitmap> emitter) throws DBException {
        byte[] msgBlockEncoded = daemon.getMsg(imageHash);
        if (msgBlockEncoded != null) {
            MsgBlock block = new MsgBlock(msgBlockEncoded);
            if (block.isHaveHorizontalHash()) {
                byte[] hashListEncoded = daemon.getMsg(block.getHorizontalHash());
                if (hashListEncoded != null) {
                    HashList hashList = new HashList(hashListEncoded);
                    List<byte[]> msgList = hashList.getHashList();
                    if (msgList != null && msgList.size() > 0) {
                        for (byte[] msgHash : msgList) {
                            byte[] msgContent = daemon.getMsg(msgHash);
                            if (!emitter.isCancelled()) {
                                refreshImageView(msgContent, emitter);
                            }
                        }
                    }
                }
            }
            if (block.isHaveVerticalHash() && !emitter.isCancelled()) {
                showHorizontalData(block.getVerticalHash(), emitter);
            }
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
//        bytesToBitmap(totalBytes);
//        Bitmap bitmap = bytesToBitmap(totalBytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(totalBytes, 0, totalBytes.length);
        totalBytes[totalBytes.length - 2] = lastTwo;
        totalBytes[totalBytes.length - 1] = lastOne;
        return bitmap;
    }

    @SuppressWarnings("unchecked")
    private Bitmap bytesToBitmap(byte[] totalBytes) {
        InputStream input = null;
        Bitmap bitmap = null;
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            input = new ByteArrayInputStream(totalBytes);
            SoftReference softRef = new SoftReference(BitmapFactory.decodeStream(input, null, options));
            bitmap = (Bitmap)softRef.get();
        } catch(Exception e) {
            logger.warn("bytesToBitmap warn", e);
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                logger.warn("bytesToBitmap warn", e);
            }
        }
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