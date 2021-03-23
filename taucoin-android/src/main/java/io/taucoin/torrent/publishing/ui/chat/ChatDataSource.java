package io.taucoin.torrent.publishing.ui.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.PositionalDataSource;
import io.reactivex.disposables.Disposable;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.util.ByteUtil;
import io.taucoin.util.CryptoUtil;

class ChatDataSource extends PositionalDataSource<ChatMsgAndUser> {
    private static final Logger logger = LoggerFactory.getLogger("ChatDataSource");
    private ChatRepository chatRepo;
    private String friendPk;
    private byte[] friendCryptoKey;
    private byte[] userCryptoKey;
    private Disposable disposable;

    ChatDataSource(@NonNull ChatRepository chatRepo, @NonNull String friendPk, byte[] userCryptoKey,
                   byte[] friendCryptoKey) {
        this.chatRepo = chatRepo;
        this.friendPk = friendPk;
        this.userCryptoKey = userCryptoKey;
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
        long getNumTime = System.currentTimeMillis();
        logger.trace("loadInitial getNumTime::{}", getNumTime - startTime);
        int pos;
        int loadSize = params.requestedLoadSize;
        // 初始加载大小大于等于数据总数，开始位置为0，否则为二者之差
        if (loadSize >= numMessages) {
            pos = 0;
        } else {
            pos = numMessages - loadSize;
        }
        List<ChatMsgAndUser> messages = chatRepo.getMessages(friendPk, pos, loadSize);
        long getMessagesTime = System.currentTimeMillis();
        logger.trace("loadInitial getMessagesTime::{}", getMessagesTime - getNumTime);
        for (ChatMsgAndUser msg : messages) {
            byte[] content = ByteUtil.toByte(msg.content);
            try {
                if (StringUtil.isEquals(friendPk, msg.senderPk)) {
                    msg.rawContent = CryptoUtil.decrypt(content, userCryptoKey);
                } else {
                    msg.rawContent = CryptoUtil.decrypt(content, friendCryptoKey);
                }
                msg.content = null;
            } catch (Exception e) {
                logger.error("loadInitial decrypt error::", e);
            }
        }
        long endTime = System.currentTimeMillis();
        logger.trace("loadInitial decryptTime Time::{}", endTime - getMessagesTime);

        logger.debug("loadInitial pos::{}, loadSize::{}, resultSize::{}, numMessages::{}, queryTime::{}",
                pos, loadSize, messages.size(), numMessages, endTime - startTime);
        if (messages.isEmpty()) {
            callback.onResult(messages, 0);
        } else {
            callback.onResult(messages, pos);
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
        if (pos < numMessages) {
            // 开始位置小于数据总数
            messages = chatRepo.getMessages(friendPk, pos, loadSize);
        } else {
            // 否则数据为空
            messages = new ArrayList<>(0);
        }
        long endTime = System.currentTimeMillis();
        logger.debug("loadRange pos::{}, loadSize::{}, resultSize::{}, numEntries::{}, queryTime::{}",
                pos, loadSize, messages.size(), numMessages, endTime - startTime);
        callback.onResult(messages);
    }
}
