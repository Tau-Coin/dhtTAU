package io.taucoin.torrent.publishing.ui.download;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.Version;
import io.taucoin.torrent.publishing.core.utils.AppUtil;
import io.taucoin.torrent.publishing.core.utils.PermissionUtils;
import io.taucoin.torrent.publishing.databinding.DialogDownloadProgressBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.permission.EasyPermissions;

/**
 * version upgrade activity
 */

public class UpgradeActivity extends BaseActivity {

    private static final Logger logger = LoggerFactory.getLogger("UpgradeActivity");
    private DownloadViewModel downloadViewModel;
    private Version version;
    private AlertDialog mDialog;
    private DialogDownloadProgressBinding binding;
    private boolean isDownload = false;
    private CompositeDisposable disposables = new CompositeDisposable();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setIsFullScreen(false);
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        downloadViewModel = provider.get(DownloadViewModel.class);
        version = getIntent().getParcelableExtra(IntentExtra.BEAN);
        if (null == version) {
            this.finish();
        } else {
            logger.info("Upgrade show dialog start");
            showUpgradeDialog();
        }
    }

    /**
     * 观察下载进度
     */
    private void observeDownloadProgress() {
        downloadViewModel.getOnProgress().observe(this, progress -> {
            updateProgress(progress.intValue());
        });
    }

    /**
     * show UpGrade Dialog
     */
    public void showUpgradeDialog() {
        logger.info("show upgrade dialog for user");
        String storagePath = version.getDownloadFilePath() + version.getDownloadFileName();
        isDownload = AppUtil.isApkFileExists(this, storagePath);
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.cancel();
        }
        int leftButton = version.isForced() ? R.string.exit : R.string.cancel;
        int rightButton = isDownload ? R.string.app_upgrade_install : R.string.ok;
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.app_upgrade_title)
                .setMessage(Html.fromHtml(version.getContent()))
                .setNegativeButton(leftButton, null)
                .setPositiveButton(rightButton, null)
                .setCancelable(false);

        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);
        mDialog.show();

        setMessageStyle(mDialog);

        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            mDialog.cancel();
            if(!version.isForced()){
                downloadViewModel.setNeedPromptUser(false);
            }
            this.finish();
        });

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (isDownload) {
                downloadViewModel.installApk(this, version);
            } else {
                String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                boolean isAndroidQ = Build.VERSION.SDK_INT > Build.VERSION_CODES.P;
                if (EasyPermissions.hasPermissions(this, permission) || isAndroidQ) {
                    mDialog.cancel();
                    showDownloadProgressDialog();
                    downloadViewModel.downLoadUpgradeApk(version);
                } else {
                    EasyPermissions.requestPermissions(this,
                            getString(R.string.permission_tip_upgrade_denied),
                            PermissionUtils.REQUEST_PERMISSIONS_STORAGE, permission);
                }
            }
        });
    }

    /**
     * show download progress dialog
     */
    private void showDownloadProgressDialog() {
        closeDialog();
        observeDownloadProgress();
        binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.dialog_download_progress, null, false);
        binding.progressBar.setMax(100);
        updateProgress(0);
        int leftButton = version.isForced() ? R.string.exit : R.string.cancel;
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.app_upgrade_progress)
                .setNegativeButton(leftButton, null)
                .setView(binding.getRoot());

        mDialog = builder.create();
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setCancelable(false);
        mDialog.show();
        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            mDialog.cancel();
            if(!version.isForced()){
                downloadViewModel.setNeedPromptUser(false);
            }
            this.finish();
        });
        disposables.add(Observable.create((ObservableOnSubscribe<View>)
            e -> mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(e::onNext))
            .throttleFirst(2, TimeUnit.SECONDS)
            .subscribe(view -> {
                if(isDownload){
                    downloadViewModel.installApk(this, version);
                }
            }));
    }

    /**
     * update download Progress
     */
    private void updateProgress(int progress) {
        if(null == binding){
            return;
        }
        if (progress == LocalDownloadManager.DOWNLOAD_STATUS_REMOVED) {
            this.finish();
            return;
        }
        binding.tvFailMsg.setVisibility(progress == LocalDownloadManager.DOWNLOAD_STATUS_FAILED
                ? View.VISIBLE : View.INVISIBLE);
        if (progress == LocalDownloadManager.DOWNLOAD_STATUS_FAILED) {
            progress = 0;
        }
        binding.progressBar.setProgress(progress);
        String progressStr = progress + "%";
        binding.tvProgress.setText(progressStr);
        if (progress == 0) {
            mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
        } else if (progress == 100){
            isDownload = true;
            mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.app_upgrade_install);
            mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.VISIBLE);
        }
    }

    public void closeDialog() {
        if(mDialog != null && mDialog.isShowing()){
            mDialog.cancel();
        }
    }

    private void setMessageStyle(AlertDialog mDialog) {
        try {
            Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
            mAlert.setAccessible(true);
            Object mAlertController = mAlert.get(mDialog);
            Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
            mMessage.setAccessible(true);
            TextView mMessageView = (TextView) mMessage.get(mAlertController);
            mMessageView.setLineSpacing(getResources().getDimensionPixelSize(R.dimen.widget_size_5), 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.REQUEST_PERMISSIONS_STORAGE) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    String storagePath = version.getDownloadFilePath() + version.getDownloadFileName();
                    isDownload = AppUtil.isApkFileExists(this, storagePath);
                    if (isDownload) {
                        downloadViewModel.installApk(this, version);
                    } else {
                        showDownloadProgressDialog();
                        downloadViewModel.downLoadUpgradeApk(version);
                    }
                } else {
                    PermissionUtils.checkUserBanPermission(this, permissions[0], R.string.permission_tip_upgrade_never_ask_again);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeDialog();
        if(version != null && version.isForced()){
            downloadViewModel.closeDownloading();
            AppUtil.appSafeExit();
        }else{
            downloadViewModel.closeQuerySchedule();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}