package io.taucoin.torrent.publishing.ui.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.PositionalDataSource;
import io.reactivex.disposables.Disposable;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.TxRepository;
import io.taucoin.torrent.publishing.core.utils.StringUtil;

class TxDataSource extends PositionalDataSource<UserAndTx> {
    private static final Logger logger = LoggerFactory.getLogger("TxDataSource");
    private TxRepository txRepo;
    private String chainID;
    private long txType;
    private Disposable disposable;

    TxDataSource(TxRepository txRepo, String chainID, long txType) {
        this.txRepo = txRepo;
        this.chainID = chainID;
        this.txType = txType;
        disposable = txRepo.observeDataSetChanged()
                .subscribe(s -> invalidate());
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
                            @NonNull LoadInitialCallback<UserAndTx> callback) {
        if(StringUtil.isEmpty(chainID)) {
            return;
        }
        int numTxs = txRepo.queryNumCommunityTxs(chainID, txType);
        int pos;
        int loadSize = params.requestedLoadSize;
        // 初始加载大小大于等于数据总数，开始位置为0，否则为二者之差
        if (loadSize >= numTxs) {
            pos = 0;
        } else {
            pos = numTxs - loadSize;
        }
        logger.debug("loadInitial pos::{}, LoadSize::{}, numTxs::{}", pos, loadSize, numTxs);
        List<UserAndTx> txs = txRepo.queryCommunityTxs(chainID, txType, pos, loadSize);
        logger.debug("loadInitial txs.size::{}", txs.size());
        if (txs.isEmpty()) {
            callback.onResult(txs, 0);
        } else {
            callback.onResult(txs, pos);
        }
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params,
                          @NonNull LoadRangeCallback<UserAndTx> callback) {
        if(StringUtil.isEmpty(chainID)) {
            return;
        }

        List<UserAndTx> txs;
        int numTxs = txRepo.queryNumCommunityTxs(chainID, txType);
        int pos = params.startPosition;
        int loadSize = params.loadSize;
        logger.debug("loadRange pos::{}, loadSize::{}, numTxs::{}", pos, loadSize, numTxs);
        if (pos < numTxs) {
            // 开始位置小于数据总数
            txs = txRepo.queryCommunityTxs(chainID, txType, pos, loadSize);
        } else {
            // 否则数据为空
            txs = new ArrayList<>(0);
        }
        logger.debug("loadRange txs.size::{}", txs.size());
        callback.onResult(txs);
    }
}
