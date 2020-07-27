package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityChooseBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;

/**
 * 群组选择页面
 */
public class CommunityChooseActivity extends BaseActivity {

    private ActivityCommunityChooseBinding binding;
    private CommunityViewModel communityViewModel;
    private ChooseListAdapter adapter;
    private String chainID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community_choose);
        initParameter();
        initLayout();
        observeJoinedList();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getIntent() != null){
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
            if(StringUtil.isNotEmpty(chainID)){
                communityViewModel.getJoinedCommunityList(chainID);
            }
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        binding.toolbarInclude.toolbar.setTitle(R.string.community_choose_community);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new ChooseListAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.joinedList.setLayoutManager(layoutManager);
        binding.joinedList.setOnItemClickListener((view, adapterPosition) -> {
            // 选择社区退出返回数据
            Community community = adapter.getItemKey(adapterPosition);
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.BEAN, community);
            CommunityChooseActivity.this.setResult(RESULT_OK, intent);
            CommunityChooseActivity.this.finish();
        });
        binding.joinedList.setAdapter(adapter);
    }

    /**
     * 观察加入的社区列表
     */
    private void observeJoinedList() {
        communityViewModel.getJoinedList()
                .observe(this, communities -> {
                    if(adapter != null){
                        adapter.setCommunityList(communities);
                    }
                });
    }
}