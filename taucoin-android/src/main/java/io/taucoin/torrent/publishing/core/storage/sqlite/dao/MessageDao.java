package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;

/**
 * Room:Message操作接口
 */
@Dao
public interface MessageDao {
    String QUERY_GET_MESSAGES_BY_CHAIN_ID = "SELECT * FROM Messages WHERE chainID = :chainID";

    /**
     * 添加新的消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addMessage(Message msg);

    /**
     * 根据chainID查询社区消息
     * @param chainID 社区链id
     */
    @Query(QUERY_GET_MESSAGES_BY_CHAIN_ID)
    List<Message> getMessagesByChainID(String chainID);

    /**
     * 根据chainID获取社区的消息的被观察者
     * @param chainID 社区链id
     */
    @Query(QUERY_GET_MESSAGES_BY_CHAIN_ID)
    Flowable<List<Message>> observeMessagesByChainID(String chainID);
}