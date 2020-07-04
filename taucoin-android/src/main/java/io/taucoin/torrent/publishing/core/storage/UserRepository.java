package io.taucoin.torrent.publishing.core.storage;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.storage.entity.User;

/**
 * 提供外部操作User数据的接口
 */
public interface UserRepository {

    /**
     * 添加新的User/Seed
     * @param user User实例
     * @return 结果
     */
    long addUser(@NonNull User user);

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
}
