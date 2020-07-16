package io.taucoin.torrent.publishing.ui.transaction;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


import com.google.gson.Gson;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.model.data.NameDescription;
import io.taucoin.torrent.publishing.core.storage.entity.Community;
import io.taucoin.torrent.publishing.core.storage.entity.Tx;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.PlatformType;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityNicknameBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.PopUpDialog;
import io.taucoin.types.MsgType;

/**
 * 修改群组个人昵称页面
 */
public class NicknameActivity extends BaseActivity implements View.OnClickListener {
    private ActivityNicknameBinding binding;

    private TxViewModel txViewModel;
    private PopUpDialog popUpDialog;
    private Community community;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        txViewModel = provider.get(TxViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_nickname);
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
        binding.toolbarInclude.toolbar.setTitle(R.string.community_nickname);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        binding.tvContactPlatform.setText(PlatformType.Nothing.name());
        binding.tvContactPlatform.setTag(PlatformType.Nothing.getCode());

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
        String name = ViewUtils.getText(binding.etName);
        NameDescription nameDescription = new NameDescription();
        nameDescription.setContactID(ViewUtils.getText(binding.etContactId));
        nameDescription.setContactPlatform(ViewUtils.getIntTag(binding.tvContactPlatform));
        nameDescription.setPersonalProfile(ViewUtils.getText(binding.etPersonalProfile));
        if(nameDescription.getContactPlatform() != PlatformType.Nothing.getCode()
                && StringUtil.isEmpty(nameDescription.getContactID())){
            ToastUtils.showShortToast(R.string.tx_error_invalid_contact);
            return null;
        }
        String memo = new Gson().toJson(nameDescription);
        return new Tx(chainID, name, FmtMicrometer.fmtTxLongValue(fee), txType, memo);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_contact_platform:
            case R.id.iv_contact_platform:
                showContactPlatformDialog();
                break;
            case R.id.tv_fee:
                txViewModel.showEditFeeDialog(this, binding.tvFee, community);
                break;
        }
    }

    /**
     * 显示联系平台的对话框
     */
    private void showContactPlatformDialog() {
        PopUpDialog.Builder builder = new PopUpDialog.Builder(this);
        builder.setOnItemClickListener((dialog, name, code) -> {
            dialog.dismiss();
            PlatformType type = PlatformType.getPlatformType(code);
            if(type != null){
                binding.tvContactPlatform.setText(type.name());
                binding.tvContactPlatform.setTag(type.getCode());
            }
        });
        for (PlatformType type : PlatformType.values()) {
            builder.addItems(type.name(), type.getCode());
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