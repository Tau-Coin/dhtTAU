package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgType;

/**
 * Room:User操作接口
 */
@Dao
public interface ChatDao {
    String QUERY_GET_CHAT_MSG = "SELECT * FROM ChatMessages WHERE friendPk = :friendPk AND hash = :hash";

    String QUERY_MESSAGES_WHERE = " WHERE (msg.senderPk = :senderPk OR msg.senderPk = :friendPk)" +
            " AND (msg.friendPk = :friendPk OR msg.friendPk = :senderPk) " +
            " AND msg.friendPk NOT IN" +
            UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST;

    String QUERY_NUM_MESSAGES = "SELECT count(*) FROM ChatMessages msg" +
            QUERY_MESSAGES_WHERE;

    String QUERY_MESSAGES_BY_FRIEND_PK = "SELECT msg.*" +
            " FROM ChatMessages msg" +
            QUERY_MESSAGES_WHERE + " ORDER BY msg.timestamp, msg.id" +
            " LIMIT :loadSize OFFSET :startPosition ";

    String QUERY_UNSENT_MESSAGES = "SELECT msg.*" +
            " FROM ChatMessages msg" +
            " WHERE msg.senderPk in (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND msg.status = 0" +
            " ORDER BY msg.timestamp, msg.id";

    String QUERY_UN_CONFIRMATION_FRIENDS = "SELECT msg.friendPk" +
            " FROM ChatMessages msg" +
            " WHERE msg.senderPk in (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND msg.status == 1" +
            " GROUP BY msg.friendPk" +
            " ORDER BY msg.timestamp";

    String QUERY_MESSAGES_BY_TIME = "SELECT msg.*" +
            " FROM ChatMessages msg" +
            " WHERE msg.senderPk in (" + UserDao.QUERY_GET_CURRENT_USER_PK + ")" +
            " AND msg.status = 1" +
            " AND msg.timestamp >= :time" +
            " ORDER BY msg.timestamp";

    /**
     * 添加聊天信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addChat(ChatMsg msg);

    /**
     * 更新聊天信息
     */
    @Update
    int updateChat(ChatMsg msg);

    /**
     * 获取当前的用户
     */
    @Query(QUERY_GET_CHAT_MSG)
    ChatMsg queryChatByHash(String friendPk, String hash);

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

    @Query(QUERY_UN_CONFIRMATION_FRIENDS)
    List<String> getUnConfirmationFriends();

    @Query(QUERY_MESSAGES_BY_TIME)
    List<ChatMsg> getMessagesByTime(long time);
}