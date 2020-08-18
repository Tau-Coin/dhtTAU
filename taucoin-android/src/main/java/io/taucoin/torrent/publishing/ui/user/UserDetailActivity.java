package io.taucoin.torrent.publishing.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
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
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.UserAndMember;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Member;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityUserDetailBinding;
import io.taucoin.torrent.publishing.databinding.ContactsDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;

/**
 * 用户详情
 */
public class UserDetailActivity extends BaseActivity implements View.OnClickListener,
        UserCommunityListAdapter.ClickListener {
    private ActivityUserDetailBinding binding;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private TxViewModel txViewModel;
    private UserCommunityListAdapter adapter;
    private CommonDialog commonDialog;
    private String publicKey;
    private UserAndMember user;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        txViewModel = provider.get(TxViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_detail);
        binding.setListener(this);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        if(getIntent() != null){
            publicKey = getIntent().getStringExtra(IntentExtra.PUBLIC_KEY);
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

        if(StringUtil.isEquals(publicKey, MainApplication.getInstance().getPublicKey())){
            binding.tvStartChat.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        userViewModel.getUserDetail().observe(this, this::showUserInfo);

        userViewModel.getAddContactResult().observe(this, isExist -> {
            closeProgressDialog();
            if (isExist) {
                ToastUtils.showShortToast(R.string.contacts_friend_already_exists);
            } else {
                binding.tvAddToContact.setVisibility(View.GONE);
                ToastUtils.showShortToast(R.string.contacts_add_successfully);
            }
        });

        communityViewModel.getChatState().observe(this, state -> {
            closeProgressDialog();
            if (StringUtil.isNotEmpty(state)) {
                ToastUtils.showShortToast(state);
            } else {
                String airdropResult = getString(R.string.contacts_airdrop_successfully,
                        FmtMicrometer.fmtFeeValue(Constants.AIRDROP_COIN.longValue()));
                ToastUtils.showShortToast(airdropResult);
                onBackPressed();
            }
            if (commonDialog != null) {
                commonDialog.closeDialog();
            }
        });
    }

    private void showUserInfo(UserAndMember userInfo) {
        if(null == userInfo || StringUtil.isEmpty(userInfo.publicKey) ){
            userInfo = new UserAndMember(publicKey);
            binding.tvAddToContact.setVisibility(View.VISIBLE);
        }
        this.user = userInfo;
        String showName = UsersUtil.getCurrentUserName(user);
        binding.tvName.setText(showName);
        binding.leftView.setText(StringUtil.getFirstLettersOfName(showName));
        binding.leftView.setBgColor(Utils.getGroupColor(user.publicKey));
        binding.tvPublicKey.setText(UsersUtil.getMidHideName(user.publicKey));
        binding.ivPublicKeyCopy.setOnClickListener(v -> {
            CopyManager.copyText(user.publicKey);
            ToastUtils.showShortToast(R.string.copy_public_key);
        });
        if(user.members != null){
            adapter.setDataList(user.members);
        }
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
                userViewModel.addContact(publicKey);
                break;
            case R.id.tv_start_chat:
                showChatDialog();
                break;
        }
    }

    @Override
    public void onItemClicked(@NonNull Member member) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.CHAIN_ID, member.chainID);
        ActivityUtil.startActivity(intent, this, CommunityActivity.class);
    }

    /**
     * 显示和朋友Chatting的对话框
     */
    private void showChatDialog() {
        if(null == user){
            return;
        }
        ContactsDialogBinding contactsBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.contacts_dialog, null, false);
        String showName = UsersUtil.getShowName(user);
        contactsBinding.tvTitle.setText(Html.fromHtml(getString(R.string.contacts_chatting, showName)));
        contactsBinding.tvTitle.setVisibility(View.VISIBLE);
        contactsBinding.etChatName.setVisibility(View.VISIBLE);
        contactsBinding.etPublicKey.setVisibility(View.GONE);
        contactsBinding.tvSubmit.setText(R.string.common_start);
        contactsBinding.ivClose.setOnClickListener(v -> {
            if (commonDialog != null) {
                commonDialog.closeDialog();
            }
        });
        contactsBinding.tvSubmit.setOnClickListener(v -> {
            String chatName = ViewUtils.getText(contactsBinding.etChatName);
            showProgressDialog();
            communityViewModel.createChat(txViewModel, chatName, user.publicKey);
        });

        commonDialog = new CommonDialog.Builder(this)
                .setContentView(contactsBinding.getRoot())
                .setButtonWidth(240)
                .create();
        commonDialog.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (commonDialog != null) {
            commonDialog.closeDialog();
        }
    }
}