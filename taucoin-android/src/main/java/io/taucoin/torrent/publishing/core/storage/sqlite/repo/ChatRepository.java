package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import java.util.List;

import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Chat;

/**
 * 提供操作Friend数据的接口
 */
public interface ChatRepository {

    /**
     * 添加Chat
     */
    void addChat(Chat chat);

    /**
     * 更新Chat
     */
    void updateChat(Chat chat);

    /**
     * 查询Chat
     * @param hash
     * @return
     */
    Chat queryChatByHash(String hash);

    /**
     * 观察社区的消息的变化
     */
    Observable<String> observeDataSetChanged();

    /**
     * 提交数据变化
     */
    void submitDataSetChanged();

    int getNumMessages(String friendPk);

    List<Chat> getMessages(String friendPk, int pos, int loadSize);
}