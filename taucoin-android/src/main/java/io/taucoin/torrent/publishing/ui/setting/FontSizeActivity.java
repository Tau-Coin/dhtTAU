package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.widget.SeekBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.databinding.DataBindingUtil;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.databinding.ActivityFontSizeBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.Constants;

/**
 * 字体大小设置页面
 */
public class FontSizeActivity extends BaseActivity {

    public static final int REQUEST_CODE_FONT_SIZE = 0x0001;
    private static final Logger logger = LoggerFactory.getLogger("FontSizeActivity");
    private ActivityFontSizeBinding binding;
    private SettingsRepository settingsRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_font_size);
        settingsRepo = RepositoryHelper.getSettingsRepository(this);
        initView();
    }

    @Override
    protected void refreshAllView() {
        binding = DataBindingUtil.setContentView(FontSizeActivity.this, R.layout.activity_font_size);
        initView();
        setResult(RESULT_OK);
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_font_size);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String[] scaleTitles = getResources().getStringArray(R.array.font_scale_title);
        String[] scaleSizes = getResources().getStringArray(R.array.font_scale_size);

        binding.seekBar.setFontScaleSizes(scaleSizes);
        binding.seekBar.setFontScaleTitles(scaleTitles);

        float fontScaleSize = settingsRepo.getFloatValue(Constants.PREF_KEY_FONT_SCALE_SIZE, 1.0f);
        binding.seekBar.setFontScaleSize(fontScaleSize);
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scaleSize = binding.seekBar.getFontScaleSize(progress);
                logger.debug("scaleSize::{}", scaleSize);
                settingsRepo.setFloatValue("pref_key_font_scale_size", scaleSize);
                refreshAllView();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
}