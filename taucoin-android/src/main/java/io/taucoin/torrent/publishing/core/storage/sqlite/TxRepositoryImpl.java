package io.taucoin.torrent.publishing.core.storage.sqlite;

import android.content.Context;

import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

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
    public List<UserAndTx> getTxsByChainID(String chainID){
        return db.txDao().getTxsByChainID(chainID);
    }

    /**
     * 根据chainID获取社区的交易的被被观察者
     * @param chainID 社区链id
     */
    @Override
    public Flowable<List<UserAndTx>> observeTxsByChainID(String chainID, int txType){
        return db.txDao().observeTxsByChainID(chainID, txType);
    }

    @Override
    public DataSource.Factory<Integer, UserAndTx> queryCommunityTxs(String chainID, int txType){
        return db.txDao().queryCommunityTxs(chainID, txType);
    }

    /**
     * 获取社区里用户未上链并且未过期的交易数
     * @param chainID chainID
     * @param senderPk 公钥
     * @param expireTime 过期时间时长
     * @return int
     */
    @Override
    public int getPendingTxsNotExpired(String chainID, String senderPk, long expireTime){
        long expireTimePoint = DateUtil.getTime() - expireTime;
        return db.txDao().getPendingTxsNotExpired(chainID, senderPk, expireTimePoint);
    }

    /**
     * 获取社区里用户未上链并且过期的最早的交易
     * @param chainID chainID
     * @param senderPk 公钥
     * @param expireTime 过期时间时长
     * @return int
     */
    @Override
    public Tx getEarliestExpireTx(String chainID, String senderPk, long expireTime){
        long expireTimePoint = DateUtil.getTime() - expireTime;
        return db.txDao().getEarliestExpireTx(chainID, senderPk, expireTimePoint);
    }

    /**
     * 根据txID查询交易
     * @param txID 交易ID
     */
    public Single<Tx> getTxByTxIDSingle(String txID){
        return db.txDao().getTxByTxIDSingle(txID);
    }

    /**
     * 观察中位数交易费
     * @param chainID 交易所属的社区chainID
     */
    public Single<List<Long>> observeMedianFee(String chainID){
        return db.txDao().observeMedianFee(chainID);
    }
}
