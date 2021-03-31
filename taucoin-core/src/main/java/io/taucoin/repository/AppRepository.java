package io.taucoin.repository;

import java.util.List;
import java.util.Set;

import io.taucoin.core.FriendInfo;
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

    /**
     * 获取和自己正在聊天的朋友
     * @return 朋友公钥
     */
    byte[] getChattingFriend();

    /**
     * 获取主循环的时间间隔
     * @return 时间间隔
     */
    int getMainLoopInterval();

    /**
     * 获取朋友的信息
     * 朋友的公钥
     * @return 朋友信息类
     */
    FriendInfo getFriendInfo(byte[] friendPk);


    /**
     * 获取活跃的朋友
     * @return 活跃的朋友列表
     */
    List<byte[]> getActiveFriends();
}
