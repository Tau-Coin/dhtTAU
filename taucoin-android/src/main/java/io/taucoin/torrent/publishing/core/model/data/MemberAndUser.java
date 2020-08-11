package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 用户和成员联合查询
 */
public class MemberAndUser extends Member {
    @Relation(parentColumn = "publicKey",
            entityColumn = "publicKey")
    public User user;


    public MemberAndUser(@NonNull String chainID, @NonNull String publicKey) {
        super(chainID, publicKey);
    }
}
