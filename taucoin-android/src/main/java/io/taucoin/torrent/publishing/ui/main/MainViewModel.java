package io.taucoin.torrent.publishing.ui.main;

import android.app.Application;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.storage.CommunityRepository;
import io.taucoin.torrent.publishing.storage.RepositoryHelper;
import io.taucoin.torrent.publishing.storage.entity.Community;

/**
 * 主页的ViewModel
 */
public class MainViewModel extends AndroidViewModel {

    private CommunityRepository communityRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    public MainViewModel(@NonNull Application application) {
        super(application);
        communityRepo = RepositoryHelper.getCommunityRepository(getApplication());
    }

    /**
     * 观察不在黑名单的社区列表数据变化
     * @return 被观察的社区数据列表
     */
    Flowable<List<Community>> observeCommunitiesNotBlocked(){
        return communityRepo.observeCommunitiesNotBlocked();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}