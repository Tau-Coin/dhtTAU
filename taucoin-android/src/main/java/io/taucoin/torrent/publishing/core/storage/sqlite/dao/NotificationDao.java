package io.taucoin.torrent.publishing.core.storage.sqlite.dao;

import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.model.data.NotificationAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Notification;

/**
 * Room:Notification操作接口
 */
@Dao
public interface NotificationDao {
    String QUERY_NOTIFICATIONS = "SELECT * FROM Notifications ORDER BY timestamp DESC";
    String QUERY_NOTIFICATION = "SELECT * FROM Notifications" +
            " WHERE senderPk = :senderPk AND chainID = :chainID";

    String QUERY_UNREAD_NOTIFICATIONS_NUM = "SELECT count(*) FROM Notifications WHERE isRead = 0";

    String QUERY_READ_ALL_NOTIFICATIONS = "UPDATE Notifications SET isRead = 1";
    String QUERY_READ_NOTIFICATION = QUERY_READ_ALL_NOTIFICATIONS +
            " WHERE senderPk = :senderPk AND chainID = :chainID";

    /**
     * 添加新社区成员
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addNotification(Notification notification);

    @Query(QUERY_READ_ALL_NOTIFICATIONS)
    void readAllNotifications();

    @Query(QUERY_READ_NOTIFICATION)
    void readNotification(String senderPk, String chainID);

    @Delete
    void deleteNotifications(Notification... notifications);

    @Query(QUERY_NOTIFICATIONS)
    @Transaction
    DataSource.Factory<Integer, NotificationAndUser> queryNotifications();

    @Query(QUERY_NOTIFICATION)
    Notification queryNotification(String senderPk, String chainID);

    @Query(QUERY_UNREAD_NOTIFICATIONS_NUM)
    Flowable<Integer> queryUnreadNotificationsNum();
}
