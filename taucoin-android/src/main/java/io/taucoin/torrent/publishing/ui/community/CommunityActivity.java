package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

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
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.databinding.ActivityCommunityBinding;
import io.taucoin.torrent.publishing.core.storage.sqlite.entity.Community;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.chat.ChatsTabFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.search.SearchActivity;
import io.taucoin.torrent.publishing.ui.transaction.TxsTabFragment;
import io.taucoin.types.MsgType;

/**
 * 单个群组页面
 */
public class CommunityActivity extends BaseActivity implements View.OnClickListener {

    private static final int REQUEST_CODE = 100;
    private ActivityCommunityBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Community community;
    private List<Fragment> fragmentList = new ArrayList<>();
    private int[] titles = new int[]{R.string.community_instant_chat, R.string.community_chain_note,
            R.string.community_wired, R.string.community_queue};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_community);
        binding.setListener(this);
        binding.toolbarInclude.setListener(this);
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

        // 添加Chat页面
        Fragment chatTab = new ChatsTabFragment();
        Bundle chatBundle = new Bundle();
        chatBundle.putParcelable(IntentExtra.BEAN, community);
        chatTab.setArguments(chatBundle);
        fragmentList.add(chatTab);

        // 添加Chain Note页面
        Fragment chainNoteTab = new TxsTabFragment();
        Bundle noteBundle = new Bundle();
        noteBundle.putParcelable(IntentExtra.BEAN, community);
        noteBundle.putInt(IntentExtra.TYPE, MsgType.RegularForum.getVaLue());
        chainNoteTab.setArguments(noteBundle);
        fragmentList.add(chainNoteTab);

        // 添加Wired页面
        Fragment wiringTab = new TxsTabFragment();
        Bundle wiringBundle = new Bundle();
        wiringBundle.putParcelable(IntentExtra.BEAN, community);
        wiringBundle.putInt(IntentExtra.TYPE, MsgType.Wiring.getVaLue());
        wiringTab.setArguments(wiringBundle);
        fragmentList.add(wiringTab);

        // 添加Queue页面
        Fragment queueTab = new TxsTabFragment();
        Bundle queueBundle = new Bundle();
        queueBundle.putParcelable(IntentExtra.BEAN, community);
        queueTab.setArguments(queueBundle);
        fragmentList.add(queueTab);

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
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if( null == community){
            return false;
        }
        if (item.getItemId() == R.id.community_search) {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.BEAN, community);
            ActivityUtil.startActivity(intent, this, SearchActivity.class);
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_mining_income_close) {
            binding.llTodayIncomeTips.setVisibility(View.GONE);
        }else if (v.getId() == R.id.toolbar_title) {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.BEAN, community);
            ActivityUtil.startActivityForResult(intent, this, MembersActivity.class, REQUEST_CODE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK){
            onBackPressed();
        }
    }
}