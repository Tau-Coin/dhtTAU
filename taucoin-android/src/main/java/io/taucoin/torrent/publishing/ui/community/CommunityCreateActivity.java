package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import java.nio.charset.StandardCharsets;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.genesis.GenesisConfig;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.EditTextInhibitInput;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityCreateBinding;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.databinding.ViewDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.friends.FriendsActivity;

/**
 * 群组/社区创建页面
 */
public class CommunityCreateActivity extends BaseActivity {
    private ActivityCommunityCreateBinding binding;
    private CommunityViewModel viewModel;
    private CommonDialog successDialog;
    private GenesisConfig cf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(CommunityViewModel.class);
        viewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_create);
        initLayout();
        observeAddCommunityState();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.main_new_community);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        binding.tvPublicKey.setText(MainApplication.getInstance().getPublicKey());
        // 社区名字禁止输入#特殊符号
        binding.etCommunityName.setFilters(new InputFilter[]{new EditTextInhibitInput(EditTextInhibitInput.WELL_REGEX)});
        binding.etCommunityName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String communityName = s.toString();
                String chainID;
                if (StringUtil.isEmpty(communityName)) {
                    cf = null;
                    chainID = null;
                } else {
                    Community community = buildCommunity();
                    cf = viewModel.createGenesisConfig(community);
                    chainID = new String(cf.getChainID(), StandardCharsets.UTF_8);
                }

                String firstLetters = StringUtil.getFirstLettersOfName(s.toString());
                binding.roundButton.setText(firstLetters);
                int defaultColor = getResources().getColor(R.color.primary_light);
                int bgColor = StringUtil.isEmpty(chainID) ? defaultColor : Utils.getGroupColor(chainID);
                binding.roundButton.setBgColor(bgColor);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * 观察添加社区的状态
     */
    private void observeAddCommunityState() {
        viewModel.getAddCommunityState().observe(this, state -> {
            if(state.isSuccess()){
                showSuccessDialog(state.getMsg());
            }else{
                ToastUtils.showShortToast(state.getMsg());
            }
        });
    }

    /**
     * 显示添加新社区成功后的对话框
     */
    private void showSuccessDialog(String chainID) {
        ViewDialogBinding binding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.view_dialog, null, false);
        binding.tvMsg.setText(R.string.community_added_successfully);
        binding.tvMsg.setTextColor(getResources().getColor(R.color.color_black));
        successDialog = new CommonDialog.Builder(this)
                .setContentView(binding.getRoot())
                .setButtonWidth(R.dimen.widget_size_240)
                .setPositiveButton(R.string.community_added_members, (dialog, which) -> {
                    dialog.cancel();
                    onBackPressed();
                    Intent intent = new Intent();
                    intent.putExtra(IntentExtra.TYPE, FriendsActivity.PAGE_ADD_MEMBERS);
                    intent.putExtra(IntentExtra.CHAIN_ID, chainID);
                    ActivityUtil.startActivity(intent, this, FriendsActivity.class);
                }).create();
        successDialog.setOnCancelListener(dialog -> {
            dialog.dismiss();
            onBackPressed();
        });
        successDialog.show();

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
        // 添加新社区处理事件
        if (item.getItemId() == R.id.menu_done) {
            Community community = buildCommunity();
            if(viewModel.validateCommunity(community)){
                viewModel.addCommunity(community, cf);
            }
        }
        return true;
    }

    private Community buildCommunity(){
        String communityName = ViewUtils.getText(binding.etCommunityName);
        String publicKey = ViewUtils.getText(binding.tvPublicKey);
        return new Community(communityName, publicKey,
                Constants.TOTAL_COIN.longValue(), Constants.BLOCK_IN_AVG);
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(successDialog != null){
            successDialog.closeDialog();
        }
    }
}