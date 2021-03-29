package io.taucoin.torrent.publishing.ui.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import androidx.annotation.Nullable;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.CryptoUtil;

/**
 * 根据文本信息的Hash，递归获取全部信息并显示
 */
@SuppressLint("AppCompatCustomView")
public class HashTextView extends TextView {

    private static final Logger logger = LoggerFactory.getLogger("HashTextView");
    private byte[] content;
    private String senderPk;
    private String receiverPk;
    private Disposable disposable;
    private boolean isLoadSuccess;
    private boolean reload = false;

    public HashTextView(Context context) {
        this(context, null);
    }

    public HashTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HashTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 显示Text
     */
    private void showText(String text) {
        setText(text);
    }

    public void setTextContent(byte[] content, String senderPk, String receiverPk) {
        setTextContent(content, null, senderPk, receiverPk);
    }

    public void setTextContent(byte[] content, byte[] rawContent, String senderPk, String receiverPk) {
        if (rawContent != null) {
            String rawContentStr = Utils.textBytesToString(rawContent);
            showText(rawContentStr);
            return;
        }
        if (isLoadSuccess && content != null
                && Arrays.equals(this.content, content)) {
            logger.trace("showTextContent isLoadSuccess::{}, isEquals::{}::", isLoadSuccess,
                    Arrays.equals(this.content, content));
            return;
        }
        this.content = content;
        this.senderPk = senderPk;
        this.receiverPk = receiverPk;
        isLoadSuccess = false;
        showText(null);
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            try {
                byte[] cryptoKey;
                long startTime = System.currentTimeMillis();
                if (StringUtil.isEquals(senderPk, MainApplication.getInstance().getPublicKey())) {
                    cryptoKey = Utils.keyExchange(receiverPk, MainApplication.getInstance().getSeed());
                } else {
                    cryptoKey = Utils.keyExchange(senderPk, MainApplication.getInstance().getSeed());
                }
                long keyExchangeTime = System.currentTimeMillis() - startTime;
                byte[] rawContentTemp = CryptoUtil.decrypt(content, cryptoKey);
                String rawContentStr = Utils.textBytesToString(rawContentTemp);
                long decryptTime = System.currentTimeMillis() - startTime;
                String rawContentLog = rawContentStr.length() > 50 ?
                        rawContentStr.substring(0, 10) : rawContentStr;
                logger.trace("showTextContent decryptTime::{}, keyExchangeTime::{}, rawContent::{}",
                        decryptTime, keyExchangeTime, rawContentLog);
                emitter.onNext(rawContentStr);
            } catch (Exception e) {
                logger.error("showTextContent error::", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    isLoadSuccess = true;
                    showText(result);
                });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 加在View
        if (reload && content != null
                && disposable != null && disposable.isDisposed()) {
            setTextContent(content, senderPk, receiverPk);
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