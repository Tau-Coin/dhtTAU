package io.taucoin.torrent.publishing.ui.community;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.ActivityMembersBinding;
import io.taucoin.torrent.publishing.databinding.ViewDialogBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.customviews.CommonDialog;
import io.taucoin.torrent.publishing.ui.friends.FriendsActivity;
import io.taucoin.torrent.publishing.ui.qrcode.CommunityQRCodeActivity;

/**
 * 群组成员页面
 */
public class MembersActivity extends BaseActivity {

    private ActivityMembersBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private String chainID;
    private List<Fragment> fragmentList = new ArrayList<>();
    private int[] titles = new int[]{R.string.community_on_chain, R.string.community_queue};
    private CommonDialog banDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        communityViewModel.observeNeedStartDaemon();
        binding = DataBindingUtil.setContentView(this, R.layout.activity_members);
        initParameter();
        initLayout();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getIntent() != null){
            chainID = getIntent().getStringExtra(IntentExtra.CHAIN_ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        if(StringUtil.isNotEmpty(chainID)){
            String communityName = UsersUtil.getCommunityName(chainID);
            binding.toolbarInclude.tvGroupName.setText(Html.fromHtml(communityName));
            binding.toolbarInclude.tvUsersStats.setText(getString(R.string.community_users_stats, 0, 0));
        }
        binding.toolbarInclude.toolbar.setNavigationIcon(R.mipmap.icon_back);
        setSupportActionBar(binding.toolbarInclude.toolbar);
        binding.toolbarInclude.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // 添加OnChain页面
        Fragment chainTab = new MemberFragment();
        Bundle chainBundle = new Bundle();
        chainBundle.putString(IntentExtra.CHAIN_ID, chainID);
        chainBundle.putBoolean(IntentExtra.ON_CHAIN, true);
        chainTab.setArguments(chainBundle);
        fragmentList.add(chainTab);

        // 添加Queue页面
        Fragment queueTab = new MemberFragment();
        Bundle queueBundle = new Bundle();
        queueBundle.putString(IntentExtra.CHAIN_ID, chainID);
        queueBundle.putBoolean(IntentExtra.ON_CHAIN, false);
        queueTab.setArguments(queueBundle);
        fragmentList.add(queueTab);

        FragmentManager fragmentManager = getSupportFragmentManager();
        MyAdapter fragmentAdapter = new MyAdapter(fragmentManager);
        binding.viewPager.setAdapter(fragmentAdapter);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
        binding.viewPager.setOffscreenPageLimit(fragmentList.size());
    }

    /**
     *  创建右上角Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_member, menu);
        return true;
    }
    /**
     * 右上角Menu选项选择事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(StringUtil.isEmpty(chainID)){
            return false;
        }
        if (item.getItemId() == R.id.member_add) {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.TYPE, FriendsActivity.PAGE_ADD_MEMBERS);
            intent.putExtra(IntentExtra.CHAIN_ID, chainID);
            ActivityUtil.startActivity(intent, this, FriendsActivity.class);
        } else if (item.getItemId() == R.id.member_ban) {
            showBanCommunityTipsDialog(chainID);
        } else if (item.getItemId() == R.id.community_qr_code) {
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, chainID);
            ActivityUtil.startActivity(intent, this, CommunityQRCodeActivity.class);
        }
        return true;
    }

    /**
     * 显示禁止社区的提示对话框
     * @param chainID
     */
    private void showBanCommunityTipsDialog(String chainID) {
        ViewDialogBinding dialogBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.view_dialog, null, false);
        dialogBinding.tvMsg.setText(R.string.community_ban_community);
        dialogBinding.tvMsg.setTextColor(getResources().getColor(R.color.color_black));
        banDialog = new CommonDialog.Builder(this)
                .setContentView(dialogBinding.getRoot())
                .setCanceledOnTouchOutside(false)
                .setHorizontal()
                .setPositiveButton(R.string.common_yes, (dialog, which) -> {
                    dialog.dismiss();
                    communityViewModel.setCommunityBlacklist(chainID, true);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        banDialog.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        communityViewModel.getSetBlacklistState().observe(this, isSuccess -> {
            if(isSuccess){
                this.setResult(RESULT_OK);
                this.finish();
            }
        });
        disposables.add(communityViewModel.getMembersStatistics(chainID)
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(statistics ->
                binding.toolbarInclude.tvUsersStats.setText(getString(R.string.community_users_stats,
                        statistics.getMembers(), statistics.getOnline()))));
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.clear();
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