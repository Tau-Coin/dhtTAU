package io.taucoin.torrent.publishing.core.storage;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;

/**
 * TxRepository接口实现
 */
public class TxRepositoryImpl implements TxRepository{

    private Context appContext;
    private AppDatabase db;

    /**
     * CommunityRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    TxRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 添加新的交易
     */
    @Override
    public long addTransaction(Tx transaction){
        return db.txDao().addTransaction(transaction);
    }

    /**
     * 更新交易
     */
    @Override
    public int updateTransaction(Tx transaction){
        return db.txDao().updateTransaction(transaction);
    }

    /**
     * 根据chainID查询社区
     * @param chainID 社区链id
     * @param chat 区分聊天和链上交易
     */
    @Override
    public List<Tx> getTxsByChainID(String chainID, int chat){
        return db.txDao().getTxsByChainID(chainID, chat);
    }

    /**
     * 根据chainID获取社区的交易的被被观察者
     * @param chainID 社区链id
     * @param chat 区分聊天和链上交易
     */
    public Flowable<List<Tx>> observeTxsByChainID(String chainID, int chat){
        return db.txDao().observeTxsByChainID(chainID, chat);
    }
}
