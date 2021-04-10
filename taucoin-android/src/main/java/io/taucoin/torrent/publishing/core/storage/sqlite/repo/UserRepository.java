package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * 提供外部操作User数据的接口
 */
public interface UserRepository {

    /**
     * 添加新的User/Seed
     * @param user User实例
     * @return 结果
     */
    void addUser(@NonNull User user);

    /**
     * 更新的User/Seed
     * @param user User实例
     * @return 结果
     */
    int updateUser(@NonNull User user);

    /**
     * 获取当前的用户
     * @return 当前用户User实例
     */
    User getCurrentUser();

    List<User> getUserList();

    /**
     * 观察当前用户信息是否变化
     * @return 当前用户最新User实例
     */
    Flowable<User> observeCurrentUser();

    /**
     * 观察当前用户Seed变化
     * @return seed
     */
    Flowable<String> observeCurrentUserSeed();

    /**
     * 设置当前用户是否是当前用户
     * @param isCurrentUser 是否是当前用户
     */
    void setCurrentUser(String publicKey, boolean isCurrentUser);

    /**
     * 获取在黑名单的用户列表
     * @return  List<User>
     */
    List<User> getUsersInBlacklist();

    /**
     * 设置用户是否加入黑名单
     * @param publicKey 公钥
     * @param blacklist 是否加入黑名单
     */
    void setUserBlacklist(String publicKey, boolean blacklist);

    /**
     * 观察Sees历史列表
     */
    Flowable<List<User>> observeSeedHistoryList();

    /**
     * 根据公钥获取用户
     * @param publicKey 公钥
     * @return 当前用户User实例
     */
    User getUserByPublicKey(String publicKey);

    /**
     * 添加新的多个User
     */
    long[] addUsers(User... user);

    /**
     * 观察不在黑名单的用户数
     * @param isAll
     */
    int getNumUsers(boolean isAll, String friendPk);

    /**
     * 观察不在黑名单的用户列表
     */
    List<UserAndFriend> getUsers(boolean isAll, int order, String friendPk, int pos, int loadSize);

    /**
     * 获取用户和朋友的信息
     */
    UserAndFriend getFriend(String publicKey);

    /**
     * 观察朋友信息变化
     */
    Flowable<Friend> observeFriend(String friendPk);

    /**
     * 观察用户的消息的变化
     */
    Observable<String> observeDataSetChanged();

    /**
     * 提交数据变化
     */
    void submitDataSetChanged();
}
