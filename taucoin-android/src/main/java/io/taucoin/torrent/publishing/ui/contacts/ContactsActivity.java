package io.taucoin.torrent.publishing.ui.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.frostwire.jlibtorrent.Ed25519;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.UserAndMember;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityContactsBinding;
import io.taucoin.torrent.publishing.databinding.ContactsDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.customviews.ShareDialog;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;
import io.taucoin.util.ByteUtil;

/**
 * 联系人页面
 */
public class ContactsActivity extends BaseActivity implements ContactListAdapter.ClickListener {
    public static final int PAGE_CONTACT_LIST = 0;
    public static final int PAGE_SELECT_CONTACT = 1;
    public static final int PAGE_ADD_MEMBERS = 2;
    private ActivityContactsBinding binding;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private TxViewModel txViewModel;
    private ContactListAdapter adapter;
    private CommonDialog commonDialog;
    private ShareDialog shareDialog;
    private CompositeDisposable disposables = new CompositeDisposable();
    private String chainID;
    // 代表不同的入口页面
    private int page;
    private long medianFee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contacts);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        txViewModel = provider.get(TxViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        initParameter();
        initView();
        initFabSpeedDial();
        getMedianFee();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        page = getIntent().getIntExtra(IntentExtra.TYPE, PAGE_CONTACT_LIST);
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.contacts_title);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new ContactListAdapter(this, page);
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
        binding.recyclerList.setEmptyView(binding.emptyViewList);
        binding.recyclerList.setAdapter(adapter);
        if(page == PAGE_ADD_MEMBERS){
            binding.fabButton.setVisibility(View.GONE);
        }
    }

    /**
     * 获取交易费中位数
     */
    private void getMedianFee() {
        if(StringUtil.isNotEmpty(chainID) && page == PAGE_ADD_MEMBERS){
            disposables.add(txViewModel.observeMedianFee(chainID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(fees -> medianFee = Utils.getMedianData(fees)));
        }
    }

    /**
     * 初始化右下角悬浮按钮组件
     */
    private void initFabSpeedDial() {
        // 自定义点击事件
        binding.fabButton.getMainFab().setOnClickListener(v -> {
            showAddPublicKeyDialog();
        });
        binding.llInviteFriends.setOnClickListener(v -> {
            showAddPublicKeyDialog();
        });
    }

    private void subscribeUserViewModel() {
        disposables.add(userViewModel.observeUsersNotInBanList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showUserList));

        userViewModel.getAddContactResult().observe(this, isExist -> {
            closeProgressDialog();
            if (isExist) {
                ToastUtils.showShortToast(R.string.contacts_friend_already_exists);
            } else {
                ToastUtils.showShortToast(R.string.contacts_add_successfully);
                if (commonDialog != null) {
                    commonDialog.closeDialog();
                }
            }
        });

        txViewModel.getAirdropState().observe(this, state -> {
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

    private void showUserList(List<UserAndMember> users) {
        if (users != null) {
            adapter.setDataList(users);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeUserViewModel();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    /**
     * 创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.menu_done);
        menuItem.setVisible(page == PAGE_ADD_MEMBERS);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_done) {
            showProgressDialog();
            txViewModel.airdropToFriends(chainID, adapter.getSelectedList(), medianFee);
        }
        return true;
    }

    @Override
    public void onSelectClicked() {
        int total = adapter.getSelectedList().size();
        binding.llTotalPay.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
        if(total > 0){
            long airdropCoin = Constants.AIRDROP_COIN.longValue();
            long totalPay = (airdropCoin + medianFee) * total;
            long totalFee = medianFee * total;
            String totalPayStr = FmtMicrometer.fmtBalance(totalPay);
            String totalFeeStr = FmtMicrometer.fmtFeeValue(totalFee);
            String coinName = UsersUtil.getCoinName(chainID);
            String totalPayHtml = getString(R.string.contacts_total_pay, total,
                    totalPayStr, coinName, totalFeeStr);
            binding.tvTotalPay.setText(Html.fromHtml(totalPayHtml));
        }
    }

    @Override
    public void onItemClicked(@NonNull UserAndMember user) {
        if(page == PAGE_SELECT_CONTACT){
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.PUBLIC_KEY, user.publicKey);
            setResult(RESULT_OK, intent);
            onBackPressed();
        }else{
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.PUBLIC_KEY, user.publicKey);
            ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
        }
    }

    @Override
    public void onShareClicked(UserAndMember user) {
        String currentUserPk = MainApplication.getInstance().getPublicKey();
        if(StringUtil.isEquals(currentUserPk, user.publicKey)){
            showShareDialog(user, false);
        }else{
            showProgressDialog();
            disposables.add(communityViewModel.getCommunityNumInCommon(currentUserPk, user.publicKey)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                        closeProgressDialog();
                        showShareDialog(user, list.size() > 0);
                    }));
        }
    }

    /**
     * 显示联系平台的对话框
     */
    private void showShareDialog(UserAndMember user, boolean isShareTau) {
        ShareDialog.Builder builder = new ShareDialog.Builder(this);
        builder.setOnItemClickListener((dialog, imgRid, titleRid) -> {
            dialog.dismiss();
            // 获取10个社区成员的公钥
            disposables.add(communityViewModel.getCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                        String communityInviteLink = UsersUtil.getCommunityInviteLink(chainID, list);
                        if (imgRid == R.mipmap.icon_share_copy_link) {
                            CopyManager.copyText(communityInviteLink);
                            ToastUtils.showShortToast(R.string.copy_share_link);
                        } else if (imgRid == R.mipmap.ic_launcher_round) {
                            userViewModel.shareInvitedLinkToFriend(communityInviteLink, user.publicKey);
                            ToastUtils.showShortToast(R.string.share_link_successfully);
                        } else if (imgRid == R.mipmap.icon_share_sms) {
                            ActivityUtil.doSendSMSTo(this, communityInviteLink);
                        }
                    }));
        });
        builder.addItems(R.mipmap.icon_share_copy_link, R.string.contacts_copy_link);
        if(isShareTau){
            builder.addItems(R.mipmap.ic_launcher_round, R.string.contacts_community);
        }
        builder.addItems(R.mipmap.icon_share_sms, R.string.contacts_sms);
        shareDialog = builder.create();
        shareDialog.show();
    }

    /**
     * 显示新增朋友公钥的对话框
     */
    private void showAddPublicKeyDialog() {
        ContactsDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.contacts_dialog, null, false);
        binding.ivClose.setOnClickListener(v -> {
            if (commonDialog != null) {
                commonDialog.closeDialog();
            }
        });
        binding.tvSubmit.setOnClickListener(v -> {
            String publicKey = ViewUtils.getText(binding.etPublicKey);
            boolean isValid = true;
            try {
                if (StringUtil.isEmpty(publicKey) ||
                        ByteUtil.toByte(publicKey).length != Ed25519.PUBLIC_KEY_SIZE) {
                    isValid = false;
                }
            } catch(Exception e) {
                isValid = false;
            }
            if (!isValid){
                ToastUtils.showShortToast(R.string.contacts_error_invalid_pk);
                return;
            }
            showProgressDialog();
            userViewModel.addContact(publicKey);
        });
        commonDialog = new CommonDialog.Builder(this)
                .setContentView(binding.getRoot())
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
        if (shareDialog != null) {
            shareDialog.closeDialog();
        }
    }
}