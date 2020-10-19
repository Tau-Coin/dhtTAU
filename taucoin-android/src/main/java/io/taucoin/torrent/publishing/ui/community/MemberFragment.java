package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.ChainLinkUtil;
import io.taucoin.torrent.publishing.databinding.FragmentMemberBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;

/**
 * 交易Tab页
 */
public class MemberFragment extends BaseFragment implements MemberListAdapter.ClickListener {

    private static final Logger logger = LoggerFactory.getLogger("MemberFragment");
    private BaseActivity activity;
    private FragmentMemberBinding binding;
    private CommunityViewModel communityViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MemberListAdapter adapter;

    private String chainID;
    private boolean onChain;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_member, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = (BaseActivity) getActivity();
        assert activity != null;
        ViewModelProvider provider = new ViewModelProvider(activity);
        communityViewModel = provider.get(CommunityViewModel.class);
        initParameter();
        initView();
    }

    /**
     * 初始化参数
     */
    private void initParameter() {
        if(getArguments() != null){
            chainID = getArguments().getString(IntentExtra.CHAIN_ID);
            onChain = getArguments().getBoolean(IntentExtra.ON_CHAIN, true);
        }
    }

    /**
     * 初始化视图
     */
    private void initView() {
        adapter = new MemberListAdapter(this);
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.recyclerView.setLayoutManager(layoutManager);

        binding.recyclerView.setItemAnimator(animator);
        binding.recyclerView.setAdapter(adapter);

        communityViewModel.observerCommunityMembers(chainID, onChain).observe(activity, members -> {
            adapter.submitList(members);
            logger.debug("adapter.size::{}, newSize::{}", adapter.getItemCount(), members.size());
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof BaseActivity)
            activity = (BaseActivity)context;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        disposables.clear();
    }

    @Override
    public void onItemClicked(MemberAndUser member) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, member.publicKey);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

    @Override
    public void onShareClicked(MemberAndUser member) {
        showShareDialog();
    }

    /**
     * 显示联系平台的对话框
     */
    private void showShareDialog() {
        // 获取10个社区成员的公钥
        disposables.add(communityViewModel.getCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                    String communityInviteLink = ChainLinkUtil.encode(chainID, list);
                    ActivityUtil.shareText(activity, getString(R.string.contacts_share_link_via),
                            communityInviteLink);
                }));
    }
}
