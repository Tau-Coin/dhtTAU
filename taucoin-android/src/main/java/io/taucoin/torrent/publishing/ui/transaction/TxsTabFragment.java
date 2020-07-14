package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Context;
import android.os.Bundle;
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
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.entity.Community;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;
import io.taucoin.torrent.publishing.databinding.FragmentTxsTabBinding;
import io.taucoin.torrent.publishing.databinding.ItemOperationsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;

/**
 * 交易Tab页
 */
public class TxsTabFragment extends BaseFragment implements TxListAdapter.ClickListener, View.OnClickListener {

    public static final int TAB_CHAIN_NOTE = 0;
    public static final int TAB_INSTANT_CHAT = 1;
    private BaseActivity activity;
    private FragmentTxsTabBinding binding;
    private TxViewModel txViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private TxListAdapter adapter;
    private CommonDialog operationsDialog;

    private Community community;
    private int tabType;

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
        initParameter();
        initView();
//        initFabSpeedDial();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            community = getArguments().getParcelable(IntentExtra.BEAN);
            tabType = getArguments().getInt(IntentExtra.TYPE);
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
    }

    /**
     * 订阅社区交易列表
     */
    private void subscribeTxViewModel() {
        if(null == community){
            return;
        }
        disposables.add(txViewModel.observeTxsByChainID(community.chainID, tabType)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(list -> adapter.setDataList(list)));
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
    public void onItemClicked(Tx tx) {

    }

    @Override
    public void onItemLongClicked(View view, Tx tx) {
        showItemOperationDialog(view, tx);
    }

    /**
     * 显示每个item长按操作选项对话框
     */
    private void showItemOperationDialog(View view, Tx tx) {
        ItemOperationsBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.item_operations, null, false);
        binding.setListener(this);
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
            case R.id.replay:
                break;
            case R.id.copy:
                break;
            case R.id.copy_link:
                break;
            case R.id.forward:
                break;
            case R.id.blacklist:
                break;
            case R.id.favourite:
                break;
        }
    }
}