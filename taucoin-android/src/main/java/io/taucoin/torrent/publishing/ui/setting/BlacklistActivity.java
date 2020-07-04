package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ActivityDataStorageBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 设置页面
 */
public class BlacklistActivity extends BaseActivity implements View.OnClickListener {

    static final String TYPE_USERS = "users";
    static final String TYPE_COMMUNITIES = "communities";
    private ActivityDataStorageBinding binding;
    private UserViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_data_storage);
        binding.setListener(this);
        initParameter();
        initView();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getIntent() != null){
            type = getIntent().getStringExtra(IntentExtra.TYPE);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        int titleRes = R.string.setting_blacklist_users;
        if(StringUtil.isEquals(type, TYPE_COMMUNITIES)){
            titleRes = R.string.setting_blacklist_communities;
        }
        binding.toolbarInclude.toolbar.setTitle(titleRes);
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