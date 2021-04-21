package io.taucoin.torrent.publishing.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.Interval;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ActivityUserDetailBinding;
import io.taucoin.torrent.publishing.databinding.ViewDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.qrcode.UserQRCodeActivity;

/**
 * 用户详情
 */
public class UserDetailActivity extends BaseActivity implements View.OnClickListener,
        UserCommunityListAdapter.ClickListener {
    public static final int TYPE_COMMUNITY = 0x01;
    public static final int TYPE_FRIEND_LIST = 0x02;
    public static final int TYPE_CHAT_PAGE = 0x03;
    private ActivityUserDetailBinding binding;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private UserCommunityListAdapter adapter;
    private CommonDialog shareQRDialog;
    private String publicKey;
    private String nickName;
    private UserAndFriend user;
    private int type;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        userViewModel.observeNeedStartDaemon();
        communityViewModel = provider.get(CommunityViewModel.class);
        communityViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_detail);
        binding.setListener(this);
        initParam();
        initView();
    }

    /**
     * 初始化参数
     */
    private void initParam() {
        if (getIntent() != null) {
            type = getIntent().getIntExtra(IntentExtra.TYPE, TYPE_COMMUNITY);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        if(getIntent() != null){
            publicKey = getIntent().getStringExtra(IntentExtra.PUBLIC_KEY);
            nickName = getIntent().getStringExtra(IntentExtra.NICK_NAME);
        }
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle("");
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new UserCommunityListAdapter(this);
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
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(animator);
        binding.recyclerList.setAdapter(adapter);

        // 获取用户详情数据
        userViewModel.getUserDetail(publicKey);

        boolean isMine = StringUtil.isEquals(publicKey, MainApplication.getInstance().getPublicKey());
        if ((type == TYPE_CHAT_PAGE || type == TYPE_FRIEND_LIST) && !isMine) {
            binding.tvBan.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        userViewModel.getUserDetail().observe(this, this::showUserInfo);

        userViewModel.getAddFriendResult().observe(this, result -> {
            closeProgressDialog();
            if (result.isSuccess()) {
                ToastUtils.showShortToast(R.string.contacts_friend_already_exists);
            } else {
                binding.tvAddToContact.setVisibility(View.GONE);
                binding.tvShareQr.setVisibility(View.VISIBLE);
                showShareQRDialog();
            }
        });

        communityViewModel.getChatState().observe(this, state -> {
            closeProgressDialog();
            if (!state.isSuccess()) {
                ToastUtils.showShortToast(state.getMsg());
            } else {
                onBackPressed();
                openChatActivity(state.getMsg());
            }
        });
    }

    private void showUserInfo(UserAndFriend userInfo) {
        boolean isMine = StringUtil.isEquals(publicKey, MainApplication.getInstance().getPublicKey());
        binding.tvAddToContact.setVisibility(!isMine && userInfo.isDiscovered() ? View.VISIBLE : View.GONE);
        boolean isShowChat = !isMine && userInfo.isConnected() && type != TYPE_CHAT_PAGE;
        binding.tvStartChat.setVisibility(isShowChat ? View.VISIBLE : View.GONE);
        binding.tvShareQr.setVisibility(!isMine && userInfo.isAdded() ? View.VISIBLE : View.GONE);
        this.user = userInfo;
        String showName = UsersUtil.getCurrentUserName(user);
        if (StringUtil.isNotEmpty(nickName)) {
            showName = nickName;
        }
        binding.tvName.setText(showName);
        binding.leftView.setText(StringUtil.getFirstLettersOfName(showName));
        binding.leftView.setBgColor(Utils.getGroupColor(user.publicKey));
        binding.tvPublicKey.setText(UsersUtil.getMidHideName(user.publicKey));
        binding.ivPublicKeyCopy.setOnClickListener(v -> {
            copyPublicKey(user.publicKey);
        });
        binding.tvPublicKey.setOnClickListener(v -> {
            copyPublicKey(user.publicKey);
        });
        if(user.members != null){
            adapter.setDataList(user.members);
            if (user.members.size() > 0) {
                binding.llMutualCommunities.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 复制公钥
     * @param publicKey public Key
     */
    private void copyPublicKey(String publicKey) {
        CopyManager.copyText(publicKey);
        ToastUtils.showShortToast(R.string.copy_public_key);
    }

    private void showShareQRDialog() {
        if (shareQRDialog != null && shareQRDialog.isShowing()) {
            return;
        }
        ViewDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.view_dialog, null, false);
        binding.tvMsg.setText(R.string.contacts_confirm_shared);
        binding.tvMsg.setTextColor(getResources().getColor(R.color.color_black));
        shareQRDialog = new CommonDialog.Builder(this)
                .setContentView(binding.getRoot())
                .setHorizontal()
                .setPositiveButton(R.string.common_share, (dialog, which) -> {
                        Intent intent = new Intent();
                        intent.putExtra(IntentExtra.TYPE, UserQRCodeActivity.TYPE_QR_SHARE);
                        ActivityUtil.startActivity(intent, UserDetailActivity.this, UserQRCodeActivity.class);
                    })
                .setNegativeButton(R.string.common_later, (dialog, which) -> dialog.cancel())
                .setCanceledOnTouchOutside(false)
                .create();
        shareQRDialog.show();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_add_to_contact:
                showProgressDialog();
                userViewModel.addFriend(publicKey);
                break;
            case R.id.tv_start_chat:
                showProgressDialog();
                communityViewModel.createChat(user.publicKey);
                break;
            case R.id.tv_share_qr:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.TYPE, UserQRCodeActivity.TYPE_QR_SHARE);
                ActivityUtil.startActivity(intent, UserDetailActivity.this, UserQRCodeActivity.class);
                break;
            case R.id.tv_ban:
                userViewModel.setUserBlacklist(publicKey, true);
                ToastUtils.showShortToast(R.string.blacklist_successfully);
                onBackPressed();
                break;
        }
    }

    @Override
    public void onItemClicked(@NonNull Member member) {
        openCommunityActivity(member.chainID);
    }

    /**
     * 打开社区页面
     * @param chainID
     */
    private void openCommunityActivity(String chainID){
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, chainID);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentExtra.TYPE, 0);
        ActivityUtil.startActivity(intent, this, MainActivity.class);
    }

    /**
     * 打开聊天页面
     * @param chainID
     */
    private void openChatActivity(String chainID){
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, chainID);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentExtra.TYPE, 1);
        intent.putExtra(IntentExtra.BEAN, this.user);
        ActivityUtil.startActivity(intent, this, MainActivity.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (shareQRDialog != null) {
            shareQRDialog.closeDialog();
        }
    }
}