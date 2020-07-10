package io.taucoin.torrent.publishing.core.storage;

import java.util.List;

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
    List<Tx> getTxsBychainID(String chainID, int chat);
}
