package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;

/**
 * Room:Message操作接口
 */
@Dao
public interface MessageDao {
    String QUERY_GET_MESSAGES_BY_CHAIN_ID = "SELECT * FROM Messages WHERE chainID = :chainID";
    String QUERY_GET_MESSAGE_BY_ID = "SELECT * FROM Messages WHERE msgID = :msgID";

    String QUERY_REPLY_USERS = " (SELECT u.nickname, m.senderPk, m.msgID FROM Users u" +
            " LEFT JOIN Messages m ON u.publicKey = m.senderPk) ";

    String QUERY_MESSAGES_WHERE = " WHERE msg.chainID = :chainID AND msg.senderPk NOT IN" +
            UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST;

    String QUERY_NUM_MESSAGES = "SELECT count(*) FROM Messages msg" +
            QUERY_MESSAGES_WHERE;

    String QUERY_MESSAGES_AND_REPLY_BY_CHAIN_ID = "SELECT msg.*, user.nickname AS replyName," +
            " mem.balance AS senderBalance FROM Messages msg" +
            " LEFT JOIN " + QUERY_REPLY_USERS + " AS user ON msg.replyID = user.msgID" +
            " LEFT JOIN Members AS mem ON msg.chainID = mem.chainID AND msg.senderPk = mem.publicKey" +
            QUERY_MESSAGES_WHERE + "limit :loadSize offset :startPosition ";

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
     * 获取社区的消息
     * @param chainID 社区chainID
     * @return 消息总数
     */
    @Query(QUERY_NUM_MESSAGES)
    int getNumMessages(String chainID);

    /**
     * 获取社区的消息
     * @param chainID 社区chainID
     * @param startPosition 数据开始位置
     * @param loadSize 加载数据大小
     * @return List<MsgAndReply>
     */
    @Query(QUERY_MESSAGES_AND_REPLY_BY_CHAIN_ID)
    @Transaction
    List<MsgAndReply> getMessages(String chainID, int startPosition, int loadSize);
}