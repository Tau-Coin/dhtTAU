package io.taucoin.torrent.publishing.ui.chat;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MsgRepository;

class MsgSourceFactory extends MsgDataSource.Factory<Integer, MsgAndReply> {
    private MsgRepository msgRepo;
    private String chainID;

    MsgSourceFactory(@NonNull MsgRepository msgRepo) {
        this.msgRepo = msgRepo;
    }

    void setChainID(@NonNull String chainID) {
        this.chainID = chainID;
    }

    @NonNull
    @Override
    public DataSource<Integer, MsgAndReply> create() {
        return new MsgDataSource(msgRepo, chainID);
    }
}