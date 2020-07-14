package io.taucoin.torrent.publishing.ui.transaction;

import android.app.Application;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.TxRepository;
import io.taucoin.torrent.publishing.core.storage.UserRepository;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.entity.User;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.types.MsgType;
import io.taucoin.types.Transaction;
import io.taucoin.types.TxData;
import io.taucoin.types.WireTransaction;
import io.taucoin.util.ByteUtil;

/**
 * 交易模块相关的ViewModel
 */
public class TxViewModel extends AndroidViewModel {

    private TxRepository txRepo;
    private UserRepository userRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<List<Tx>> chainTxs = new MutableLiveData<>();
    private MutableLiveData<String> addState = new MutableLiveData<>();
    public TxViewModel(@NonNull Application application) {
        super(application);
        txRepo = RepositoryHelper.getTxRepository(application);
        userRepo = RepositoryHelper.getUserRepository(application);
    }

    public MutableLiveData<String> getAddState() {
        return addState;
    }

    public void setAddState(MutableLiveData<String> addState) {
        this.addState = addState;
    }

    /**
     * 获取社区链交易的被观察者
     * @return 被观察者
     */
    public MutableLiveData<List<Tx>> getChainTxs() {
        return chainTxs;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    /**
     * 根据chainID查询社区的交易
     * @param chainID 社区链id
     * @param chat 区分聊天和链上交易
     */
    public void getTxsByChainID(String chainID, int chat){
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<Tx>>) emitter -> {
            List<Tx> txs = txRepo.getTxsByChainID(chainID, chat);
            emitter.onNext(txs);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> chainTxs.postValue(state));
        disposables.add(disposable);
    }

    /**
     * 根据chainID获取社区的交易的被被观察者
     * @param chainID 社区链id
     * @param chat 区分聊天和链上交易
     */
    public Flowable<List<Tx>> observeTxsByChainID(String chainID, int chat){
        return txRepo.observeTxsByChainID(chainID, chat);
    }

    public void addTransaction(Tx tx) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            // 获取当前用户
            User currentUser = userRepo.getCurrentUser();
            byte[] receiverPubKey = ByteUtil.toByte(tx.receiverPubKey);
            byte[] senderSeed = ByteUtil.toByte(currentUser.seed);
            Pair<byte[], byte[]> keypair = Ed25519.createKeypair(senderSeed);
            WireTransaction wireTransaction = new WireTransaction(receiverPubKey, tx.amount, tx.memo);
            TxData txData = new TxData(MsgType.Wiring, wireTransaction.getEncode());
            long timestamp = DateUtil.getTime();
            long nonce = 0;
            Transaction transaction = new Transaction((byte)1, tx.chainID.getBytes(),timestamp, (int)tx.fee, keypair.first, nonce, txData);
            byte[] signature = Ed25519.sign(transaction.getSigEncoded(), keypair.first, keypair.second);
            transaction.setSignature(signature);
            tx.txID = ByteUtil.toHexString(transaction.getTxID());
            tx.timestamp = timestamp;
            tx.senderPubKey = currentUser.publicKey;
            tx.nonce = nonce;
            txRepo.addTransaction(tx);
            emitter.onNext("");
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> addState.postValue(state));
        disposables.add(disposable);
    }
}
