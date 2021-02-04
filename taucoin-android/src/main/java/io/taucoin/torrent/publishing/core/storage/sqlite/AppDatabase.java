package io.taucoin.torrent.publishing.core.storage.sqlite;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.dao.ChatDao;
import io.taucoin.torrent.publishing.core.storage.sqlite.dao.CommunityDao;
import io.taucoin.torrent.publishing.core.storage.sqlite.dao.DeviceDao;
import io.taucoin.torrent.publishing.core.storage.sqlite.dao.FavoriteDao;
import io.taucoin.torrent.publishing.core.storage.sqlite.dao.FriendDao;
import io.taucoin.torrent.publishing.core.storage.sqlite.dao.MemberDao;
import io.taucoin.torrent.publishing.core.storage.sqlite.dao.MessageDao;
import io.taucoin.torrent.publishing.core.storage.sqlite.dao.NotificationDao;
import io.taucoin.torrent.publishing.core.storage.sqlite.dao.TxDao;
import io.taucoin.torrent.publishing.core.storage.sqlite.dao.UserDao;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Device;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Favorite;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Notification;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

@Database(entities = {Community.class,
        Member.class,
        User.class,
        Tx.class,
        Message.class,
        Favorite.class,
        Notification.class,
        Friend.class,
        ChatMsg.class,
        ChatMsgLog.class,
        Device.class
    }, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "tau.db";

    private static volatile AppDatabase INSTANCE;

    public abstract CommunityDao communityDao();
    public abstract MemberDao memberDao();
    public abstract UserDao userDao();
    public abstract TxDao txDao();
    public abstract MessageDao msgDao();
    public abstract FavoriteDao favoriteDao();
    public abstract NotificationDao notificationDao();
    public abstract FriendDao friendDao();
    public abstract ChatDao chatDao();
    public abstract DeviceDao deviceDao();

    /**
     * 获取数据库实例
     * @param appContext app上下文
     * @return AppDatabase
     */
    public static AppDatabase getInstance(@NonNull Context appContext) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null)
                    INSTANCE = buildDatabase(appContext);
            }
        }

        return INSTANCE;
    }

    /**
     * 构建数据库实例
     * @param appContext app上下文
     * @return AppDatabase
     */
    private static AppDatabase buildDatabase(Context appContext) {
        return Room.databaseBuilder(appContext, AppDatabase.class, DATABASE_NAME)
                .addMigrations(DatabaseMigration.getMigrations(appContext))
                .build();
    }
}