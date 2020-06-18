package io.taucoin.torrent.publishing.ui.message;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leinardi.android.speeddial.SpeedDialActionItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.databinding.FragmentMainBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.customviews.RecyclerViewDividerDecoration;
import io.taucoin.torrent.publishing.ui.main.MainViewModel;

/**
 * Messages主页面相关内容
 */
public class MessageFragment extends BaseFragment implements MessageListAdapter.ClickListener{

    private BaseActivity activity;
    private MessageListAdapter adapter;
    private LinearLayoutManager layoutManager;
    private FragmentMainBinding binding;
    private MainViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
//
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (activity == null){
            activity = (BaseActivity) getActivity();
        }
        ViewModelProvider provider = new ViewModelProvider(activity);
        viewModel = provider.get(MainViewModel.class);
        adapter = new MessageListAdapter(this);
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
        binding.videoList.setLayoutManager(layoutManager);
        binding.videoList.setItemAnimator(animator);
        binding.videoList.setEmptyView(binding.emptyViewTorrentList);
        TypedArray a = activity.obtainStyledAttributes(new TypedValue().data, new int[]{R.attr.divider});
        binding.videoList.addItemDecoration(new RecyclerViewDividerDecoration(a.getDrawable(0)));
        a.recycle();
        binding.videoList.setAdapter(adapter);
        initFabSpeedDial();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BaseActivity)
            activity = (BaseActivity)context;
    }

    /**
     * 初始化右下角悬浮按钮组件
     */
    private void initFabSpeedDial() {
        binding.fabButton.setOnActionSelectedListener((item) -> {
            switch (item.getId()) {
                case R.id.main_publish_video:
                    break;
                case R.id.main_create_community:
                    break;
                case R.id.main_other_transaction:
                    break;
                default:
                    return false;
            }
            binding.fabButton.close();
            return true;
        });

        binding.fabButton.addActionItem(new SpeedDialActionItem.Builder(
                R.id.main_other_transaction,
                R.drawable.ic_add_36dp)
                .setLabel(R.string.main_other_transaction)
                .create());

        binding.fabButton.addActionItem(new SpeedDialActionItem.Builder(
                R.id.main_create_community,
                R.drawable.ic_add_36dp)
                .setLabel(R.string.main_create_community)
                .create());

        binding.fabButton.addActionItem(new SpeedDialActionItem.Builder(
                R.id.main_publish_video,
                R.drawable.ic_add_36dp)
                .setLabel(R.string.main_publish_video)
                .create());
    }

    /**
     * VideoListItem点击事件
     */
    @Override
    public void onItemClicked(@NonNull MessageListItem item) {

    }

    /**
     * VideoListItem暂停事件
     */
    @Override
    public void onItemPauseClicked(@NonNull MessageListItem item) {

    }
}
