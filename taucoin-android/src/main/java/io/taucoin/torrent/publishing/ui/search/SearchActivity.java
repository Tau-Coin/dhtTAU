package io.taucoin.torrent.publishing.ui.search;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import java.util.Objects;

import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.main.MainViewModel;

/**
 * 搜索页面：包含交易、区块链搜索
 */
public class SearchActivity extends BaseActivity {
    private MainViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(MainViewModel.class);
        setContentView(R.layout.activity_search);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        if (toggle != null){
//            toggle.syncState();
//        }
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
////        if(Objects.equals(curFragmentTag, getString(R.string.drawer_messages))){
////            menu.findItem(R.id.menu_pause_all).setVisible(false);
////            menu.findItem(R.id.menu_resume_all).setVisible(false);
////        }else if(Objects.equals(curFragmentTag, getString(R.string.drawer_block_chains))){
////            menu.findItem(R.id.menu_filter).setVisible(false);
////        }
//        return super.onPrepareOptionsMenu(menu);
//    }

    /**
     * 右上角选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_search:
//                startActivity(new Intent(this, SearchActivity.class));
                break;
//            case R.id.menu_mining_pool:
////                startActivity(new Intent(this, MiningPoolActivity.class));
//                break;
//            case R.id.menu_filter:
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
                break;
        }
    }
}