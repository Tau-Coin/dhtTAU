package io.taucoin.torrent.publishing.core.storage;

import java.util.List;

import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;

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
     * @param chainID 社区链id
     * @param chat 区分聊天和链上交易
     */
    List<Tx> getTxsByChainID(String chainID, int chat);

    /**
     * 根据chainID获取社区的交易的被被观察者
     * @param chainID 社区链id
     * @param chat 区分聊天和链上交易
     */
    Flowable<List<Tx>> observeTxsByChainID(String chainID, int chat);
}
