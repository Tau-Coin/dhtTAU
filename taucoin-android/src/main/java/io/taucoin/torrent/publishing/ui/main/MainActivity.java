package io.taucoin.torrent.publishing.ui.main;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.core.view.MenuItemCompat;
import io.taucoin.torrent.SessionStats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.model.TauInfoProvider;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.TrafficUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityMainDrawerBinding;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.databinding.ExternalLinkDialogBinding;
import io.taucoin.torrent.publishing.databinding.UserDialogBinding;
import io.taucoin.torrent.publishing.receiver.NotificationReceiver;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.ExternalLinkActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityCreateActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.contacts.ContactsActivity;
import io.taucoin.torrent.publishing.ui.customviews.BadgeActionProvider;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.download.DownloadViewModel;
import io.taucoin.torrent.publishing.ui.notify.NotificationActivity;
import io.taucoin.torrent.publishing.ui.notify.NotificationViewModel;
import io.taucoin.torrent.publishing.ui.setting.SettingActivity;
import io.taucoin.torrent.publishing.ui.user.ScanQRCodeActivity;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.torrent.publishing.ui.user.UserQRCodeActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * APP主页面：包含左侧抽屉页面，顶部工具栏，群组列表
 */
public class MainActivity extends BaseActivity {
    private static final Logger logger = LoggerFactory.getLogger("MainActivity");
    private ActivityMainDrawerBinding binding;
    private ActionBarDrawerToggle toggle;

    private UserViewModel userViewModel;
    private MainViewModel mainViewModel;
    private TauInfoProvider infoProvider;
    private CommunityViewModel communityViewModel;
    private DownloadViewModel downloadViewModel;
    private NotificationViewModel notificationViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Subject<Integer> mBackClick = PublishSubject.create();
    private CommonDialog seedDialog;
    private CommonDialog linkDialog;
    private BadgeActionProvider actionProvider;
    private User user;
    private int unreadNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String action =  intent.getAction();
        if (StringUtil.isNotEmpty(action) && StringUtil.isEquals(action,
                NotificationReceiver.NOTIFY_ACTION_SHUTDOWN_APP)) {
            logger.info("MainActivity finished");
            finish();
            return;
        }

        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        mainViewModel = provider.get(MainViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        notificationViewModel = provider.get(NotificationViewModel.class);
        downloadViewModel = provider.get(DownloadViewModel.class);
        infoProvider = TauInfoProvider.getInstance(getApplicationContext());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_drawer);
        initLayout();
        checkCurrentUser();
        initExitApp();
        subscribeAddCommunity();

        if (StringUtil.isNotEmpty(action) && StringUtil.isEquals(action,
                ExternalLinkActivity.ACTION_CHAIN_LINK_CLICK)) {
            logger.info("MainActivity::chain link clicked");
            if (intent.hasExtra(IntentExtra.CHAIN_LINK)) {
                String chainLink = intent.getStringExtra(IntentExtra.CHAIN_LINK);
                ChainLinkUtil.ChainLink decode = ChainLinkUtil.decode(chainLink);
                if(decode.isValid()){
                    openExternalLink(decode.getDn(), chainLink);
                }
            }
        }
    }

    /**
     * 检查当前用户
     */
    private void checkCurrentUser() {
        userViewModel.checkCurrentUser();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setTitle(R.string.main_title);
        setSupportActionBar(binding.toolbarInclude.toolbar);

        toggle = new ActionBarDrawerToggle(this,
                binding.drawerLayout,
                binding.toolbarInclude.toolbar,
                R.string.open_navigation_drawer,
                R.string.close_navigation_drawer);
        binding.drawerLayout.addDrawerListener(toggle);

        updateDHTStats(null);

        initFabSpeedDial();
    }

    /**
     * 初始化右下角悬浮按钮组件
     */
    private void initFabSpeedDial() {
        // 自定义点击事件
        binding.fabButton.getMainFab().setOnClickListener(v ->
                ActivityUtil.startActivity(MainActivity.this, CommunityCreateActivity.class));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (toggle != null){
            toggle.syncState();
        }
    }

    /**
     * 订阅当前用户
     */
    private void subscribeCurrentUser() {
        disposables.add(userViewModel.observeCurrentUser()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::updateUserInfo));

    }

    /**
     * 订阅DHT的状态
     */
    private void subscribeDHTStatus() {
        disposables.add(infoProvider.observeSessionStats()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateDHTStats));
    }

    /**
     * 更新DHT的状态
     */
    private void updateDHTStats(SessionStats sessionStats) {
        long dhtNodes = 0;
        if(sessionStats != null){
            dhtNodes = sessionStats.dhtNodes();
        }
        long totalData = TrafficUtil.getTrafficTotal();
        String totalDataStr = Formatter.formatFileSize(this, totalData);
        logger.info("dhtNodes::{}, totalDataStr::{}", dhtNodes, totalDataStr);
        binding.drawer.itemDhtNodes.setRightText(getString(R.string.drawer_dht_nodes, dhtNodes));
        binding.drawer.itemDailyData.setRightText(getString(R.string.drawer_daily_data,
                totalDataStr));
    }

    /**
     * 订阅是否需要启动TauDaemon
     */
    private void subscribeNeedStartDaemon(){
        disposables.add(mainViewModel.observeNeedStartEngine()
                .subscribeOn(Schedulers.io())
                .filter((needStart) -> needStart)
                .subscribe((needStart) -> mainViewModel.startDaemon()));
    }

    /**
     * 订阅是否需要启动TauDaemon
     */
    private void subscribeAddCommunity(){
        communityViewModel.getAddCommunityState().observe(this, result -> {
            if(result.isSuccess()){
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, result.getMsg());
                ActivityUtil.startActivity(intent, this, CommunityActivity.class);
            }
        });
    }

    /**
     * 更新当前用户信息
     * @param user 当前用户
     */
    private void updateUserInfo(User user) {
        if(null == user){
            return;
        }
        this.user = user;
        MainApplication.getInstance().setPublicKey(user.publicKey);
        binding.drawer.tvPublicKey.setText(UsersUtil.getMidHideName(user.publicKey));
        binding.drawer.tvPublicKey.setTag(user.publicKey);
        binding.drawer.ivPublicKeyCopy.setTag(user.publicKey);
        String showName = UsersUtil.getCurrentUserName(user);
        binding.drawer.tvNoteName.setText(showName);
        binding.drawer.roundButton.setText(StringUtil.getFirstLettersOfName(showName));
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCurrentUser();
        subscribeDHTStatus();
        subscribeNeedStartDaemon();
        subscribeUnreadNotificationNum();
//        downloadViewModel.checkAppVersion(this);
    }

    private void subscribeUnreadNotificationNum() {
        disposables.add(notificationViewModel.queryUnreadNotificationsNum()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(num -> unreadNum = num, e -> unreadNum = 0));
        ;
    }

    private void handleClipboardContent() {
        String content = CopyManager.getClipboardContent(this);
        if(StringUtil.isNotEmpty(content)){
            boolean isShowLinkDialog = showOpenExternalLinkDialog(content);
            if(isShowLinkDialog){
                CopyManager.clearClipboardContent();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // android10中规定, 目前处于焦点的应用, 才能访问到剪贴板数据
        this.getWindow().getDecorView().post(this::handleClipboardContent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(seedDialog != null){
            seedDialog.closeDialog();
        }
        if(linkDialog != null){
            linkDialog.closeDialog();
        }
    }

    /**
     * 左侧抽屉布局点击事件
     */
    public void onClick(View view) {
        if(null == user){
            return;
        }
        switch (view.getId()) {
            case R.id.iv_user_qr_code:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.BEAN, user);
                ActivityUtil.startActivity(intent, this, UserQRCodeActivity.class);
                break;
            case R.id.round_button:
                intent = new Intent();
                intent.putExtra(IntentExtra.PUBLIC_KEY, user.publicKey);
                ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
                break;
            case R.id.tv_public_key:
            case R.id.tv_public_key_title:
            case R.id.tv_note_name:
                showSeedDialog();
                break;
            case R.id.iv_public_key_copy:
                String publicKey = ViewUtils.getStringTag(view);
                CopyManager.copyText(publicKey);
                ToastUtils.showShortToast(R.string.copy_public_key);
                break;
            case R.id.item_new_community:
                ActivityUtil.startActivity(this, CommunityCreateActivity.class);
                break;
            case R.id.item_contacts:
                ActivityUtil.startActivity(this, ContactsActivity.class);
                break;
            case R.id.item_setting:
                ActivityUtil.startActivity(this, SettingActivity.class);
                break;
            case R.id.item_share:
                ActivityUtil.shareText(this, getString(R.string.app_share), Constants.APP_SHARE_URL);
                break;
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }

    /**
     * 显示用户Seed的对话框
     */
    private void showSeedDialog() {
        if(null == user){
            return;
        }
        UserDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.user_dialog, null, false);
        dialogBinding.tvPublicKey.setText(UsersUtil.getMidHideName(user.publicKey));
        dialogBinding.ivClose.setOnClickListener(v -> {
            if(seedDialog != null){
                seedDialog.closeDialog();
            }
        });
        dialogBinding.ivPublicKeyCopy.setOnClickListener(v -> {
            CopyManager.copyText(user.publicKey);
            ToastUtils.showShortToast(R.string.copy_public_key);
        });
        dialogBinding.llExportSeed.setOnClickListener(v -> {
            CopyManager.copyText(user.seed);
            ToastUtils.showShortToast(R.string.copy_seed);
        });
        seedDialog = new CommonDialog.Builder(this)
                .setContentView(dialogBinding.getRoot())
                .enableWarpWidth(true)
                .setCanceledOnTouchOutside(false)
                .create();
        seedDialog.show();
    }

    /**
     * 显示打开外部chain link的对话框（来自剪切板或外部链接）
     */
    private boolean showOpenExternalLinkDialog(String chainLink) {
        ChainLinkUtil.ChainLink decode = ChainLinkUtil.decode(chainLink);
        if(decode.isValid()){
            String chainID = decode.getDn();
            ExternalLinkDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                    R.layout.external_link_dialog, null, false);
            dialogBinding.tvName.setText(UsersUtil.getCommunityName(chainID));
            dialogBinding.ivClose.setOnClickListener(v -> {
                if(linkDialog != null){
                    linkDialog.closeDialog();
                }
            });
            dialogBinding.tvYes.setOnClickListener(v -> {
                if(linkDialog != null){
                    linkDialog.closeDialog();
                }
                openExternalLink(chainID, chainLink);
            });
            linkDialog = new CommonDialog.Builder(this)
                    .setContentView(dialogBinding.getRoot())
                    .setCanceledOnTouchOutside(false)
                    .create();
            linkDialog.show();
            return true;
        }
        return false;
    }

    /**
     * 打开外部chain link
     * @param chainID
     */
    private void openExternalLink(String chainID, String chainLink) {
        disposables.add(communityViewModel.getCommunityByChainIDSingle(chainID)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(community -> {
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                ActivityUtil.startActivity(intent, this, CommunityActivity.class);
            }, it -> {
                communityViewModel.addCommunity(chainID, chainLink);
            }));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(unreadNum >= 10){
            actionProvider.setText("···");
        }else{
            actionProvider.setBadge(unreadNum);
        }
        actionProvider.setVisibility(unreadNum > 0);
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem menuItem = menu.findItem(R.id.menu_alert);
        actionProvider = (BadgeActionProvider) MenuItemCompat.getActionProvider(menuItem);
        actionProvider.setOnClickListener(0, what ->
                ActivityUtil.startActivity(this, NotificationActivity.class));
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_scan) {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.BEAN, user);
            ActivityUtil.startActivity(intent,this, ScanQRCodeActivity.class);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        mBackClick.onNext(1);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CheckResult")
    private void initExitApp() {
        mBackClick.mergeWith(mBackClick.debounce(2000, TimeUnit.MILLISECONDS)
                .map(i -> 0))
                .scan((prev, cur) -> {
                    if (cur == 0) return 0;
                    return prev + 1;
                })
                .filter(v -> v > 0)
                .subscribe(v -> {
                    if (v == 1) {
                        ToastUtils.showLongToast(R.string.main_exit);
                    } else if (v == 2) {
                        appExit();
                    }
                });
    }

    /**
     * APP退出
     */
    private void appExit(){
        this.finish();
        ToastUtils.cancleToast();
        TauDaemon.getInstance(this).forceStop();
    }
}
