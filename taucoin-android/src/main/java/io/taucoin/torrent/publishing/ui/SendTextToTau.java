package io.taucoin.torrent.publishing.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.RepositoryHelper;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Notification;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.ui.notify.NotificationViewModel;

/*
 * Adds "TAU" item in share dialog.
 */

public class SendTextToTau extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        if (intent.hasExtra(Intent.EXTRA_TEXT) && intent.hasExtra(Intent.EXTRA_SUBJECT)) {
            String inviteLink = intent.getStringExtra(Intent.EXTRA_TEXT);
            String friendPk = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            // TODO: 完善验证参数是否有效
            if(StringUtil.isNotEmpty(inviteLink) && StringUtil.isNotEmpty(friendPk)){
                shareInvitedLinkToFriend(inviteLink, friendPk);
                ToastUtils.showShortToast(R.string.share_link_successfully);

                ViewModelProvider provider = new ViewModelProvider(this);
                NotificationViewModel notifyViewModel = provider.get(NotificationViewModel.class);
                notifyViewModel.addNotification(inviteLink, friendPk);
            }

        }

        finish();
        overridePendingTransition(0, 0);
    }


    /**
     * 发送社区邀请链接给朋友
     * @param inviteLink 社区邀请链接
     */
    public void shareInvitedLinkToFriend(String inviteLink, String friendPk) {
        // TODO: 通过msg channel发送
    }
}
