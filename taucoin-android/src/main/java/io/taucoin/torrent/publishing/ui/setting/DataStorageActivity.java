package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.databinding.ActivityDataStorageBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 数据存储页面
 */
public class DataStorageActivity extends BaseActivity implements View.OnClickListener {

    private ActivityDataStorageBinding binding;
    private UserViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_data_storage);
        binding.setListener(this);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_data_storage);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
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

    @Override
    public void onClick(View v) {
        switch (v.getId()){

        }
    }
}