package io.taucoin.torrent.publishing.ui.community;

import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.naturs.logger.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.disposables.CompositeDisposable;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityBinding;
import io.taucoin.torrent.publishing.core.storage.entity.Community;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.chat.ChatsTabFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.transaction.TxsTabFragment;

/**
 * 单个群组页面
 */
public class CommunityActivity extends BaseActivity implements View.OnClickListener {
    private ActivityCommunityBinding binding;

    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Community community;
    private List<Fragment> fragmentList = new ArrayList<>();
    private int[] titles = new int[]{R.string.community_instant_chat, R.string.community_chain_note};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community);
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
        if(community != null){
            String communityName = community.communityName;
            binding.toolbarInclude.tvGroupName.setText(Html.fromHtml(communityName));
            binding.toolbarInclude.tvUsersStats.setText(getString(R.string.community_users_stats, 0, 0));
        }
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);

        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // 初始化Tab页
        // 添加Chain Note页面
        Fragment chainNoteTab = new ChatsTabFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(IntentExtra.BEAN, community);
        chainNoteTab.setArguments(bundle);
        fragmentList.add(chainNoteTab);
        // 添加Instant Chat页面
        Fragment instantChatTab = new TxsTabFragment();
        instantChatTab.setArguments(bundle);
        fragmentList.add(instantChatTab);
        FragmentManager fragmentManager = getSupportFragmentManager();
        MyAdapter fragmentAdapter = new MyAdapter(fragmentManager);
        binding.viewPager.setAdapter(fragmentAdapter);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
        binding.viewPager.setOffscreenPageLimit(fragmentList.size());
    }

    @Override
    public void onStart() {
        super.onStart();
        subscribeCommunityViewModel();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
    }

    /**
     * 订阅社区相关的被观察者
     */
    private void subscribeCommunityViewModel() {
        communityViewModel.getSetBlacklistState().observe(this, state -> {
            if(state){
                onBackPressed();
            }
        });
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_community, menu);
        return true;
    }

    /**
     *  invalidateOptionsMenu执行后重新控制menu的显示
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        String publicKey = MainApplication.getInstance().getPublicKey();
        boolean isCreator = community != null && StringUtil.equals(community.publicKey, publicKey);
        Logger.d("publicKey=%s", community != null ? community.publicKey:"");
        Logger.d("publicKey=%s", publicKey);
        menu.findItem(R.id.menu_blacklist).setVisible(!isCreator);
        menu.findItem(R.id.menu_settings).setVisible(isCreator);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     *  重构onMenuOpened方法，通过反射显示icon
     */
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                } catch (Exception ignore) {
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }
    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if( null == community){
            return false;
        }
        switch (item.getItemId()) {
            case R.id.menu_settings:
                ActivityUtil.startActivity(this, CommunitySettingActivity.class);
                break;
            case R.id.menu_blacklist:
                communityViewModel.setCommunityBlacklist(community.chainID, true);
                break;
            case R.id.menu_invite_friends:
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.iv_mining_income_close:
                binding.llTodayIncomeTips.setVisibility(View.GONE);
                break;
        }
    }

    public class MyAdapter extends FragmentPagerAdapter {
        MyAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getText(titles[position]);
        }
    }
}