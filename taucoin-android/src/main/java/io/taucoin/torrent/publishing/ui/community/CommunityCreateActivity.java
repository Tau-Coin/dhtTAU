package io.taucoin.torrent.publishing.ui.community;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.utils.EditTextInhibitInput;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.Utils;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityCreateBinding;
import io.taucoin.torrent.publishing.storage.entity.Community;
import io.taucoin.torrent.publishing.ui.BaseActivity;

/**
 * 群组/社区创建页面
 */
public class CommunityCreateActivity extends BaseActivity {
    private ActivityCommunityCreateBinding binding;

    private CommunityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(CommunityViewModel.class);
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

        String totalCoin = FmtMicrometer.fmtLong(Constants.TOTAL_COIN);
        binding.etTotalCoin.setText(getString(R.string.community_total_coin, totalCoin));
        binding.etTotalCoin.setTag(Constants.TOTAL_COIN);
        binding.etBlockAvg.setText(getString(R.string.community_block_avg, Constants.BLOCK_IN_AVG / 60));
        binding.etBlockAvg.setTag(Constants.BLOCK_IN_AVG);
        binding.tvPublicKey.setText(Constants.PUBLIC_KEY);
        // 社区名字禁止输入#特殊符号
        binding.etCommunityName.setFilters(new InputFilter[]{new EditTextInhibitInput(EditTextInhibitInput.WELL_REGEX)});
        binding.etCommunityName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String firstLetters = StringUtil.getFirstLettersOfName(s.toString());
                binding.roundButton.setText(firstLetters);
                int defaultColor = getResources().getColor(R.color.primary_light);
                int bgColor = StringUtil.isEmpty(firstLetters) ? defaultColor : Utils.getGroupColor(firstLetters);
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
            if(state){
                ToastUtils.showShortToast(R.string.community_added_successfully);
                onBackPressed();
            }
        });
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_community_create, menu);
        return true;
    }

    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 添加新社区处理事件
        if (item.getItemId() == R.id.menu_community_create) {
            Community community = viewModel.buildCommunity(binding);
            if(viewModel.validateCommunity(community)){
                viewModel.addCommunity(community);
            }
        }
        return true;
    }
}