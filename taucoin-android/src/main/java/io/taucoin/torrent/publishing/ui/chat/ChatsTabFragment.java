package io.taucoin.torrent.publishing.ui.chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.MsgAndReply;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Message;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.FragmentChatsTabBinding;
import io.taucoin.torrent.publishing.databinding.ItemOperationsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.CommunityTabFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.setting.FavoriteViewModel;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 聊天Tab页
 */
public class ChatsTabFragment extends CommunityTabFragment implements MsgListAdapter.ClickListener,
        View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger("ChatsTabFragment");
    private BaseActivity activity;
    private FragmentChatsTabBinding binding;
    private MsgViewModel msgViewModel;
    private UserViewModel userViewModel;
    private FavoriteViewModel favoriteViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MsgListAdapter adapter;
    private CommonDialog operationsDialog;

    private String chainID;
    private String replyID;
    private boolean isReadOnly = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chats_tab, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (BaseActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(this);
        msgViewModel = provider.get(MsgViewModel.class);
        userViewModel = provider.get(UserViewModel.class);
        favoriteViewModel = provider.get(FavoriteViewModel.class);
        binding.setListener(this);
        initParameter();
        initView();
        handleReadOnly(isReadOnly);
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            chainID = getArguments().getString(IntentExtra.CHAIN_ID);
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

    private final Runnable handleUpdateAdapter = () -> {
        if (binding.txList.getLayoutManager() != null) {
            int bottomPosition = adapter.getItemCount() - 1;
            logger.debug("handleUpdateAdapter scrollToPosition::{}", bottomPosition);
            binding.txList.getLayoutManager().scrollToPosition(bottomPosition);
        }
    };

    /**
     * 订阅社区交易列表
     */
    private void subscribeTxViewModel() {
        if (StringUtil.isNotEmpty(chainID)) {
            msgViewModel.observeMessages(chainID).observe(this, messages -> {
                adapter.submitList(messages, handleUpdateAdapter);
                logger.debug("messages.size::{}", messages.size());
            });
        }

        msgViewModel.getAddState().observe(this, result -> {
            if(isHidden()){
                return;
            }
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

    @Override
    public void onItemLongClicked(View view, MsgAndReply msg) {
        showItemOperationDialog(msg);
    }

    /**
     * 显示每个item长按操作选项对话框
     */
    private void showItemOperationDialog(MsgAndReply msg) {
        ItemOperationsBinding binding = DataBindingUtil.inflate(LayoutInflater.from(activity),
                R.layout.item_operations, null, false);
        binding.replay.setTag(msg);
        if (isReadOnly) {
            binding.replay.setVisibility(View.GONE);
        }
        binding.copy.setTag(msg.content);
        String link = Utils.parseUrlFormStr(msg.content);
        if(StringUtil.isNotEmpty(link)){
            binding.copyLink.setTag(link);
        }else{
            binding.copyLink.setVisibility(View.GONE);
        }
        if(StringUtil.isEquals(msg.senderPk,
                MainApplication.getInstance().getPublicKey())){
            binding.blacklist.setVisibility(View.GONE);
        }else {
            binding.blacklist.setTag(msg.senderPk);
        }
        binding.favourite.setTag(msg.msgID);
        binding.msgHash.setVisibility(View.GONE);
        binding.setListener(this);
        operationsDialog = new CommonDialog.Builder(activity)
                .setContentView(binding.getRoot())
                .enableWarpWidth(true)
                .create();
        operationsDialog.show();
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
    public void onBanClicked(MsgAndReply msg){
        String showName = UsersUtil.getShowName(msg.sender, msg.senderPk);
        userViewModel.showBanDialog(activity, msg.senderPk, showName);
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
                Message message = (Message) v.getTag();
                replyID = message.msgID;
                showReplyView(message);
                break;
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
                String msgID = ViewUtils.getStringTag(v);
                favoriteViewModel.addMsgFavorite(msgID);
                ToastUtils.showShortToast(R.string.favourite_successfully);
                break;
            case R.id.tv_send:
                sendMessage();
                break;
            case R.id.iv_replay_close:
                binding.rlReply.setVisibility(View.GONE);
                replyID = null;
                break;
        }
    }

    /**
     * 显示被回复的信息
     * @param msg
     */
    private void showReplyView(Message msg) {
        binding.rlReply.setVisibility(View.VISIBLE);
        String showName = UsersUtil.getDefaultName(msg.senderPk);
        binding.tvReplyName.setText(showName);
        binding.tvReplyMsg.setText(msg.content);
    }

    /**
     * 发送instant chat消息
     */
    private void sendMessage() {
        String content = ViewUtils.getText(binding.etMessage);
        Message msg =  new Message(chainID, content);
        if(StringUtil.isNotEmpty(replyID)){
            msg.replyID = replyID;
            binding.rlReply.setVisibility(View.GONE);
            replyID = null;
        }
        msgViewModel.sendMessage(msg);
    }

    @Override
    public void handleReadOnly(boolean isReadOnly) {
        if (null == binding) {
            return;
        }
        this.isReadOnly = isReadOnly;
        binding.etMessage.setEnabled(!isReadOnly);
        int drawable = isReadOnly ? R.drawable.grey_rect_round_bg : R.drawable.white_rect_round_bg;
        binding.etMessage.getText().clear();
        binding.etMessage.setBackgroundResource(drawable);
    }
}