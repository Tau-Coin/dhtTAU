package io.taucoin.torrent.publishing.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsg;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.ChatMsgLog;
import io.taucoin.torrent.publishing.core.utils.KeyboardUtils;
import io.taucoin.torrent.publishing.core.utils.MediaUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.FragmentChatBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.MsgLogsDialog;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.types.MessageType;

import static android.app.Activity.RESULT_OK;

/**
 * 单个朋友聊天页面
 */
public class ChatFragment extends BaseFragment implements View.OnClickListener,
    ChatListAdapter.ClickListener{

    private MainActivity activity;
    private static final Logger logger = LoggerFactory.getLogger("ChatFragment");
    private FragmentChatBinding binding;
    private ChatViewModel chatViewModel;
    private CommunityViewModel communityViewModel;
    private ChatListAdapter adapter;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Disposable logsDisposable;
    private String friendPK;
    private Handler handler = new Handler();
    private MsgLogsDialog msgLogsDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false);
        binding.setListener(this);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (MainActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        chatViewModel = provider.get(ChatViewModel.class);
        chatViewModel.observeNeedStartDaemon();
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            friendPK = getArguments().getString(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.ivBack.setOnClickListener(v -> {
            KeyboardUtils.hideSoftInput(activity);
            activity.goBack();
        });
        binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isEmpty = StringUtil.isEmpty(s);
                binding.tvSend.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.ivAdd.setVisibility(!isEmpty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        binding.etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            showOrHideChatAddView(false);
            handler.postDelayed(handleUpdateAdapter, 200);
        });

        binding.etMessage.setOnClickListener(v -> {
            showOrHideChatAddView(false);
            handler.postDelayed(handleUpdateAdapter, 200);
        });

        binding.chatAdd.setVisibility(View.GONE);
        binding.chatAdd.setListener((title, icon) -> {
            if (R.string.chat_album == title) {
                MediaUtil.startOpenGallery(activity);
            } else if (R.string.chat_take_picture == title) {
                MediaUtil.startOpenCamera(activity);
            }
        });

        adapter = new ChatListAdapter(this);
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
//        layoutManager.setReverseLayout(true);
        binding.msgList.setLayoutManager(layoutManager);
        binding.msgList.setItemAnimator(animator);
        binding.msgList.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeChatViewModel();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    private final Runnable handleUpdateAdapter = () -> {
        if (binding.msgList.getLayoutManager() != null) {
            int bottomPosition = adapter.getItemCount() - 1;
            logger.debug("handleUpdateAdapter scrollToPosition::{}", bottomPosition);
            binding.msgList.getLayoutManager().scrollToPosition(bottomPosition);
        }
    };

    /**
     * 订阅社区相关的被观察者
     */
    private void subscribeChatViewModel() {
        chatViewModel.observerChat(friendPK).observe(this, messages -> {
            adapter.submitList(messages, handleUpdateAdapter);
            logger.debug("messages.size::{}", messages.size());
        });
        disposables.add(communityViewModel.observerCommunityByChainID(friendPK)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(community -> {
                    binding.toolbarInclude.tvTitle.setText(community.communityName);
                }));
        chatViewModel.getChatResult().observe(this, result -> {
            if (!result.isSuccess()) {
                ToastUtils.showShortToast(result.getMsg());
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_add:
                showOrHideChatAddView(true);
                break;
            case R.id.tv_send:
                sendMessage();
                break;
            default:
                break;
        }
    }

    /**
     * 显示聊天加号试图
     */
    private void showOrHideChatAddView(boolean isShow) {
        if (isShow) {
            KeyboardUtils.hideSoftInput(activity);
        }
        disposables.add(Observable.timer(100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(aLong -> {
                    binding.chatAdd.setVisibility(isShow ? View.VISIBLE : View.GONE);
                    handler.post(handleUpdateAdapter);
                }));
    }

    /**
     * 发送chat消息
     */
    private void sendMessage() {
        String message = ViewUtils.getText(binding.etMessage);
        chatViewModel.sendMessage(friendPK, message, MessageType.TEXT.ordinal());
        binding.etMessage.getText().clear();
    }

    @Override
    public void onMsgLogsClicked(ChatMsg msg) {
        if (logsDisposable != null) {
            disposables.remove(logsDisposable);
        }
        logsDisposable = chatViewModel.observerMsgLogs(msg.hash)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showMsgLogsDialog);
        disposables.add(logsDisposable);
    }

    /**
     * 显示消息的
     * @param logs
     */
    private void showMsgLogsDialog(List<ChatMsgLog> logs) {
        if (msgLogsDialog != null && msgLogsDialog.isShowing()) {
            msgLogsDialog.submitList(logs);
            return;
        }
        msgLogsDialog = new MsgLogsDialog.Builder(activity)
                .setMsgLogsListener(new MsgLogsDialog.MsgLogsListener() {
                    @Override
                    public void onRetry() {

                    }

                    @Override
                    public void onCancel() {
                        if (logsDisposable != null) {
                            disposables.remove(logsDisposable);
                        }
                    }
                }).create();
        msgLogsDialog.submitList(logs);
        msgLogsDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode){
                case PictureConfig.CHOOSE_REQUEST:
                case PictureConfig.REQUEST_CAMERA:
                    List<LocalMedia> selectList = PictureSelector.obtainMultipleResult(data);
                    if(selectList != null && selectList.size() == 1){
                        LocalMedia localMedia = selectList.get(0);
                        if(PictureMimeType.eqImage(localMedia.getMimeType())){
                            String imagePath = localMedia.getPath();
                            if (StringUtil.isNotEmpty(localMedia.getAndroidQToPath())) {
                                imagePath = localMedia.getAndroidQToPath();
                            }
                            chatViewModel.sendMessage(friendPK, imagePath,
                                    MessageType.PICTURE.ordinal());
                            return;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}