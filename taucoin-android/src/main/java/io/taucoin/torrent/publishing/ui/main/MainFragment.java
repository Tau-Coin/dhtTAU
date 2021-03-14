package io.taucoin.torrent.publishing.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndMember;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.FragmentMainBinding;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.community.CommunityCreateActivity;
import io.taucoin.torrent.publishing.ui.friends.ExchangeActivity;

/**
 * 群组列表页面
 */
public class MainFragment extends BaseFragment implements MainListAdapter.ClickListener, View.OnClickListener {

    private MainActivity activity;

    private MainListAdapter adapter;
    private FragmentMainBinding binding;
    private MainViewModel viewModel;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);
        binding.setListener(this);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (MainActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(activity);
        viewModel = provider.get(MainViewModel.class);
        settingsRepo = RepositoryHelper.getSettingsRepository(activity);
        initView();
    }

    private void initView() {
        adapter = new MainListAdapter(this);
//        /*
//         * A RecyclerView by default creates another copy of the ViewHolder in order to
//         * fade the views into each other. This causes the problem because the old ViewHolder gets
//         * the payload but then the new one doesn't. So needs to explicitly tell it to reuse the old one.
//         */
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.groupList.setLayoutManager(layoutManager);
        binding.groupList.setItemAnimator(animator);
        binding.groupList.setEmptyView(binding.emptyViewGroupList);
        binding.groupList.setAdapter(adapter);

        handleSettingsChanged(getString(R.string.pref_key_main_loop_interval));
    }

    private void subscribeMainViewModel() {
        disposables.add(viewModel.observeCommunitiesNotInBlacklist()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showCommunityList));
    }

    private void showCommunityList(List<CommunityAndMember> communities) {
        if(communities != null){
            adapter.submitList(communities);
        }
    }

    /**
     * 订阅配置文件改变
     */
    private void subscribeSettingsChanged() {
        Disposable disposable = settingsRepo.observeSettingsChanged()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSettingsChanged);
        disposables.add(disposable);
    }

    private void handleSettingsChanged(String key) {
        if(StringUtil.isEquals(key, getString(R.string.pref_key_main_loop_interval))) {
            long interval = settingsRepo.getLongValue(key);
            if (interval > 0) {
                double frequency = 1.0 * 1000 / interval;
                String tvFrequency = getString(R.string.main_work_frequency);
                tvFrequency = String.format(tvFrequency, FmtMicrometer.formatTwoDecimal(frequency));
                binding.tvFrequency.setText(Html.fromHtml(tvFrequency));
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity)
            activity = (MainActivity) context;
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeMainViewModel();
        subscribeSettingsChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    /**
     * 社区ListItem点击事件
     */
    @Override
    public void onItemClicked(@NonNull Community item) {
        activity.updateMainRightFragment(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_add_friends:
                ActivityUtil.startActivity(this, ExchangeActivity.class);
                break;
            case R.id.tv_create_communities:
                ActivityUtil.startActivity(this, CommunityCreateActivity.class);
                break;
//            case R.id.tv_frequency:
//                viewModel.showWorkFrequencyDialog(activity);
//                break;
        }
    }
}