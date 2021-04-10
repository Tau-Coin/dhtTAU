package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import java.util.List;

import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndUser;
import io.taucoin.torrent.publishing.core.model.data.DataChanged;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;

/**
 * 提供操作Friend数据的接口
 */
public interface ChatRepository {

    /**
     * 添加Chats
     */
    void addChatMsg(ChatMsg chat);

    /**
     * 添加多条Chats
     */
    void addChatMessages(ChatMsg... chats);

    /**
     * 更新Chat
     */
    void updateChatMsg(ChatMsg chat);

    /**
     * 更新Chat发送状态，不触发UI刷新
     */
    void updateMsgSendStatus(ChatMsg chat);

    /**
     * 查询ChatMsg
     * @param senderPk
     * @param hash
     * @return
     */
    ChatMsg queryChatMsg(String senderPk, String hash);

    /**
     * 查询ChatMsg
     * @param hash
     * @return
     */
    ChatMsg queryChatMsg(String hash);

    /**
     * 观察社区的消息的变化
     */
    Observable<DataChanged> observeDataSetChanged();

    /**
     * 提交数据变化
     */
    void submitDataSetChanged(ChatMsg chat);

    void submitDataSetChangedDirect(String usersPk);

    int getNumMessages(String friendPk);

    List<ChatMsgAndUser> getMessages(String friendPk, int pos, int loadSize);

    /**
     * 获取未入队列的消息
     * @return
     */
    List<ChatMsg> getUnsentMessages();

    /**
     * 添加消息日志
     * @param msgLogs
     */
    void addChatMsgLogs(ChatMsgLog... msgLogs);

    /**
     * 观察消息日志
     * @param hash
     * @return
     */
    Observable<List<ChatMsgLog>> observerMsgLogs(String hash);

    /**
     * 查询聊天消息日志
     * @param hash
     * @param status
     * @return
     */
    ChatMsgLog queryChatMsgLog(String hash, int status);
}
