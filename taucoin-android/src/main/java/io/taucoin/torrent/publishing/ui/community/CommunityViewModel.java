package io.taucoin.torrent.publishing.ui.community;

import android.app.Application;

import com.github.naturs.logger.Logger;

import org.slf4j.LoggerFactory;

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
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityCreateBinding;
import io.taucoin.torrent.publishing.core.storage.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.entity.Community;

/**
 * Community模块的ViewModel
 */
public class CommunityViewModel extends AndroidViewModel {

    private CommunityRepository communityRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MutableLiveData<Boolean> addCommunityState = new MutableLiveData<>();
    private MutableLiveData<Boolean> setBlacklistState = new MutableLiveData<>();
    private MutableLiveData<Boolean> setMuteState = new MutableLiveData<>();
    public CommunityViewModel(@NonNull Application application) {
        super(application);
        communityRepo = RepositoryHelper.getCommunityRepository(getApplication());
    }

    /**
     * 获取添加社区状态的被观察者
     * @return 被观察者
     */
    LiveData<Boolean> getAddCommunityState() {
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
    LiveData<Boolean> getSetMuteState() {
        return setMuteState;
    }

    /**
     * 添加新的社区到数据库
     * @param community 社区数据
     */
    void addCommunity(@NonNull Community community){
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            // TODO: 1、TauController:创建Community社区

            ChainConfig chainConfig = ChainConfig.NewChainConfig((byte)1, community.communityName, community.blockInAvg,
                    community.publicKey , "", null);
            community.chainId = new String(chainConfig.getChainid());
            communityRepo.addCommunity(community);
            Logger.d("Add community to database: communityName=%s, chainID=%s",
                    community.communityName, community.chainId);

            // TODO: 2、TauController:Announce on TAU
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> addCommunityState.postValue(state));
        disposables.add(disposable);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    /**
     * 根据用户输入数据构建Community实体类
     * @param binding view绑定对象
     * @return Community
     */
    Community buildCommunity(@NonNull ActivityCommunityCreateBinding binding) {
        String communityName = ViewUtils.getText(binding.etCommunityName);
        String coinName = ViewUtils.getText(binding.etCoinName);
        long totalCoin = ViewUtils.getLongTag(binding.etTotalCoin);
        int blockAvg = ViewUtils.getIntTag(binding.etBlockAvg);
        String intro = ViewUtils.getText(binding.etIntro);
        String telegramId = ViewUtils.getText(binding.etTelegramId);
        String publicKey = ViewUtils.getText(binding.tvPublicKey);
        return new Community(communityName, coinName, publicKey, totalCoin, blockAvg, intro, telegramId);
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

        if(StringUtil.isEmpty(community.coinName)){
            ToastUtils.showLongToast(R.string.error_coin_name_empty);
            return false;
        }
        return true;
    }

    /**
     * 设置社区黑名单
     * @param chainId 社区chainId
     * @param blacklist 是否加入黑名单
     */
    void setCommunityBlacklist(String chainId, boolean blacklist) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            communityRepo.setCommunityBlacklist(chainId, blacklist);
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> setBlacklistState.postValue(state));
        disposables.add(disposable);
    }

    /**
     * 设置社区是否静音
     * @param chainId 社区chainId
     * @param isMute 是否静音
     */
    void setCommunityMute(String chainId, boolean isMute) {
        Disposable disposable = Flowable.create((FlowableOnSubscribe<Boolean>) emitter -> {
            communityRepo.setCommunityMute(chainId, isMute);
            emitter.onNext(true);
            emitter.onComplete();
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(state -> setMuteState.postValue(state));
        disposables.add(disposable);
    }
}