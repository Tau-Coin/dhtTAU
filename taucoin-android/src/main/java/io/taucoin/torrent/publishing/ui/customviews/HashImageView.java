package io.taucoin.torrent.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.Frequency;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.types.Message;
import io.taucoin.util.ByteUtil;

/**
 * 根据图片信息的Hash，递归获取全部信息并显示
 */
@SuppressLint("AppCompatCustomView")
public class HashImageView extends RoundImageView {

    private static final Logger logger = LoggerFactory.getLogger("HashImageView");
    private static final int heightLimit = 300;
    private static final int widthLimit = 300;
    private static final int loadBitmapLimit = 40;
    private ChatRepository chatRepo;
    private TauDaemon daemon;
    private String imageHash;
    private boolean unsent;
    private byte[] totalBytes;
    private Disposable disposable;
    private boolean reload = false;
    private BitmapFactory.Options options;
    private int loadBitmapNum = 0;
    private boolean isLoadSuccess;

    public HashImageView(Context context) {
        this(context, null);
    }

    public HashImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HashImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        daemon = TauDaemon.getInstance(context);
        chatRepo = RepositoryHelper.getChatRepository(context);
        options = new BitmapFactory.Options();
    }

    private void showImage(Bitmap bitmap) {
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
            loadBitmapNum += 1;
            if (loadBitmapNum >= loadBitmapLimit) {
                isLoadSuccess = true;
                disposable.dispose();
            }
            logger.trace("showImage imageHash::{}, loadBitmapNum::{}", imageHash, loadBitmapNum);
        } else {
            setImageResource(R.mipmap.icon_image_loading);
        }
    }

    /**
     * 设置ImageHash
     * @param imageHash
     * @param unsent
     */
    public void setImageHash(boolean unsent, String imageHash) {
        // 如果是图片已加载，并且显示的图片不变，直接返回
        if (isLoadSuccess && totalBytes != null
                && StringUtil.isEquals(imageHash, this.imageHash)) {
            return;
        }
        this.imageHash = imageHash;
        this.unsent = unsent;
        setImageHash(ByteUtil.toByte(imageHash));
    }

    /**
     * 设置ImageHash
     * @param imageHash
     */
    private void setImageHash(byte[] imageHash) {
        logger.debug("setImageHash start::{}", this.imageHash);
        showImage(null);
        isLoadSuccess = false;
        loadBitmapNum = 0;
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        totalBytes = null;
        disposable = Flowable.create((FlowableOnSubscribe<Bitmap>) emitter -> {
            try {
                if (imageHash != null) {
                    logger.debug("showHorizontalData start");
                    long startTime = System.currentTimeMillis();
                    showFragmentData(this.unsent, imageHash, emitter);
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
                .subscribe(this::showImage);
    }

    /**
     * 递归显示图片切分的片段数据
     * @param imageHash
     * @param emitter
     * @throws Exception
     */
    private void showFragmentData(boolean unsent, byte[] imageHash,
                              FlowableEmitter<Bitmap> emitter) throws Exception {
        if (emitter.isCancelled()) {
            return;
        }
        BigInteger nonce = BigInteger.ZERO;
        byte[] content = null;
        byte[] previousMsgHash = null;
        if (unsent) {
            String hash = ByteUtil.toHexString(imageHash);
            ChatMsg msg = chatRepo.queryChatMsg(hash);
            if (msg.unsent == 0) {
                nonce = BigInteger.valueOf(msg.nonce);
                if (StringUtil.isNotEmpty(msg.content)) {
                    content = ByteUtil.toByte(msg.content);
                }
                if (StringUtil.isNotEmpty(msg.previousMsgHash)) {
                    previousMsgHash = ByteUtil.toByte(msg.previousMsgHash);
                }
            } else {
                showFragmentData(false, imageHash, emitter);
            }
        } else {
            byte[] fragmentEncoded = queryDataLoop(imageHash);
            Message msg = new Message(fragmentEncoded);
            nonce = msg.getNonce();
            content = msg.getContent();
            previousMsgHash = msg.getPreviousMsgDAGRoot();
        }
        if (nonce.compareTo(BigInteger.ZERO) == 0 &&
                StringUtil.isNotEquals(ByteUtil.toHexString(imageHash), this.imageHash)) {
            return;
        }
        if (!emitter.isCancelled()) {
            refreshImageView(content, emitter);
        }
        if (previousMsgHash != null) {
            showFragmentData(unsent, previousMsgHash, emitter);
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
            Thread.sleep(Frequency.FREQUENCY_RETRY.getFrequency());
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
        if (bitmap != null && !emitter.isCancelled()) {
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
        if (reload && StringUtil.isNotEmpty(imageHash)
                && disposable != null && disposable.isDisposed()) {
            setImageHash(unsent, imageHash);
        }
        reload = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 销毁View
        if (disposable != null && !disposable.isDisposed()) {
            reload = true;
            disposable.dispose();
        }
    }
}