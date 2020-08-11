package io.taucoin.torrent.publishing.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.ui.customviews.ProgressManager;

public abstract class BaseActivity extends AppCompatActivity {
    private ProgressManager progressManager = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Utils.getAppTheme(getApplicationContext()));
//        setAndroidNativeLightStatusBar(this, true);
        super.onCreate(savedInstanceState);
    }

    private static void setAndroidNativeLightStatusBar(Activity activity, boolean dark) {
        View decor = activity.getWindow().getDecorView();
        if (dark) {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
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
