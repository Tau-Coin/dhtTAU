package io.taucoin.torrent.publishing.ui.setting;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.databinding.ActivityPrivacySecurityBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.user.SeedActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 隐私安全页面页面
 */
public class PrivacySecurityActivity extends BaseActivity implements View.OnClickListener {

    private ActivityPrivacySecurityBinding binding;
    private UserViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_privacy_security);
        binding.setListener(this);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_privacy_security);
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
            case R.id.tv_blacklist_users:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.TYPE, BlacklistActivity.TYPE_USERS);
                ActivityUtil.startActivity(intent, this, BlacklistActivity.class);
                break;
            case R.id.tv_blacklist_communities:
                intent = new Intent();
                intent.putExtra(IntentExtra.TYPE, BlacklistActivity.TYPE_COMMUNITIES);
                ActivityUtil.startActivity(intent, this, BlacklistActivity.class);
                break;
            case R.id.ll_seeds:
                ActivityUtil.startActivity(this, SeedActivity.class);
                break;
            case R.id.ll_devices:
                ActivityUtil.startActivity(this, DevicesActivity.class);
                break;
        }
    }
}