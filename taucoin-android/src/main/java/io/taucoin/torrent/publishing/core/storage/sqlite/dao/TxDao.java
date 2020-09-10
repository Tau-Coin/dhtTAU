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
    // SQL:查询社区里的交易(上链，区分交易类型)
    String QUERY_GET_TXS_BY_CHAIN_WHERE = " WHERE tx.chainID = :chainID AND tx.txStatus = :txStatus" +
            " and tx.senderPk NOT IN " + UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST;

    String QUERY_GET_TXS_NUM_BY_CHAIN_ID_AND_TYPE_ON_CHAIN = "SELECT count(*) FROM Txs AS tx" +
            QUERY_GET_TXS_BY_CHAIN_WHERE;

    // SQL:查询社区里的交易(未上链，不区分交易类型)
    String QUERY_GET_TXS_BY_CHAIN_ID_NOT_ON_CHAIN = "SELECT tx.*, mem.balance AS senderBalance" +
            " FROM Txs AS tx" +
            " LEFT JOIN Members AS mem ON tx.senderPk = mem.publicKey AND tx.chainID = mem.chainID" +
            QUERY_GET_TXS_BY_CHAIN_WHERE +
            " limit :loadSize offset :startPosition";

    // SQL:查询社区里的交易数(未上链，不区分交易类型)
    String QUERY_GET_TXS_NUM_BY_CHAIN_ID_NOT_ON_CHAIN = QUERY_GET_TXS_NUM_BY_CHAIN_ID_AND_TYPE_ON_CHAIN +
            " AND tx.txType = :txType";

    // SQL:查询社区里的交易(上链，区分交易类型)
    String QUERY_GET_TXS_BY_CHAIN_ID_AND_TYPE_ON_CHAIN = "SELECT tx.*, mem.balance AS senderBalance" +
            " FROM Txs AS tx" +
            " LEFT JOIN Members AS mem ON tx.senderPk = mem.publicKey AND tx.chainID = mem.chainID" +
            QUERY_GET_TXS_BY_CHAIN_WHERE +
            " AND tx.txType = :txType" +
            " limit :loadSize offset :startPosition";


    // SQL:查询社区里的交易数(上链，区分交易类型)
    String QUERY_GET_TXS_NUM_BY_CHAIN_ID_ON_CHAIN = QUERY_GET_TXS_NUM_BY_CHAIN_ID_NOT_ON_CHAIN +
            " AND tx.txType = :txType";

    // SQL:查询未上链并且已过期的条件语句
    String QUERY_PENDING_TXS_NOT_EXPIRED_WHERE = " WHERE senderPk = :senderPk AND chainID = :chainID" +
            " and txStatus = 0 and timestamp > :expireTimePoint ";

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

    String QUERY_TX_MEDIAN_FEE = "SELECT fee FROM Txs " +
            " WHERE chainID = :chainID and " +
            " senderPk NOT IN " + UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST +
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
     * 根据chainID获取社区中的交易的被观察者
     * @param chainID 社区链id
     */
    @Transaction
    @Query(QUERY_GET_TXS_BY_CHAIN_ID_AND_TYPE_ON_CHAIN)
    List<UserAndTx> queryCommunityTxsOnChain(String chainID, long txType, int txStatus,
                                             int startPosition, int loadSize);

    @Query(QUERY_GET_TXS_NUM_BY_CHAIN_ID_ON_CHAIN)
    int queryNumCommunityTxsOnChain(String chainID, long txType, int txStatus);

    @Transaction
    @Query(QUERY_GET_TXS_BY_CHAIN_ID_NOT_ON_CHAIN)
    List<UserAndTx> queryCommunityTxsNotOnChain(String chainID, int txStatus,
                                                int startPosition, int loadSize);

    @Query(QUERY_GET_TXS_NUM_BY_CHAIN_ID_AND_TYPE_ON_CHAIN)
    int queryNumCommunityTxsNotOnChain(String chainID, int txStatus);

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
     * 根据txID查询交易
     * @param txID 交易ID
     */
    @Query(QUERY_GET_TX_BY_TX_ID)
    Tx getTxByTxID(String txID);

    /**
     * 观察中位数交易费
     * @param chainID 交易所属的社区chainID
     */
    @Query(QUERY_TX_MEDIAN_FEE)
    Single<List<Long>> observeMedianFee(String chainID);

    /**
     * 获取中位数交易费
     * @param chainID 交易所属的社区chainID
     */
    @Query(QUERY_TX_MEDIAN_FEE)
    List<Long> getMedianFee(String chainID);
}