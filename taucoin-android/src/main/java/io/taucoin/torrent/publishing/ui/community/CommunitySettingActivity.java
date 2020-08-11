package io.taucoin.torrent.publishing.ui.community;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.PlatformType;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunitySettingBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.PopUpDialog;

/**
 * 社区更多设置页面
 */
@Deprecated
public class CommunitySettingActivity extends BaseActivity implements View.OnClickListener {
    private ActivityCommunitySettingBinding binding;
    private PopUpDialog popUpDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_setting);
        binding.setListener(this);
        initLayout();
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.main_community_settings);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        String totalCoin = FmtMicrometer.fmtLong(Constants.TOTAL_COIN.longValue());
        binding.tvTotalCoins.setText(totalCoin);
        binding.tvTotalCoins.setTag(Constants.TOTAL_COIN.longValue());
        binding.tvAvgBlock.setText(getString(R.string.community_avg_block_time, Constants.BLOCK_IN_AVG / 60));
        binding.tvAvgBlock.setTag(Constants.BLOCK_IN_AVG);

        binding.tvContactPlatform.setText(PlatformType.Telegram.name());
        binding.tvContactPlatform.setTag(PlatformType.Telegram.getCode());
        binding.etContactId.setHint(getString(R.string.tx_contact_id, PlatformType.Telegram.name()));

        // 简介添加限制
        binding.tvIntroLimit.setText(getString(R.string.common_slant, 0, Constants.LENGTH_LIMIT));
        binding.etIntro.setFilters(new InputFilter[] { new InputFilter.LengthFilter(Constants.LENGTH_LIMIT) });
        binding.etIntro.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tvIntroLimit.setText(getString(R.string.common_slant, s.length(), Constants.LENGTH_LIMIT));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
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
        // 社区更多设置确认事件
        if (item.getItemId() == R.id.menu_done) {
            Community community = buildCommunity();
            // 返回上个页面的传递数据
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.BEAN, community);
            this.setResult(RESULT_OK, intent);
            this.finish();
        }
        return true;
    }

    @Override
    public boolean enterPictureInPictureMode(@NonNull PictureInPictureParams params) {
        return super.enterPictureInPictureMode(params);
    }

    /**
     * 根据用户输入数据构建Community实体类
     * @return Community
     */
    private Community buildCommunity() {
        String coinName = ViewUtils.getText(binding.etCoinName);
        long totalCoin = ViewUtils.getLongTag(binding.tvTotalCoins);
        int blockAvg = ViewUtils.getIntTag(binding.tvAvgBlock);
        String intro = ViewUtils.getText(binding.etIntro);
        String contact = ViewUtils.getText(binding.etContactId);
//        return new Community("", coinName, "", totalCoin, blockAvg, intro, contact);
        return null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.tv_contact_platform:
            case R.id.iv_contact_platform:
                showContactPlatformDialog();
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
                binding.etContactId.setHint(getString(R.string.tx_contact_id, type.name()));
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