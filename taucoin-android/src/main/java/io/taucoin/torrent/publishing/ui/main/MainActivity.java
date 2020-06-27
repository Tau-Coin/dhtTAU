package io.taucoin.torrent.publishing.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.leinardi.android.speeddial.SpeedDialActionItem;

import java.util.List;
import java.util.Objects;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.databinding.ActivityMainDrawerBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.blockchain.BlockChainsFragment;
import io.taucoin.torrent.publishing.ui.message.MessageFragment;
import io.taucoin.torrent.publishing.ui.mining.MiningPoolActivity;
import io.taucoin.torrent.publishing.ui.search.SearchActivity;
import io.taucoin.torrent.publishing.ui.video.VideoFragment;

/**
 * APP主页面：包含左侧抽屉页面，顶部工具栏
 */
public class MainActivity extends BaseActivity {
    private ActivityMainDrawerBinding binding;
    private ActionBarDrawerToggle toggle;

    private MainViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private String curFragmentTag;

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
        initDrawer();
        initFabSpeedDial();
        switchFragment(getString(R.string.drawer_video));
    }

    private void initDrawer() {
        binding.drawer.tvDhtNodesStat.setText(getString(R.string.drawer_stats_dht_nodes, 0));
        binding.drawer.tvBlockNumbersStats.setText(getString(R.string.drawer_stats_block_numbers, 0));
        binding.drawer.tvDhtPutsStats.setText(getString(R.string.drawer_stats_dht_puts, 0));
        setNetStats(binding.drawer.tvDownloadStat, 0, 0);
        setNetStats(binding.drawer.tvUploadStat, 0, 0);
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
        subscribeSessionStats();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
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
     *  根据显示Fragment动态修改Menu选项
     */
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        for (int i = 0; i < menu.size(); i++) {
//            menu.getItem(i).setVisible(true);
//        }
//        if(Objects.equals(curFragmentTag, getString(R.string.drawer_messages))){
//            menu.findItem(R.id.menu_pause_all).setVisible(false);
//            menu.findItem(R.id.menu_resume_all).setVisible(false);
//        }else if(Objects.equals(curFragmentTag, getString(R.string.drawer_block_chains))){
//            menu.findItem(R.id.menu_filter).setVisible(false);
//        }
//        return super.onPrepareOptionsMenu(menu);
//    }

    /**
     * 右上角选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
                startActivity(new Intent(this, SearchActivity.class));
                break;
//            case R.id.menu_mining_pool:
//                startActivity(new Intent(this, MiningPoolActivity.class));
//                break;
//            case R.id.menu_filter:
//                break;
//            case R.id.menu_resume_all:
//                viewModel.resumeAll();
//                break;
//            case R.id.menu_pause_all:
//                viewModel.pauseAll();
//                break;
        }
        return true;
    }

    /**
     * 左侧抽屉布局点击事件
     */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_video:
            case R.id.tv_block_chains:
            case R.id.tv_messages:
                TextView textView = (TextView) view;
                String tag = textView.getText().toString();
                switchFragment(tag);
                break;
            case R.id.tv_dashboard:
                break;
            case R.id.tv_setting:
                break;
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }

    /**
     * 根据tag切换Fragment
     * @param tag Fragment的tag
     */
    private void switchFragment(String tag) {
        curFragmentTag = tag;
        invalidateOptionsMenu();
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentByTag(tag);
        FragmentTransaction ft = fm.beginTransaction();
        if(null == fragment){
            if(Objects.equals(tag, getString(R.string.drawer_messages))){
                fragment = new MessageFragment();
            }else if(Objects.equals(tag, getString(R.string.drawer_block_chains))){
                fragment = new BlockChainsFragment();
            }else{
                fragment = new VideoFragment();
            }
            ft.add(R.id.main_fragmentContainer, fragment, tag);
        }
        List<Fragment> fragments = fm.getFragments();
        for (Fragment fragmentTemp : fragments) {
            ft.hide(fragmentTemp);
        }
        ft.show(fragment);
        ft.commit();
    }

    private void subscribeSessionStats() {
//        disposables.add(infoProvider.observeSessionStats()
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(this::updateSessionStats));
    }

    private void updateSessionStats() {
        long dhtNodes = 0;
        long totalDownload = 0;
        long totalUpload = 0;
        long downloadSpeed = 0;
        long uploadSpeed = 0;

//        if (stats != null) {
//            dhtNodes = stats.dhtNodes;
//            totalDownload = stats.totalDownload;
//            totalUpload = stats.totalUpload;
//            downloadSpeed = stats.downloadSpeed;
//            uploadSpeed = stats.uploadSpeed;
//        }

        binding.drawer.tvDhtNodesStat.setText(getString(R.string.drawer_stats_dht_nodes, dhtNodes));
        setNetStats(binding.drawer.tvDownloadStat, totalDownload, downloadSpeed);
        setNetStats(binding.drawer.tvUploadStat, totalUpload, uploadSpeed);
    }

    private void setNetStats(TextView tvStats, long total, long speed) {
        tvStats.setText(getString(R.string.drawer_stats_download_upload,
                Formatter.formatFileSize(this, total),
                Formatter.formatFileSize(this, speed)));
    }
}