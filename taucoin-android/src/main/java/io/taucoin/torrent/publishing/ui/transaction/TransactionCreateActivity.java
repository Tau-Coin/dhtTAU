package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.entity.Community;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.DateUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityTransactionCreateBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityChooseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.PopUpDialog;
import io.taucoin.types.MsgType;

/**
 * 交易创建页面页面
 */
public class TransactionCreateActivity extends BaseActivity implements View.OnClickListener {

    private static final int REQUEST_CODE = 100;
    private ActivityTransactionCreateBinding binding;

    private TxViewModel txViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Community community;
    private PopUpDialog popUpDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_transaction_create);
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
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_transactions);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if(community != null){
            binding.tvCommunity.setText(getString(R.string.tx_community_selected, community.communityName));
            binding.tvCommunity.setTag(community.chainID);

            String[] typeNames = getResources().getStringArray(R.array.tx_type_name);
            int typeCode = MsgType.Wiring.getVaLue();
            if(typeCode < typeNames.length){
                binding.tvType.setText(getString(R.string.tx_type_selected, typeNames[typeCode]));
                binding.tvType.setTag(typeCode);
            }
            binding.tvFee.setText(getString(R.string.tx_median_fee, "96.5", "TAU"));
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
        disposables.clear();
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
        // 交易创建事件
        if (item.getItemId() == R.id.menu_done) {
            Tx tx = buildTx();
            txViewModel.addTransaction(tx);
        }
        return true;
    }

    /**
     * 构建交易数据
     * @return Tx
     */
    private Tx buildTx() {
        String chainID = ViewUtils.getStringTag(binding.tvCommunity);
        int txType = ViewUtils.getIntTag(binding.tvType);
        String receiverAddress = ViewUtils.getText(binding.etAddress);
        String amount = ViewUtils.getText(binding.etAmount);
        String fee = ViewUtils.getStringTag(binding.tvFee);
        String memo = ViewUtils.getText(binding.etMemo);
        return new Tx(chainID, receiverAddress, 100000000, 1000, txType, memo, 0);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_community:
                if(community != null){
                    Intent intent = new Intent();
                    intent.putExtra(IntentExtra.CHAIN_ID, community.chainID);
                    ActivityUtil.startActivityForResult(intent, this, CommunityChooseActivity.class, REQUEST_CODE);
                }
                break;
            case R.id.tv_type:
                showPopUpDialog();
                break;
            case R.id.tv_fee:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK){
            if(data != null){
                Community community = data.getParcelableExtra(IntentExtra.BEAN);
                if(community != null){
                    this.community = community;
                    binding.tvCommunity.setText(getString(R.string.tx_community_selected, community.communityName));
                    binding.tvCommunity.setTag(community.chainID);
                }
            }
        }
    }

    /**
     * 显示底部弹出对话框
     */
    private void showPopUpDialog() {
        PopUpDialog.Builder builder = new PopUpDialog.Builder(this)
                .setOnItemClickListener((dialog, name, code) -> {
                    dialog.dismiss();
                    binding.tvType.setText(getString(R.string.tx_type_selected, name));
                    binding.tvType.setTag(code);
                });
        String[] typeNames = getResources().getStringArray(R.array.tx_type_name);
        for (int i = 0; i < MsgType.values().length; i++) {
            MsgType msgType = MsgType.setValue((byte) i);
            if(msgType != null && !msgType.equals(MsgType.IdentityAnnouncement)){
                if(i < typeNames.length){
                    builder.addItems(typeNames[i], i);
                }
            }
        }
        popUpDialog = builder.create();
        popUpDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(popUpDialog != null){
            popUpDialog.closeDialog();
        }
    }
}