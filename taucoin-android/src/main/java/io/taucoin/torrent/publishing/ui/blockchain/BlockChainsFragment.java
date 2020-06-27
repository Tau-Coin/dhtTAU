package io.taucoin.torrent.publishing.ui.blockchain;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.DrawablesUtil;
import io.taucoin.torrent.publishing.databinding.FragmentBlockChainsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.customviews.RecyclerViewSpacesItemDecoration;
import io.taucoin.torrent.publishing.ui.main.MainViewModel;

/**
 * Messages主页面相关内容
 */
public class BlockChainsFragment extends BaseFragment implements TxListAdapter.ClickListener, View.OnClickListener{

    private BaseActivity activity;
    private TxListAdapter adapter;
    private LinearLayoutManager layoutManager;
    private FragmentBlockChainsBinding binding;
    private MainViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Handler handler = new Handler();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_block_chains, container, false);
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
        binding.setEvent(this);
        initView();
        initUserInfo();
    }

    private void initView() {
        adapter = new TxListAdapter(this);
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
        HashMap<String, Integer> stringIntegerHashMap = new HashMap<>();
        stringIntegerHashMap.put(RecyclerViewSpacesItemDecoration.TOP_DECORATION, 15);
        binding.videoList.addItemDecoration(new RecyclerViewSpacesItemDecoration(stringIntegerHashMap));
        binding.videoList.setAdapter(adapter);

        binding.refreshLayout.setOnRefreshListener(this);
        binding.refreshLayout.setColorSchemeResources(R.color.color_yellow);
//        binding.refreshLayout.post(() -> binding.refreshLayout.setRefreshing(true));
    }

    /**
     * 初始化用户信息
     */
    private void initUserInfo() {
        binding.itemAddress.tvName.setText("TestUser");
        binding.itemAddress.tvBalance.setText("100");
        binding.itemAddress.tvAddress.setText("TxasdasdhjashdjhsajdhjashdjshjhhjhQMN");
        DrawablesUtil.setEndDrawable(binding.itemAddress.tvAddress, R.mipmap.icon_copy, 20);
        String defaultCommunity = getString(R.string.community_name_tau);
        binding.tvCommunityName.setText(getString(R.string.community_name_title, defaultCommunity));
        binding.tvSyncRate.setText(getString(R.string.community_sync_rate, 0));
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
    public void onItemClicked(@NonNull TxListItem item) {

    }

    /**
     * VideoListItem暂停事件
     */
    @Override
    public void onItemPauseClicked(@NonNull TxListItem item) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.ll_community_selected:
                break;
            case R.id.iv_mining_switch:
                float rotation = view.getRotation();
                view.setRotation(rotation < 0 ? 90 : -90);
                break;
            case R.id.ll_mining_fold:
                rotation = binding.ivMiningFold.getRotation();
                binding.llMiningContent.setVisibility(rotation > 0 ? View.VISIBLE : View.GONE);
                binding.ivMiningFold.setRotation(rotation > 0 ? -90 : 90);
                break;
            case R.id.ll_txs_fold:
                rotation = binding.ivTxsFold.getRotation();
                binding.rlTxsContent.setVisibility(rotation > 0 ? View.VISIBLE : View.GONE);
                binding.ivTxsFold.setRotation(rotation > 0 ? -90 : 90);
                break;
        }
    }

    @Override
    public void onRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                binding.refreshLayout.setRefreshing(false);
            }
        }, 1000);
    }
}
