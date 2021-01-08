package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;

/**
 * FriendRepository接口实现
 */
public class FriendRepositoryImpl implements FriendRepository{

    private Context appContext;
    private AppDatabase db;

    /**
     * FriendRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public FriendRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    @Override
    public void addFriend(Friend friend) {
        db.friendDao().addFriend(friend);
    }

    @Override
    public void updateFriend(Friend friend) {
        db.friendDao().updateFriend(friend);
    }

    @Override
    public Friend queryFriend(String userPK, String friendPK) {
        return db.friendDao().queryFriend(userPK, friendPK);
    }

    /**
     * 查询已获得连接的朋友列表
     * @param userPK
     * @param limit
     * @return
     */
    @Override
    public List<String> queryConnectedFriends(String userPK, int limit) {
        return db.friendDao().queryConnectedFriends(userPK, limit);
    }
}
