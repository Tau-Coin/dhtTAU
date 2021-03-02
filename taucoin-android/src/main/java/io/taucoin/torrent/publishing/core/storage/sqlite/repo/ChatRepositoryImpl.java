package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgAndUser;
import io.taucoin.torrent.publishing.core.model.data.ChatMsgStatus;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

/**
 * FriendRepository接口实现
 */
public class ChatRepositoryImpl implements ChatRepository{

    private Context appContext;
    private AppDatabase db;
    private PublishSubject<String> dataSetChangedPublish = PublishSubject.create();
    private ExecutorService sender = Executors.newSingleThreadExecutor();

    /**
     * FriendRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public ChatRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    @Override
    public void addChatMsg(ChatMsg chat) {
        db.chatDao().addChat(chat);
        submitDataSetChanged();
    }

    @Override
    public void addChatMessages(ChatMsg... chats) {
        db.chatDao().addChats(chats);
        submitDataSetChanged();
    }

    @Override
    public void updateChatMsg(ChatMsg chat) {
        db.chatDao().updateChat(chat);
        submitDataSetChanged();
    }

    @Override
    public ChatMsg queryChatMsg(String friendPk, String hash) {
        return db.chatDao().queryChatMsg(friendPk, hash);
    }

    /**
     * 查询ChatMsg
     * @param hash
     * @return
     */
    @Override
    public ChatMsg queryChatMsg(String hash) {
        return db.chatDao().queryChatMsg(hash);
    }

    @Override
    public Observable<String> observeDataSetChanged() {
        return dataSetChangedPublish;
    }

    @Override
    public void submitDataSetChanged() {
        String dateTime = DateUtil.getDateTime();
        sender.submit(() -> dataSetChangedPublish.onNext(dateTime));
    }

    @Override
    public int getNumMessages(String friendPk) {
        String senderPk = MainApplication.getInstance().getPublicKey();
        return db.chatDao().getNumMessages(senderPk, friendPk);
    }

    @Override
    public List<ChatMsgAndUser> getMessages(String friendPk, int startPosition, int loadSize) {
        String senderPk = MainApplication.getInstance().getPublicKey();
        return db.chatDao().getMessages(senderPk, friendPk, startPosition, loadSize);
    }

    /**
     * 获取未入队列的消息
     * @return
     */
    @Override
    public List<ChatMsg> getUnsentMessages() {
        return db.chatDao().getUnsentMessages();
    }

    /**
     * 添加消息日志
     * @param msgLog
     */
    @Override
    public long addChatMsgLog(ChatMsgLog msgLog) {
        return db.chatDao().addChatMsgLog(msgLog);
    }

    /**
     * 观察消息日志
     * @param hash
     * @return
     */
    @Override
    public Observable<List<ChatMsgLog>> observerMsgLogs(String hash) {
        return db.chatDao().observerMsgLogs(hash);
    }
}
