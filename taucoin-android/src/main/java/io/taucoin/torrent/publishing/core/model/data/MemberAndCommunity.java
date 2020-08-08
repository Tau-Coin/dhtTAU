package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;

/**
 * Room: 成员和社区联合查询
 */
public class MemberAndCommunity extends Member {
    @Relation(parentColumn = "chainID",
            entityColumn = "chainID")
    public Community community;              // 成员所在社区
    public String publicKey;

    public MemberAndCommunity(@NonNull String chainID, @NonNull String publicKey) {
        super(chainID, publicKey);
    }
}
