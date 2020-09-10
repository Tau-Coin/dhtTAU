package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

/**
 * TxRepository接口实现
 */
public class MsgRepositoryImpl implements MsgRepository{

    private Context appContext;
    private AppDatabase db;
    private PublishSubject<String> dataSetChangedPublish = PublishSubject.create();
    private ExecutorService sender = Executors.newSingleThreadExecutor();

    /**
     * CommunityRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public MsgRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 发送新的消息
     */
    @Override
    public void sendMessage(Message msg){
        db.msgDao().addMessage(msg);
        submitDataSetChanged();
    }

    /**
     * 根据chainID查询社区消息
     * @param chainID 社区链id
     */
    @Override
    public List<Message> getMessagesByChainID(String chainID){
        return db.msgDao().getMessagesByChainID(chainID);
    }

    /**
     * 根据msgID查询社区消息
     * @param msgID 社区消息id
     */
    @Override
    public Message getMessageByID(String msgID){
        return db.msgDao().getMessageByID(msgID);
    }

    @Override
    public int getNumMessages(String chainID) {
        return db.msgDao().getNumMessages(chainID);
    }

    @Override
    public List<MsgAndReply> getMessages(String chainID, int startPosition, int loadSize) {
        return db.msgDao().getMessages(chainID, startPosition, loadSize);
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
}
