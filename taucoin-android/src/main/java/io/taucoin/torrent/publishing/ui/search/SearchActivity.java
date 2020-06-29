package io.taucoin.torrent.publishing.ui.search;

import android.os.Bundle;
import android.view.Menu;

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
}