package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;

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
     * 根据chainID查询社区交易数
     * @param chainID 社区链ID
     */
    int queryNumCommunityTxs(String chainID, long txType);

    /**
     * 根据chainID查询社区交易
     * @param chainID 社区链ID
     */
    List<UserAndTx> queryCommunityTxs(String chainID, long txType, int startPos, int loadSize);

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

    /**
     * 根据txID查询交易
     * @param txID 交易ID
     */
    Single<Tx> getTxByTxIDSingle(String txID);

    /**
     * 根据txID查询交易
     * @param txID 交易ID
     */
    Tx getTxByTxID(String txID);

    /**
     * 观察中位数交易费
     * @param chainID 交易所属的社区chainID
     */
    Single<List<Long>> observeMedianFee(String chainID);

    /**
     * 获取中位数交易费
     * @param chainID 交易所属的社区chainID
     */
    List<Long> getMedianFee(String chainID);

    /**
     * 观察社区的交易的变化
     */
    Observable<String> observeDataSetChanged();

    /**
     * 提交数据变化
     */
    void submitDataSetChanged();
}
