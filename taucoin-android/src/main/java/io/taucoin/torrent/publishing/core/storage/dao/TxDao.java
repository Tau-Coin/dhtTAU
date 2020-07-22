package io.taucoin.torrent.publishing.core.storage.dao;

import java.util.List;

import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.model.data.ReplyAndTx;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;

/**
 * Room:Transaction操作接口
 */
@Dao
public interface TxDao {
    // SQL:查询用户的最新名字
    String QUERY_USERS_LATEST_NAME = "SELECT a.senderPk, a.name FROM" +
            " (SELECT name, senderPk FROM Txs WHERE chainID = :chainID AND txType = :txType ORDER BY timestamp) a" +
            " GROUP BY a.senderPk";

    // SQL:查询被回复用户的最新名字
    String QUERY_REPLY_TXS_AND_LATEST_NAME = "SELECT b.txID, c.name FROM Txs AS b" +
            " LEFT JOIN (" + QUERY_USERS_LATEST_NAME + ") AS c ON b.senderPK = c.senderPK" +
            " WHERE b.txID IN (SELECT replyID FROM Txs WHERE replyID NOT NULL)";

    // SQL:查询社区里的交易
    String QUERY_GET_TXS_BY_CHAIN_ID = "SELECT d.*, e.name AS nickName, f.name AS replyName" +
            " FROM Txs AS d" +
            " LEFT JOIN (" + QUERY_USERS_LATEST_NAME + ") AS e ON d.senderPk = e.senderPk" +
            " LEFT JOIN (" + QUERY_REPLY_TXS_AND_LATEST_NAME + ") AS f ON d.replyID = f.txID" +
            " WHERE d.chainID = :chainID" +
            " and d.senderPk NOT IN (SELECT publicKey FROM Users WHERE blacklist == 1 and isCurrentUser != 1)";

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
    List<ReplyAndTx> getTxsByChainID(int txType, String chainID);

    /**
     * 根据chainID获取社区的交易的被观察者
     * @param chainID 社区链id
     */
    @Transaction
    @Query(QUERY_GET_TXS_BY_CHAIN_ID)
    Flowable<List<ReplyAndTx>> observeTxsByChainID(int txType, String chainID);

    /**
     * 根据chainID获取社区中的交易的被观察者
     * @param chainID 社区链id
     */
    @Transaction
    @Query(QUERY_GET_TXS_BY_CHAIN_ID)
    DataSource.Factory<Integer, ReplyAndTx> queryCommunityTxs(int txType, String chainID);

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
    @Query(QUERY_USERS_EARLIEST_EXPIRE_TX)
    Tx getEarliestExpireTx(String chainID, String senderPk, long expireTimePoint);
}