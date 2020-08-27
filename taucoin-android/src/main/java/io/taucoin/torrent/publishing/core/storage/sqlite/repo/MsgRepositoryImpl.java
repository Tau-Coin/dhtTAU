package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;

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
    public MsgRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
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
     * 根据msgID查询社区消息
     * @param msgID 社区消息id
     */
    @Override
    public Message getMessageByID(String msgID){
        return db.msgDao().getMessageByID(msgID);
    }

    /**
     * 根据chainID获取社区的消息
     * @param chainID 社区链id
     */
    @Override
    public DataSource.Factory<Integer, MsgAndReply> queryMessagesByChainID(String chainID){
        return db.msgDao().queryMessagesByChainID(chainID);
    }
}
