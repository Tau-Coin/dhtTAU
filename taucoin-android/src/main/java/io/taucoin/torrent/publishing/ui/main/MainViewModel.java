package io.taucoin.torrent.publishing.ui.main;

import android.app.Application;
import android.view.LayoutInflater;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.AndroidViewModel;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.repo.CommunityRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.databinding.FrequencyDialogBinding;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;

/**
 * 主页的ViewModel
 */
public class MainViewModel extends AndroidViewModel {

    private CommunityRepository communityRepo;
    private TauDaemon daemon;
    private CommonDialog frequencyDialog;
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
        if (frequencyDialog != null && frequencyDialog.isShowing()) {
            frequencyDialog.dismiss();
        }
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

    /**
     * 显示解释工作频率的对话框
     * @param activity
     */
    public void showWorkFrequencyDialog(AppCompatActivity activity) {
        if (frequencyDialog != null && frequencyDialog.isShowing()) {
            return;
        }
        FrequencyDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.frequency_dialog, null, false);
        binding.tvMsg.setText(R.string.main_work_frequency_tips);
        binding.tvMsg.setTextColor(activity.getResources().getColor(R.color.color_black));
        binding.ivClose.setOnClickListener(v -> {
            if (frequencyDialog != null && frequencyDialog.isShowing()) {
                frequencyDialog.dismiss();
            }
        });
        frequencyDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(true)
                .create();
        frequencyDialog.show();
    }
}