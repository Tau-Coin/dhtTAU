package io.taucoin.torrent.publishing.ui.setting;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.BuildConfig;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.settings.SettingsRepository;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.DimensionsUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ActivityDashboardBinding;
import io.taucoin.torrent.publishing.databinding.ActivityJournalBinding;
import io.taucoin.torrent.publishing.databinding.TelecomDurationDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.customviews.BadgeActionProvider;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;

/**
 * 仪表板页面
 */
public class JournalActivity extends BaseActivity implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger("JournalActivity");
    private ActivityJournalBinding binding;
    private SettingsRepository settingsRepo;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsRepo = RepositoryHelper.getSettingsRepository(getApplicationContext());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_journal);
        binding.setListener(this);
        initView();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.setting_journal);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String todayTime = DateUtil.formatTime(DateUtil.getTime(), DateUtil.pattern12);
        @SuppressLint("SdCardPath")
        String path = getString(R.string.setting_journal_uri, todayTime);
        binding.tvJournalDirectory.setText(path);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_open_directory) {
            String path = StringUtil.getText(binding.tvJournalDirectory);
            ActivityUtil.shareFile(this, path, getString(R.string.setting_journal_share));
        }
    }
}