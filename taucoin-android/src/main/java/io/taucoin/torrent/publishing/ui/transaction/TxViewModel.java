package io.taucoin.torrent.publishing.ui.transaction;

import android.app.Application;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.frostwire.jlibtorrent.Ed25519;

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
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.types.TypesConfig;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.EditFeeDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.Chain;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.types.ForumNoteTx;
import io.taucoin.types.Transaction;
import io.taucoin.types.WiringCoinsTx;
import io.taucoin.util.ByteUtil;

/**
 * 交易模块相关的ViewModel
 */
public class TxViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("TxViewModel");
    private Context application;
    private TxRepository txRepo;
    private UserRepository userRepo;
    private MemberRepository memberRepo;
    private SettingsRepository settingsRepo;
    private TauDaemon daemon;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<List<UserAndTx>> chainTxs = new MutableLiveData<>();
    private MutableLiveData<String> airdropState = new MutableLiveData<>();
    private MutableLiveData<String> addState = new MutableLiveData<>();
    private CommonDialog editFeeDialog;
    public TxViewModel(@NonNull Application application) {
        super(application);
        this.application = application;
        txRepo = RepositoryHelper.getTxRepository(application);
        userRepo = RepositoryHelper.getUserRepository(application);
        daemon  = TauDaemon.getInstance(application);
        settingsRepo  = RepositoryHelper.getSettingsRepository(application);
        memberRepo = RepositoryHelper.getMemberRepository(application);
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
    public DataSource.Factory<Integer, UserAndTx> queryCommunityTxs(String chainID, long txType){
        return txRepo.queryCommunityTxs(chainID, txType, txType == -1 ? 0 : 1);
    }

    public MutableLiveData<String> getAirdropState(){
        return airdropState;
    }

    /**
     * 添加新的交易
     * @param tx 根据用户输入构建的用户数据
     */
    void addTransaction(Tx tx) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            String result = createTransaction(tx);
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> addState.postValue(state));
        disposables.add(disposable);
    }

    private String createTransaction(Tx tx) {
        // 获取当前用户的Seed, 获取公私钥
        User currentUser = userRepo.getCurrentUser();
        byte[] senderSeed = ByteUtil.toByte(currentUser.seed);
        byte[] senderPk = ByteUtil.toByte(currentUser.publicKey);
        String result = "";
        try {
            // 获取当前用户在社区中链上nonce值
            long nonce = daemon.getUserPower(tx.chainID, currentUser.publicKey);
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
            byte[] chainID = tx.chainID.getBytes();
            Transaction transaction;
            if(tx.txType == TypesConfig.TxType.WCoinsType.ordinal()){
                byte[] receiverPk = ByteUtil.toByte(tx.receiverPk);
                transaction = new WiringCoinsTx(1, chainID, timestamp, tx.fee, tx.txType, senderPk,
                        nonce, receiverPk, tx.amount, tx.memo);
            } else {
                byte[] forumNoteHash = new byte[24];
                transaction = new ForumNoteTx(1, chainID, timestamp, tx.fee, tx.txType,
                        senderPk, nonce, forumNoteHash);
            }
            transaction.signTransactionWithSeed(senderSeed);
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
            addMemberInfo(tx);
            settingsRepo.lastTxFee(tx.chainID, String.valueOf(tx.fee));
        }catch (Exception e){
            result = e.getMessage();
            logger.debug("Error adding transaction::{}", result);
        }
        return result;
    }

    /**
     * 添加用户信息到本地
     * @param tx 交易
     */
    private void addUserInfo(Tx tx) {
        long txType = tx.txType;
        if(txType == TypesConfig.TxType.WCoinsType.ordinal()){
            User user = userRepo.getUserByPublicKey(tx.receiverPk);
            if(null == user){
                user = new User(tx.receiverPk);
                user.lastUpdateTime = DateUtil.getTime();
                userRepo.addUser(user);
                logger.info("addUserInfo to local, publicKey::{}",
                        tx.receiverPk);
            }
        }
    }

    /**
     * 添加社区成员信息
     * @param tx 交易
     */
    private void addMemberInfo(Tx tx) {
        long txType = tx.txType;
        Member member = memberRepo.getMemberByChainIDAndPk(tx.chainID, tx.senderPk);
        if(null == member){
            member = new Member(tx.chainID, tx.senderPk);
            member.balance = daemon.getUserBalance(tx.chainID, tx.senderPk);
            member.power = daemon.getUserPower(tx.chainID, tx.senderPk);
            memberRepo.addMember(member);
        }
        if(txType == TypesConfig.TxType.WCoinsType.ordinal() && StringUtil.isNotEquals(tx.senderPk, tx.receiverPk)){
            Member receiverMember = memberRepo.getMemberByChainIDAndPk(tx.chainID, tx.receiverPk);
            if(null == receiverMember){
                receiverMember = new Member(tx.chainID, tx.receiverPk);
                receiverMember.balance = daemon.getUserBalance(tx.chainID, tx.receiverPk);
                receiverMember.power = daemon.getUserBalance(tx.chainID, tx.receiverPk);
                memberRepo.addMember(receiverMember);
                logger.info("addMemberInfo to local, publicKey::{}",
                        tx.receiverPk);
            }
        }
    }

    /**
     * 验证交易
     * @param tx 交易数据
     */
    boolean validateTx(Tx tx) {
        if(null == tx){
            return false;
        }
        long msgType = tx.txType;
        if(msgType == TypesConfig.TxType.FNoteType.ordinal()){
            if(StringUtil.isEmpty(tx.memo)){
                ToastUtils.showShortToast(R.string.tx_error_invalid_message);
                return false;
            }
            String senderPk = MainApplication.getInstance().getPublicKey();
            long balance = daemon.getUserBalance(tx.chainID, senderPk);
            if(tx.fee > balance){
                ToastUtils.showShortToast(R.string.tx_error_no_enough_coins_for_fee);
                return false;
            }
        }else if(msgType == TypesConfig.TxType.WCoinsType.ordinal()){
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

    String getLastTxFee(String chainID){
        return settingsRepo.lastTxFee(chainID);
    }

    /**
     * 显示编辑交易费的对话框
     */
    void showEditFeeDialog(BaseActivity activity, TextView tvFee, String chainID) {
        if(StringUtil.isNotEmpty(chainID)){
            return;
        }
        EditFeeDialogBinding editFeeBinding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.edit_fee_dialog, null, false);
        String fee = tvFee.getTag().toString();
        String medianFee = tvFee.getTag(R.id.median_fee).toString();
        editFeeBinding.etFee.setText(fee);
        editFeeBinding.tvMedianFee.setText(activity.getString(R.string.tx_median_fee_tips, medianFee));
        editFeeDialog = new CommonDialog.Builder(activity)
                .setContentView(editFeeBinding.getRoot())
                .setPositiveButton(R.string.common_submit, (dialog, which) -> {
                    dialog.cancel();
                    String etFee = editFeeBinding.etFee.getText().toString();
                    if(StringUtil.isNotEmpty(etFee)){
                        tvFee.setText(activity.getString(R.string.tx_median_fee, etFee, UsersUtil.getCoinName(chainID)));
                        tvFee.setTag(etFee);
                    }
                })
                .create();
        editFeeDialog.show();
    }

    /**
     * 获取中位数交易费
     * @param chainID 交易所属的社区chainID
     */
    public long getMedianFee(String chainID) {
        List<Long> fees = txRepo.getMedianFee(chainID);
        return Utils.getMedianData(fees);
    }

    /**
     * 观察中位数交易费
     * @param chainID 交易所属的社区chainID
     */
    Single<List<Long>> observeMedianFee(String chainID) {
        return txRepo.observeMedianFee(chainID);
    }

    /**
     * 空投币给朋友
     * @param chainID
     * @param friendPks
     */
    public void airdropToFriends(String chainID, List<String> friendPks) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            String result = "";
            try {
                String senderPk = MainApplication.getInstance().getPublicKey();
                long balance = daemon.getUserBalance(chainID, senderPk);
                long airdropCoin = Constants.AIRDROP_COIN.longValue();
                long medianFee = getMedianFee(chainID);
                long totalPay = (airdropCoin + medianFee) * friendPks.size();
                if(totalPay > balance){
                    result = application.getString(R.string.tx_error_no_enough_coins_for_airdrop);
                }else{
                    for (String friendPk : friendPks) {
                        result = airdropToFriend(chainID, friendPk);
                        if (StringUtil.isNotEmpty(result)) {
                            break;
                        }
                    }
                }
            }catch (Exception e){
                result = e.getMessage();
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> airdropState.postValue(result));
        disposables.add(disposable);
    }

    /**
     * 空投币给朋友
     * @param chainID
     * @param friendPk
     */
    public String airdropToFriend(String chainID, String friendPk) {
        long medianFee = getMedianFee(chainID);
        Tx tx = new Tx(chainID, friendPk, Constants.AIRDROP_COIN.longValue(),
                medianFee, TypesConfig.TxType.WCoinsType.ordinal(), "");
        return createTransaction(tx);
    }
}
