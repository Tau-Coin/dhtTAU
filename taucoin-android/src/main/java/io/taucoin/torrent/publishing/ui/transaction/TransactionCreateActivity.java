package io.taucoin.torrent.publishing.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.ui.friends.FriendsActivity;
import io.taucoin.types.TypesConfig;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityTransactionCreateBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 交易创建页面页面
 */
public class TransactionCreateActivity extends BaseActivity implements View.OnClickListener {

    private static final int REQUEST_CODE = 0x01;
    private ActivityTransactionCreateBinding binding;

    private TxViewModel txViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private String chainID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        txViewModel.observeNeedStartDaemon();
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
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
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

        if(StringUtil.isNotEmpty(chainID)){
            String fee = "0";
            binding.tvFee.setTag(R.id.median_fee, fee);
            String lastTxFee = txViewModel.getLastTxFee(chainID);
            if(StringUtil.isNotEmpty(lastTxFee)){
                fee = FmtMicrometer.fmtFeeValue(lastTxFee);
            }
            showFeeView(binding.tvFee, fee);
        }
    }

    private void showFeeView(TextView tvFee, String fee) {
        tvFee.setText(getString(R.string.tx_median_fee, fee, UsersUtil.getCoinName(chainID)));
        tvFee.setTag(fee);
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
        if(StringUtil.isNotEmpty(chainID)){
            disposables.add(txViewModel.observeMedianFee(chainID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(fees -> {
                        String medianFee = FmtMicrometer.fmtFeeValue(Utils.getMedianData(fees));
                        binding.tvFee.setTag(R.id.median_fee, medianFee);
                        if(StringUtil.isEmpty(txViewModel.getLastTxFee(chainID))){
                            showFeeView(binding.tvFee, medianFee);
                        }
                    }));
        }
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
        long txType = TypesConfig.TxType.WCoinsType.ordinal();
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
                txViewModel.showEditFeeDialog(this, binding.tvFee, chainID);
                break;
            case R.id.iv_select_pk:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.TYPE, FriendsActivity.PAGE_SELECT_CONTACT);
                ActivityUtil.startActivityForResult(intent, this, FriendsActivity.class, REQUEST_CODE);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK){
            if(data != null){
                String publicKey = data.getStringExtra(IntentExtra.PUBLIC_KEY);
                if(StringUtil.isNotEmpty(publicKey)){
                    binding.etPublicKey.setText(publicKey);
                }
            }
        }
    }
}
