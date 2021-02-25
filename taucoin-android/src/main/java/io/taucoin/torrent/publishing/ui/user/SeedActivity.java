package io.taucoin.torrent.publishing.ui.user;

import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.databinding.ActivitySeedBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.ScanTriggerActivity;
import io.taucoin.torrent.publishing.ui.qrcode.KeyQRCodeActivity;

/**
 * Seeds管理页面
 */
public class SeedActivity extends ScanTriggerActivity implements View.OnClickListener, HistoryListAdapter.ClickListener {

    private ActivitySeedBinding binding;
    private UserViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private HistoryListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_seed);
        binding.setListener(this);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_seeds);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new HistoryListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.historyList.setLayoutManager(layoutManager);
        binding.historyList.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.getChangeResult().observe(this, result -> {
            if(StringUtil.isNotEmpty(result)){
                ToastUtils.showShortToast(result);
            }
        });
        disposables.add(viewModel.observeSeedHistoryList()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list ->{
                    adapter.setUserList(list);
                }));

    }

    @Override
    protected void onStop() {
        disposables.clear();
        super.onStop();
    }

    /**
     * import seed和generate seed点击事件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.item_import_seed:
                viewModel.showSaveSeedDialog(this, false);
                break;
            case R.id.item_export_seed:
                ActivityUtil.startActivity(this, KeyQRCodeActivity.class);
                break;
            case R.id.item_generate_seed:
                viewModel.showSaveSeedDialog(this, true);
                break;
        }
    }

    @Override
    public void onItemClicked(User user) {
        viewModel.importSeed(user.seed, null);
    }
}