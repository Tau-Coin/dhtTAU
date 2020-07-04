package io.taucoin.torrent.publishing.core.storage;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.storage.entity.User;

/**
 * UserRepository接口实现
 */
public class UserRepositoryImpl implements UserRepository{

    private Context appContext;
    private AppDatabase db;

    /**
     * CommunityRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public UserRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 添加新的User/Seed
     * @param user User实例
     * @return 结果
     */
    @Override
    public long addUser(@NonNull User user) {
        return db.userDao().addUser(user);
    }

    /**
     * 更新的User/Seed
     * @param user User实例
     * @return 结果
     */
    @Override
    public int updateUser(@NonNull User user) {
        return db.userDao().updateUser(user);
    }

    /**
     * 获取当前的用户
     * @return 当前用户User实例
     */
    @Override
    public User getCurrentUser() {
        return db.userDao().getCurrentUser();
    }

    @Override
    public List<User> getUserList() {
        return db.userDao().getUserList();
    }

    /**
     * 观察当前用户信息是否变化
     * @return 当前用户最新User实例
     */
    @Override
    public Flowable<User> observeCurrentUser() {
        return db.userDao().observeCurrentUser();
    }
}
