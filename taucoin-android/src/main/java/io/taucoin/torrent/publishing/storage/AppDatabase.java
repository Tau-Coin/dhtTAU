package io.taucoin.torrent.publishing.storage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import io.taucoin.torrent.publishing.storage.dao.CommunityDao;
import io.taucoin.torrent.publishing.storage.dao.MemberDao;
import io.taucoin.torrent.publishing.storage.dao.UserDao;
import io.taucoin.torrent.publishing.storage.entity.Community;
import io.taucoin.torrent.publishing.storage.entity.Member;
import io.taucoin.torrent.publishing.storage.entity.User;

@Database(entities = {Community.class,
        Member.class,
        User.class
    }, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "tau.db";

    private static volatile AppDatabase INSTANCE;

    public abstract CommunityDao communityDao();
    public abstract MemberDao memberDao();
    public abstract UserDao userDao();

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