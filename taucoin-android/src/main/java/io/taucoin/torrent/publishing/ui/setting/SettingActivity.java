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
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivitySettingBinding;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 设置页面
 */
public class SettingActivity extends BaseActivity implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger("SettingActivity");
    private ActivitySettingBinding binding;
    private UserViewModel viewModel;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(UserViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting);
        binding.setListener(this);
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
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

        binding.switchServerMode.setChecked(settingsRepo.serverMode());
        handleSettingsChanged(getString(R.string.pref_key_internet_state));
        handleSettingsChanged(getString(R.string.pref_key_charging_state));
        handleSettingsChanged(getString(R.string.pref_key_wake_lock));

        binding.switchServerMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsRepo.serverMode(isChecked));

        Disposable disposable = settingsRepo.observeSettingsChanged()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSettingsChanged);
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
        String userName = UsersUtil.getCurrentUserName(user);
        binding.etUsername.setText(userName);
        binding.etUsername.setTag(userName);
        disposables.clear();
    }

    private void handleSettingsChanged(String key) {
        if (StringUtil.isEquals(key, getString(R.string.pref_key_internet_state))) {
            boolean internetState = settingsRepo.internetState();
            binding.tvInternet.setText(internetState ? R.string.common_on : R.string.common_off);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_charging_state))) {
            boolean chargingState = settingsRepo.chargingState();
            binding.tvCharging.setText(chargingState ? R.string.common_on : R.string.common_off);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_wake_lock))) {
            boolean wakeLock = settingsRepo.wakeLock();
            binding.tvWakeLock.setText(wakeLock ? R.string.common_on : R.string.common_off);
        }
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
                ActivityUtil.startActivity(this, FavoritesActivity.class);
                break;
            case R.id.item_privacy_security:
                ActivityUtil.startActivity(this, PrivacySecurityActivity.class);
                break;
            case R.id.item_journal:
                ActivityUtil.startActivity(this, JournalActivity.class);
                break;
            case R.id.item_help:
                break;
        }
    }
}