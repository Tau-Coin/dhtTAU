package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.databinding.DataBindingUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.utils.DimensionsUtil;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.TrafficUtil;
import io.taucoin.torrent.publishing.databinding.ActivityDashboardBinding;
import io.taucoin.torrent.publishing.databinding.TelecomDurationDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;

/**
 * 仪表板页面
 */
public class DashboardActivity extends BaseActivity implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger("DashboardActivity");
    private ActivityDashboardBinding binding;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CommonDialog dialog;
    private String[] limits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard);
        binding.setListener(this);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        limits = getResources().getStringArray(R.array.metered_limit);

        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_dashboard);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.switchServerMode.setChecked(settingsRepo.serverMode());
        binding.switchAutoMode.setChecked(NetworkSetting.autoMode());
        showAndUpdateMeteredNetworkSetting(false);

        handleSettingsChanged(getString(R.string.pref_key_is_metered_network));
        handleSettingsChanged(getString(R.string.pref_key_internet_state));
        handleSettingsChanged(getString(R.string.pref_key_charging_state));
        handleSettingsChanged(getString(R.string.pref_key_wake_lock));
        handleSettingsChanged(TrafficUtil.getMeteredKey());

        binding.tvMeteredNetworkSpeed.setText(getString(R.string.setting_metered_network_limit_speed,
                Formatter.formatFileSize(this, 0).toUpperCase()));

        // 先更新，再显示
        NetworkSetting.updateMeteredSpeedLimit();
        handleSettingsChanged(getString(R.string.pref_key_metered_speed_limit));

        binding.switchServerMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                settingsRepo.serverMode(isChecked));

        binding.switchAutoMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                NetworkSetting.setAutoMode(isChecked);
            showAndUpdateMeteredNetworkSetting(true);
            });
    }

    /**
     * 显示或更新计费网络的设置
     */
    private void showAndUpdateMeteredNetworkSetting(boolean isUpdate) {
        if (NetworkSetting.autoMode()) {
            binding.ivMeteredLimited.setVisibility(View.GONE);
            binding.llMeteredLimit.setEnabled(false);
            if (isUpdate) {
                NetworkSetting.setAutoModeMeteredLimit();
            }
        } else {
            binding.ivMeteredLimited.setVisibility(View.VISIBLE);
            binding.llMeteredLimit.setEnabled(true);
            if (isUpdate) {
                NetworkSetting.setMeteredLimit(StringUtil.getLongString(limits[0]));
            }
        }
        NetworkSetting.updateMeteredSpeedLimit();
        handleSettingsChanged(TrafficUtil.getMeteredKey());
    }

    /**
     * 显示选择收费网络流量限制的对话框
     */
    private void showSelectMeteredLimitDialog() {
        TelecomDurationDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.telecom_duration_dialog, null, false);
        for (int i = 0; i < limits.length; i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(i);
            radioButton.setTextAppearance(this, R.style.TextAppearanceBody1);
            long limit = StringUtil.getLongString(limits[i]);
            if (limit == 0) {
                radioButton.setText(limits[i]);
            } else {
                radioButton.setText(Formatter.formatShortFileSize(this, limit));
            }
            radioButton.setMinHeight(DimensionsUtil.dip2px(this, 50));
            radioButton.setTag(limits[i]);
            radioButton.setIncludeFontPadding(false);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = DimensionsUtil.dip2px(this, 5);
            binding.radioGroup.addView(radioButton, i, layoutParams);
            if(i == 0){
                radioButton.setChecked(true);
            }
        }
        dialog = new CommonDialog.Builder(this)
                .setContentView(binding.getRoot())
                .setCanceledOnTouchOutside(false)
                .setPositiveButton(R.string.common_proceed, (dialog, which) -> {
                    dialog.dismiss();
                    int checkedId = binding.radioGroup.getCheckedRadioButtonId();
                    String duration = limits[checkedId];
                    long limit = StringUtil.getLongString(duration);
                    NetworkSetting.setMeteredLimit(limit);
                    logger.info("Metered limit selected::{}, {}, {}", checkedId, duration, limit);
                    handleSettingsChanged(TrafficUtil.getMeteredKey());
                    NetworkSetting.updateMeteredSpeedLimit();
                    handleSettingsChanged(getString(R.string.pref_key_metered_speed_limit));
                })
                .create();
        dialog.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        disposables.add(settingsRepo.observeSettingsChanged().subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSettingsChanged));
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
        } else if(StringUtil.isEquals(key, TrafficUtil.getMeteredKey())) {
            long meteredTotal = TrafficUtil.getMeteredTrafficTotal();
            long meteredLimited = NetworkSetting.meteredLimit();
            String meteredLimitedStr = Formatter.formatShortFileSize(this,
                    meteredLimited).toUpperCase();
            if (meteredLimited == 0) {
                meteredLimitedStr = limits[limits.length - 1];
            }
            binding.tvMeteredNetworkUsage.setText(getString(R.string.setting_metered_network_limit,
                    Formatter.formatFileSize(this, meteredTotal).toUpperCase(),
                    meteredLimitedStr));
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_current_speed_list))) {
            long meteredSpeed = NetworkSetting.getCurrentSpeed();
            binding.tvMeteredNetworkSpeed.setText(getString(R.string.setting_metered_network_limit_speed,
                    Formatter.formatFileSize(this, meteredSpeed).toUpperCase()));
        }else if(StringUtil.isEquals(key, getString(R.string.pref_key_metered_speed_limit))) {
            long meteredLimited = NetworkSetting.meteredLimit();
            String meteredSpeedLimitStr;
            if (meteredLimited == 0) {
                meteredSpeedLimitStr = limits[limits.length - 1];
            } else {
                long meteredSpeedLimit = NetworkSetting.getMeteredSpeedLimit();
                meteredSpeedLimitStr = Formatter.formatFileSize(this, meteredSpeedLimit).toUpperCase();
                meteredSpeedLimitStr = getString(R.string.setting_metered_network_limit_speed,
                        meteredSpeedLimitStr);
            }
            binding.tvMeteredNetworkSpeedLimit.setText(meteredSpeedLimitStr);
        }  else if (key.equals(getString(R.string.pref_key_is_metered_network))) {
            int networkType = NetworkSetting.isMeteredNetwork() ? R.string.setting_metered_network :
                    R.string.setting_un_metered_network;
            binding.tvMeteredNetworkType.setText(networkType);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(dialog != null){
            dialog.closeDialog();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ll_metered_limit) {
            showSelectMeteredLimitDialog();
        }
    }
}