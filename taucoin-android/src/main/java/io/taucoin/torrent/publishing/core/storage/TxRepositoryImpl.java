package io.taucoin.torrent.publishing.core.storage;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.core.model.data.ReplyAndAllTxs;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;
import io.taucoin.types.MsgType;

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
     */
    @Override
    public List<ReplyAndAllTxs> getTxsByChainID(String chainID){
        return db.txDao().getTxsByChainID(MsgType.IdentityAnnouncement.getVaLue(), chainID);
    }

    /**
     * 根据chainID获取社区的交易的被被观察者
     * @param chainID 社区链id
     */
    @Override
    public Flowable<List<ReplyAndAllTxs>> observeTxsByChainID(String chainID){
        return db.txDao().observeTxsByChainID(MsgType.IdentityAnnouncement.getVaLue(), chainID);
    }

    @Override
    public DataSource.Factory<Integer, ReplyAndAllTxs> queryCommunityTxs(String chainID){
        return db.txDao().queryCommunityTxs(MsgType.IdentityAnnouncement.getVaLue(), chainID);
    }
}
