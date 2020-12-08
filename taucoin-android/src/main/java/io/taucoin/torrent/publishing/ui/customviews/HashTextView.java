package io.taucoin.torrent.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.db.DBException;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.MsgBlock;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.util.ByteUtil;

/**
 * 根据文本信息的Hash，递归获取全部信息并显示
 */
@SuppressLint("AppCompatCustomView")
public class HashTextView extends TextView {

    private static final Logger logger = LoggerFactory.getLogger("HashTextView");
    private TauDaemon daemon;
    private String textHash;
    private Disposable disposable;
    private byte[] totalBytes = null;
    private boolean reload = false;

    public HashTextView(Context context) {
        this(context, null);
    }

    public HashTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HashTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        daemon = TauDaemon.getInstance(context);
    }

    /**
     * 显示Text
     */
    private void showText() {
        setText(new String(totalBytes, StandardCharsets.UTF_8));
    }

    /**
     * 设置TextHash
     */
    public void setTextHash(String textHash) {
        this.textHash = textHash;
        setTextHash(ByteUtil.toByte(textHash));
    }

    /**
     * 设置TextHash
     */
    private void setTextHash(byte[] textHash) {
        totalBytes = null;
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            try {
                if (textHash != null) {
                    long startTime = System.currentTimeMillis();
                    showHorizontalData(textHash);
                    long endTime = System.currentTimeMillis();
                    logger.debug("showTextFromDB textHash::{}, text::{}, times::{}ms", textHash,
                            new String(totalBytes, StandardCharsets.UTF_8), endTime - startTime);
                }
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                logger.error("showTextFromDB error", e);
            }
            emitter.onNext(totalBytes != null);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result) {
                        showText();
                    }
                });
    }

    /**
     * 递归显示HorizontalData数据
     * @param verticalHash
     * @throws DBException
     */
    private void showHorizontalData(byte[] verticalHash) throws Exception {
        if (null == verticalHash) {
            return;
        }
        byte[] msgRoot = queryDataLoop(verticalHash);
        MsgBlock msgBlock = new MsgBlock(msgRoot);
        logger.debug("HorizontalData::{}, VerticalHash::{}",
                msgBlock.getHorizontalHash(), msgBlock.getVerticalHash());
        if (msgBlock.isHaveHorizontalHash()) {
            byte[] fragment = queryDataLoop(msgBlock.getHorizontalHash());
            if (totalBytes == null) {
                totalBytes = new byte[fragment.length];
                System.arraycopy(fragment, 0, totalBytes, 0, totalBytes.length);
            } else {
                byte[] tempBytes = new byte[totalBytes.length + fragment.length];
                System.arraycopy(totalBytes, 0, tempBytes, 0, totalBytes.length);
                System.arraycopy(fragment, 0, tempBytes, totalBytes.length, fragment.length);
                totalBytes = tempBytes;
            }
        }
        if (msgBlock.isHaveVerticalHash()) {
            showHorizontalData(msgBlock.getVerticalHash());
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
                logger.debug("queryDataLoop hash::{}, empty::{}",
                        ByteUtil.toHexString(hash), null == data);
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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 加在View
        if (reload && StringUtil.isNotEmpty(textHash)) {
            setTextHash(textHash);
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