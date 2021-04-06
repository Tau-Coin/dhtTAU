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
    private long initialDataNum; // 初次加载数据条数

    ChatDataSource(@NonNull ChatRepository chatRepo, @NonNull String friendPk,
                   byte[] friendCryptoKey) {
        this.chatRepo = chatRepo;
        this.friendPk = friendPk;
        this.friendCryptoKey = friendCryptoKey;
        disposable = chatRepo.observeDataSetChanged()
                .subscribe(result -> {
                    // 跟当前用户有关系的才触发刷新
                    if (StringUtil.isNotEmpty(result)
                            && result.contains(friendPk)) {
                        invalidate();
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
        int numMessages = chatRepo.getNumMessages(friendPk);
        initialDataNum = numMessages;
        long getNumTime = System.currentTimeMillis();
        logger.trace("loadInitial getNumTime::{}", getNumTime - startTime);
        int pos = 0;
        int loadSize = params.requestedLoadSize;
        List<ChatMsgAndUser> messages = chatRepo.getMessages(friendPk, pos, loadSize);
        long getMessagesTime = System.currentTimeMillis();
        logger.trace("loadInitial getMessagesTime::{}", getMessagesTime - getNumTime);
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
        logger.trace("loadInitial decryptTime Time::{}", endTime - getMessagesTime);

        logger.debug("loadInitial friendPk::{}, pos::{}, loadSize::{}, resultSize::{}, numMessages::{}, queryTime::{}",
                friendPk, pos, loadSize, messages.size(), numMessages, endTime - startTime);
        if (messages.isEmpty()) {
            callback.onResult(messages, 0);
        } else {
            if (numMessages > loadSize) {
                callback.onResult(messages, numMessages - loadSize);
            } else {
                callback.onResult(messages, 0);
            }
        }
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params,
                          @NonNull LoadRangeCallback<ChatMsgAndUser> callback) {
        if(StringUtil.isEmpty(friendPk)) {
            return;
        }

        long startTime = System.currentTimeMillis();
        List<ChatMsgAndUser> messages;
        int numMessages = chatRepo.getNumMessages(friendPk);
        int pos = params.startPosition;
        int loadSize = params.loadSize;
        if (numMessages > pos) {
            pos = numMessages - loadSize - pos;
        }
        if (pos > 0 && pos < numMessages && initialDataNum == numMessages) {
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
        logger.debug("loadRange friendPk::{},  pos::{}, loadSize::{}, resultSize::{}, numEntries::{}, queryTime::{}",
                friendPk, pos, loadSize, messages.size(), numMessages, endTime - startTime);
        callback.onResult(messages);
    }
}
