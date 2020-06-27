package io.taucoin.torrent.publishing.ui.video;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
 * Video主页面相关内容
 */
public class VideoFragment extends BaseFragment implements VideoListAdapter.ClickListener{

    private BaseActivity activity;
    private VideoListAdapter adapter;
    private LinearLayoutManager layoutManager;
    private FragmentMainBinding binding;
    private MainViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Handler handler = new Handler();

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
        initView();
    }

    private void initView() {
        adapter = new VideoListAdapter(this);
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
        binding.videoList.setAdapter(adapter);

        binding.refreshLayout.setOnRefreshListener(this);
        binding.refreshLayout.setColorSchemeResources(R.color.color_yellow);
        binding.refreshLayout.post(() -> binding.refreshLayout.setRefreshing(true));

        binding.videoList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                Log.d("test", "StateChanged = " + newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                Log.d("test", "onScrolled");

                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                if (lastVisibleItemPosition + 1 == adapter.getItemCount()) {
                    Log.d("test", "loading executed");

                    boolean isRefreshing = binding.refreshLayout.isRefreshing();
                    if (isRefreshing) {
                        adapter.notifyItemRemoved(adapter.getItemCount());
                        return;
                    }
                    adapter.setDataCount(adapter.getItemCount() + 10);
                    getData();
                    Log.d("test", "load more completed");
                }
            }
        });
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
    public void onItemClicked(@NonNull VideoListItem item) {

    }

    /**
     * VideoListItem暂停事件
     */
    @Override
    public void onItemPauseClicked(@NonNull VideoListItem item) {

    }

    @Override
    public void onRefresh() {
        adapter.setDataCount(20);
        getData();
    }

    private void getData() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
                binding.refreshLayout.setRefreshing(false);
                adapter.notifyItemRemoved(adapter.getItemCount());
            }
        }, 1000);
    }
}
