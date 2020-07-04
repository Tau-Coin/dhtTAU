package io.taucoin.torrent.publishing.ui.user;

import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.databinding.ActivitySettingBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * 设置页面
 */
public class SeedActivity extends BaseActivity {

    private ActivitySettingBinding binding;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_title);
        binding.tvPublicKey.setText(Constants.PUBLIC_KEY);
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
}