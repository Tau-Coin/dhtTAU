package io.taucoin.torrent.publishing.ui.transaction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.entity.Community;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityTransactionCreateBinding;
import io.taucoin.torrent.publishing.databinding.EditFeeDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.types.MsgType;

/**
 * 交易创建页面页面
 */
public class TransactionCreateActivity extends BaseActivity implements View.OnClickListener {

    private ActivityTransactionCreateBinding binding;

    private TxViewModel txViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Community community;
    private CommonDialog editFeeDialog;

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
        binding.toolbarInclude.toolbar.setTitle(R.string.community_transaction);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

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
                ToastUtils.showShortToast(R.string.tx_created_successfully);
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
        int txType = MsgType.Wiring.getVaLue();
        String receiverPk = ViewUtils.getText(binding.etPublicKey);
        String amount = ViewUtils.getText(binding.etAmount);
        String fee = ViewUtils.getStringTag(binding.tvFee);
        String memo = ViewUtils.getText(binding.etMemo);
        return new Tx(chainID, receiverPk, FmtMicrometer.fmtTxLongValue(amount),
                FmtMicrometer.fmtTxLongValue(fee), txType, memo);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_fee:
                showEditFeeDialog();
                break;
        }
    }

    /**
     * 显示编辑交易费的对话框
     */
    private void showEditFeeDialog() {
        EditFeeDialogBinding editFeeBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.edit_fee_dialog, null, false);
        editFeeBinding.etFee.setText("96.5");
        editFeeBinding.tvMedianFee.setText(getString(R.string.tx_median_fee_tips, "96.5"));
        editFeeDialog = new CommonDialog.Builder(this)
                .setContentView(editFeeBinding.getRoot())
                .setPositiveButton(R.string.common_submit, (dialog, which) -> {
                    dialog.cancel();
                    String fee = editFeeBinding.etFee.getText().toString();
                    if(StringUtil.isNotEmpty(fee)){
                        binding.tvFee.setText(getString(R.string.tx_median_fee, fee, UsersUtil.getCoinName(community)));
                        binding.tvFee.setTag(fee);
                    }
                })
                .create();
        editFeeDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(editFeeDialog != null){
            editFeeDialog.closeDialog();
        }
    }
}