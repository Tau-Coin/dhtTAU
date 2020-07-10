package io.taucoin.torrent.publishing.ui.community;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.storage.entity.Community;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.ViewUtils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunitySettingBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 社区更多设置页面
 */
public class CommunitySettingActivity extends BaseActivity {
    private ActivityCommunitySettingBinding binding;

    private CommunityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_setting);
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

        String totalCoin = FmtMicrometer.fmtLong(Constants.TOTAL_COIN);
        binding.etTotalCoin.setText(getString(R.string.community_total_coin, totalCoin));
        binding.etTotalCoin.setTag(Constants.TOTAL_COIN);
        binding.etBlockAvg.setText(getString(R.string.community_block_avg, Constants.BLOCK_IN_AVG / 60));
        binding.etBlockAvg.setTag(Constants.BLOCK_IN_AVG);

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
            boolean isAnnounce = binding.cbAnnounce.isChecked();
            // 返回上个页面的传递数据
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.BEAN, community);
            intent.putExtra(IntentExtra.ANNOUNCE, isAnnounce);
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
        long totalCoin = ViewUtils.getLongTag(binding.etTotalCoin);
        int blockAvg = ViewUtils.getIntTag(binding.etBlockAvg);
        String intro = ViewUtils.getText(binding.etIntro);
        String contact = ViewUtils.getText(binding.etContact);
        return new Community("", coinName, "", totalCoin, blockAvg, intro, contact);
    }
}