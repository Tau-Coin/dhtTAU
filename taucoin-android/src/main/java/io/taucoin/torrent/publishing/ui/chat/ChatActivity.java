package io.taucoin.torrent.publishing.ui.chat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.KeyboardUtils;
import io.taucoin.torrent.publishing.core.utils.MediaUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityChatBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.ChatAddView;

/**
 * 单个Peers聊天页面
 */
public class ChatActivity extends BaseActivity implements View.OnClickListener{

    private static final Logger logger = LoggerFactory.getLogger("ChatActivity");
    private ActivityChatBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private String friendPK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat);
        binding.setListener(this);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getIntent() != null){
            friendPK = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);

        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isHiddenSend = StringUtil.isEmpty(s);
                binding.tvSend.setVisibility(isHiddenSend ? View.GONE : View.VISIBLE);
                binding.ivAdd.setVisibility(!isHiddenSend ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        binding.etMessage.setOnFocusChangeListener((v, hasFocus) ->
                showOrHideChatAddView(false));
        binding.etMessage.setOnClickListener(v ->
                showOrHideChatAddView(false));

        binding.chatAdd.setVisibility(View.GONE);
        binding.chatAdd.setListener((title, icon) -> {
            if (R.string.chat_album == title) {
                MediaUtil.startOpenGallery(this);
            } else if (R.string.chat_take_picture == title) {
                MediaUtil.startOpenCamera(this);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCommunityViewModel();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        if(KeyboardUtils.isSoftInputVisible(this)){
            KeyboardUtils.hideSoftInput(this);
        }
        super.onDestroy();
    }

    /**
     * 订阅社区相关的被观察者
     */
    private void subscribeCommunityViewModel() {
        disposables.add(communityViewModel.observerCommunityByChainID(friendPK)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(community -> {
                    binding.toolbarInclude.toolbar.setTitle(community.communityName);
                }));

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
            KeyboardUtils.hideSoftInput(this);
        }
        disposables.add(Observable.timer(50, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(aLong -> binding.chatAdd.setVisibility(isShow ? View.VISIBLE : View.GONE)));
    }

    /**
     * 发送chat消息
     */
    private void sendMessage() {
        String content = ViewUtils.getText(binding.etMessage);
        binding.etMessage.getText().clear();
    }
}
