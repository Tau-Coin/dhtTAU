package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;

/**
 * Room:Message操作接口
 */
@Dao
public interface MessageDao {
    String QUERY_GET_MESSAGES_BY_CHAIN_ID = "SELECT * FROM Messages WHERE chainID = :chainID";
    String QUERY_GET_MESSAGE_BY_ID = "SELECT * FROM Messages WHERE msgID = :msgID";

    String QUERY_REPLY_USERS = " (SELECT u.localName, m.senderPk, m.msgID FROM Users u" +
            " LEFT JOIN Messages m ON u.publicKey = m.senderPk) ";
    String QUERY_MESSAGES_AND_REPLY_BY_CHAIN_ID = "SELECT msg.*, user.localName AS replyName FROM Messages msg" +
            " LEFT JOIN " + QUERY_REPLY_USERS + " AS user ON msg.replyID = user.msgID" +
            " WHERE chainID = :chainID";

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
     * 根据msgID查询社区消息
     * @param msgID 社区消息id
     */
    @Query(QUERY_GET_MESSAGE_BY_ID)
    Message getMessageByID(String msgID);

    /**
     * 根据chainID获取社区的消息的被观察者
     * @param chainID 社区链id
     */
    @Query(QUERY_MESSAGES_AND_REPLY_BY_CHAIN_ID)
    @Transaction
    DataSource.Factory<Integer, MsgAndReply> queryMessagesByChainID(String chainID);
}