package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivitySettingBinding;
import io.taucoin.torrent.publishing.core.storage.entity.User;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 设置页面
 */
public class SettingActivity extends BaseActivity implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger("SettingActivity");
    private ActivitySettingBinding binding;
    private UserViewModel viewModel;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);
        binding.setListener(this);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_title);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        binding.etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String newName = s.toString().trim();
                String oldName = ViewUtils.getStringTag(binding.etUsername);
                if(StringUtil.isNotEquals(oldName, newName)){
                    logger.debug("saveUserName::{}", newName);
                    binding.etUsername.setTag(newName);
                    saveUserName(newName);
                }
            }
        });
    }

    /**
     * 保存用户名
     */
    private void saveUserName(String name) {
        viewModel.saveUserName(name);
    }

    /**
     * 订阅当前用户
     */
    private void subscribeCurrentUser() {
        disposables.add(viewModel.observeCurrentUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateUserInfo));

    }

    /**
     * 更新当前用户信息
     * @param user 当前用户
     */
    private void updateUserInfo(User user) {
        if(null == user){
            return;
        }
        binding.tvPublicKey.setText(UsersUtil.getMidHideName(user.publicKey));
        String userName = UsersUtil.getShowName(user);
        userName = UsersUtil.getDefaultName(userName);
        binding.etUsername.setText(userName);
        binding.etUsername.setTag(userName);
        disposables.clear();
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCurrentUser();
    }


    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ll_favorites:
                break;
            case R.id.item_dashboard:
                ActivityUtil.startActivity(this, DashboardActivity.class);
                break;
            case R.id.item_privacy_security:
                ActivityUtil.startActivity(this, PrivacySecurityActivity.class);
                break;
            case R.id.item_journal:
                break;
            case R.id.item_help:
                break;
        }
    }
}