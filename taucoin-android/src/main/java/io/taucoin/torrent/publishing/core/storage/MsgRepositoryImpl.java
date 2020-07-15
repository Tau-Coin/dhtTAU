package io.taucoin.torrent.publishing.core.storage;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.storage.entity.Message;

/**
 * TxRepository接口实现
 */
public class MsgRepositoryImpl implements MsgRepository{

    private Context appContext;
    private AppDatabase db;

    /**
     * CommunityRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    MsgRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 发送新的消息
     */
    @Override
    public long sendMessage(Message msg){
        return db.msgDao().addMessage(msg);
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
     * 根据chainID获取社区的消息的被观察者
     * @param chainID 社区链id
     */
    @Override
    public Flowable<List<Message>> observeMessagesByChainID(String chainID){
        return db.msgDao().observeMessagesByChainID(chainID);
    }
}
