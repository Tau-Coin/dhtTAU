package io.taucoin.torrent.publishing.ui.main;

import android.app.Application;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;

/**
 * 主页的ViewModel
 */
public class MainViewModel extends AndroidViewModel {

    private CommunityRepository communityRepo;
    private TauDaemon daemon;
    private CompositeDisposable disposables = new CompositeDisposable();
    public MainViewModel(@NonNull Application application) {
        super(application);
        communityRepo = RepositoryHelper.getCommunityRepository(getApplication());
        daemon = TauDaemon.getInstance(application);
    }

    /**
     * 观察不在黑名单的社区列表数据变化
     * @return 被观察的社区数据列表
     */
    Flowable<List<CommunityAndFriend>> observeCommunitiesAndFriends(){
        return communityRepo.observeCommunitiesAndFriends();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    /**
     * 观察是否需要启动Daemon
     * @return Flowable
     */
    Flowable<Boolean> observeNeedStartEngine() {
        return daemon.observeNeedStartDaemon();
    }

    /**
     * 启动TauDaemon
     */
    void startDaemon() {
        daemon.start();
    }
}