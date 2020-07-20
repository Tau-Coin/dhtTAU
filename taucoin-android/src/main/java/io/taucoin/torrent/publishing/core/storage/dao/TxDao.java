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
import io.taucoin.torrent.publishing.core.model.data.ReplyAndAllTxs;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;

/**
 * Room:Transaction操作接口
 */
@Dao
public interface TxDao {
    String QUERY_USERS_LATEST_NAME = "SELECT a.* FROM (SELECT name, senderPk FROM Txs WHERE chainID = :chainID AND txType = :txType" +
            " ORDER BY timestamp DESC) a GROUP BY a.senderPk";

    String QUERY_REPLY_TXS_AND_LATEST_NAME = "SELECT b.txID, c.name FROM Txs AS b" +
            " LEFT JOIN(" + QUERY_USERS_LATEST_NAME + ") AS c ON b.senderPK = c.senderPK" +
            " WHERE b.txID IN (SELECT replyID FROM Txs WHERE replyID NOT NULL)";

    String QUERY_GET_TXS_BY_CHAIN_ID = "SELECT d.*, e.name AS nickName, f.name AS replyName" +
            " FROM Txs AS d" +
            " LEFT JOIN(" + QUERY_USERS_LATEST_NAME + ") AS e ON d.senderPk = e.senderPk" +
            " LEFT JOIN(" + QUERY_REPLY_TXS_AND_LATEST_NAME + ") AS f ON d.replyID = f.txID" +
            " WHERE d.chainID = :chainID" +
            " and d.senderPk NOT IN (SELECT publicKey FROM Users WHERE blacklist == 1 and isCurrentUser != 1)";

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
    List<ReplyAndAllTxs> getTxsByChainID(int txType, String chainID);

    /**
     * 根据chainID获取社区的交易的被被观察者
     * @param chainID 社区链id
     */
    @Transaction
    @Query(QUERY_GET_TXS_BY_CHAIN_ID)
    Flowable<List<ReplyAndAllTxs>> observeTxsByChainID(int txType, String chainID);

    @Transaction
    @Query(QUERY_GET_TXS_BY_CHAIN_ID)
    DataSource.Factory<Integer, ReplyAndAllTxs> queryCommunityTxs(int txType, String chainID);
}