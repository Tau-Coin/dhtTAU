package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityInfoBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 社区信息页面
 */
public class CommunityInfoActivity extends BaseActivity implements View.OnClickListener {
    private ActivityCommunityInfoBinding binding;

    private CommunityViewModel viewModel;
    private Community community;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_info);
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
        binding.toolbarInclude.toolbar.setTitle(R.string.menu_community_info);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        if(community != null){
            binding.tvCommunityName.setText(community.communityName);
//            if(StringUtil.isEmpty(community.intro)){
//                binding.tvCommunityIntro.setVisibility(View.GONE);
//            }
//            binding.tvCommunityIntro.setText(community.intro);

            // 用户不能把自己创建的社区拉入黑名单
//            if(StringUtil.isEquals(community.publicKey,
//                    MainApplication.getInstance().getPublicKey())){
//                binding.llBlacklist.setVisibility(View.GONE);
//            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.ll_mining_info:
                Intent intent = new Intent();
                intent.putExtra(IntentExtra.BEAN, community);
                ActivityUtil.startActivity(intent, this, MiningInfoActivity.class);
                break;
            case R.id.ll_settings:
                ActivityUtil.startActivity(this, CommunitySettingActivity.class);
                break;
            case R.id.ll_invite_friends:
                break;
            case R.id.ll_blacklist:
                viewModel.setCommunityBlacklist(community.chainID, true);
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.getSetBlacklistState().observe(this, isSuccess -> {
            if(isSuccess){
                this.setResult(RESULT_OK);
                this.finish();
            }
        });
    }
}