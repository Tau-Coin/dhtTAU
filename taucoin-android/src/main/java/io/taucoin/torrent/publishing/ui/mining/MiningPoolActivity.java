package io.taucoin.torrent.publishing.ui.mining;

import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * MiningPool
 */
public class MiningPoolActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
//        viewModel = provider.get(MainViewModel.class);
        setContentView(R.layout.activity_mining_pool);
        initView();
    }

    private void initView() {

    }
}