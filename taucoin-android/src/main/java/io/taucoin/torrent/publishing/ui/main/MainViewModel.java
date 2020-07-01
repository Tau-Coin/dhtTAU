package io.taucoin.torrent.publishing.ui.main;

import android.app.Application;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityCreateBinding;
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