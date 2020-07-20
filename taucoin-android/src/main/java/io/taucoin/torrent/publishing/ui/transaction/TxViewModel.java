package io.taucoin.torrent.publishing.ui.transaction;

import android.app.Application;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.frostwire.jlibtorrent.Ed25519;
import com.frostwire.jlibtorrent.Pair;
import com.github.naturs.logger.Logger;

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
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.ReplyAndAllTxs;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.TxRepository;
import io.taucoin.torrent.publishing.core.storage.UserRepository;
import io.taucoin.torrent.publishing.core.storage.entity.Community;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.entity.User;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.EditFeeDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
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
    private MutableLiveData<List<ReplyAndAllTxs>> chainTxs = new MutableLiveData<>();
    private MutableLiveData<String> addState = new MutableLiveData<>();
    private CommonDialog editFeeDialog;
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
    public MutableLiveData<List<ReplyAndAllTxs>> getChainTxs() {
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
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<ReplyAndAllTxs>>) emitter -> {
            List<ReplyAndAllTxs> txs = txRepo.getTxsByChainID(chainID);
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
    public Flowable<List<ReplyAndAllTxs>> observeTxsByChainID(String chainID){
        return txRepo.observeTxsByChainID(chainID);
    }

    /**
     * 根据chainID获取社区的交易的被被观察者
     * @param chainID 社区链id
     */
    public DataSource.Factory<Integer, ReplyAndAllTxs> queryCommunityTxs(String chainID){
        return txRepo.queryCommunityTxs(chainID);
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
                    // 保存交易数据到本地数据库
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
                case Wiring:
                    byte[] receiverPk = ByteUtil.toByte(tx.receiverPk);
                    WireTransaction wireTx = new WireTransaction(receiverPk, tx.amount, tx.memo);
                    txData = new TxData(msgType, wireTx.getEncode());
                    break;
                case IdentityAnnouncement:
                    IdentityAnnouncement identityAnn = new IdentityAnnouncement(tx.name, tx.memo);
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
            // TODO: 获取当前用户的余额
            long balance = 100000000000L;
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
        }else if(msgType == MsgType.IdentityAnnouncement.getVaLue()){
            if(StringUtil.isEmpty(tx.name)){
                ToastUtils.showShortToast(R.string.tx_error_invalid_name);
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
