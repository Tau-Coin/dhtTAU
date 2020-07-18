package io.taucoin.torrent.publishing.ui.main;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityMainDrawerBinding;
import io.taucoin.torrent.publishing.core.storage.entity.User;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityCreateActivity;
import io.taucoin.torrent.publishing.ui.contacts.ContactsActivity;
import io.taucoin.torrent.publishing.ui.setting.SettingActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * APP主页面：包含左侧抽屉页面，顶部工具栏，群组列表
 */
public class MainActivity extends BaseActivity {
    private ActivityMainDrawerBinding binding;
    private ActionBarDrawerToggle toggle;

    private UserViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_drawer);
        initLayout();
        checkCurrentUser();
    }

    /**
     * 检查当前用户
     */
    private void checkCurrentUser() {
        viewModel.checkCurrentUser();
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

        binding.drawer.itemDhtNodes.setRightText(getString(R.string.drawer_dht_nodes, 0));
        binding.drawer.itemWifiSpeed.setRightText(getString(R.string.drawer_net_speed, 0));
        binding.drawer.itemTelecomSpeed.setRightText(getString(R.string.drawer_net_speed, 0));

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
        disposables.add(viewModel.observeCurrentUser()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::updateUserInfo));

    }

    /**
     * 更新当前用户信息
     * @param user 当前用户
     */
    private void updateUserInfo(User user) {
        if(null == user){
            return;
        }
        MainApplication.getInstance().setPublicKey(user.publicKey);
        binding.drawer.tvPublicKey.setText(UsersUtil.getMidHideName(user.publicKey));
        binding.drawer.tvPublicKey.setTag(user.publicKey);
        binding.drawer.ivPublicKeyCopy.setTag(user.publicKey);
        String noteName = UsersUtil.getShowName(user);
        binding.drawer.tvNoteName.setText(UsersUtil.getDefaultName(noteName));
        binding.drawer.roundButton.setText(StringUtil.getFirstLettersOfName(noteName));
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCurrentUser();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    /**
     * 左侧抽屉布局点击事件
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_public_key:
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
                break;
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_alert) {
        }
        return true;
    }
}