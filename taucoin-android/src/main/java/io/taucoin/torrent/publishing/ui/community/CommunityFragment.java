package io.taucoin.torrent.publishing.ui.community;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.PagerAdapter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.FmtMicrometer;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.FragmentCommunityBinding;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.CommunityTabFragment;
import io.taucoin.torrent.publishing.ui.chat.ChatsTabFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.main.MainActivity;
import io.taucoin.torrent.publishing.ui.transaction.TxsTabFragment;
import io.taucoin.types.TypesConfig;

/**
 * 单个群组页面
 */
public class CommunityFragment extends BaseFragment implements View.OnClickListener {

    public static final int REQUEST_CODE = 100;
    private MainActivity activity;
    private FragmentCommunityBinding binding;
    private MyAdapter fragmentAdapter;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private String chainID;
    private List<CommunityTabFragment> fragmentList = new ArrayList<>();
    private int[] titles = new int[]{R.string.community_instant_chat, R.string.community_chain_note,
            R.string.community_wired, R.string.community_queue};


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_community, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (MainActivity) getActivity();
        ViewModelProvider provider = new ViewModelProvider(this);
        communityViewModel = provider.get(CommunityViewModel.class);
        binding.setListener(this);
        binding.toolbarInclude.setListener(this);
        initParameter();
        initLayout();
    }

//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        if (0 != (Intent.FLAG_ACTIVITY_CLEAR_TOP & intent.getFlags())) {
//            finish();
//            String chainID = intent.getStringExtra(IntentExtra.CHAIN_ID);
//            intent = new Intent();
//            intent.putExtra(IntentExtra.CHAIN_ID, chainID);
//            ActivityUtil.startActivity(intent, this, CommunityFragment.class);
//        }
//    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            chainID = getArguments().getString(IntentExtra.ID);
        }
    }

    /**
     * 初始化布局
     */
    private void initLayout() {
        if(StringUtil.isNotEmpty(chainID)){
            String communityName = UsersUtil.getCommunityName(chainID);
            binding.toolbarInclude.tvTitle.setText(Html.fromHtml(communityName));
            binding.toolbarInclude.tvSubtitle.setText(getString(R.string.community_users_stats, 0, 0));
        }
        binding.toolbarInclude.ivBack.setOnClickListener(v -> activity.goBack());
        binding.toolbarInclude.tvSubtitle.setVisibility(View.VISIBLE);
        binding.toolbarInclude.ivAction.setVisibility(View.VISIBLE);
        binding.toolbarInclude.ivAction.setOnClickListener(v -> {
            if(StringUtil.isEmpty(chainID)){
                return;
            }
            Intent intent = new Intent();
            intent.putExtra(IntentExtra.CHAIN_ID, chainID);
            ActivityUtil.startActivityForResult(intent, activity,
                    MembersActivity.class, REQUEST_CODE);
        });
        // 初始化Tab页
        // 添加Chat页面
        CommunityTabFragment chatTab = new ChatsTabFragment();
        Bundle chatBundle = new Bundle();
        chatBundle.putString(IntentExtra.CHAIN_ID, chainID);
        chatTab.setArguments(chatBundle);
        fragmentList.add(chatTab);

        // 添加Chain Note页面
        CommunityTabFragment chainNoteTab = new TxsTabFragment();
        Bundle noteBundle = new Bundle();
        noteBundle.putString(IntentExtra.CHAIN_ID, chainID);
        noteBundle.putInt(IntentExtra.TYPE, TypesConfig.TxType.FNoteType.ordinal());
        chainNoteTab.setArguments(noteBundle);
        fragmentList.add(chainNoteTab);

        // 添加Wired页面
        CommunityTabFragment wiringTab = new TxsTabFragment();
        Bundle wiringBundle = new Bundle();
        wiringBundle.putString(IntentExtra.CHAIN_ID, chainID);
        wiringBundle.putInt(IntentExtra.TYPE, TypesConfig.TxType.WCoinsType.ordinal());
        wiringTab.setArguments(wiringBundle);
        fragmentList.add(wiringTab);

        // 添加Queue页面
        CommunityTabFragment queueTab = new TxsTabFragment();
        Bundle queueBundle = new Bundle();
        queueBundle.putString(IntentExtra.CHAIN_ID, chainID);
        queueTab.setArguments(queueBundle);
        fragmentList.add(queueTab);

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        fragmentAdapter = new MyAdapter(fragmentManager);
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
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onDestroyView() {
        try{
            // 清除子ViewPager中的Fragment,防止一直在内存里
            // 需要与FragmentStatePagerAdapter结合使用
            fragmentList.clear();
            fragmentAdapter.notifyDataSetChanged();
        }catch(Exception ignore) {
        } finally {
            super.onDestroyView();
        }
    }

    /**
     * 订阅社区相关的被观察者
     */
    private void subscribeCommunityViewModel() {
        communityViewModel.getSetBlacklistState().observe(this, state -> {
            if(state){
                activity.goBack();
            }
        });
        disposables.add(communityViewModel.getMembersStatistics(chainID)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(statistics ->
                    binding.toolbarInclude.tvSubtitle.setText(getString(R.string.community_users_stats,
                            statistics.getMembers(), statistics.getOnline())))
        );

        disposables.add(communityViewModel.observerCommunityByChainID(chainID)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(community -> {
                    long totalBlocks = community.totalBlocks + 1;
                    long syncBlocks = totalBlocks - community.syncBlock;
                    String communityState = getString(R.string.community_state,
                            FmtMicrometer.fmtLong(totalBlocks),
                            FmtMicrometer.fmtLong(syncBlocks));
                    binding.tvCommunityState.setText(communityState);
                }));

        disposables.add(communityViewModel.observerCurrentMember(chainID)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(member -> {
                    boolean isReadOnly = member.balance <= 0 && member.power <= 0;
                    for (int i = 0; i < fragmentList.size() - 1; i++) {
                        CommunityTabFragment fragment = fragmentList.get(i);
                        fragment.handleReadOnly(isReadOnly);
                    }
                }));

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_community_state_close) {
            binding.llCommunityState.setVisibility(View.GONE);
        }
    }

    public class MyAdapter extends FragmentStatePagerAdapter {
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

        @Override
        public int getItemPosition(@NonNull Object object) {
            return PagerAdapter.POSITION_NONE;
        }
    }
}
