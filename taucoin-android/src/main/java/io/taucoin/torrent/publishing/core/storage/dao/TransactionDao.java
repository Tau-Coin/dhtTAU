package io.taucoin.torrent.publishing.core.storage.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;

/**
 * Room:Transaction操作接口
 */
@Dao
public interface TransactionDao {
    String QUERY_GET_TXS_BY_CHAIN_ID = "SELECT * FROM Tx WHERE chainID = :chainID and chat = :chat";

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
     * 根据chainIDc查询社区
     * @param chainID 社区链id
     * @param chat 区分聊天和链上交易
     */
    @Query(QUERY_GET_TXS_BY_CHAIN_ID)
    List<Tx> getTxsBychainID(String chainID, int chat);
}