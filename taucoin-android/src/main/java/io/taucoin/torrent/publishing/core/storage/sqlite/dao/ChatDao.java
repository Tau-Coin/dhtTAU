package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;

/**
 * Room:User操作接口
 */
@Dao
public interface ChatDao {
    String QUERY_GET_CHAT_MSG = "SELECT * FROM ChatMessages WHERE friendPk = :friendPk AND hash = :hash";
    String QUERY_GET_CHAT_MSG_BY_HASH = "SELECT * FROM ChatMessages WHERE hash = :hash";

    String QUERY_WHERE_NONCE = " AND (msg.contentType = 0 OR (msg.contentType = 1 AND msg.nonce = 0)) ";
    String QUERY_MESSAGES_WHERE = " WHERE (msg.senderPk = :senderPk OR msg.senderPk = :friendPk)" +
            " AND (msg.friendPk = :friendPk OR msg.friendPk = :senderPk) " +
            QUERY_WHERE_NONCE +
            " AND msg.friendPk NOT IN" +
            UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST;

    String QUERY_NUM_MESSAGES = "SELECT count(*) FROM ChatMessages msg" +
            QUERY_MESSAGES_WHERE;

    String QUERY_MESSAGES_BY_FRIEND_PK = "SELECT msg.*" +
            " FROM ChatMessages msg" +
            QUERY_MESSAGES_WHERE +
            " ORDER BY msg.timestamp, msg.nonce" +
            " LIMIT :loadSize OFFSET :startPosition ";

    String QUERY_UNSENT_MESSAGES = "SELECT msg.*" +
            " FROM ChatMessages msg" +
            " WHERE msg.senderPk in (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND msg.unsent = 0" +
            " ORDER BY msg.timestamp, msg.nonce";

    String QUERY_BUILT_AND_UNSENT_MESSAGES = "SELECT msg.*" +
            " FROM ChatMessages msg" +
            " WHERE msg.senderPk in (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND msg.unsent = 0 AND msg.hash NOT NULL" +
            " ORDER BY msg.timestamp DESC, msg.nonce DESC" +
            " limit 1";

    // 查询消息未确认的HASH
    String QUERY_UN_CONFIRMATION_MSG_HASH = " (SELECT hash FROM ChatMsgLogs WHERE status = :status) ";

    // 查询消息未确认朋友
    String QUERY_UN_CONFIRMATION_FRIENDS = "SELECT msg.friendPk" +
            " FROM ChatMessages msg" +
            " WHERE msg.senderPk in (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            QUERY_WHERE_NONCE +
            " AND msg.hash NOT IN" + QUERY_UN_CONFIRMATION_MSG_HASH +
            " GROUP BY msg.friendPk" +
            " ORDER BY msg.timestamp";

    // 查询消息的最新log
    String QUERY_CHAT_MSG_LAST_LOG = "SELECT * FROM ChatMsgLogs WHERE hash = :hash" +
            " ORDER BY timestamp LIMIT 1";

    // 查询消息的所有日志
    String QUERY_CHAT_MSG_LOGS = "SELECT * FROM ChatMsgLogs WHERE hash = :hash" +
            " ORDER BY status DESC";

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
    ChatMsg queryChatMsg(String friendPk, String hash);

    @Query(QUERY_GET_CHAT_MSG_BY_HASH)
    ChatMsg queryChatMsg(String hash);

    /**
     * 获取社区的消息
     * @param friendPk 朋友公钥
     * @return 消息总数
     */
    @Query(QUERY_NUM_MESSAGES)
    int getNumMessages(String senderPk, String friendPk);

    /**
     * 获取聊天的消息
     * @param friendPk 朋友公钥
     * @param startPosition 数据开始位置
     * @param loadSize 加载数据大小
     * @return List<Chat>
     */
    @Query(QUERY_MESSAGES_BY_FRIEND_PK)
    @Transaction
    List<ChatMsg> getMessages(String senderPk, String friendPk, int startPosition, int loadSize);

    @Query(QUERY_UNSENT_MESSAGES)
    List<ChatMsg> getUnsentMessages();

    @Query(QUERY_BUILT_AND_UNSENT_MESSAGES)
    ChatMsg getLatestBuiltAndUnsentMsg();

    @Query(QUERY_UN_CONFIRMATION_FRIENDS)
    List<String> getUnConfirmationFriends(int status);

    /**
     * 添加消息日志
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long addChatMsgLog(ChatMsgLog msgLog);

    @Query(QUERY_CHAT_MSG_LAST_LOG)
    ChatMsgLog queryChatMsgLastLog(String hash);

    @Query(QUERY_CHAT_MSG_LOGS)
    Observable<List<ChatMsgLog>> observerMsgLogs(String hash);
}