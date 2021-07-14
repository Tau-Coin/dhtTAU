package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.TauDaemon;
import io.taucoin.torrent.publishing.core.utils.NetworkSetting;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * 流量提示页面
 */

public class TrafficTipsActivity extends BaseActivity {

    private static final Logger logger = LoggerFactory.getLogger("TrafficTipsActivity");
    private AlertDialog mDialog;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setIsFullScreen(false);
        super.onCreate(savedInstanceState);
        logger.info("Show no remaining data tips dialog start");
        showNoRemainingDataTipsDialog();
    }

    /**
     * 显示没有剩余流量提示对话框
     */
    public void showNoRemainingDataTipsDialog() {
        int leftButton = R.string.cancel;
        int rightButton = R.string.common_proceed;
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(R.string.setting_daily_traffic_limit_used_up)
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
            handleUserSelected(false);
        });

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            mDialog.cancel();
            handleUserSelected(true);
        });
    }

    /**
     * 处理用户流量提示选择
     * @param isProceed 是否继续
     */
    private void handleUserSelected(boolean isProceed) {
        boolean updateDailyDataLimit = false;
        if (isProceed) {
            boolean meteredNetwork = NetworkSetting.isMeteredNetwork();
            if (meteredNetwork) {
                int meteredLimit = NetworkSetting.getMeteredLimit();
                int[] meteredLimits = getResources().getIntArray(R.array.metered_limit);
                for (int i = 0; i < meteredLimits.length; i++) {
                    if (meteredLimit == meteredLimits[i] && i < meteredLimits.length - 1) {
                        NetworkSetting.setMeteredLimit(meteredLimits[i + 1]);
                        NetworkSetting.updateMeteredSpeedLimit();
                        updateDailyDataLimit = true;
                        break;
                    }
                }
            } else {
                int wifiLimit = NetworkSetting.getWiFiLimit();
                int[] wifiLimits = getResources().getIntArray(R.array.wifi_limit);
                for (int i = 0; i < wifiLimits.length; i++) {
                    if (wifiLimit == wifiLimits[i] && i < wifiLimits.length - 1) {
                        NetworkSetting.setWiFiLimit(wifiLimits[i + 1]);
                        NetworkSetting.updateWiFiSpeedLimit();
                        updateDailyDataLimit = true;
                        break;
                    }
                }
            }
        }
        TauDaemon.getInstance(this).handleUserSelected(updateDailyDataLimit);
        this.finish();
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
    protected void onDestroy() {
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        handleUserSelected(false);
    }
}