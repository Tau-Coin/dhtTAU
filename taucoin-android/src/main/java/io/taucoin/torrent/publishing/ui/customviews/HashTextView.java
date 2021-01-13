package io.taucoin.torrent.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.core.model.Frequency;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.utils.MsgSplitUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.types.Message;
import io.taucoin.util.ByteUtil;

/**
 * 根据文本信息的Hash，递归获取全部信息并显示
 */
@SuppressLint("AppCompatCustomView")
public class HashTextView extends TextView {

    private static final Logger logger = LoggerFactory.getLogger("HashTextView");
    private TauDaemon daemon;
    private String textHash;
    private String text;
    private Disposable disposable;

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
        if (StringUtil.isNotEmpty(this.text)) {
            setText(this.text);
        } else {
            setText("");
        }
    }


    public void setTextAndHash(String text, String textHash) {
        this.text = text;
        this.textHash = textHash;
        if (StringUtil.isNotEmpty(text)) {
            logger.debug("setText directly::{}", this.text);
            setText(text);
        } else {
            setTextHash(textHash);
        }
    }

    /**
     * 设置TextHash
     */
    private void setTextHash(String textHash) {
        logger.debug("setTextHash::{}", textHash);
        if (StringUtil.isEmpty(textHash)) {
            return;
        }
        if (StringUtil.isNotEmpty(this.text)
                && StringUtil.isEquals(this.textHash, textHash)) {
            showText();
            return;
        }
        this.textHash = textHash;
        logger.debug("setTextHash start::{}", this.textHash);
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            try {
                if (StringUtil.isNotEmpty(this.textHash)) {
                    long startTime = System.currentTimeMillis();
                    byte[] msgEncoded = queryDataLoop(ByteUtil.toByte(textHash));
                    if (msgEncoded != null) {
                        Message message = new Message(msgEncoded);
                        this.text = MsgSplitUtil.textMsgToString(message.getContent());
                    }
                    long endTime = System.currentTimeMillis();
                    logger.debug("setTextHash textHash::{}, text::{}, times::{}ms", textHash,
                            text, endTime - startTime);
                }
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                logger.error("showTextFromDB error", e);
            }
            emitter.onNext(StringUtil.isNotEmpty(this.text));
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
            Thread.sleep(Frequency.FREQUENCY_RETRY.getFrequency());
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // 销毁View
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}