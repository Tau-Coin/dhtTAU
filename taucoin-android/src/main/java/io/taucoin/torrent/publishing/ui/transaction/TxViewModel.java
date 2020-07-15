package io.taucoin.torrent.publishing.ui.transaction;

import android.app.Application;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;
import com.github.naturs.logger.Logger;

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
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.TxRepository;
import io.taucoin.torrent.publishing.core.storage.UserRepository;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.entity.User;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.types.Comment;
import io.taucoin.types.CommunityAnnouncement;
import io.taucoin.types.DHTbootstrapNodeAnnouncement;
import io.taucoin.types.IdentityAnnouncement;
import io.taucoin.types.MsgType;
import io.taucoin.types.Note;
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

    /**
     * 添加新的交易
     * @param tx 根据用户输入构建的用户数据
     */
    public void addTransaction(Tx tx) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            // 获取当前用户的Seed, 获取公私钥
            User currentUser = userRepo.getCurrentUser();
            byte[] senderSeed = ByteUtil.toByte(currentUser.seed);
            Pair<byte[], byte[]> keypair = Ed25519.createKeypair(senderSeed);

            String result = "";
            try {
                TxData txData = buildChainTxData(tx);
                if(txData != null){
                    // TODO: 获取当前的交易最大nonce值
                    long nonce = 0;
                    nonce += 1;
                    long timestamp = DateUtil.getTime();
                    Transaction transaction = new Transaction((byte)1, tx.chainID.getBytes(), timestamp, (int)tx.fee, keypair.first, nonce, txData);
                    byte[] signature = Ed25519.sign(transaction.getSigEncoded(), keypair.first, keypair.second);
                    transaction.setSignature(signature);
                    // TODO: 把交易数据transaction.getEncoded()提交给链端
                    // 保存数据到本地数据库
                    tx.txID = ByteUtil.toHexString(transaction.getTxID());
                    tx.timestamp = timestamp;
                    tx.senderPk = currentUser.publicKey;
                    tx.nonce = nonce;
                    txRepo.addTransaction(tx);
                }else{
                    result = getApplication().getString(R.string.tx_error_type);
                }
            }catch (Exception e){
                result = e.getMessage();
                Logger.d("Error adding transaction::%s", result);
            }

            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> addState.postValue(state));
        disposables.add(disposable);
    }

    /**
     * 根据不同交易类型构建不同的链上数据
     * @param tx 交易数据
     */
    private TxData buildChainTxData(Tx tx){
        TxData txData = null;
        MsgType msgType = MsgType.setValue((byte) tx.txType);
        if(msgType != null){
            switch (msgType){
                case RegularForum:
                    Note note = new Note(tx.memo);
                    txData = new TxData(msgType, note.getTxCode());
                    break;
                case ForumComment:
                    byte[] replyID = ByteUtil.toByte(tx.replyID);
                    Comment comment = new Comment(replyID, tx.memo);
                    txData = new TxData(msgType, comment.getEncode());
                    break;
                case CommunityAnnouncement:
                    byte[] annChainID = tx.chainID.getBytes();
                    byte[] genesisPk = ByteUtil.toByte(tx.genesisPk);
                    CommunityAnnouncement communityAnn = new CommunityAnnouncement(annChainID, genesisPk, tx.memo);
                    txData = new TxData(msgType, communityAnn.getEncode());
                    break;
                case DHTBootStrapNodeAnnouncement:
                    String[] bootNodes = tx.memo.split(",");
                    DHTbootstrapNodeAnnouncement dhTBootstrapNodeAnn = new DHTbootstrapNodeAnnouncement(new byte[64], bootNodes);
                    txData = new TxData(msgType, dhTBootstrapNodeAnn.getEncode());
                    break;
                case Wiring:
                    byte[] receiverPk = ByteUtil.toByte(tx.receiverPk);
                    WireTransaction wireTx = new WireTransaction(receiverPk, tx.amount, tx.memo);
                    txData = new TxData(msgType, wireTx.getEncode());
                    break;
                case IdentityAnnouncement:
                    IdentityAnnouncement identityAnn = new IdentityAnnouncement(new byte[32], tx.name);
                    txData = new TxData(msgType, identityAnn.getEncode());
                    break;
            }
        }
        return txData;
    }

    /**
     * 验证交易
     * @param tx 交易数据
     */
    boolean validateTx(Tx tx) {
        int msgType = tx.txType;
        if(msgType == MsgType.DHTBootStrapNodeAnnouncement.getVaLue()){
            if(StringUtil.isEmpty(tx.memo)){
                ToastUtils.showShortToast(R.string.tx_error_invalid_bootstrap);
                return false;
            }else{
                String[] bootstraps = tx.memo.split(",");
                if(bootstraps.length == 0){
                    ToastUtils.showShortToast(R.string.tx_error_invalid_bootstrap);
                    return false;
                }
            }
        }else if(msgType == MsgType.Wiring.getVaLue()){
            // TODO: 获取当前用户的余额
            long balance = 100000000000L;
            if(StringUtil.isEmpty(tx.receiverPk) ||
                    ByteUtil.toByte(tx.receiverPk).length != Ed25519.PUBLIC_KEY_SIZE){
                ToastUtils.showShortToast(R.string.tx_error_invalid_pk);
                return false;
            }else if(tx.amount <= 0){
                ToastUtils.showShortToast(R.string.tx_error_invalid_fee);
                return false;
            }else if(tx.amount > balance || tx.amount + tx.fee > balance){
                ToastUtils.showShortToast(R.string.tx_error_no_enough_coins);
                return false;
            }
        }else if(msgType == MsgType.IdentityAnnouncement.getVaLue()){
            if(StringUtil.isEmpty(tx.name)){
                ToastUtils.showShortToast(R.string.tx_error_invalid_name);
                return false;
            }
        }
        return true;
    }
}
