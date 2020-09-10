package io.taucoin.torrent.publishing.ui.transaction;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;

class TxSourceFactory extends TxDataSource.Factory<Integer, UserAndTx> {
    private TxRepository txRepo;
    private String chainID;
    private long txType;

    TxSourceFactory(@NonNull TxRepository txRepo) {
        this.txRepo = txRepo;
    }

    void setTxQueryParam(@NonNull String chainID, long txType) {
        this.chainID = chainID;
        this.txType = txType;
    }

    @NonNull
    @Override
    public DataSource<Integer, UserAndTx> create() {
        return new TxDataSource(txRepo, chainID, txType);
    }
}