package io.taucoin.torrent.publishing.core.model.data;

import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 查询Community, 返回社区成员的信息
 */
public class FriendAndUser extends Community {

    public int state;            // 朋友状态

    @Relation(parentColumn = "chainID",
            entityColumn = "publicKey")
    public User user;           // 朋友信息
}
