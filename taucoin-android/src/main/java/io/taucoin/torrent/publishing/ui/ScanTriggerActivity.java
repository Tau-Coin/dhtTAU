package io.taucoin.torrent.publishing.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.PermissionUtils;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.permission.EasyPermissions;
import io.taucoin.torrent.publishing.ui.qrcode.ScanQRCodeActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 触发扫码的页面需要
 */
public abstract class ScanTriggerActivity extends BaseActivity {

    private static final int SCAN_CODE = 0X100;
    private User userTemp;
    private boolean isExit = false;
    private UserViewModel viewModel;
    private boolean scanKeyOnly = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void openScanQRActivityAndExit() {
        this.isExit = true;
        requestCameraPermissions();
    }

    protected void openScanQRActivity(User user) {
        this.userTemp = user;
        requestCameraPermissions();
    }

    protected void openScanQRActivity() {
        requestCameraPermissions();
    }

    public void openScanQRActivity(UserViewModel viewModel) {
        this.scanKeyOnly = true;
        this.viewModel = viewModel;
        requestCameraPermissions();
    }

    private void directOpenScanQRActivity() {
        if (isExit) {
            onBackPressed();
            isExit = false;
        }
        Intent intent = new Intent();
        if (userTemp != null) {
            intent.putExtra(IntentExtra.BEAN, userTemp);
        }
        intent.putExtra(IntentExtra.SCAN_KEY_ONLY, scanKeyOnly);
        ActivityUtil.startActivityForResult(intent,this, ScanQRCodeActivity.class, SCAN_CODE);

    }
    /**
     * 请求摄像头权限
     */
    private void requestCameraPermissions() {
        String permission = Manifest.permission.CAMERA;
        if(!EasyPermissions.hasPermissions(this, permission)){
            EasyPermissions.requestPermissions(this,
                    this.getString(R.string.permission_tip_camera_denied),
                    PermissionUtils.REQUEST_PERMISSIONS_CAMERA, permission);
        } else {
            directOpenScanQRActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PermissionUtils.REQUEST_PERMISSIONS_CAMERA:
                if (grantResults.length > 0) {
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        PermissionUtils.checkUserBanPermission(this, (dialog, which) -> { },
                                permissions[0], R.string.permission_tip_camera_never_ask_again);
                    } else {
                        directOpenScanQRActivity();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == SCAN_CODE) {
            if (data != null && viewModel != null) {
                String key = data.getStringExtra(IntentExtra.DATA);
                viewModel.importSeed(key, null);
            }
        }
    }
}
