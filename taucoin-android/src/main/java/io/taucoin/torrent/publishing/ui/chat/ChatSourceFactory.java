package io.taucoin.torrent.publishing.ui.chat;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;

class ChatSourceFactory extends MsgDataSource.Factory<Integer, ChatMsgAndUser> {
    private ChatRepository chatRepo;
    private String friendPk;

    ChatSourceFactory(@NonNull ChatRepository chatRepo) {
        this.chatRepo = chatRepo;
    }

    void setFriendPk(@NonNull String friendPk) {
        this.friendPk = friendPk;
    }

    @NonNull
    @Override
    public DataSource<Integer, ChatMsgAndUser> create() {
        return new ChatDataSource(chatRepo, friendPk);
    }
}