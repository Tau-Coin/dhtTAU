package io.taucoin.torrent.publishing.core.storage.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.storage.entity.User;

/**
 * Room:User操作接口
 */
@Dao
public interface UserDao {
    String QUERY_GET_CURRENT_USER = "SELECT * FROM User WHERE isCurrentUser = 1";
    String QUERY_GET_USER_LIST = "SELECT * FROM User";
    String QUERY_GET_USERS_IN_BLACKLIST = "SELECT * FROM User where blacklist = 1";
    String QUERY_SET_CURRENT_USER = "UPDATE User SET isCurrentUser = :isCurrentUser WHERE publicKey = :publicKey";
    String QUERY_ADD_USER_BLACKLIST = "UPDATE User SET blacklist = :blacklist WHERE publicKey = :publicKey";

    /**
     * 添加新的User/Seed
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addUser(User user);

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
    @Query(QUERY_GET_USERS_IN_BLACKLIST)
    List<User> getUsersInBlacklist();

    /**
     * 设置用户是否加入黑名单
     */
    @Query(QUERY_ADD_USER_BLACKLIST)
    void setUserBlacklist(String publicKey, int blacklist);
}