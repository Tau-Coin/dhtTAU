package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.view.LayoutInflater;
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
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.DimensionsUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ActivityDashboardBinding;
import io.taucoin.torrent.publishing.databinding.TelecomDurationDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;

/**
 * 仪表板页面
 */
public class DashboardActivity extends BaseActivity {

    private static final Logger logger = LoggerFactory.getLogger("DashboardActivity");
    private ActivityDashboardBinding binding;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();
    private CommonDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dashboard);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.drawer_dashboard);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.switchServerMode.setChecked(settingsRepo.serverMode());
        binding.switchWifiOnly.setChecked(settingsRepo.wifiOnly());

        boolean internetState = settingsRepo.internetState();
        binding.tvInternet.setText(internetState ? R.string.common_on : R.string.common_off);

        boolean chargingState = settingsRepo.chargingState();
        binding.tvCharging.setText(chargingState ? R.string.common_on : R.string.common_off);

        boolean wakeLock = settingsRepo.wakeLock();
        binding.tvWakeLock.setText(wakeLock ? R.string.common_on : R.string.common_off);

        binding.switchServerMode.setOnCheckedChangeListener((buttonView, isChecked) -> settingsRepo.serverMode(isChecked));

        binding.switchWifiOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsRepo.wifiOnly(isChecked);
            if(!isChecked){
                showSelectTelecomDurationDialog();
            }
        });
    }

    /**
     * 显示选择使用电信数据时长的对话框
     */
    private void showSelectTelecomDurationDialog() {
        TelecomDurationDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.telecom_duration_dialog, null, false);
        final String[] names = getResources().getStringArray(R.array.duration_name);
        final int[] values = getResources().getIntArray(R.array.duration_value);
        for (int i = 0; i < names.length; i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(i);
            radioButton.setTextAppearance(this, R.style.TextAppearanceBody1);
            radioButton.setText(names[i]);
            radioButton.setTag(values[i]);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = DimensionsUtil.dip2px(this, 20);
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
                    int duration = values[checkedId];
                    long endTime = DateUtil.addTimeDuration(duration);
                    settingsRepo.telecomDataEndTime(endTime);
                    logger.info("Duration selected::{}, {}, {}", checkedId, values[checkedId], names[checkedId]);
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
        if(StringUtil.isEquals(key, getString(R.string.pref_key_internet_state))){
            boolean internetState = settingsRepo.internetState();
            binding.tvInternet.setText(internetState ? R.string.common_on : R.string.common_off);
        }else if(StringUtil.isEquals(key, getString(R.string.pref_key_charging_state))){
            boolean chargingState = settingsRepo.chargingState();
            binding.tvCharging.setText(chargingState ? R.string.common_on : R.string.common_off);
        }else if(StringUtil.isEquals(key, getString(R.string.pref_key_wake_lock))){
            boolean wakeLock = settingsRepo.wakeLock();
            binding.tvWakeLock.setText(wakeLock ? R.string.common_on : R.string.common_off);
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
}