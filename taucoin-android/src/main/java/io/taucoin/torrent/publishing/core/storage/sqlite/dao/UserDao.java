package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.model.data.UserAndMember;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;

/**
 * Room:User操作接口
 */
@Dao
public interface UserDao {
    String QUERY_GET_CURRENT_USER = "SELECT * FROM Users WHERE isCurrentUser = 1";
    String QUERY_GET_USER_LIST = "SELECT * FROM Users";
    String QUERY_GET_USERS_IN_BAN_LIST = "SELECT * FROM Users where isBanned = 1 and isCurrentUser != 1";
    String QUERY_GET_USERS_NOT_IN_BAN_LIST = "SELECT * FROM Users where isBanned = 0 and isCurrentUser != 1 ORDER BY lastUpdateTime DESC";
    String QUERY_SET_CURRENT_USER = "UPDATE Users SET isCurrentUser = :isCurrentUser WHERE publicKey = :publicKey";
    String QUERY_ADD_USER_BLACKLIST = "UPDATE Users SET isBanned = :isBanned WHERE publicKey = :publicKey";
    String QUERY_SEED_HISTORY_LIST = "SELECT * FROM Users WHERE isCurrentUser != 1 and seed not null";
    String QUERY_USER_BY_PUBLIC_KEY = "SELECT * FROM Users WHERE publicKey = :publicKey";

    /**
     * 添加新的User/Seed
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addUser(User user);

    /**
     * 添加新的多个User
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] addUsers(User... user);

    /**
     * 更新的User/Seed
     */
    @Update
    int updateUser(User user);

    /**
     * 获取当前的用户
     */
    @Query(QUERY_GET_CURRENT_USER)
    User getCurrentUser();

    @Query(QUERY_GET_USER_LIST)
    List<User> getUserList();

    /**
     * 观察当前用户信息是否变化
     */
    @Query(QUERY_GET_CURRENT_USER)
    Flowable<User> observeCurrentUser();

    /**
     * 设置当前用户是否是当前用户
     * @param isCurrentUser 是否是当前用户
     */
    @Query(QUERY_SET_CURRENT_USER)
    void setCurrentUser(String publicKey, boolean isCurrentUser);

    /**
     * 获取在黑名单的用户列表
     * @return  List<User>
     */
    @Query(QUERY_GET_USERS_IN_BAN_LIST)
    List<User> getUsersInBlacklist();

    /**
     * 设置用户是否加入黑名单
     */
    @Query(QUERY_ADD_USER_BLACKLIST)
    void setUserBlacklist(String publicKey, int isBanned);

    /**
     * 观察Sees历史列表
     */
    @Query(QUERY_SEED_HISTORY_LIST)
    Flowable<List<User>> observeSeedHistoryList();

    /**
     * 根据公钥获取用户
     * @param publicKey 公钥
     * @return 当前用户User实例
     */
    @Query(QUERY_USER_BY_PUBLIC_KEY)
    User getUserByPublicKey(String publicKey);

    /**
     * 观察不在黑名单的列表中
     */
    @Transaction
    @Query(QUERY_GET_USERS_NOT_IN_BAN_LIST)
    Flowable<List<UserAndMember>> observeUsersNotInBanList();

    /**
     * 获取用户和用户所在的社区信息
     */
    @Transaction
    @Query(QUERY_USER_BY_PUBLIC_KEY)
    UserAndMember getUserAndMember(String publicKey);
}