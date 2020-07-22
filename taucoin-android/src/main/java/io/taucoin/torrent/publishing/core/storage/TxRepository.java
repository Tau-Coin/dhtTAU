package io.taucoin.torrent.publishing.core.storage;

import java.util.List;

import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.model.data.ReplyAndTx;
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
     * @param chainID 社区链ID
     */
    List<ReplyAndTx> getTxsByChainID(String chainID);

    /**
     * 根据chainID获取社区的交易的被被观察者
     * @param chainID 社区链ID
     */
    Flowable<List<ReplyAndTx>> observeTxsByChainID(String chainID);

    DataSource.Factory<Integer, ReplyAndTx> queryCommunityTxs(String chainID);

    /**
     * 获取社区里用户未上链并且未过期的交易数
     * @param chainID chainID
     * @param publicKey 公钥
     * @param expireTime 过期时间时长
     * @return int
     */
    int getPendingTxsNotExpired(String chainID, String publicKey, long expireTime);
    /**
     * 获取社区里用户未上链并且未过期的交易数
     * @param chainID chainID
     * @param senderPk 公钥
     * @param expireTime 过期时间时长
     * @return int
     */
    Tx getEarliestExpireTx(String chainID, String senderPk, long expireTime);
}
