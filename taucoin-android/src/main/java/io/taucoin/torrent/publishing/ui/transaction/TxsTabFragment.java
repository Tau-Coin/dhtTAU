package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leinardi.android.speeddial.SpeedDialActionItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.ReplyAndTx;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.FragmentTxsTabBinding;
import io.taucoin.torrent.publishing.databinding.ItemOperationsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 交易Tab页
 */
public class TxsTabFragment extends BaseFragment implements TxListAdapter.ClickListener, View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger("TxsTabFragment");
    private BaseActivity activity;
    private FragmentTxsTabBinding binding;
    private TxViewModel txViewModel;
    private UserViewModel userViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private TxListAdapter adapter;
    private CommonDialog operationsDialog;

    private Community community;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_txs_tab, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (BaseActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(activity);
        txViewModel = provider.get(TxViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        initParameter();
        initView();
        initFabSpeedDial();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            community = getArguments().getParcelable(IntentExtra.BEAN);
        }
    }

    /**
     * 初始化视图
     */
    private void initView() {
        adapter = new TxListAdapter(this);
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.txList.setLayoutManager(layoutManager);

        binding.txList.setItemAnimator(animator);
        binding.txList.setAdapter(adapter);

        PagedList.Config pagedListConfig = new PagedList.Config.Builder()
                .setEnablePlaceholders(Page.ENABLE_PLACEHOLDERS)
                .setPageSize(Page.PAGE_SIZE)
                .setInitialLoadSizeHint(Page.PAGE_SIZE)
                .build();
        LiveData<PagedList<ReplyAndTx>> postList = new LivePagedListBuilder<>(
                txViewModel.queryCommunityTxs(community.chainID), pagedListConfig).build();
        postList.observe(activity, replyAndAllTxs -> {
            adapter.submitList(replyAndAllTxs);
            logger.debug("adapter.size::{}, newSize::{}", adapter.getItemCount(), replyAndAllTxs.size());
            binding.txList.scrollToPosition(adapter.getItemCount() - 1);
        });
    }

    /**
     * 初始化右下角悬浮按钮组件
     */
    private void initFabSpeedDial() {
        binding.fabButton.setOnActionSelectedListener((item) -> {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.BEAN, community);
            switch (item.getId()) {
                case R.id.community_transaction:
                    ActivityUtil.startActivity(intent, this, TransactionCreateActivity.class);
                    break;
                case R.id.community_message:
                    ActivityUtil.startActivity(intent, this, MessageActivity.class);
                    break;
                default:
                    return false;
            }
            binding.fabButton.close();
            return true;
        });

        binding.fabButton.addActionItem(new SpeedDialActionItem.Builder(
                R.id.community_transaction,
                R.drawable.ic_add_36dp)
                .setLabel(R.string.community_transaction)
                .create());

        binding.fabButton.addActionItem(new SpeedDialActionItem.Builder(
                R.id.community_message,
                R.drawable.ic_add_36dp)
                .setLabel(R.string.community_message)
                .create());
    }

    /**
     * 订阅社区交易列表
     */
    private void subscribeTxViewModel() {
        if(null == community){
            return;
        }
//        disposables.add(txViewModel.observeTxsByChainID(community.chainID)
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe(list -> {
//                adapter.setDataList(list);
//                binding.txList.smoothScrollToPosition(adapter.getItemCount());
//            }));
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BaseActivity)
            activity = (BaseActivity)context;
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeTxViewModel();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    /**
     * TxListItem点击事件
     */

    @Override
    public void onItemClicked(ReplyAndTx tx) {

    }

    @Override
    public void onItemLongClicked(ReplyAndTx tx, String msg) {
        showItemOperationDialog(tx, msg);
    }

    /**
     * 显示每个item长按操作选项对话框
     */
    private void showItemOperationDialog(ReplyAndTx tx, String msg) {
        ItemOperationsBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.item_operations, null, false);
        binding.setListener(this);
        // 用户不能回复自己的消息
        if(StringUtil.isEquals(tx.senderPk,
                MainApplication.getInstance().getPublicKey())){
            binding.replay.setVisibility(View.GONE);
            binding.blacklist.setVisibility(View.GONE);
        }
        binding.replay.setTag(tx.txID);
        binding.copy.setTag(msg);
        binding.copyLink.setTag(msg);
        binding.blacklist.setTag(tx.senderPk);
        binding.favourite.setTag(tx.txID);
        operationsDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .enableWarpWidth(true)
                .create();
        operationsDialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(operationsDialog != null){
            operationsDialog.closeDialog();
        }
    }

    @Override
    public void onClick(View v) {
        if(operationsDialog != null){
            operationsDialog.closeDialog();
        }
        String tag = ViewUtils.getStringTag(v);
        switch (v.getId()){
            case R.id.replay:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.BEAN, community);
                intent.putExtra(IntentExtra.REPLY_ID, tag);
                ActivityUtil.startActivity(intent, this, MessageActivity.class);
                break;
            case R.id.copy:
            case R.id.copy_link:
                CopyManager.copyText(tag);
                break;
            case R.id.blacklist:
                userViewModel.setUserBlacklist(tag, true);
                break;
            case R.id.favourite:
                break;
        }
    }
}