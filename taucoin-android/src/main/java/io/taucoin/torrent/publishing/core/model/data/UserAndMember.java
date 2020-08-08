package io.taucoin.torrent.publishing.core.model.data;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 用户和成员联合查询
 */
public class UserAndMember extends User {
    @Relation(parentColumn = "publicKey",
            entityColumn = "publicKey")
    public List<Member> members;                  // 所在社区列表

    public UserAndMember(@NonNull String publicKey) {
        super(publicKey);
    }
}
