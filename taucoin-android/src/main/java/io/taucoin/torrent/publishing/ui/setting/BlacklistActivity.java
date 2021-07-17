package io.taucoin.torrent.publishing.ui.setting;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.ViewGroup;
import com.yanzhenjie.recyclerview.SwipeMenuItem;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.User;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ActivityBlacklistBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.community.CommunityViewModel;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 设置页面
 */
public class BlacklistActivity extends BaseActivity {

    static final String TYPE_USERS = "users";
    static final String TYPE_COMMUNITIES = "communities";
    private ActivityBlacklistBinding binding;
    private BlackListAdapter adapter;
    private UserViewModel userViewModel;
    private CommunityViewModel communityViewModel;
    private String currentType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        userViewModel = provider.get(UserViewModel.class);
        communityViewModel = provider.get(CommunityViewModel.class);
        communityViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_blacklist);
        initParameter();
        initView();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getIntent() != null){
            currentType = getIntent().getStringExtra(IntentExtra.TYPE);
        }
    }

    /**
     * 初始化布局
     */
    private void initView() {
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        int titleRes = R.string.setting_blacklist_users;
        if(StringUtil.isEquals(currentType, TYPE_COMMUNITIES)){
            titleRes = R.string.setting_blacklist_communities;
            communityViewModel.getCommunitiesInBlacklist();
            communityViewModel.getBlackList().observe(this, list -> {
                adapter.setCommunityList(list);
            });
        }else {
            userViewModel.getUsersInBlacklist();
            userViewModel.getBlackList().observe(this, list -> {
                adapter.setUserList(list);
            });
        }
        binding.toolbarInclude.toolbar.setTitle(titleRes);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        adapter = new BlackListAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.blacklist.setLayoutManager(layoutManager);

        // 设置侧滑菜单
        binding.blacklist.setSwipeMenuCreator((swipeLeftMenu, swipeRightMenu, viewType) -> {
            SwipeMenuItem deleteItem = new SwipeMenuItem(BlacklistActivity.this)
                    .setBackground(R.drawable.grey_rect_left_round_bg_no_border)
                    .setText(R.string.setting_Unblock)
                    .setTextColor(BlacklistActivity.this.getResources().getColor(R.color.color_white))
                    .setHeight(ViewGroup.LayoutParams.MATCH_PARENT)//设置高，这里使用match_parent，就是与item的高相同
                    .setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);//设置宽
            swipeRightMenu.addMenuItem(deleteItem);//设置右边的侧滑
        });
        // 设置侧滑菜单的点击事件
        binding.blacklist.setOnItemMenuClickListener((menuBridge, adapterPosition) -> {
            menuBridge.closeMenu();

            Parcelable item = adapter.getItemKey(adapterPosition);
            if(item instanceof Community){
                communityViewModel.setCommunityBlacklist(((Community) item).chainID, false);
            }else if(item instanceof User){
                userViewModel.setUserBlacklist(((User) item).publicKey, false);
            }
            adapter.deleteItem(adapterPosition);
        });
        // 设置adapter
        binding.blacklist.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();

    }
}