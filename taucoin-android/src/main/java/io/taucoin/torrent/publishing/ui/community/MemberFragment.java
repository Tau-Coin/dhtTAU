package io.taucoin.torrent.publishing.ui.community;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import io.taucoin.torrent.publishing.MainApplication;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.Constants;
import io.taucoin.torrent.publishing.core.model.data.MemberAndUser;
import io.taucoin.torrent.publishing.core.utils.ActivityUtil;
import io.taucoin.torrent.publishing.core.utils.CopyManager;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.core.utils.ToastUtils;
import io.taucoin.torrent.publishing.core.utils.UsersUtil;
import io.taucoin.torrent.publishing.databinding.FragmentMemberBinding;
import io.taucoin.torrent.publishing.ui.BaseActivity;
import io.taucoin.torrent.publishing.ui.BaseFragment;
import io.taucoin.torrent.publishing.ui.constant.IntentExtra;
import io.taucoin.torrent.publishing.ui.constant.Page;
import io.taucoin.torrent.publishing.ui.customviews.ShareDialog;
import io.taucoin.torrent.publishing.ui.user.UserDetailActivity;
import io.taucoin.torrent.publishing.ui.user.UserViewModel;

/**
 * 交易Tab页
 */
public class MemberFragment extends BaseFragment implements MemberListAdapter.ClickListener {

    private static final Logger logger = LoggerFactory.getLogger("MemberFragment");
    private BaseActivity activity;
    private FragmentMemberBinding binding;
    private CommunityViewModel communityViewModel;
    private UserViewModel userViewModel;
    private CompositeDisposable disposables = new CompositeDisposable();
    private MemberListAdapter adapter;
    private ShareDialog shareDialog;

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
        userViewModel = provider.get(UserViewModel.class);
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

        PagedList.Config pagedListConfig = new PagedList.Config.Builder()
                .setEnablePlaceholders(Page.ENABLE_PLACEHOLDERS)
                .setPageSize(Page.PAGE_SIZE)
                .setInitialLoadSizeHint(Page.PAGE_SIZE)
                .build();
        LiveData<PagedList<MemberAndUser>> postList = new LivePagedListBuilder<>(
                communityViewModel.queryCommunityMembers(chainID, onChain), pagedListConfig).build();
        postList.observe(activity, members -> {
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
    public void onDestroy() {
        super.onDestroy();
        if (shareDialog != null) {
            shareDialog.closeDialog();
        }
    }

    @Override
    public void onItemClicked(MemberAndUser member) {
        Intent intent = new Intent();
        intent.putExtra(IntentExtra.PUBLIC_KEY, member.publicKey);
        ActivityUtil.startActivity(intent, this, UserDetailActivity.class);
    }

    @Override
    public void onShareClicked(MemberAndUser member) {
        String currentUserPk = MainApplication.getInstance().getPublicKey();
        if(StringUtil.isEquals(currentUserPk, member.publicKey)){
            showShareDialog(member, false);
        }else{
            showProgressDialog();
            disposables.add(communityViewModel.getCommunityNumInCommon(currentUserPk, member.publicKey)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                        closeProgressDialog();
                        showShareDialog(member, list.size() > 0);
                    }));
        }
    }

    /**
     * 显示联系平台的对话框
     */
    private void showShareDialog(MemberAndUser member, boolean isShareTau) {
        ShareDialog.Builder builder = new ShareDialog.Builder(activity);
        builder.setOnItemClickListener((dialog, imgRid, titleRid) -> {
            dialog.dismiss();
            // 获取10个社区成员的公钥
            disposables.add(communityViewModel.getCommunityMembersLimit(chainID, Constants.CHAIN_LINK_BS_LIMIT)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread()).subscribe(list -> {
                        String communityInviteLink = UsersUtil.getCommunityInviteLink(chainID, list);
                        if (imgRid == R.mipmap.icon_share_copy_link) {
                            CopyManager.copyText(communityInviteLink);
                            ToastUtils.showShortToast(R.string.copy_share_link);
                        } else if (imgRid == R.mipmap.ic_launcher_round) {
                            userViewModel.shareInvitedLinkToFriend(communityInviteLink, member.publicKey);
                            ToastUtils.showShortToast(R.string.share_link_successfully);
                        } else if (imgRid == R.mipmap.icon_share_sms) {
                            doSendSMSTo(communityInviteLink);
                        }
                    }));
        });
        builder.addItems(R.mipmap.icon_share_copy_link, R.string.contacts_copy_link);
        if(isShareTau){
            builder.addItems(R.mipmap.ic_launcher_round, R.string.contacts_community);
        }
        builder.addItems(R.mipmap.icon_share_sms, R.string.contacts_sms);
        shareDialog = builder.create();
        shareDialog.show();
    }

    /**
     * 调起系统功能发短信
     *
     * @param message 消息内容
     */
    private void doSendSMSTo(String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
        intent.putExtra("sms_body", message);
        startActivity(intent);
    }
}
