package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;

/**
 * Room:User操作接口
 */
@Dao
public interface ChatDao {
    String QUERY_GET_CHAT_MSG = "SELECT * FROM ChatMessages WHERE senderPk = :senderPk AND hash = :hash";
    String QUERY_GET_CHAT_MSG_BY_HASH = "SELECT * FROM ChatMessages WHERE hash = :hash";

    String QUERY_MESSAGES_WHERE = " WHERE ((msg.senderPk = :senderPk AND msg.receiverPk = :receiverPk)" +
            " OR (msg.senderPk = :receiverPk AND msg.receiverPk = :senderPk)) " +
            " AND (msg.contentType = 0 OR (msg.contentType = 1 AND msg.nonce = 0))";

    String QUERY_NUM_MESSAGES = "SELECT count(hash) FROM ChatMessages msg" +
            QUERY_MESSAGES_WHERE;

    String QUERY_MESSAGES_BY_FRIEND_PK = "SELECT msg.*" +
            " FROM ChatMessages msg" +
            QUERY_MESSAGES_WHERE +
            " ORDER BY msg.timestamp DESC, msg.logicMsgHash COLLATE UNICODE DESC, msg.nonce DESC" +
            " LIMIT :loadSize OFFSET :startPosition ";

    String QUERY_MESSAGE_LIST = "SELECT msg.*" +
            " FROM ChatMessages msg" +
            " WHERE ((msg.senderPk = :senderPk AND msg.receiverPk = :receiverPk)" +
            " OR (msg.senderPk = :receiverPk AND msg.receiverPk = :senderPk)) " +
            " ORDER BY msg.timestamp DESC, msg.logicMsgHash COLLATE UNICODE DESC, msg.nonce DESC" +
            " LIMIT :loadSize OFFSET :startPosition";

    String QUERY_UNSENT_MESSAGES = "SELECT msg.*" +
            " FROM ChatMessages msg" +
            " WHERE msg.senderPk in (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND msg.unsent = 0" +
            " ORDER BY msg.timestamp, msg.logicMsgHash COLLATE UNICODE, msg.nonce";

    // 查询消息的所有日志
    String QUERY_CHAT_MSG_LOGS = "SELECT * FROM ChatMsgLogs WHERE hash = :hash" +
            " GROUP BY status ORDER BY status DESC";

    // 查询消息单独状态日志数据
    String QUERY_CHAT_MSG_LOG = "SELECT * FROM ChatMsgLogs WHERE hash = :hash AND status = :status";

    /**
     * 添加聊天信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addChat(ChatMsg msg);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] addChats(ChatMsg... msg);

    /**
     * 更新聊天信息
     */
    @Update
    int updateChat(ChatMsg msg);

    /**
     * 获取当前的用户
     */
    @Query(QUERY_GET_CHAT_MSG)
    ChatMsg queryChatMsg(String senderPk, String hash);

    @Query(QUERY_GET_CHAT_MSG_BY_HASH)
    ChatMsg queryChatMsg(String hash);

    /**
     * 获取社区的消息
     * @param receiverPk 朋友公钥
     * @return 消息总数
     */
    @Query(QUERY_NUM_MESSAGES)
    int getNumMessages(String senderPk, String receiverPk);

    /**
     * 获取聊天的消息
     * @param receiverPk 朋友公钥
     * @param startPosition 数据开始位置
     * @param loadSize 加载数据大小
     * @return List<Chat>
     */
    @Query(QUERY_MESSAGES_BY_FRIEND_PK)
    List<ChatMsgAndUser> getMessages(String senderPk, String receiverPk, int startPosition, int loadSize);

    @Query(QUERY_MESSAGE_LIST)
    List<ChatMsg> getMessageList(String senderPk, String receiverPk, int startPosition, int loadSize);

    @Query(QUERY_UNSENT_MESSAGES)
    List<ChatMsg> getUnsentMessages();

    /**
     * 添加消息日志
     */
    @Insert()
    void addChatMsgLogs(ChatMsgLog... msgLog);

    @Query(QUERY_CHAT_MSG_LOGS)
    Observable<List<ChatMsgLog>> observerMsgLogs(String hash);

    /**
     * 查询聊天消息日志
     */
    @Query(QUERY_CHAT_MSG_LOG)
    ChatMsgLog queryChatMsgLog(String hash, int status);
}