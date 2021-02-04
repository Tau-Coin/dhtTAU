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
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.core.model.Interval;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
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
    private byte[] friendPk;
    private boolean unsent;
    private Disposable disposable;
    private boolean isLoadSuccess;
    private ChatRepository chatRepo;
    private boolean reload = false;
    private StringBuilder textBuilder;

    public HashTextView(Context context) {
        this(context, null);
    }

    public HashTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HashTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        daemon = TauDaemon.getInstance(context);
        chatRepo = RepositoryHelper.getChatRepository(context);
    }

    /**
     * 显示Text
     */
    private void showText() {
        setText(textBuilder);
    }

    public void setTextHash(boolean unsent, String textHash, String friendPk) {
        // 如果是图片已加载，并且显示的图片不变，直接返回
        if (StringUtil.isEmpty(textHash)) {
            return;
        }
        if (isLoadSuccess && textBuilder.length() > 0
                && StringUtil.isEquals(textHash, this.textHash)) {
            return;
        }
        this.textHash = textHash;
        this.unsent = unsent;
        this.friendPk = ByteUtil.toByte(friendPk);
        textBuilder = new StringBuilder();
        setTextHash(ByteUtil.toByte(textHash), friendPk);
    }

    /**
     * 设置TextHash
     */
    private void setTextHash(byte[] textHash, String friendPk) {
        logger.debug("setTextHash::{}", this.textHash);
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        showText();
        isLoadSuccess = false;
        logger.debug("setTextHash start::{}", this.textHash);
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            try {
                if (StringUtil.isNotEmpty(this.textHash)) {
                    long startTime = System.currentTimeMillis();
                    showFragmentData(this.unsent, textHash, friendPk, emitter);
                    long endTime = System.currentTimeMillis();
                    logger.debug("setTextHash textHash::{}, text::{}, times::{}ms", this.textHash,
                            textBuilder, endTime - startTime);
                }
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                logger.error("showTextFromDB error", e);
            }
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
     * 递归显示文本消息切分的片段数据
     * @param textHash
     * @param friendPk
     * @param emitter
     * @throws Exception
     */
    private void showFragmentData(boolean unsent, byte[] textHash, String friendPk,
                                  FlowableEmitter<Boolean> emitter) throws Exception {
        if (emitter.isCancelled()) {
            return;
        }
        String content = null;
        byte[] previousMsgHash = null;
        if (unsent) {
            String hash = ByteUtil.toHexString(textHash);
            ChatMsg msg = chatRepo.queryChatMsg(friendPk, hash);
            if (msg.unsent == 0) {
                if (StringUtil.isNotEmpty(msg.content)) {
                    content = msg.content;
                }
                if (StringUtil.isNotEmpty(msg.previousHash)) {
                    previousMsgHash = ByteUtil.toByte(msg.previousHash);
                }
            } else {
                showFragmentData(false, textHash, friendPk, emitter);
            }
        } else {
            byte[] fragmentEncoded = queryDataLoop(textHash);
            Message msg = new Message(fragmentEncoded);
            content = MsgSplitUtil.textMsgToString(msg.getContent());
            previousMsgHash = msg.getPreviousHash();
        }
        if (!emitter.isCancelled()) {
            if (StringUtil.isNotEmpty(content)) {
                textBuilder.append(content);
                emitter.onNext(true);
            }
        }
        if (previousMsgHash != null) {
            showFragmentData(unsent, previousMsgHash, friendPk, emitter);
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
                    daemon.requestMessageData(hash, friendPk);
                } else {
                    return data;
                }
            } catch (Exception e) {
                logger.debug("queryDataLoop error::{}", hash);
            }
            // 如果获取不到，1秒后重试
            Thread.sleep(Interval.INTERVAL_RETRY.getInterval());
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 加在View
        if (reload && StringUtil.isNotEmpty(textHash)
                && disposable != null && disposable.isDisposed()) {
            setTextHash(unsent, textHash, ByteUtil.toHexString(friendPk));
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