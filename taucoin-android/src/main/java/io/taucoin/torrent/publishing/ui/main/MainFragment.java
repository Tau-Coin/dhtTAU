package io.taucoin.torrent.publishing.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.databinding.FragmentMainBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.group.GroupActivity;

/**
 * 群组列表页面
 */
public class MainFragment extends BaseFragment implements GroupListAdapter.ClickListener{

    private BaseActivity activity;
    private GroupListAdapter adapter;
    private LinearLayoutManager layoutManager;
    private FragmentMainBinding binding;
    private MainViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (BaseActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(activity);
        viewModel = provider.get(MainViewModel.class);
        initView();
    }

    private void initView() {
        adapter = new GroupListAdapter(this);
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
        layoutManager = new LinearLayoutManager(activity);
        binding.groupList.setLayoutManager(layoutManager);
        binding.groupList.setItemAnimator(animator);
        binding.groupList.setEmptyView(binding.emptyViewGroupList);
        binding.groupList.setAdapter(adapter);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BaseActivity)
            activity = (BaseActivity)context;
    }

    /**
     * VideoListItem点击事件
     */
    @Override
    public void onItemClicked(@NonNull CommunityItem item) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.BEAN, item);
        ActivityUtil.startActivity(intent, this, GroupActivity.class);
    }
}
