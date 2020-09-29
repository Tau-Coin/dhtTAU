package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.model.data.UserAndMember;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

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

    @Override
    public Flowable<String> observeCurrentUserSeed() {
        return db.userDao().observeCurrentUserSeed();
    }

    /**
     * 设置当前用户是否是当前用户
     * @param isCurrentUser 是否是当前用户
     */
    @Override
    public void setCurrentUser(String publicKey, boolean isCurrentUser){
        db.userDao().setCurrentUser(publicKey, isCurrentUser);
    }

    /**
     * 获取在黑名单的用户列表
     * @return  List<User>
     */
    @Override
    public List<User> getUsersInBlacklist(){
        return db.userDao().getUsersInBlacklist();
    }

    /**
     * 设置用户是否加入黑名单
     * @param publicKey 公钥
     * @param blacklist 是否加入黑名单
     */
    @Override
    public void setUserBlacklist(String publicKey, boolean blacklist){
        db.userDao().setUserBlacklist(publicKey, blacklist ? 1 : 0);
    }

    /**
     * 观察Sees历史列表
     */
    @Override
    public Flowable<List<User>> observeSeedHistoryList(){
        return db.userDao().observeSeedHistoryList();
    }

    /**
     * 根据公钥获取用户
     * @param publicKey 公钥
     * @return 当前用户User实例
     */
    @Override
    public User getUserByPublicKey(String publicKey){
        return db.userDao().getUserByPublicKey(publicKey);
    }

    /**
     * 添加新的多个User
     */
    @Override
    public long[] addUsers(User... user){
        return db.userDao().addUsers(user);
    }

    /**
     * 观察不在黑名单的列表中
     * @param order
     */
    @Override
    public Flowable<List<UserAndMember>> observeUsersNotInBanList(int order){
        return db.userDao().observeUsersNotInBanList(order == 0 ? "lastUpdateTime" : "lastCommTime");
    }

    @Override
    public DataSource.Factory<Integer, UserAndMember> queryUsers(int order) {
        return db.userDao().queryUsers(order == 0 ? "lastUpdateTime" : "lastCommTime");
    }

    /**
     * 获取用户和用户所在的社区信息
     */
    @Override
    public UserAndMember getUserAndMember(String publicKey){
        return db.userDao().getUserAndMember(publicKey);
    }
}
