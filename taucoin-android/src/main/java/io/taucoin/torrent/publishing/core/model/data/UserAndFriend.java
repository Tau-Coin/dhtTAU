package io.taucoin.torrent.publishing.core.model.data;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 用户和朋友联合查询
 */
public class UserAndFriend extends User {
    @Relation(parentColumn = "publicKey",
            entityColumn = "publicKey")
    public List<Member> members;                  // 所在社区列表
    public long lastCommTime;
    public int state;

    public UserAndFriend(@NonNull String publicKey) {
        super(publicKey);
    }

    public boolean isDiscovered() {
        return state == 0;
    }

    public boolean isAdded() {
        return state == 1;
    }

    public boolean isConnected() {
        return state == 2;
    }
}