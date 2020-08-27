package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.model.data.NotificationAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Notification;

/**
 * 提供操作Notification数据的接口
 */
public interface NotificationRepository {

    /**
     * 查询所有通知
     * @return DataSource.Factory
     */
    DataSource.Factory<Integer, NotificationAndUser> queryNotifications();

    /**
     * 删除通知
     */
    void deleteNotifications(NotificationAndUser... deleteList);

    /**
     * 添加通知
     */
    void addNotification(Notification notification);

    /**
     * 标记所有通知已读
     */
    void readAllNotifications();

    /**
     * 标记通知已读
     */
    void readNotification(String chainID, String senderPK);

    /**
     * 查询单个通知
     * @param senderPk
     * @param chainID
     * @return
     */
    Notification queryNotification(String senderPk, String chainID);

    /**
     * 查询未读通知的数目
     * @return Flowable
     */
    Flowable<Integer> queryUnreadNotificationsNum();
}
