package io.taucoin.torrent.publishing.core.model.data;

import androidx.annotation.NonNull;
import androidx.room.Relation;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Notification;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room: 通知和成员联合查询
 */
public class NotificationAndUser extends Notification {
    @Relation(parentColumn = "senderPk",
            entityColumn = "publicKey")
    public User user;

    @Relation(parentColumn = "chainID",
            entityColumn = "chainID")
    public Community community;

    public NotificationAndUser(@NonNull String senderPk, @NonNull String chainLink, @NonNull String chainID, long timestamp) {
        super(senderPk, chainLink, chainID, timestamp);
    }
}
