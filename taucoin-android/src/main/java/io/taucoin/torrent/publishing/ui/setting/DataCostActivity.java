package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import com.google.common.primitives.Ints;

import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.BuildConfig;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.FrequencyUtil;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ActivityDataCostBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * 仪表板页面
 */
public class DataCostActivity extends BaseActivity implements DailyQuotaAdapter.OnCheckedChangeListener,
        CompoundButton.OnCheckedChangeListener {

    private ActivityDataCostBinding binding;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private DailyQuotaAdapter adapterMetered;
    private DailyQuotaAdapter adapterWiFi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_data_cost);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_data_cost);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        LinearLayoutManager layoutManagerMetered = new LinearLayoutManager(this);
        layoutManagerMetered.setOrientation(RecyclerView.HORIZONTAL);
        binding.rvMeteredDailyQuota.setLayoutManager(layoutManagerMetered);
        adapterMetered = new DailyQuotaAdapter(this,
                DailyQuotaAdapter.TYPE_METERED, NetworkSetting.getMeteredLimit());
        binding.rvMeteredDailyQuota.setAdapter(adapterMetered);
        int[] meteredLimits = getResources().getIntArray(R.array.metered_limit);
        List<Integer> meteredList = Ints.asList(meteredLimits);
        adapterMetered.submitList(meteredList);

        LinearLayoutManager layoutManagerWiFi = new LinearLayoutManager(this);
        layoutManagerWiFi.setOrientation(RecyclerView.HORIZONTAL);
        binding.rvWifiDailyQuota.setLayoutManager(layoutManagerWiFi);
        adapterWiFi = new DailyQuotaAdapter(this,
                DailyQuotaAdapter.TYPE_WIFI, NetworkSetting.getWiFiLimit());
        binding.rvWifiDailyQuota.setAdapter(adapterWiFi);
        int[] wifiLimits = getResources().getIntArray(R.array.wifi_limit);
        List<Integer> wifiList = Ints.asList(wifiLimits);
        adapterWiFi.submitList(wifiList);

        if (!BuildConfig.DEBUG) {
            binding.switchBackground.setVisibility(View.GONE);
        }
        binding.switchBackground.setChecked(NetworkSetting.isEnableBackgroundMode());
        binding.switchBackground.setOnCheckedChangeListener(this);

        handleSettingsChanged(getString(R.string.pref_key_is_metered_network));
        handleSettingsChanged(getString(R.string.pref_key_current_speed));
        handleSettingsChanged(getString(R.string.pref_key_main_loop_interval_list));
        handleSettingsChanged(getString(R.string.pref_key_metered_foreground_running_time));
        handleSettingsChanged(getString(R.string.pref_key_background_running_time));

        // 先更新，再显示
        NetworkSetting.updateMeteredSpeedLimit();
        handleSettingsChanged(getString(R.string.pref_key_metered_screen_time_average_speed));
        handleSettingsChanged(getString(R.string.pref_key_metered_background_average_speed));
        handleSettingsChanged(getString(R.string.pref_key_metered_available_data));
        NetworkSetting.updateWiFiSpeedLimit();
        handleSettingsChanged(getString(R.string.pref_key_wifi_screen_time_average_speed));
        handleSettingsChanged(getString(R.string.pref_key_wifi_background_average_speed));
        handleSettingsChanged(getString(R.string.pref_key_wifi_available_data));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        NetworkSetting.enableBackgroundMode(isChecked);
    }

    @Override
    public void onCheckedChanged(int type, int limit) {
        if (type == DailyQuotaAdapter.TYPE_METERED) {
            NetworkSetting.setMeteredLimit(limit);
            NetworkSetting.updateMeteredSpeedLimit();
        } else if (type == DailyQuotaAdapter.TYPE_WIFI) {
            NetworkSetting.setWiFiLimit(limit);
            NetworkSetting.updateWiFiSpeedLimit();
        }
        handleSettingsChanged(getString(R.string.pref_key_metered_foreground_running_time));
    }

    @Override
    public void onStart() {
        super.onStart();
        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSettingsChanged));
    }

    /**
     * 用户设置参数变化
     * @param key
     */
    private void handleSettingsChanged(String key) {
        if(StringUtil.isEquals(key, getString(R.string.pref_key_current_speed))) {
            long currentSpeed = NetworkSetting.getCurrentSpeed();
            String currentSpeedStr = getString(R.string.setting_metered_network_limit_speed,
                    Formatter.formatFileSize(this, currentSpeed).toUpperCase());
            String noSpeedStr = getString(R.string.setting_metered_network_limit_speed,
                    Formatter.formatFileSize(this, 0).toUpperCase());
            boolean internetState = settingsRepo.internetState();
            boolean meteredNetwork = NetworkSetting.isMeteredNetwork();
            binding.tvMeteredCurrentSpeed.setText(internetState && meteredNetwork ? currentSpeedStr : noSpeedStr);
            binding.tvWifiCurrentSpeed.setText(internetState && !meteredNetwork ? currentSpeedStr : noSpeedStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_metered_screen_time_average_speed))) {
            long averageSpeed = NetworkSetting.getMeteredScreenTimeAverageSpeed();
            String averageSpeedStr = Formatter.formatFileSize(this, averageSpeed).toUpperCase();
            averageSpeedStr = getString(R.string.setting_metered_network_limit_speed, averageSpeedStr);
            binding.tvMeteredAverageSpeedScreenTime.setText(averageSpeedStr);
        }  else if(StringUtil.isEquals(key, getString(R.string.pref_key_metered_background_average_speed))) {
            long averageSpeed = NetworkSetting.getMeteredBackgroundAverageSpeed();
            String averageSpeedStr = Formatter.formatFileSize(this, averageSpeed).toUpperCase();
            averageSpeedStr = getString(R.string.setting_metered_network_limit_speed, averageSpeedStr);
            binding.tvMeteredAverageSpeedBackground.setText(averageSpeedStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_wifi_screen_time_average_speed))) {
            long averageSpeed = NetworkSetting.getWifiScreenTimeAverageSpeed();
            String averageSpeedStr = Formatter.formatFileSize(this, averageSpeed).toUpperCase();
            averageSpeedStr = getString(R.string.setting_metered_network_limit_speed, averageSpeedStr);
            binding.tvWifiAverageSpeedScreenTime.setText(averageSpeedStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_wifi_background_average_speed))) {
            long averageSpeed = NetworkSetting.getWifiBackgroundAverageSpeed();
            String averageSpeedStr = Formatter.formatFileSize(this, averageSpeed).toUpperCase();
            averageSpeedStr = getString(R.string.setting_metered_network_limit_speed, averageSpeedStr);
            binding.tvWifiAverageSpeedBackground.setText(averageSpeedStr);
        } else if (key.equals(getString(R.string.pref_key_is_metered_network))) {
            boolean internetState = settingsRepo.internetState();
            boolean meteredNetwork = NetworkSetting.isMeteredNetwork();
            binding.ivMeteredState.setVisibility(internetState && meteredNetwork ? View.VISIBLE : View.INVISIBLE);
            binding.ivWifiState.setVisibility(internetState && !meteredNetwork ? View.VISIBLE : View.INVISIBLE);

            if (meteredNetwork) {
                binding.llRoot.removeView(binding.llMetered);
                binding.llRoot.addView(binding.llMetered, 1);
            } else {
                binding.llRoot.removeView(binding.llWifi);
                binding.llRoot.addView(binding.llWifi, 1);
            }
        } else if (key.equals(getString(R.string.pref_key_internet_state))) {
            handleSettingsChanged(getString(R.string.pref_key_is_metered_network));
        } else if (key.equals(getString(R.string.pref_key_metered_available_data))) {
            long availableData = NetworkSetting.getMeteredAvailableData();
            String availableDataStr = Formatter.formatFileSize(this, availableData).toUpperCase();
            binding.tvMeteredAvailableData.setText(availableDataStr);
        } else if (key.equals(getString(R.string.pref_key_wifi_available_data))) {
            long availableData = NetworkSetting.getWiFiAvailableData();
            String availableDataStr = Formatter.formatFileSize(this, availableData).toUpperCase();
            binding.tvWifiAvailableData.setText(availableDataStr);
        } else if (key.equals(getString(R.string.pref_key_metered_limit))) {
            adapterMetered.updateSelectLimit(NetworkSetting.getMeteredLimit());
        } else if (key.equals(getString(R.string.pref_key_wifi_limit))) {
            adapterWiFi.updateSelectLimit(NetworkSetting.getWiFiLimit());
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_main_loop_interval_list))) {
            double frequency = FrequencyUtil.getMainLoopFrequency();
            binding.tvWorkingFrequency.setText(FmtMicrometer.formatTwoDecimal(frequency));
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_metered_foreground_running_time)) ||
                StringUtil.isEquals(key, getString(R.string.pref_key_wifi_foreground_running_time))) {
            int screenTime = NetworkSetting.getForegroundModeTime();
            String model;
            int limitHours = NetworkSetting.getScreenTimeLimitHours();
            int screenTimeLimit = limitHours * 60 * 60;
            if (screenTime < screenTimeLimit) {
                String times = DateUtil.getFormatTime(screenTime);
                model = getString(R.string.setting_work_frequency_in_screen_time,
                        limitHours, times);
            } else {
                model = getString(R.string.setting_work_frequency_not_in_screen_time,
                        limitHours, limitHours);
            }
            binding.tvWorkFrequencyModel.setText(model);
        }  else if(StringUtil.isEquals(key, getString(R.string.pref_key_background_running_time))) {
            int backgroundTime = settingsRepo.getIntValue(key);
            String backgroundTimeStr = DateUtil.getFormatTime(backgroundTime);
            binding.tvBackgroundTime.setText(getString(R.string.setting_running_time, backgroundTimeStr));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}