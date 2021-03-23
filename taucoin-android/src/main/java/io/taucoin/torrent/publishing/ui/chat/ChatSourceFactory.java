package io.taucoin.torrent.publishing.ui.chat;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;
import io.taucoin.torrent.publishing.core.utils.Utils;

class ChatSourceFactory extends MsgDataSource.Factory<Integer, ChatMsgAndUser> {
    private ChatRepository chatRepo;
    private String friendPk;
    private byte[] friendCryptoKey;
    private byte[] userCryptoKey;
    private ChatDataSource chatDataSource;

    ChatSourceFactory(@NonNull ChatRepository chatRepo) {
        this.chatRepo = chatRepo;
    }

    void setFriendPk(@NonNull String friendPk) {
        this.friendPk = friendPk;
        String userPk = MainApplication.getInstance().getPublicKey();
        userCryptoKey = Utils.keyExchange(userPk, MainApplication.getInstance().getSeed());
        friendCryptoKey = Utils.keyExchange(friendPk, MainApplication.getInstance().getSeed());
    }

    void onCleared() {
        if (chatDataSource != null) {
            chatDataSource.onCleared();
        }
    }

    @NonNull
    @Override
    public DataSource<Integer, ChatMsgAndUser> create() {
        chatDataSource = new ChatDataSource(chatRepo, friendPk, userCryptoKey, friendCryptoKey);
        return chatDataSource;
    }
}