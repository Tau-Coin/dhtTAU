package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.ui.CommunityTabFragment;
import io.taucoin.torrent.publishing.ui.setting.FavoriteViewModel;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.types.TypesConfig;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.UserAndTx;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.FragmentTxsTabBinding;
import io.taucoin.torrent.publishing.databinding.ItemOperationsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 交易Tab页
 */
public class TxsTabFragment extends CommunityTabFragment implements TxListAdapter.ClickListener,
        View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger("TxsTabFragment");
    private BaseActivity activity;
    private FragmentTxsTabBinding binding;
    private TxViewModel txViewModel;
    private UserViewModel userViewModel;
    private FavoriteViewModel favoriteViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private TxListAdapter adapter;
    private CommonDialog operationsDialog;

    private String chainID;
    private int txType;
    private boolean isReadOnly = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_txs_tab, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (BaseActivity) getActivity();
        assert activity != null;
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        favoriteViewModel = provider.get(FavoriteViewModel.class);
        initParameter();
        initView();
        initFabSpeedDial();
        handleReadOnly(isReadOnly);
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            chainID = getArguments().getString(IntentExtra.CHAIN_ID);
            txType = getArguments().getInt(IntentExtra.TYPE, -1);
            if(txType == -1){
                binding.fabButton.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 初始化视图
     */
    private void initView() {
        adapter = new TxListAdapter(this, chainID);
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
    }

    private final Runnable handleUpdateAdapter = () -> {
        if (binding.txList.getLayoutManager() != null) {
            int bottomPosition = adapter.getItemCount() - 1;
            logger.debug("handleUpdateAdapter scrollToPosition::{}", bottomPosition);
            binding.txList.getLayoutManager().scrollToPosition(bottomPosition);
        }
    };

    /**
     * 初始化右下角悬浮按钮组件
     */
    private void initFabSpeedDial() {
        // 自定义点击事件
        binding.fabButton.getMainFab().setOnClickListener(v ->{
            if(isReadOnly){
                return;
            }
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, chainID);
            if(txType == TypesConfig.TxType.WCoinsType.ordinal()){
                ActivityUtil.startActivity(intent, this, TransactionCreateActivity.class);
            }else{
                ActivityUtil.startActivity(intent, this, MessageActivity.class);
            }
        });
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
        txViewModel.observerCommunityTxs(chainID, txType).observe(this, replyAndAllTxs -> {
            adapter.submitList(replyAndAllTxs, handleUpdateAdapter);
            logger.debug("adapter.size::{}, newSize::{}", adapter.getItemCount(), replyAndAllTxs.size());
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onItemLongClicked(UserAndTx tx, String msg) {
        showItemOperationDialog(tx, msg);
    }

    @Override
    public void onUserClicked(String senderPk) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, senderPk);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }
    @Override
    public void onEditNameClicked(String senderPk){
        userViewModel.showEditNameDialog(activity, senderPk);
    }
    @Override
    public void onBanClicked(UserAndTx tx){
        String showName = UsersUtil.getShowName(tx.sender, tx.senderPk);
        userViewModel.showBanDialog(activity, tx.senderPk, showName);
    }

    /**
     * 显示每个item长按操作选项对话框
     */
    private void showItemOperationDialog(UserAndTx tx, String msg) {
        ItemOperationsBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.item_operations, null, false);
        binding.setListener(this);
        binding.replay.setVisibility(View.GONE);
        // 用户不能拉黑自己
        if(StringUtil.isEquals(tx.senderPk,
                MainApplication.getInstance().getPublicKey())){
            binding.blacklist.setVisibility(View.GONE);
        }
        binding.replay.setTag(tx);
        binding.copy.setTag(msg);
        String link = Utils.parseUrlFormStr(msg);
        if(StringUtil.isNotEmpty(link)){
            binding.copyLink.setTag(link);
        }else{
            binding.copyLink.setVisibility(View.GONE);
        }
        binding.blacklist.setTag(tx.senderPk);
        binding.favourite.setTag(tx.txID);
        binding.msgHash.setTag(tx.txID);
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
        switch (v.getId()){
//            case R.id.replay:
//                UserAndTx tx = (UserAndTx) v.getTag();
//                Intent intent = new Intent();
//                intent.putExtra(IntentExtra.BEAN, community);
//                ActivityUtil.startActivity(intent, this, MessageActivity.class);
//                break;
            case R.id.copy:
                String msg = ViewUtils.getStringTag(v);
                CopyManager.copyText(msg);
                ToastUtils.showShortToast(R.string.copy_successfully);
                break;
            case R.id.copy_link:
                String link = ViewUtils.getStringTag(v);
                CopyManager.copyText(link);
                ToastUtils.showShortToast(R.string.copy_link_successfully);
                break;
            case R.id.blacklist:
                String publicKey = ViewUtils.getStringTag(v);
                userViewModel.setUserBlacklist(publicKey, true);
                ToastUtils.showShortToast(R.string.blacklist_successfully);
                break;
            case R.id.favourite:
                String txID = ViewUtils.getStringTag(v);
                favoriteViewModel.addTxFavorite(txID);
                ToastUtils.showShortToast(R.string.favourite_successfully);
                break;
            case R.id.msg_hash:
                String msgHash = ViewUtils.getStringTag(v);
                CopyManager.copyText(msgHash);
                ToastUtils.showShortToast(R.string.copy_message_hash);
                break;
        }
    }

    @Override
    public void handleReadOnly(boolean isReadOnly) {
        if (null == binding) {
            return;
        }
        this.isReadOnly = isReadOnly;
        int color = isReadOnly ? R.color.gray_light : R.color.primary;
        binding.fabButton.setMainFabClosedBackgroundColor(getResources().getColor(color));
    }
}
