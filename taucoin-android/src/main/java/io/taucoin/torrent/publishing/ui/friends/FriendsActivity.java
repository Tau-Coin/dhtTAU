package io.taucoin.torrent.publishing.ui.friends;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.google.gson.Gson;
import com.king.zxing.util.CodeUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.BuildConfig;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.UserAndFriend;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.core.utils.DimensionsUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ActivityFriendsBinding;
import io.taucoin.torrent.publishing.databinding.QrCodeBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.transaction.TxViewModel;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 连接的对等点
 */
public class FriendsActivity extends BaseActivity implements FriendsListAdapter.ClickListener {
    public static final int PAGE_FRIENDS_LIST = 0;
    public static final int PAGE_SELECT_CONTACT = 1;
    public static final int PAGE_ADD_MEMBERS = 2;
    private ActivityFriendsBinding binding;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private TxViewModel txViewModel;
    private FriendsListAdapter adapter;
    private AlertDialog qrDialog;
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
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        txViewModel = provider.get(TxViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        initParameter();
        initView();
        initFabSpeedDial();
        getMedianFee();
        observeQRContent();
        if (BuildConfig.DEBUG) {
            userViewModel.addFriend("63ec42130442c91e23d56dc73708e06eb164883ab74c9813764c3fd0e2042dc4");
            userViewModel.addFriend("809df518ee450ded0a659aeb4bc5bec636e2cff012fc88d343b7419af974bb81");
            userViewModel.addFriend("2a62868271f3d3455e4b1ea0c1f96263732d0347349f9daa3247107ce1b2b2f9");
            userViewModel.addFriend("3e87c35d2079858d88dcb113edadaf1b339fcd4f74c539faa9a9bd59e787f124");
        }
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        page = getIntent().getIntExtra(IntentExtra.TYPE, PAGE_FRIENDS_LIST);
        friendPk = getIntent().getStringExtra(IntentExtra.PUBLIC_KEY);
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
        /*
         * A RecyclerView by default creates another copy of the ViewHolder in order to
         * fade the views into each other. This causes the problem because the old ViewHolder gets
         * the payload but then the new one doesn't. So needs to explicitly tell it to reuse the old one.
         */
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
            ActivityUtil.startActivity(this, ExchangeActivity.class);
        });
        binding.llInviteFriends.setOnClickListener(v -> {
            ActivityUtil.startActivity(this, ExchangeActivity.class);
        });
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
        }else{
            if (user.isAdded()) {
                showShareQRDialog();
            } else {
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.PUBLIC_KEY, user.publicKey);
                ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
            }
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

    /**
     * 显示分享用户二维码的对话框
     */
    private void showShareQRDialog() {
        // 查询用户二维码内容信息
        userViewModel.queryCurrentUserAndFriends();
    }

    /**
     * 观察二维码内容变化
     */
    private void observeQRContent() {
        userViewModel.getQRContent().observe(this, qrContent -> {
            if (qrDialog != null && qrDialog.isShowing()) {
                qrDialog.dismiss();
            }
            QrCodeBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                    R.layout.qr_code, null, false);
            binding.tvQrCode.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            binding.tvQrCode.setText(R.string.user_share_qr_dialog_tips);
            binding.roundButton.setText(qrContent.getNickName());
            binding.roundButton.setBgColor(Utils.getGroupColor(qrContent.getPublicKey()));
            String contentJson = new Gson().toJson(qrContent);
            Bitmap bitmap = CodeUtils.createQRCode(contentJson, 480);
            binding.ivQrCode.setImageBitmap(bitmap);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)
                    binding.rlQrCode.getLayoutParams();
            layoutParams.topMargin = DimensionsUtil.dip2px(this, 15);
            layoutParams.bottomMargin = DimensionsUtil.dip2px(this, 0);
            binding.rlQrCode.setLayoutParams(layoutParams);
            qrDialog = new AlertDialog.Builder(this)
                    .setView(binding.getRoot())
                    .setNegativeButton(R.string.cancel, (dialog, which) -> qrDialog.cancel())
                    .setPositiveButton(R.string.drawer_share, (dialog, which) -> {
                        qrDialog.cancel();
                        userViewModel.shareQRCode(FriendsActivity.this, binding.rlQrCode, 240);
                    })
                    .create();
            qrDialog.setCanceledOnTouchOutside(false);
            qrDialog.show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qrDialog != null && qrDialog.isShowing()) {
            qrDialog.dismiss();
        }
    }
}