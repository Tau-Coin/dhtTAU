package io.taucoin.torrent.publishing.core.storage.sqlite.repo;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.AppDatabase;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.DateUtil;

/**
 * TxRepository接口实现
 */
public class TxRepositoryImpl implements TxRepository{

    private Context appContext;
    private AppDatabase db;
    private PublishSubject<String> dataSetChangedPublish = PublishSubject.create();
    private ExecutorService sender = Executors.newSingleThreadExecutor();

    /**
     * CommunityRepositoryImpl 构造函数
     * @param appContext 上下文
     * @param db 数据库实例
     */
    public TxRepositoryImpl(@NonNull Context appContext, @NonNull AppDatabase db) {
        this.appContext = appContext;
        this.db = db;
    }

    /**
     * 添加新的交易
     */
    @Override
    public long addTransaction(Tx transaction){
        long result = db.txDao().addTransaction(transaction);
        submitDataSetChanged();
        return result;
    }

    /**
     * 更新交易
     */
    @Override
    public int updateTransaction(Tx transaction){
        int result = db.txDao().updateTransaction(transaction);
        submitDataSetChanged();
        return result;
    }

//    /**
//     * 根据chainID获取社区的交易的被被观察者
//     * @param chainID 社区链id
//     */
//    @Override
//    public DataSource.Factory<Integer, UserAndTx> queryCommunityTxs(String chainID, long txType, int txStatus){
//        if(txType == -1){
//            return db.txDao().queryCommunityTxsNotOnChain(chainID);
//        }else{
//            return db.txDao().queryCommunityTxsOnChain(chainID, txType);
//        }
//    }

    /**
     * 根据chainID查询社区交易
     * @param chainID 社区链ID
     */
    @Override
    public int queryNumCommunityTxs(String chainID, long txType){
        if(txType == -1){
            return db.txDao().queryNumCommunityTxsNotOnChain(chainID, 0);
        }else{
            return db.txDao().queryNumCommunityTxsOnChain(chainID, txType, 1);
        }
    }

    /**
     * 根据chainID查询社区交易
     * @param chainID 社区链ID
     */
    @Override
    public List<UserAndTx> queryCommunityTxs(String chainID, long txType, int startPos, int loadSize){
        if(txType == -1){
            return db.txDao().queryCommunityTxsNotOnChain(chainID, 0, startPos, loadSize);
        }else{
            return db.txDao().queryCommunityTxsOnChain(chainID, txType, 1, startPos, loadSize);
        }
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
    @Override
    public Single<Tx> getTxByTxIDSingle(String txID){
        return db.txDao().getTxByTxIDSingle(txID);
    }

    /**
     * 根据txID查询交易
     * @param txID 交易ID
     */
    @Override
    public Tx getTxByTxID(String txID){
        return db.txDao().getTxByTxID(txID);
    }

    /**
     * 观察中位数交易费
     * @param chainID 交易所属的社区chainID
     */
    @Override
    public Single<List<Long>> observeMedianFee(String chainID){
        return db.txDao().observeMedianFee(chainID);
    }

    /**
     * 获取中位数交易费
     * @param chainID 交易所属的社区chainID
     */
    @Override
    public List<Long> getMedianFee(String chainID){
        return db.txDao().getMedianFee(chainID);
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
