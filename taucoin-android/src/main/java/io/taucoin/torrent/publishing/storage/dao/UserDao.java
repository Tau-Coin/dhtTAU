package io.taucoin.torrent.publishing.storage.dao;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.storage.entity.User;

/**
 * Room:User操作接口
 */
@Dao
public interface UserDao {
    String QUERY_GET_CURRENT_USER = "SELECT * FROM User WHERE isCurrentUser = 1";
    String QUERY_GET_USER_LIST = "SELECT * FROM User";

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
}