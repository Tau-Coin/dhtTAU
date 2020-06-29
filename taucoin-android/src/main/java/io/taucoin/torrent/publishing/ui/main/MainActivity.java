package io.taucoin.torrent.publishing.ui.main;

import android.os.Bundle;
import android.view.View;

import com.leinardi.android.speeddial.SpeedDialActionItem;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.databinding.ActivityMainDrawerBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * APP主页面：包含左侧抽屉页面，顶部工具栏，群组列表
 */
public class MainActivity extends BaseActivity {
    private ActivityMainDrawerBinding binding;
    private ActionBarDrawerToggle toggle;

    private MainViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(MainViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_drawer);
        initLayout();
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
        initFabSpeedDial();
    }

    /**
     * 初始化右下角悬浮按钮组件
     */
    private void initFabSpeedDial() {
        binding.fabButton.setOnActionSelectedListener((item) -> {
            switch (item.getId()) {
                case R.id.main_publish_video:
                    break;
                case R.id.main_create_community:
                    break;
                case R.id.main_other_transaction:
                    break;
                default:
                    return false;
            }
            binding.fabButton.close();
            return true;
        });

        binding.fabButton.addActionItem(new SpeedDialActionItem.Builder(
                R.id.main_other_transaction,
                R.drawable.ic_add_36dp)
                .setLabel(R.string.main_other_transaction)
                .create());

        binding.fabButton.addActionItem(new SpeedDialActionItem.Builder(
                R.id.main_create_community,
                R.drawable.ic_add_36dp)
                .setLabel(R.string.main_create_community)
                .create());

        binding.fabButton.addActionItem(new SpeedDialActionItem.Builder(
                R.id.main_publish_video,
                R.drawable.ic_add_36dp)
                .setLabel(R.string.main_publish_video)
                .create());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (toggle != null){
            toggle.syncState();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
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
            case R.id.item_import_key:
                break;
            case R.id.item_new_community:
                break;
            case R.id.item_contacts:
                break;
            case R.id.item_setting:
                break;
            case R.id.item_invited_friends:
                break;
            case R.id.item_telegram_faq:
                break;
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }
}