package io.taucoin.torrent.publishing.ui.main;

import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.List;
import java.util.Objects;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.blockchain.BlockChainFragment;
import io.taucoin.torrent.publishing.ui.message.MessageFragment;
import io.taucoin.torrent.publishing.ui.video.VideoFragment;

/**
 * APP主页面：包含左侧抽屉页面，顶部工具栏
 */
public class MainActivity extends BaseActivity {
    /* Android data binding doesn't work with layout aliases */
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private TextView tvDhtNodesStat, tvDownloadStat, tvUploadStat,
        tvBlockNumbersStat, tvDhtPutsStat;

    private MainViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private String curFragmentTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(MainViewModel.class);
        setContentView(R.layout.activity_main);
        initLayout();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        toolbar = findViewById(R.id.toolbar);
        tvDhtNodesStat = findViewById(R.id.tv_dht_nodes_stat);
        tvDownloadStat = findViewById(R.id.tv_download_stat);
        tvUploadStat = findViewById(R.id.tv_upload_stat);
        tvBlockNumbersStat = findViewById(R.id.tv_block_numbers_stats);
        tvDhtPutsStat = findViewById(R.id.tv_dht_puts_stats);
        tvUploadStat = findViewById(R.id.tv_upload_stat);
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout = findViewById(R.id.drawer_layout);

        toolbar.setTitle(R.string.main_title);
        setSupportActionBar(toolbar);

        if (drawerLayout != null) {
            toggle = new ActionBarDrawerToggle(this,
                    drawerLayout,
                    toolbar,
                    R.string.open_navigation_drawer,
                    R.string.close_navigation_drawer);
            drawerLayout.addDrawerListener(toggle);
        }
        initDrawer();
        switchFragment(getString(R.string.drawer_video));
    }

    private void initDrawer() {
        tvDhtNodesStat.setText(getString(R.string.drawer_stats_dht_nodes, 0));
        tvBlockNumbersStat.setText(getString(R.string.drawer_stats_block_numbers, 0));
        tvDhtPutsStat.setText(getString(R.string.drawer_stats_dht_puts, 0));
        setNetStats(tvDownloadStat, 0, 0);
        setNetStats(tvUploadStat, 0, 0);
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
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(Objects.equals(curFragmentTag, getString(R.string.drawer_video))){
            showOrHideMenu(menu, false);
            menu.findItem(R.id.menu_search).setVisible(true);
            menu.findItem(R.id.menu_mining_pool).setVisible(true);
            menu.findItem(R.id.menu_filter).setVisible(true);
        }else if(Objects.equals(curFragmentTag, getString(R.string.drawer_messages))){
            showOrHideMenu(menu, true);
            menu.findItem(R.id.menu_filter).setVisible(false);
        }else if(Objects.equals(curFragmentTag, getString(R.string.drawer_block_chains))){
            showOrHideMenu(menu, true);
            menu.findItem(R.id.menu_filter).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * 显示或者隐藏所有Menu
     */
    private void showOrHideMenu(Menu menu, boolean visible) {
        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setVisible(visible);
        }
    }

    /**
     * 右上角选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_filter:
                break;
            case R.id.menu_mining_pool:
//                startActivity(new Intent(this, FeedActivity.class));
                break;
            case R.id.menu_keys:
//                startActivity(new Intent(this, KeysActivity.class));
                break;
            case R.id.menu_resume_all:
                viewModel.resumeAll();
                break;
            case R.id.menu_pause_all:
                viewModel.pauseAll();
                break;
            case R.id.menu_journal:
//                startActivity(new Intent(this, JournalActivity.class));
                break;
            case R.id.menu_rename:
//                startActivity(new Intent(this, RenameActivity.class));
                break;
            case R.id.menu_mining_group:
                break;
            case R.id.menu_about:
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
        }
        if (drawerLayout != null){
            drawerLayout.closeDrawer(GravityCompat.START);
        }
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
                fragment = new BlockChainFragment();
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

        tvDhtNodesStat.setText(getString(R.string.drawer_stats_dht_nodes, dhtNodes));
        setNetStats(tvDownloadStat, totalDownload, downloadSpeed);
        setNetStats(tvUploadStat, totalUpload, uploadSpeed);
    }

    private void setNetStats(TextView tvStats, long total, long speed) {
        tvStats.setText(getString(R.string.drawer_stats_download_upload,
                Formatter.formatFileSize(this, total),
                Formatter.formatFileSize(this, speed)));
    }
}