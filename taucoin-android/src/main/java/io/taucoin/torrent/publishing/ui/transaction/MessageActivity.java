package io.taucoin.torrent.publishing.ui.transaction;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityMessageBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.types.MsgType;

/**
 * Chain note页面
 */
public class MessageActivity extends BaseActivity implements View.OnClickListener {

    private ActivityMessageBinding binding;
    private TxViewModel txViewModel;
    private Community community;
    private String replyID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        txViewModel = provider.get(TxViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_message);
        binding.setListener(this);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getIntent() != null){
            community = getIntent().getParcelableExtra(IntentExtra.BEAN);
            replyID = getIntent().getStringExtra(IntentExtra.REPLY_ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_message);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if(StringUtil.isNotEmpty(replyID)){
            binding.toolbarInclude.toolbar.setTitle(R.string.community_comment);
            binding.etInput.setHint(R.string.tx_comment);
        }
        if(community != null){
            binding.tvFee.setText(getString(R.string.tx_median_fee, "96.5", UsersUtil.getCoinName(community)));
            binding.tvFee.setTag("96.5");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        txViewModel.getAddState().observe(this, result -> {
            if(StringUtil.isNotEmpty(result)){
                ToastUtils.showShortToast(result);
            }else {
                onBackPressed();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_done) {
            Tx tx = buildTx();
            if(txViewModel.validateTx(tx)){
                txViewModel.addTransaction(tx);
            }
        }
        return true;
    }

    /**
     * 构建交易数据
     * @return Tx
     */
    private Tx buildTx() {
        String chainID = community.chainID;
        int txType = MsgType.RegularForum.getVaLue();
        String fee = ViewUtils.getStringTag(binding.tvFee);
        String memo = ViewUtils.getText(binding.etInput);
        Tx tx = new Tx(chainID, FmtMicrometer.fmtTxLongValue(fee), txType, memo);
        if(StringUtil.isNotEmpty(replyID)){
            tx.replyID = replyID;
            tx.txType = MsgType.ForumComment.getVaLue();
        }
        return tx;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_fee:
                txViewModel.showEditFeeDialog(this, binding.tvFee, community);
                break;
        }
    }
}