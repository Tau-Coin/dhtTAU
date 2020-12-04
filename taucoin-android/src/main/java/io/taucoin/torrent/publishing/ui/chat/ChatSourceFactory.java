package io.taucoin.torrent.publishing.ui.chat;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Chat;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;

class ChatSourceFactory extends MsgDataSource.Factory<Integer, Chat> {
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
    public DataSource<Integer, Chat> create() {
        return new ChatDataSource(chatRepo, friendPk);
    }
}