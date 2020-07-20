package io.taucoin.torrent.publishing.core.storage;

import java.util.List;

import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.model.data.ReplyAndAllTxs;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;
import io.taucoin.types.MsgType;

/**
 * 提供外部操作User数据的接口
 */
public interface TxRepository {

    /**
     * 添加新的交易
     */
    long addTransaction(Tx transaction);

    /**
     * 更新交易
     */
    int updateTransaction(Tx transaction);

    /**
     * 根据chainID查询社区
     * @param chainID 社区链ID
     */
    List<ReplyAndAllTxs> getTxsByChainID(String chainID);

    /**
     * 根据chainID获取社区的交易的被被观察者
     * @param chainID 社区链ID
     */
    Flowable<List<ReplyAndAllTxs>> observeTxsByChainID(String chainID);

    DataSource.Factory<Integer, ReplyAndAllTxs> queryCommunityTxs(String chainID);
}
