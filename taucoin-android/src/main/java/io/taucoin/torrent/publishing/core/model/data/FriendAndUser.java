package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 查询Community, 返回社区成员的信息
 */
public class FriendAndUser extends Friend {

    @Relation(parentColumn = "friendPK",
            entityColumn = "publicKey")
    public User user;           // 朋友信息

    public FriendAndUser(@NonNull String userPK, @NonNull String friendPK) {
        super(userPK, friendPK);
    }
}
