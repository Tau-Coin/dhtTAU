package io.taucoin.torrent.publishing.core.storage.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;

/**
 * Room:Transaction操作接口
 */
@Dao
public interface TxDao {
    String QUERY_GET_TXS_BY_CHAIN_ID = "SELECT * FROM Tx WHERE chainID = :chainID";

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
    @Query(QUERY_GET_TXS_BY_CHAIN_ID)
    List<Tx> getTxsByChainID(String chainID);

    /**
     * 根据chainID获取社区的交易的被被观察者
     * @param chainID 社区链id
     */
    @Query(QUERY_GET_TXS_BY_CHAIN_ID)
    Flowable<List<Tx>> observeTxsByChainID(String chainID);
}