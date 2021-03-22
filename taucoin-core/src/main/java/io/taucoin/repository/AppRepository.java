package io.taucoin.repository;

import java.util.List;
import java.util.Set;

import io.taucoin.types.Message;

/**
 * APP数据存储库
 */
public interface AppRepository {

    /**
     * 获取自己所有朋友
     * @return 朋友集合
     */
    Set<byte[]> getAllFriends();

    /**
     * 获取自己和朋友的最新消息列表
     * @param friendPk 朋友的公钥
     * @param num 查询的消息的条数
     * @return 消息列表
     */
    List<Message> getLatestMessageList(byte[] friendPk, int num);
}