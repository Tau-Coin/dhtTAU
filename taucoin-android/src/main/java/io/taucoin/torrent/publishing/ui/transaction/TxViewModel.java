package io.taucoin.torrent.publishing.ui.transaction;

import android.app.Application;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.EditFeeDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.Chain;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
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

    private static final Logger logger = LoggerFactory.getLogger("TxViewModel");
    private TxRepository txRepo;
    private UserRepository userRepo;
    private TauDaemon daemon;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<List<UserAndTx>> chainTxs = new MutableLiveData<>();
    private MutableLiveData<String> addState = new MutableLiveData<>();
    private CommonDialog editFeeDialog;
    public TxViewModel(@NonNull Application application) {
        super(application);
        txRepo = RepositoryHelper.getTxRepository(application);
        userRepo = RepositoryHelper.getUserRepository(application);
        daemon  = TauDaemon.getInstance(application);
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
    public MutableLiveData<List<UserAndTx>> getChainTxs() {
        return chainTxs;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        if(editFeeDialog != null){
            editFeeDialog.closeDialog();
        }
    }

    /**
     * 根据chainID查询社区的交易
     * @param chainID 社区链id
     */
    public void getTxsByChainID(String chainID){
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<UserAndTx>>) emitter -> {
            List<UserAndTx> txs = txRepo.getTxsByChainID(chainID);
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
     */
    public Flowable<List<UserAndTx>> observeTxsByChainID(String chainID){
        return txRepo.observeTxsByChainID(chainID);
    }

    /**
     * 根据chainID获取社区的交易的被被观察者
     * @param chainID 社区链id
     */
    public DataSource.Factory<Integer, UserAndTx> queryCommunityTxs(String chainID){
        return txRepo.queryCommunityTxs(chainID);
    }


    /**
     * 添加新的交易
     * @param tx 根据用户输入构建的用户数据
     */
    void addTransaction(Tx tx) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            // 获取当前用户的Seed, 获取公私钥
            User currentUser = userRepo.getCurrentUser();
            byte[] senderSeed = ByteUtil.toByte(currentUser.seed);
            byte[] senderPk = ByteUtil.toByte(currentUser.publicKey);

            String result = "";
            try {
                TxData txData = buildChainTxData(tx);
                if(txData != null){
                    // 获取当前用户在社区中链上nonce值
                    long nonce = daemon.getUserBalance(tx.chainID, currentUser.publicKey);
                    // 获取最早过期的交易，并且其nonce后来未被使用
                    Tx earliestExpireTx = txRepo.getEarliestExpireTx(tx.chainID, currentUser.publicKey, Chain.EXPIRE_TIME);
                    if(earliestExpireTx != null){
                        logger.debug("chain nonce::{}, earliestExpireTx.nonce::{}", nonce, earliestExpireTx.nonce);
                        nonce = earliestExpireTx.nonce;
                    }else{
                        // 获取社区里用户未上链并且未过期的交易数
                        int pendingTxs = txRepo.getPendingTxsNotExpired(tx.chainID, currentUser.publicKey, Chain.EXPIRE_TIME);
                        logger.debug("chain nonce::{}, pending txs::{}", nonce, pendingTxs);
                        nonce = nonce == 0 ? 0 : nonce + 1;
                        if(pendingTxs > 0){
                            nonce += pendingTxs;
                        }
                    }
                    // 交易签名
                    long timestamp = DateUtil.getTime();
                    byte[] chainID = ByteUtil.toByte(tx.chainID);
                    Transaction transaction = new Transaction((byte)1, chainID, timestamp, (int)tx.fee, senderPk, nonce, txData);
                    Pair<byte[], byte[]> keypair = Ed25519.createKeypair(senderSeed);
                    byte[] privateKey = keypair.second;
                    transaction.signTransaction(privateKey);
                    // 把交易数据transaction.getEncoded()提交给链端
                    daemon.submitTransaction(transaction);
                    // 保存交易数据到本地数据库
                    tx.txID = ByteUtil.toHexString(transaction.getTxID());
                    tx.timestamp = timestamp;
                    tx.senderPk = currentUser.publicKey;
                    tx.nonce = nonce;
                    txRepo.addTransaction(tx);
                    logger.debug("adding transaction txID::{}", tx.txID);
                    // 如果是WiringTransaction交易
                    addUserInfo(tx);
                }else{
                    result = getApplication().getString(R.string.tx_error_type);
                }
            }catch (Exception e){
                result = e.getMessage();
                logger.debug("Error adding transaction::{}", result);
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
     * 添加用户信息到本地
     * @param tx 交易
     */
    private void addUserInfo(Tx tx) {
        MsgType msgType = MsgType.setValue((byte) tx.txType);
        if(msgType == MsgType.Wiring){
            User user = userRepo.getUserByPublicKey(tx.receiverPk);
            if(null == user){
                user = new User(tx.receiverPk);
                userRepo.addUser(user);
                logger.info("addUserInfo to local, publicKey::{}",
                        tx.receiverPk);
            }
        }
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
                case Wiring:
                    byte[] receiverPk = ByteUtil.toByte(tx.receiverPk);
                    WireTransaction wireTx = new WireTransaction(receiverPk, tx.amount, tx.memo);
                    txData = new TxData(msgType, wireTx.getEncode());
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
        if(null == tx){
            return false;
        }
        int msgType = tx.txType;
        if(msgType == MsgType.RegularForum.getVaLue()){
            if(StringUtil.isEmpty(tx.memo)){
                ToastUtils.showShortToast(R.string.tx_error_invalid_message);
                return false;
            }
        }else if(msgType == MsgType.ForumComment.getVaLue()){
            if(StringUtil.isEmpty(tx.memo)){
                ToastUtils.showShortToast(R.string.tx_error_invalid_comment);
                return false;
            }
        }else if(msgType == MsgType.Wiring.getVaLue()){
            // 获取当前用户的余额
            String senderPk = MainApplication.getInstance().getPublicKey();
            long balance = daemon.getUserBalance(tx.chainID, senderPk);
            if(StringUtil.isEmpty(tx.receiverPk) ||
                    ByteUtil.toByte(tx.receiverPk).length != Ed25519.PUBLIC_KEY_SIZE){
                ToastUtils.showShortToast(R.string.tx_error_invalid_pk);
                return false;
            }else if(tx.amount <= 0){
                ToastUtils.showShortToast(R.string.tx_error_invalid_amount);
                return false;
            }else if(tx.amount > balance || tx.amount + tx.fee > balance){
                ToastUtils.showShortToast(R.string.tx_error_no_enough_coins);
                return false;
            }
        }
        return true;
    }

    /**
     * 显示编辑交易费的对话框
     */
    public void showEditFeeDialog(BaseActivity activity, TextView tvFee, Community community) {
        EditFeeDialogBinding editFeeBinding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.edit_fee_dialog, null, false);
        editFeeBinding.etFee.setText("96.5");
        editFeeBinding.tvMedianFee.setText(activity.getString(R.string.tx_median_fee_tips, "96.5"));
        editFeeDialog = new CommonDialog.Builder(activity)
                .setContentView(editFeeBinding.getRoot())
                .setPositiveButton(R.string.common_submit, (dialog, which) -> {
                    dialog.cancel();
                    String fee = editFeeBinding.etFee.getText().toString();
                    if(StringUtil.isNotEmpty(fee)){
                        tvFee.setText(activity.getString(R.string.tx_median_fee, fee, UsersUtil.getCoinName(community)));
                        tvFee.setTag(fee);
                    }
                })
                .create();
        editFeeDialog.show();
    }
}
