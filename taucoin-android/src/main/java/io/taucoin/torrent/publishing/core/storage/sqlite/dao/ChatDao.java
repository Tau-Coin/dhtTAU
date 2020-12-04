package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Chat;

/**
 * Room:User操作接口
 */
@Dao
public interface ChatDao {
    String QUERY_GET_CHAT_BY_HASH = "SELECT * FROM Chats WHERE hash = :hash";

    String QUERY_MESSAGES_WHERE = " WHERE (chat.senderPk = :senderPk OR chat.senderPk = :friendPk)" +
            " AND (chat.friendPk = :friendPk OR chat.friendPk = :senderPk) " +
            " AND chat.friendPk NOT IN" +
            UserDao.QUERY_GET_USER_PKS_IN_BAN_LIST;

    String QUERY_NUM_MESSAGES = "SELECT count(*) FROM Chats chat" +
            QUERY_MESSAGES_WHERE;

    String QUERY_MESSAGES_BY_FRIEND_PK = "SELECT chat.*" +
            " FROM Chats chat" +
            QUERY_MESSAGES_WHERE + "limit :loadSize offset :startPosition ";
    /**
     * 添加聊天信息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addChat(Chat chat);

    /**
     * 更新聊天信息
     */
    @Update
    int updateChat(Chat chat);

    /**
     * 获取当前的用户
     */
    @Query(QUERY_GET_CHAT_BY_HASH)
    Chat queryChatByHash(String hash);

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
    List<Chat> getMessages(String senderPk, String friendPk, int startPosition, int loadSize);
}