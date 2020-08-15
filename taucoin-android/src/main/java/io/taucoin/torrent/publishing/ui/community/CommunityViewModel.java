package io.taucoin.torrent.publishing.ui.community;

import android.app.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.config.ChainConfig;
import io.taucoin.genesis.GenesisItem;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.storage.sqlite.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.TxRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.storage.sqlite.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;
import io.taucoin.types.GenesisTx;
import io.taucoin.types.Transaction;
import io.taucoin.util.ByteArrayWrapper;
import io.taucoin.util.ByteUtil;

/**
 * Community模块的ViewModel
 */
public class CommunityViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("CommunityViewModel");
    private CommunityRepository communityRepo;
    private MemberRepository memberRepo;
    private UserRepository userRepo;
    private TxRepository txRepo;
    private TauDaemon daemon;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<String> addCommunityState = new MutableLiveData<>();
    private MutableLiveData<Boolean> setBlacklistState = new MutableLiveData<>();
    private MutableLiveData<String> chatState = new MutableLiveData<>();
    private MutableLiveData<List<Community>> blackList = new MutableLiveData<>();
    private MutableLiveData<List<Community>> joinedList = new MutableLiveData<>();

    public CommunityViewModel(@NonNull Application application) {
        super(application);
        communityRepo = RepositoryHelper.getCommunityRepository(getApplication());
        memberRepo = RepositoryHelper.getMemberRepository(getApplication());
        userRepo = RepositoryHelper.getUserRepository(getApplication());
        txRepo = RepositoryHelper.getTxRepository(getApplication());
        daemon = TauDaemon.getInstance(getApplication());
    }

    public MutableLiveData<List<Community>> getJoinedList() {
        return joinedList;
    }

    /**
     * 获取添加社区状态的被观察者
     * @return 被观察者
     */
    LiveData<String> getAddCommunityState() {
        return addCommunityState;
    }

    /**
     * 获取设置黑名单状态的被观察者
     * @return 被观察者
     */
    LiveData<Boolean> getSetBlacklistState() {
        return setBlacklistState;
    }

    /**
     * 获取设置社区静音状态的被观察者
     * @return 被观察者
     */
    public LiveData<String> getChatState() {
        return chatState;
    }

    /**
     * 获取社区黑名单的被观察者
     * @return 被观察者
     */
    public MutableLiveData<List<Community>> getBlackList() {
        return blackList;
    }

    /**
     * 添加新的社区到数据库
     * @param community 社区数据
     */
    void addCommunity(@NonNull Community community){
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            // TauController:创建Community社区
            String state = createCommunity(community);
            emitter.onNext(state);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> addCommunityState.postValue(state));
        disposables.add(disposable);
    }

    /**
     * 创建Community社区
     * @param community
     */
    private String createCommunity(Community community) {
        String result = "";
        try {
            User user = userRepo.getCurrentUser();
            byte[] seed = ByteUtil.toByte(user.seed);
            BigInteger totalCoin = BigInteger.valueOf(community.totalCoin);
            byte[] publicKey = ByteUtil.toByte(community.publicKey);
            HashMap<ByteArrayWrapper, GenesisItem> genesisMsg = new HashMap<>();
            GenesisItem item = new GenesisItem(totalCoin);
            genesisMsg.put(new ByteArrayWrapper(publicKey), item);
//            GenesisTx genesisTx = new GenesisTx();
            // TODO:
            ChainConfig chainConfig = new ChainConfig(1, community.communityName, community.blockInAvg,
                    publicKey, DateUtil.getTime(), null, null);
            daemon.createCommunity(chainConfig);
            community.chainID = new String(chainConfig.getChainid());
            communityRepo.addCommunity(community);
            logger.debug("Add community to database: communityName={}, chainID={}",
                    community.communityName, community.chainID);
        }catch (Exception e){
            result = e.getMessage();
        }
        return result;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    /**
     * 验证Community实体类中的数据
     * @param community view数据对象
     * @return 是否验证通过
     */
    boolean validateCommunity(@NonNull Community community) {
        String communityName = community.communityName;
        if(StringUtil.isEmpty(communityName)){
            ToastUtils.showLongToast(R.string.error_community_name_empty);
            return false;
        }
        return true;
    }

    /**
     * 设置社区黑名单
     * @param chainID 社区chainID
     * @param blacklist 是否加入黑名单
     */
    public void setCommunityBlacklist(String chainID, boolean blacklist) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            communityRepo.setCommunityBlacklist(chainID, blacklist);
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> setBlacklistState.postValue(state));
        disposables.add(disposable);
    }

    /**
     * 获取在黑名单的社区列表
     */
    public void getCommunitiesInBlacklist(){
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<Community>>) emitter -> {
            List<Community> list = communityRepo.getCommunitiesInBlacklist();
            emitter.onNext(list);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> blackList.postValue(list));
        disposables.add(disposable);
    }

    /**
     * 获取用户加入的社区列表
     * @param chainID
     */
    void getJoinedCommunityList(String chainID) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<List<Community>>) emitter -> {
            List<Community> list = communityRepo.getJoinedCommunityList(chainID);
            emitter.onNext(list);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> joinedList.postValue(list));
        disposables.add(disposable);
    }

    /**
     * 观察社区成员变化
     * @param chainID
     * @return
     */
    public Flowable<List<MemberAndUser>> observeCommunityMembers(String chainID) {
        return memberRepo.observeCommunityMembers(chainID);
    }

    /**
     * 和联系人创建Chat
     * @param chatName chatName
     * @param friendPk friend's PK
     */
    public void createChat(TxViewModel txViewModel, String chatName, String friendPk) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<String>) emitter -> {
            String result = "";
            try {
                // 处理社区名，如果为空，取显示名的首位
                String communityName = chatName;
                User currentUser = userRepo.getCurrentUser();
                User friend = userRepo.getUserByPublicKey(friendPk);
                if(StringUtil.isEmpty(communityName)){
                    String currentUserName = UsersUtil.getShowName(currentUser);
                    String friendName = UsersUtil.getShowName(friend);
                    communityName = currentUserName.substring(0, 1) + friendName.substring(0, 1);
                }
                Community community = new Community(communityName, currentUser.publicKey,
                        Constants.TOTAL_COIN.longValue(), Constants.BLOCK_IN_AVG);
                // TauController:创建Community社区
                result = createCommunity(community);
                if(StringUtil.isEmpty(result)){
                    // 社区创建完成，直接给要聊天的朋友空投币
                    result = txViewModel.airdropToFriend(community.chainID, friendPk);
                }
            }catch (Exception e){
                result = e.getMessage();
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> chatState.postValue(state));
        disposables.add(disposable);
    }
}