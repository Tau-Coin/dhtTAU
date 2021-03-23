package io.taucoin.torrent.publishing.ui.chat;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.reactivex.disposables.Disposable;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.ChatRepository;

class ChatSourceFactory extends MsgDataSource.Factory<Integer, ChatMsgAndUser> {
    private ChatRepository chatRepo;
    private String friendPk;
    private ChatDataSource chatDataSource;

    ChatSourceFactory(@NonNull ChatRepository chatRepo) {
        this.chatRepo = chatRepo;
    }

    void setFriendPk(@NonNull String friendPk) {
        this.friendPk = friendPk;
    }

    void onCleared() {
        if (chatDataSource != null) {
            chatDataSource.onCleared();
        }
    }

    @NonNull
    @Override
    public DataSource<Integer, ChatMsgAndUser> create() {
        chatDataSource = new ChatDataSource(chatRepo, friendPk);
        return chatDataSource;
    }
}