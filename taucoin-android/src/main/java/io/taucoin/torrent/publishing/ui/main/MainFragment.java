package io.taucoin.torrent.publishing.ui.main;

import android.content.Context;
import android.os.Bundle;
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
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauInfoProvider;
import io.taucoin.torrent.publishing.core.model.data.CommunityAndFriend;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.DeviceUtils;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.databinding.FragmentMainBinding;
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
    private TauInfoProvider infoProvider;
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
        infoProvider = TauInfoProvider.getInstance(MainApplication.getInstance());
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
    }

    private void subscribeMainViewModel() {
        disposables.add(viewModel.observeCommunitiesAndFriends()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showCommunityList));

        disposables.add(infoProvider.observeSessionStats()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(nodes -> {
                    binding.llWarning.setVisibility(View.VISIBLE);
                    if (nodes <= 0) {
                        binding.tvWarning.setText(getString(R.string.main_connecting));
                    } else if (!NetworkSetting.isHaveAvailableData()) {
                        binding.tvWarning.setText(getString(R.string.main_data_used_up));
                    } else if (DeviceUtils.isSpaceInsufficient()) {
                        binding.tvWarning.setText(getString(R.string.main_insufficient_device_space));
                    } else {
                        binding.llWarning.setVisibility(View.GONE);
                    }
                }));
    }

    private void showCommunityList(List<CommunityAndFriend> communities) {
        if(communities != null){
            adapter.submitList(communities);
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
    public void onItemClicked(@NonNull CommunityAndFriend item) {
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