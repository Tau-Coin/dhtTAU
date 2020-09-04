package io.taucoin.torrent.publishing.ui;

import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.ui.customviews.ProgressManager;

public abstract class BaseActivity extends AppCompatActivity {
    private ProgressManager progressManager = null;

    private boolean isFullScreen = true;
    public void setIsFullScreen(boolean isFullScreen){
        this.isFullScreen = isFullScreen;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(isFullScreen){
            setTheme(Utils.getAppTheme(getApplicationContext()));
        }
        if(isFullScreen || Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            ActivityUtil.setRequestedOrientation(this);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        closeProgressDialog();
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if(progressManager != null){
            progressManager.closeProgressDialog();
        }
        this.finish();
    }

    public void showProgressDialog(){
        showProgressDialog(true);
    }

    public void showProgressDialog(boolean isCanCancel){
        progressManager = ProgressManager.newInstance();
        progressManager.showProgressDialog(this, isCanCancel);
    }

    public void closeProgressDialog(){
        if(progressManager != null){
            progressManager.closeProgressDialog();
        }
    }
}
