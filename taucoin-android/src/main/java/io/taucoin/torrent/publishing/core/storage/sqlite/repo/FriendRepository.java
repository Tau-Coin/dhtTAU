package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;

/**
 * 提供操作Friend数据的接口
 */
public interface FriendRepository {

    /**
     * 添加朋友关系
     */
    void addFriend(Friend friend);

    /**
     * 更新朋友关系
     */
    void updateFriend(Friend friend);

    /**
     * 查询朋友关系
     * @param userPK
     * @param friendPK
     * @return
     */
    Friend queryFriend(String userPK, String friendPK);

}
