package io.taucoin.torrent.publishing.ui.user;

import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivitySeedChangeBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * Seed切换页面
 */
public class SeedChangeActivity extends BaseActivity implements View.OnClickListener {

    private ActivitySeedChangeBinding binding;
    private UserViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_seed_change);
        binding.setListener(this);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_change_seed);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    public void onStart() {
        super.onStart();
        viewModel.getChangeResult().observe(this, result -> {
            if(StringUtil.isNotEmpty(result)){
                ToastUtils.showShortToast(result);
            }else {
                onBackPressed();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * import seed和generate seed点击事件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_seed_import:
                String seed = ViewUtils.getText(binding.etSeed);
                viewModel.importSeed(seed);
                break;
            case R.id.btn_seed_generate:
                viewModel.generateSeed();
                break;
        }
    }
}