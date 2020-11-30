package io.taucoin.torrent.publishing.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.main.MainActivity;

/**
 * 外部点击TAUchain link跳转页面
 */

public class ExternalLinkActivity extends BaseActivity {
    public static final String ACTION_CHAIN_LINK_CLICK = "io.taucoin.torrent.publishing.ui.ACTION_CHAIN_LINK_CLICK";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setIsFullScreen(false);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if(null != uri){
            String chainLink = uri.toString();
            ChainLinkUtil.ChainLink decode = ChainLinkUtil.decode(chainLink);
            if(decode.isValid()){
                Intent mainIntent = new Intent(this.getApplicationContext(), MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                mainIntent.setAction(ACTION_CHAIN_LINK_CLICK);
                mainIntent.putExtra(IntentExtra.CHAIN_LINK, chainLink);
                this.startActivity(mainIntent);
            }
        }

        finish();
        overridePendingTransition(0, 0);
    }
}
