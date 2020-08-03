package io.taucoin.torrent.publishing.ui.chat;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.FragmentChatsTabBinding;
import io.taucoin.torrent.publishing.databinding.ItemOperationsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;

/**
 * 聊天Tab页
 */
public class ChatsTabFragment extends BaseFragment implements MsgListAdapter.ClickListener, View.OnClickListener {

    private BaseActivity activity;
    private FragmentChatsTabBinding binding;
    private MsgViewModel msgViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MsgListAdapter adapter;
    private CommonDialog operationsDialog;

    private Community community;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chats_tab, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (BaseActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(activity);
        msgViewModel = provider.get(MsgViewModel.class);
        binding.setListener(this);
        initParameter();
        initView();
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
        adapter = new MsgListAdapter(this);
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
//        layoutManager.setReverseLayout(true);
        binding.txList.setLayoutManager(layoutManager);
        binding.txList.setItemAnimator(animator);
        binding.txList.setAdapter(adapter);

        binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isHiddenSend = StringUtil.isEmpty(s);
                binding.tvSend.setVisibility(isHiddenSend ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * 订阅社区交易列表
     */
    private void subscribeTxViewModel() {
        if(null == community){
            return;
        }
        disposables.add(msgViewModel.observeMessagesByChainID(community.chainID)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(list -> {
                adapter.setDataList(list);
                binding.txList.smoothScrollToPosition(adapter.getItemCount());
            }));

        msgViewModel.getAddState().observe(this, result -> {
            if(StringUtil.isNotEmpty(result)){
                ToastUtils.showShortToast(result);
            }else {
                binding.etMessage.getText().clear();
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
    public void onItemClicked(Message msg) {

    }

    @Override
    public void onItemLongClicked(View view, Message msg) {
        showItemOperationDialog(view, msg);
    }

    /**
     * 显示每个item长按操作选项对话框
     */
    private void showItemOperationDialog(View view, Message msg) {
        ItemOperationsBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.item_operations, null, false);
        binding.replay.setVisibility(View.GONE);
        binding.copy.setTag(msg);
        binding.copyLink.setTag(msg);
        binding.blacklist.setTag(msg.senderPk);
        binding.favourite.setTag(msg.msgHash);
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
            case R.id.blacklist:
                break;
            case R.id.favourite:
                break;
            case R.id.tv_send:
                sendMessage();
                break;
        }
    }

    /**
     * 发送instant chat消息
     */
    private void sendMessage() {
        String chainID = community.chainID;
        String content = ViewUtils.getText(binding.etMessage);
        Message msg =  new Message(chainID, content);
        msgViewModel.sendMessage(msg);
    }
}