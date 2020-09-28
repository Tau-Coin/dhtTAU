package io.taucoin.torrent.publishing.ui.splash;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.PermissionUtils;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.customviews.permission.EasyPermissions;
import io.taucoin.torrent.publishing.ui.main.MainActivity;

public class SplashActivity extends BaseActivity {

    private static final Logger logger = LoggerFactory.getLogger("SplashActivity");
    private volatile boolean isAsk = false;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logger.info("SplashActivity.onCreate");
        if (!this.isTaskRoot()) {
            Intent intent = getIntent();
            if (intent != null) {
                String action = intent.getAction();
                if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                    logger.info("SplashActivity immediate finish");
                    finish();
                    ActivityUtil.moveTaskToFront();
                }
            }
        } else {
            ActivityUtil.fullScreenAll(this);
            logger.info("SplashActivity show");
            // Open for the first time
            setContentView(R.layout.activity_splash);

            // 每次APP重新启动如果有新版本更新需要提示用户
            RepositoryHelper.getSettingsRepository(this).setNeedPromptUser(true);

            logger.info("SplashActivity onCreate");

            requestWriteLogPermissions();

            // delay 3 seconds jump
            disposables.add(Observable.timer(3, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(aLong -> splashJump()));
        }
    }

    private synchronized void splashJump() {
        if(!isAsk){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            logger.info("Jump to MainActivity");
        }
        isAsk = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    /**
     * Shielded return key
     * */
    @Override
    public void onBackPressed() {

    }

    private void requestWriteLogPermissions() {
        boolean isAndroidQ = Build.VERSION.SDK_INT > Build.VERSION_CODES.P;
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if(!isAndroidQ && !EasyPermissions.hasPermissions(this, permission)){
            isAsk = true;
            EasyPermissions.requestPermissions(this,
                    this.getString(R.string.permission_tip_upgrade_denied),
                    permissionCallbacks,
                    PermissionUtils.REQUEST_PERMISSIONS_STORAGE, permission);
        }
    }

    private synchronized void handlePermissionsCallBack() {
        if(!isAsk){
            splashJump();
        }
        isAsk = false;
    }

    private EasyPermissions.PermissionCallbacks permissionCallbacks = new EasyPermissions.PermissionCallbacks(){

        @Override
        public void onPermissionsGranted(int requestCode, List<String> granted) {

        }

        @Override
        public void onPermissionsDenied(int requestCode, List<String> denied) {
            handlePermissionsCallBack();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PermissionUtils.REQUEST_PERMISSIONS_STORAGE:
                handlePermissionsCallBack();
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                        loggerContext.reset();
                        ContextInitializer ci = new ContextInitializer(loggerContext);
                        try {
                            ci.autoConfig();
                        } catch (JoranException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            default:
                break;
        }
    }
}