package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;

/**
 * Room:Transaction操作接口
 */
@Dao
public interface TxDao {
    // SQL:查询社区里的交易
    String QUERY_GET_TXS_BY_CHAIN_ID_AND_TYPE = "SELECT * FROM Txs" +
            " WHERE chainID = :chainID AND txType = :txType" +
            " and senderPk NOT IN (SELECT publicKey FROM Users WHERE blacklist == 1 and isCurrentUser != 1)";

    // SQL:查询社区里的交易
    String QUERY_GET_TXS_BY_CHAIN_ID = "SELECT * FROM Txs" +
            " WHERE chainID = :chainID" +
            " and senderPk NOT IN (SELECT publicKey FROM Users WHERE blacklist == 1 and isCurrentUser != 1)";

    // SQL:查询未上链并且已过期的条件语句
    String QUERY_PENDING_TXS_NOT_EXPIRED_WHERE = " WHERE senderPk = :senderPk AND chainID = :chainID and txStatus = 0 and timestamp > :expireTimePoint ";

    // SQL:查询未上链、未过期的交易
    String QUERY_PENDING_TXS_NOT_EXPIRED = "SELECT count(*) FROM Txs" + QUERY_PENDING_TXS_NOT_EXPIRED_WHERE;

    // SQL:查询未上链并且未过期的txID
    String QUERY_PENDING_TX_IDS_NOT_EXPIRED = "SELECT txID FROM Txs" + QUERY_PENDING_TXS_NOT_EXPIRED_WHERE;

    // SQL:查询未上链、已过期的并且nonce值未被再次使用的最早的交易
    String QUERY_USERS_EARLIEST_EXPIRE_TX = "SELECT * FROM Txs" +
            " WHERE senderPk = :senderPk AND chainID = :chainID AND txStatus = 0 AND timestamp <= :expireTimePoint" +
            " AND nonce NOT IN (" + QUERY_PENDING_TX_IDS_NOT_EXPIRED + ")" +
            " ORDER BY nonce LIMIT 1";

    String QUERY_GET_TX_BY_TX_ID = "SELECT * FROM Txs" +
            " WHERE txID = :txID";

    String QUERY_SET_TX_FAVOURITE = "UPDATE Txs SET favourite = :favourite" +
            " WHERE txID = :txID";

    String QUERY_TX_MEDIAN_FEE = "SELECT fee FROM Txs " +
            " WHERE chainID = :chainID and " +
            " senderPk NOT IN (SELECT publicKey FROM Users WHERE blacklist == 1 and isCurrentUser != 1)" +
            " ORDER BY fee";
    /**
     * 添加新的交易
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addTransaction(Tx tx);

    /**
     * 更新交易
     */
    @Update
    int updateTransaction(Tx tx);

    /**
     * 根据chainID查询社区
     * @param chainID 社区链id
     */
    @Transaction
    @Query(QUERY_GET_TXS_BY_CHAIN_ID)
    List<UserAndTx> getTxsByChainID(String chainID);

    /**
     * 根据chainID获取社区的交易的被观察者
     * @param chainID 社区链id
     */
    @Transaction
    @Query(QUERY_GET_TXS_BY_CHAIN_ID_AND_TYPE)
    Flowable<List<UserAndTx>> observeTxsByChainID(String chainID, int txType);

    /**
     * 根据chainID获取社区中的交易的被观察者
     * @param chainID 社区链id
     */
    @Transaction
    @Query(QUERY_GET_TXS_BY_CHAIN_ID_AND_TYPE)
    DataSource.Factory<Integer, UserAndTx> queryCommunityTxs(String chainID, int txType);

    /**
     * 获取社区里用户未上链并且未过期的交易数
     * @param chainID chainID
     * @param senderPk 公钥
     * @param expireTimePoint 过期的时间点
     * @return int
     */
    @Query(QUERY_PENDING_TXS_NOT_EXPIRED)
    int getPendingTxsNotExpired(String chainID, String senderPk, long expireTimePoint);

    /**
     * 获取社区里用户未上链并且过期的最早的交易
     * @param chainID chainID
     * @param senderPk 公钥
     * @param expireTimePoint 过期的时间点
     * @return Tx
     */
    @Transaction
    @Query(QUERY_USERS_EARLIEST_EXPIRE_TX)
    Tx getEarliestExpireTx(String chainID, String senderPk, long expireTimePoint);

    /**
     * 根据txID查询交易
     * @param txID 交易ID
     */
    @Query(QUERY_GET_TX_BY_TX_ID)
    Single<Tx> getTxByTxIDSingle(String txID);

    /**
     * 设置交易加入到收藏
     * @param favourite 收藏
     */
    @Query(QUERY_SET_TX_FAVOURITE)
    void setFavourite(String txID, int favourite);

    /**
     * 观察中位数交易费
     * @param chainID 交易所属的社区chainID
     */
    @Query(QUERY_TX_MEDIAN_FEE)
    Single<List<Long>> observeMedianFee(String chainID);
}