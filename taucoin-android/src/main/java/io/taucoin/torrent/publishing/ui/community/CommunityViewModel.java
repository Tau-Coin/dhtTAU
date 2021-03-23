package io.taucoin.torrent.publishing.ui.community;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.genesis.GenesisConfig;
import io.taucoin.genesis.GenesisItem;
import io.taucoin.param.ChainParam;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.DrawBean;
import io.taucoin.torrent.publishing.core.model.data.FriendStatus;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.model.data.Statistics;
import io.taucoin.torrent.publishing.core.model.data.Result;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Friend;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.FriendRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.MemberRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.UserRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.BitmapUtil;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.core.utils.DimensionsUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.util.ByteUtil;

/**
 * Community模块的ViewModel
 */
public class CommunityViewModel extends AndroidViewModel {

    private static final Logger logger = LoggerFactory.getLogger("CommunityViewModel");
    private CommunityRepository communityRepo;
    private MemberRepository memberRepo;
    private FriendRepository friendRepo;
    private UserRepository userRepo;
    private TauDaemon daemon;
    private Disposable observeDaemonRunning;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<Result> addCommunityState = new MutableLiveData<>();
    private MutableLiveData<Boolean> setBlacklistState = new MutableLiveData<>();
    private MutableLiveData<Result> chatState = new MutableLiveData<>();
    private MutableLiveData<List<Community>> blackList = new MutableLiveData<>();
    private MutableLiveData<List<Community>> joinedList = new MutableLiveData<>();
    private MutableLiveData<Bitmap> qrBitmap = new MutableLiveData<>();

    public CommunityViewModel(@NonNull Application application) {
        super(application);
        communityRepo = RepositoryHelper.getCommunityRepository(getApplication());
        memberRepo = RepositoryHelper.getMemberRepository(getApplication());
        userRepo = RepositoryHelper.getUserRepository(getApplication());
        friendRepo = RepositoryHelper.getFriendsRepository(getApplication());
        daemon = TauDaemon.getInstance(getApplication());
    }

    public void observeNeedStartDaemon () {
        disposables.add(daemon.observeNeedStartDaemon()
                .subscribeOn(Schedulers.io())
                .filter((needStart) -> needStart)
                .subscribe((needStart) -> daemon.start()));
    }

    MutableLiveData<List<Community>> getJoinedList() {
        return joinedList;
    }

    /**
     * 获取添加社区状态的被观察者
     * @return 被观察者
     */
    public LiveData<Result> getAddCommunityState() {
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
    public LiveData<Result> getChatState() {
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
     * 观察生成的二维码
     */
    public MutableLiveData<Bitmap> getQRBitmap() {
        return qrBitmap;
    }

    /**
     * 添加新的社区到数据库
     * @param chainID
     * @param chainLink
     */
    public void addCommunity(String chainID, String chainLink){
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            String communityName = UsersUtil.getCommunityName(chainID);
            if(StringUtil.isNotEmpty(communityName)){
                Community community = new Community(chainID, communityName);
                communityRepo.addCommunity(community);
                // 链端follow community
                daemon.followCommunity(chainLink);
                result.setMsg(chainID);
            }else{
                result.setSuccess(false);
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> {
                    addCommunityState.postValue(state);
                });
        disposables.add(disposable);
    }

    GenesisConfig createGenesisConfig(@NonNull Community community){
        byte[] publicKey = ByteUtil.toByte(community.publicKey);
        BigInteger totalCoin = BigInteger.valueOf(community.totalCoin);
        GenesisItem item = new GenesisItem(publicKey, totalCoin);
        ArrayList<GenesisItem> list = new ArrayList<>();
        list.add(item);
        return new GenesisConfig(community.communityName, list);
    }

    /**
     * 添加新的社区到数据库
     * @param community 社区数据
     */
    void addCommunity(@NonNull Community community, GenesisConfig cf){
        if (observeDaemonRunning != null && !observeDaemonRunning.isDisposed()) {
            return;
        }
        observeDaemonRunning = daemon.observeDaemonRunning()
            .subscribeOn(Schedulers.io())
            .subscribe((isRunning) -> {
                if (isRunning) {
                    newCommunity(community, cf);
                    if (observeDaemonRunning != null) {
                        observeDaemonRunning.dispose();
                    }
                }
            });
    }

    private void newCommunity(@NonNull Community community, GenesisConfig cf){
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            // TauController:创建Community社区
            Result result = createCommunity(community, cf);
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> addCommunityState.postValue(state));
        disposables.add(disposable);
    }

    private Result createCommunity(Community community, GenesisConfig cf) {
        Result result = new Result();
        try {
            if (null == cf) {
                cf = createGenesisConfig(community);
            }
            daemon.createNewCommunity(cf);

            community.chainID = new String(cf.getChainID(), StandardCharsets.UTF_8);
            communityRepo.addCommunity(community);
            logger.debug("Add community to database: communityName={}, chainID={}",
                    community.communityName, community.chainID);
            // 把社区创建者添加为社区成员
            Member member = new Member(community.chainID, community.publicKey,
                    community.totalCoin, ChainParam.DefaultGeneisisPower.longValue());
            memberRepo.addMember(member);
        }catch (Exception e){
            result.setFailMsg(e.getMessage());
        }
        return result;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
        if (observeDaemonRunning != null && !observeDaemonRunning.isDisposed()) {
            observeDaemonRunning.dispose();
        }
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
        if (observeDaemonRunning != null && !observeDaemonRunning.isDisposed()) {
            return;
        }
        observeDaemonRunning = daemon.observeDaemonRunning()
                .subscribeOn(Schedulers.io())
                .subscribe((isRunning) -> {
                    if (isRunning) {
                        setCommunityBlacklistTask(chainID, blacklist);
                        if (observeDaemonRunning != null) {
                            observeDaemonRunning.dispose();
                        }
                    }
                });
    }

    private void setCommunityBlacklistTask(String chainID, boolean blacklist) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            communityRepo.setCommunityBlacklist(chainID, blacklist);
            if (blacklist) {
                daemon.unfollowCommunity(chainID);
            } else {
                List<String> list = queryCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT);
                String communityInviteLink = ChainLinkUtil.encode(chainID, list);
                daemon.followCommunity(communityInviteLink);
            }
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
     * @param friendPk friend's PK
     */
    public void createChat(String friendPk) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Result>) emitter -> {
            Result result = new Result();
            try {
                // 处理ChatName，如果为空，取显朋友显示名
                String userPk = MainApplication.getInstance().getPublicKey();
                Friend friend = friendRepo.queryFriend(userPk, friendPk);
                if (friend != null) {
                    friend.status = FriendStatus.CONNECTED.getStatus();
                }
                result.setMsg(friendPk);
            }catch (Exception e){
                result.setFailMsg(e.getMessage());
            }
            emitter.onNext(result);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> chatState.postValue(state));
        disposables.add(disposable);
    }

    /**
     * 查询社区成员
     * @param chainID
     * @param onChain
     * @return DataSource.Factory
     */
    public DataSource.Factory<Integer, MemberAndUser> queryCommunityMembers(String chainID, boolean onChain) {
        return memberRepo.queryCommunityMembers(chainID, onChain);
    }

    /**
     * 获取和社区成员共在的社区数
     * @param currentUserPk
     * @param memberPk
     */
    public Single<List<String>> getCommunityNumInCommon(String currentUserPk, String memberPk) {
        return memberRepo.getCommunityNumInCommon(currentUserPk, memberPk);
    }
    /**
     * 获取社区limit个成员
     * @param chainID
     * @param limit
     */
    public Single<List<String>> getCommunityMembersLimit(String chainID, int limit) {
        return memberRepo.getCommunityMembersLimit(chainID, limit);
    }

    public List<String> queryCommunityMembersLimit(String chainID, int limit) {
        return memberRepo.queryCommunityMembersLimit(chainID, limit);
    }

    /**
     * 获取社区成员统计
     * @param chainID
     */
    public Flowable<Statistics> getMembersStatistics(String chainID) {
        return memberRepo.getMembersStatistics(chainID);
    }


    public Single<Community> getCommunityByChainIDSingle(String chainID) {
        return communityRepo.getCommunityByChainIDSingle(chainID);
    }

    public Observable<Community> observerCommunityByChainID(String chainID) {
        return communityRepo.observerCommunityByChainID(chainID);
    }

    LiveData<PagedList<MemberAndUser>> observerCommunityMembers(String chainID, boolean onChain) {
        return new LivePagedListBuilder<>(queryCommunityMembers(chainID, onChain),
                Page.getPageListConfig()).build();
    }

    /**
     * 观察当前登陆的社区成员
     * @param chainID
     * @return
     */
    Observable<Member> observerCurrentMember(String chainID) {
        String publicKey = MainApplication.getInstance().getPublicKey();
        return communityRepo.observerCurrentMember(chainID, publicKey);
    }

    /**
     * 生成二维码
     * @param context
     * @param QRContent 二维码内容
     * @param logo
     */
    public void generateQRCode(Context context, String QRContent, View logo) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Bitmap>) emitter -> {
            try {
                logo.setDrawingCacheEnabled(true);
                logo.buildDrawingCache();
                Bitmap logoBitmap = logo.getDrawingCache();
                if (logoBitmap != null) {
                    int heightPix = DimensionsUtil.dip2px(context, 480);
                    Bitmap bitmap = BitmapUtil.createQRCode(QRContent, heightPix, logoBitmap,
                            context.getResources().getColor(R.color.color_transparent));
                    logger.debug("shareQRCode bitmap::{}", bitmap);
                    logo.destroyDrawingCache();

                    // 二维码背景样式
                    Bitmap bgBitmap = BitmapFactory.decodeResource(context.getResources(),
                            R.mipmap.icon_community_qr_bg);
                    bgBitmap.getWidth();
                    DrawBean bean = new DrawBean();
                    bean.setSize(DimensionsUtil.dip2px(context, 210));
                    bean.setX(DimensionsUtil.dip2px(context, 60));
                    bean.setY(DimensionsUtil.dip2px(context, 60));
                    // 二维码背景
                    Bitmap qrbgBitmap = BitmapUtil.createBitmap(bitmap.getWidth(),
                            bgBitmap.getHeight(), Color.BLACK);
                    Bitmap lastBitmap = BitmapUtil.drawStyleQRcode(bgBitmap,
                            qrbgBitmap, bitmap, bean);
                    emitter.onNext(lastBitmap);
                }
            } catch (Exception e) {
                logger.error("generateTAUIDQRCode error ", e);
            }
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> qrBitmap.postValue(result));
        disposables.add(disposable);
    }
}
