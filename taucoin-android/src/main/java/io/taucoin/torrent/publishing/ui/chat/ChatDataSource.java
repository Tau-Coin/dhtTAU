package io.taucoin.torrent.publishing.ui.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.PositionalDataSource;
import io.reactivex.disposables.Disposable;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.util.CryptoUtil;

class ChatDataSource extends PositionalDataSource<ChatMsgAndUser> {
    private static final Logger logger = LoggerFactory.getLogger("ChatDataSource");
    private ChatRepository chatRepo;
    private String friendPk;
    private byte[] friendCryptoKey;
    private Disposable disposable;
    private int initialDataNum; // 初次加载数据条数
    private boolean isFirstLoadRange = true; // 是否是第一次加载
    private boolean isEndLoadRange = false;  // 是否是结束加载
    private int loadRangePos = 0;            // 加载的位置
    private int maxMum = 2147483647;            // 加载的位置

    ChatDataSource(@NonNull ChatRepository chatRepo, @NonNull String friendPk,
                   byte[] friendCryptoKey) {
        this.chatRepo = chatRepo;
        this.friendPk = friendPk;
        this.friendCryptoKey = friendCryptoKey;
        disposable = chatRepo.observeDataSetChanged()
                .subscribe(result -> {
                    // 跟当前用户有关系的才触发刷新
                    if (result != null && StringUtil.isNotEmpty(result.getMsg())
                            && result.getMsg().contains(friendPk)) {
                        // 立即执行刷新
                        if (result.isRefresh()) {
                            invalidate();
                        } else {
                            // 结束数据加载
                            isEndLoadRange = true;
                        }
                    }
                });
    }

    void onCleared() {
        if(disposable != null && !disposable.isDisposed()){
            disposable.dispose();
        }
    }

    @Override
    public void invalidate() {
        if(disposable != null && !disposable.isDisposed()){
            disposable.dispose();
        }
        super.invalidate();
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams params,
                            @NonNull LoadInitialCallback<ChatMsgAndUser> callback) {
        if(StringUtil.isEmpty(friendPk)) {
            return;
        }
        long startTime = System.currentTimeMillis();
        int pos = 0;
        int loadSize = params.requestedLoadSize;
        initialDataNum = loadSize;
        List<ChatMsgAndUser> messages = chatRepo.getMessages(friendPk, pos, loadSize);
        long getMessagesTime = System.currentTimeMillis();
        logger.trace("loadInitial getMessagesTime::{}", getMessagesTime - startTime);
        for (ChatMsgAndUser msg : messages) {
            byte[] encryptedContent = msg.content;
            try {
                msg.rawContent = CryptoUtil.decrypt(encryptedContent, friendCryptoKey);
                msg.content = null;
            } catch (Exception e) {
                logger.error("loadInitial decrypt error::", e);
            }
        }
        long decryptTime = System.currentTimeMillis();
        logger.trace("loadInitial decryptTime Time::{}", decryptTime - getMessagesTime);
        Collections.reverse(messages);
        long endTime = System.currentTimeMillis();
        logger.trace("loadInitial reverseTime Time::{}", endTime - decryptTime);

        logger.debug("loadInitial friendPk::{}, pos::{}, loadSize::{}, resultSize::{}, queryTime::{}",
                friendPk, pos, loadSize, messages.size(), endTime - startTime);
        if (messages.isEmpty()) {
            callback.onResult(messages, 0);
        } else {
            if (messages.size() >= loadSize) {
                callback.onResult(messages, maxMum - loadSize);
            } else {
                callback.onResult(messages, 0);
            }
        }
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params,
                          @NonNull LoadRangeCallback<ChatMsgAndUser> callback) {
        if(isEndLoadRange || StringUtil.isEmpty(friendPk)) {
            return;
        }
        long startTime = System.currentTimeMillis();
        List<ChatMsgAndUser> messages;
        int pos = params.startPosition;
        int loadSize = params.loadSize;
        // 第一次不处理，同时计算位置
        if (isFirstLoadRange || pos >= maxMum) {
            isFirstLoadRange = false;
            loadRangePos += initialDataNum;
            return;
        }
        pos = loadRangePos;
        if (pos > 0) {
            // 开始位置小于数据总数
            messages = chatRepo.getMessages(friendPk, pos, loadSize);
        } else {
            // 否则数据为空
            messages = new ArrayList<>(0);
        }
        for (ChatMsgAndUser msg : messages) {
            byte[] encryptedContent = msg.content;
            try {
                msg.rawContent = CryptoUtil.decrypt(encryptedContent, friendCryptoKey);
                msg.content = null;
            } catch (Exception e) {
                logger.error("loadInitial decrypt error::", e);
            }
        }
        Collections.reverse(messages);
        long endTime = System.currentTimeMillis();
        logger.debug("loadRange friendPk::{},  pos::{}, loadSize::{}, resultSize::{}, queryTime::{}",
                friendPk, pos, loadSize, messages.size(), endTime - startTime);
        loadRangePos += loadSize;
        if (messages.size() < loadSize) {
            isEndLoadRange = true;
        }
        callback.onResult(messages);
    }
}