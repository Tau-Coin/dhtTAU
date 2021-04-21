package io.taucoin.torrent.publishing.ui.friends;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ActivityFriendsBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.chat.ChatViewModel;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.qrcode.UserQRCodeActivity;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 连接的对等点
 */
public class FriendsActivity extends BaseActivity implements FriendsListAdapter.ClickListener,
        View.OnClickListener {
    public static final int PAGE_FRIENDS_LIST = 0;
    public static final int PAGE_SELECT_CONTACT = 1;
    public static final int PAGE_ADD_MEMBERS = 2;
    private ActivityFriendsBinding binding;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private TxViewModel txViewModel;
    private ChatViewModel chatViewModel;
    private FriendsListAdapter adapter;
    private CompositeDisposable disposables = new CompositeDisposable();
    // 联系人列表资源
    private LiveData<PagedList<UserAndFriend>> pagedListLiveData;
    private String chainID;
    // 代表不同的入口页面
    private int page;
    private String friendPk; // 新扫描的朋友的公钥
    private long medianFee;
    private int order = 0; // 0:last seen, 1:last communication

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_friends);
        binding.setListener(this);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        txViewModel = provider.get(TxViewModel.class);
        chatViewModel = provider.get(ChatViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        initParameter(getIntent());
        initView();
        getMedianFee();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initParameter(intent);
        initView();
        subscribeUserList();
    }

    /**
     * 初始化参数
     */
    private void initParameter(Intent intent) {
        if (intent != null) {
            chainID = intent.getStringExtra(IntentExtra.CHAIN_ID);
            page = intent.getIntExtra(IntentExtra.TYPE, PAGE_FRIENDS_LIST);
            friendPk = intent.getStringExtra(IntentExtra.PUBLIC_KEY);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_peers);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new FriendsListAdapter(this, page, order, friendPk);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerList.setLayoutManager(layoutManager);
        binding.recyclerList.setItemAnimator(null);
        binding.recyclerList.setAdapter(adapter);
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

    private void subscribeUserList() {
        if (pagedListLiveData != null && !pagedListLiveData.hasObservers()) {
            pagedListLiveData.removeObservers(this);
        }
        pagedListLiveData = userViewModel.observerUsers(order, page == PAGE_FRIENDS_LIST, friendPk);
        pagedListLiveData.observe(this, list -> {
            adapter.setOrder(order);
            adapter.submitList(list);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeUserList();
        chatViewModel.startVisitFriend(friendPk);

        userViewModel.getAddFriendResult().observe(this, result -> {
            if (result.isSuccess()) {
                ToastUtils.showShortToast(R.string.contacts_friend_already_exists);
            } else {
                userViewModel.closeDialog();
                chatViewModel.startVisitFriend(friendPk);
                friendPk = result.getMsg();
                adapter = new FriendsListAdapter(this, page, order, friendPk);
                binding.recyclerList.setAdapter(adapter);
                subscribeUserList();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        chatViewModel.stopVisitFriend();
        disposables.clear();
    }

    /**
     * 创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contacts, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.menu_done);
        MenuItem menuRankC = menu.findItem(R.id.menu_rank_c);
        MenuItem menuRankA = menu.findItem(R.id.menu_rank_a);
        menuItem.setVisible(page == PAGE_ADD_MEMBERS);
        menuRankC.setVisible(page == PAGE_FRIENDS_LIST && order == 0);
        menuRankA.setVisible(page == PAGE_FRIENDS_LIST && order != 0);
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
        } else if (item.getItemId() == R.id.menu_rank_c) {
            order = 1;
            invalidateOptionsMenu();
            ToastUtils.showShortToast(R.string.menu_rank_a);
            subscribeUserList();
        } else if (item.getItemId() == R.id.menu_rank_a) {
            order = 0;
            invalidateOptionsMenu();
            ToastUtils.showShortToast(R.string.menu_rank_c);
            subscribeUserList();
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
    public void onItemClicked(@NonNull UserAndFriend user) {
        if(page == PAGE_SELECT_CONTACT){
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.PUBLIC_KEY, user.publicKey);
            setResult(RESULT_OK, intent);
            onBackPressed();
        } else {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.PUBLIC_KEY, user.publicKey);
            intent.putExtra(IntentExtra.TYPE, UserDetailActivity.TYPE_FRIEND_LIST);
            ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
        }
    }

    @Override
    public void onProcessClicked(UserAndFriend user) {
        if (user.isAdded()) {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.TYPE, UserQRCodeActivity.TYPE_QR_SHARE_ADDED);
            ActivityUtil.startActivity(intent, this, UserQRCodeActivity.class);
        } else {
            onItemClicked(user);
        }
    }

    @Override
    public void onShareClicked(UserAndFriend user) {
        showShareDialog();
    }

    /**
     * 显示联系平台的对话框
     */
    private void showShareDialog() {
        disposables.add(communityViewModel.getCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                    String communityInviteLink = ChainLinkUtil.encode(chainID, list);
                    ActivityUtil.shareText(this, getString(R.string.contacts_share_link_via),
                            communityInviteLink);
                }));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ll_exchange_qr) {
            ActivityUtil.startActivity(this, ExchangeActivity.class);
        } else if (v.getId() == R.id.ll_add_friend) {
            userViewModel.showAddFriendDialog(this);
        }
    }
}