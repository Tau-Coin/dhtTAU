package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.view.View;
import com.google.common.primitives.Ints;

import java.util.List;

import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.utils.Formatter;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ActivityDataCostBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * 仪表板页面
 */
public class DataCostActivity extends BaseActivity implements DailyQuotaAdapter.OnCheckedChangeListener {

    private ActivityDataCostBinding binding;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();

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

        handleSettingsChanged(getString(R.string.pref_key_is_metered_network));
        handleSettingsChanged(getString(R.string.pref_key_current_speed_list));

        // 先更新，再显示
        NetworkSetting.updateMeteredSpeedLimit();
        handleSettingsChanged(getString(R.string.pref_key_metered_speed_limit));
        NetworkSetting.updateWiFiSpeedLimit();
        handleSettingsChanged(getString(R.string.pref_key_wifi_speed_limit));

        LinearLayoutManager layoutManagerMetered = new LinearLayoutManager(this);
        layoutManagerMetered.setOrientation(RecyclerView.HORIZONTAL);
        binding.rvMeteredDailyQuota.setLayoutManager(layoutManagerMetered);
        DailyQuotaAdapter adapterMetered = new DailyQuotaAdapter(this,
                DailyQuotaAdapter.TYPE_METERED, NetworkSetting.getMeteredLimit());
        binding.rvMeteredDailyQuota.setAdapter(adapterMetered);
        int[] meteredLimits = getResources().getIntArray(R.array.metered_limit);
        List<Integer> meteredList = Ints.asList(meteredLimits);
        adapterMetered.submitList(meteredList);

        LinearLayoutManager layoutManagerWiFi = new LinearLayoutManager(this);
        layoutManagerWiFi.setOrientation(RecyclerView.HORIZONTAL);
        binding.rvWifiDailyQuota.setLayoutManager(layoutManagerWiFi);
        DailyQuotaAdapter adapterWiFi = new DailyQuotaAdapter(this,
                DailyQuotaAdapter.TYPE_WIFI, NetworkSetting.getWiFiLimit());
        binding.rvWifiDailyQuota.setAdapter(adapterWiFi);
        int[] wifiLimits = getResources().getIntArray(R.array.wifi_limit);
        List<Integer> wifiList = Ints.asList(wifiLimits);
        adapterWiFi.submitList(wifiList);
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
    }

    @Override
    public void onStart() {
        super.onStart();
        disposables.add(settingsRepo.observeSettingsChanged()
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSettingsChanged));
    }

    private void handleSettingsChanged(String key) {
        if(StringUtil.isEquals(key, getString(R.string.pref_key_current_speed_list))) {
            long currentSpeed = NetworkSetting.getCurrentSpeed();
            String currentSpeedStr = getString(R.string.setting_metered_network_limit_speed,
                    Formatter.formatFileSize(this, currentSpeed).toUpperCase());
            String noSpeedStr = getString(R.string.setting_metered_network_limit_speed,
                    Formatter.formatFileSize(this, 0).toUpperCase());
            boolean internetState = settingsRepo.internetState();
            boolean meteredNetwork = NetworkSetting.isMeteredNetwork();
            binding.tvMeteredCurrentSpeed.setText(internetState && meteredNetwork ? currentSpeedStr : noSpeedStr);
            binding.tvWifiCurrentSpeed.setText(internetState && !meteredNetwork ? currentSpeedStr : noSpeedStr);
        } else if(StringUtil.isEquals(key, getString(R.string.pref_key_metered_speed_limit))) {
            long meteredSpeedLimit = NetworkSetting.getMeteredSpeedLimit();
            String meteredSpeedLimitStr = Formatter.formatFileSize(this, meteredSpeedLimit).toUpperCase();
            meteredSpeedLimitStr = getString(R.string.setting_metered_network_limit_speed,
                        meteredSpeedLimitStr);
            binding.tvMeteredSpeedLimit.setText(meteredSpeedLimitStr);
        }   else if(StringUtil.isEquals(key, getString(R.string.pref_key_wifi_speed_limit))) {
            long meteredSpeedLimit = NetworkSetting.getWiFiSpeedLimit();
            String meteredSpeedLimitStr = Formatter.formatFileSize(this, meteredSpeedLimit).toUpperCase();
            meteredSpeedLimitStr = getString(R.string.setting_metered_network_limit_speed,
                    meteredSpeedLimitStr);
            binding.tvWifiSpeedLimit.setText(meteredSpeedLimitStr);
        }  else if (key.equals(getString(R.string.pref_key_is_metered_network))) {
            boolean internetState = settingsRepo.internetState();
            boolean meteredNetwork = NetworkSetting.isMeteredNetwork();
            binding.ivMeteredState.setVisibility(internetState && meteredNetwork ? View.VISIBLE : View.INVISIBLE);
            binding.ivWifiState.setVisibility(internetState && !meteredNetwork ? View.VISIBLE : View.INVISIBLE);
        }  else if (key.equals(getString(R.string.pref_key_internet_state))) {
            handleSettingsChanged(getString(R.string.pref_key_is_metered_network));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }
}