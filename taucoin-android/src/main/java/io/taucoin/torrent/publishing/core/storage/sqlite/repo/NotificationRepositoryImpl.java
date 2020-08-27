package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.model.data.NotificationAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Notification;

/**
 * NotificationRepository接口实现
 */
public class NotificationRepositoryImpl implements NotificationRepository{

    private Context appContext;
    private AppDatabase db;

    /**
     * MemberRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public NotificationRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    @Override
    public DataSource.Factory<Integer, NotificationAndUser> queryNotifications() {
        return db.notificationDao().queryNotifications();
    }

    @Override
    public void deleteNotifications(NotificationAndUser... deleteList) {
        db.notificationDao().deleteNotifications(deleteList);
    }

    @Override
    public void addNotification(Notification notification){
        db.notificationDao().addNotification(notification);
    }

    @Override
    public void readAllNotifications() {
        db.notificationDao().readAllNotifications();
    }

    @Override
    public void readNotification(String senderPk, String chainID) {
        db.notificationDao().readNotification(senderPk, chainID);
    }

    @Override
    public Notification queryNotification(String senderPk, String chainID) {
        return db.notificationDao().queryNotification(senderPk, chainID);
    }

    @Override
    public Flowable<Integer> queryUnreadNotificationsNum() {
        return db.notificationDao().queryUnreadNotificationsNum();
    }
}